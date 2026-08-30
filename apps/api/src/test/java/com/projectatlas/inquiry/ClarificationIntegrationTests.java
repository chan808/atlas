package com.projectatlas.inquiry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.projectatlas.TestcontainersConfiguration;
import com.projectatlas.inquiry.application.ClarificationConflictException;
import com.projectatlas.inquiry.application.ClarificationProposalPort;
import com.projectatlas.inquiry.application.ClarificationService;
import com.projectatlas.inquiry.application.ClarificationStore;
import com.projectatlas.inquiry.application.InquiryService;
import com.projectatlas.inquiry.domain.ClarificationProposal;
import com.projectatlas.inquiry.domain.ClarificationTurn;
import com.projectatlas.inquiry.domain.Inquiry;
import com.projectatlas.inquiry.domain.InquiryStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ClarificationIntegrationTests {

	private static final ClarificationProposal VALID_PROPOSAL = new ClarificationProposal(
			"먼저 원리를 설명하고 싶은가요, 작은 예제로 확인하고 싶은가요?",
			"답에 따라 다음 질문의 방향과 필요한 증거가 달라집니다.",
			"deterministic-fake-v1",
			"clarification-question-v1");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private InquiryService inquiryService;

	@Autowired
	private ClarificationService clarificationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private ClarificationProposalPort proposalPort;

	@MockitoSpyBean
	private ClarificationStore clarificationStore;

	private ExecutorService executor;

	@BeforeEach
	void prepare() {
		reset(proposalPort, clarificationStore);
		when(proposalPort.propose(any())).thenReturn(VALID_PROPOSAL);
		jdbcTemplate.update("DELETE FROM clarification_turns");
		jdbcTemplate.update("DELETE FROM clarification_start_requests");
		jdbcTemplate.update("DELETE FROM inquiries");
	}

	@AfterEach
	void stopExecutor() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	@Test
	void startsAndRetrievesOnePersistedTurnWithoutChangingTheRawText() throws Exception {
		String rawText = "  timeout과 retry\r\n한글\t😀e\u0301  ";
		Inquiry captured = inquiryService.create(rawText, "capture-before-clarification");

		MvcResult started = start(captured.id(), 0, "start-round-trip")
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();
		JsonNode body = objectMapper.readTree(started.getResponse().getContentAsString());
		String turnId = body.required("id").stringValue();

		assertEquals(captured.id().toString(), body.required("inquiryId").stringValue());
		assertEquals(1, body.required("sequence").intValue());
		assertEquals(1, body.required("inquiryVersion").longValue());
		assertEquals(VALID_PROPOSAL.question(), body.required("question").stringValue());
		assertEquals(VALID_PROPOSAL.reason(), body.required("reason").stringValue());
		assertEquals(
				"/api/inquiries/" + captured.id() + "/clarification-turns/" + turnId,
				started.getResponse().getHeader("Location"));

		MvcResult stable = mockMvc.perform(get(
				"/api/inquiries/{inquiryId}/clarification-turns/{turnId}",
				captured.id(),
				turnId))
				.andExpect(status().isOk())
				.andReturn();
		MvcResult current = mockMvc.perform(get(
				"/api/inquiries/{inquiryId}/clarification-turns/current",
				captured.id()))
				.andExpect(status().isOk())
				.andReturn();
		assertEquals(started.getResponse().getContentAsString(), stable.getResponse().getContentAsString());
		assertEquals(started.getResponse().getContentAsString(), current.getResponse().getContentAsString());

		Inquiry transitioned = inquiryService.get(captured.id());
		assertEquals(InquiryStatus.CLARIFYING, transitioned.status());
		assertEquals(1, transitioned.version());
		assertEquals(rawText, transitioned.brainDump().value());
		assertEquals(captured.createdAt(), transitioned.createdAt());
		assertEquals("deterministic-fake-v1", jdbcTemplate.queryForObject(
				"SELECT proposal_source FROM clarification_turns WHERE id = ?",
				String.class,
				UUID.fromString(turnId)));
		assertEquals(1, turnCount());
		assertEquals(1, startRequestCount());
	}

	@Test
	void sequentialReplayReturnsTheOriginalPersistedRepresentation() throws Exception {
		Inquiry captured = inquiryService.create("retry를 이해하고 싶다", "capture-sequential-start");

		MvcResult first = start(captured.id(), 0, "sequential-start")
				.andExpect(status().isCreated())
				.andReturn();
		MvcResult replay = start(captured.id(), 0, "sequential-start")
				.andExpect(status().isCreated())
				.andReturn();

		assertEquals(first.getResponse().getHeader("Location"), replay.getResponse().getHeader("Location"));
		assertEquals(first.getResponse().getContentAsString(), replay.getResponse().getContentAsString());
		assertEquals(1, turnCount());
		assertEquals(1, startRequestCount());
	}

	@Test
	void concurrentSameKeyRequestsConvergeOnOneTurn() throws Exception {
		Inquiry captured = inquiryService.create("동시 retry가 궁금하다", "capture-concurrent-same-key");
		int requestCount = 8;
		executor = Executors.newFixedThreadPool(requestCount);
		CountDownLatch ready = new CountDownLatch(requestCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<ClarificationTurn>> futures = new ArrayList<>();

		for (int index = 0; index < requestCount; index++) {
			futures.add(executor.submit(() -> {
				ready.countDown();
				assertTrue(start.await(10, TimeUnit.SECONDS));
				return clarificationService.start(captured.id(), 0L, "concurrent-same-key");
			}));
		}
		assertTrue(ready.await(10, TimeUnit.SECONDS));
		start.countDown();

		Set<ClarificationTurn> turns = new HashSet<>();
		for (Future<ClarificationTurn> future : futures) {
			turns.add(future.get(20, TimeUnit.SECONDS));
		}

		assertEquals(1, turns.size());
		assertEquals(1, turnCount());
		assertEquals(1, startRequestCount());
	}

	@Test
	void differentKeysRaceSoOneWinsAndTheOtherConflicts() throws Exception {
		Inquiry captured = inquiryService.create("경쟁 요청을 시험한다", "capture-concurrent-different-key");
		executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Object>> futures = new ArrayList<>();

		for (String key : List.of("different-key-one", "different-key-two")) {
			futures.add(executor.submit(() -> {
				ready.countDown();
				assertTrue(start.await(10, TimeUnit.SECONDS));
				try {
					return clarificationService.start(captured.id(), 0L, key);
				}
				catch (RuntimeException exception) {
					return exception;
				}
			}));
		}
		assertTrue(ready.await(10, TimeUnit.SECONDS));
		start.countDown();

		List<Object> outcomes = new ArrayList<>();
		for (Future<Object> future : futures) {
			outcomes.add(future.get(20, TimeUnit.SECONDS));
		}

		assertEquals(1, outcomes.stream().filter(ClarificationTurn.class::isInstance).count());
		assertEquals(1, outcomes.stream().filter(ClarificationConflictException.class::isInstance).count());
		assertEquals(1, turnCount());
		assertEquals(1, startRequestCount());
		assertEquals(1, inquiryService.get(captured.id()).version());
	}

	@Test
	void staleWrongStateAndReusedKeyConflictWithoutMutation(CapturedOutput output) throws Exception {
		String privateKey = "private-start-key";
		String privateRawText = "private raw curiosity";
		Inquiry first = inquiryService.create(privateRawText, "capture-private-first");
		Inquiry second = inquiryService.create("another curiosity", "capture-private-second");

		start(first.id(), 1, "stale-version")
				.andExpect(status().isConflict());
		start(first.id(), 0, privateKey)
				.andExpect(status().isCreated());
		MvcResult repeatedState = start(first.id(), 1, "different-after-start")
				.andExpect(status().isConflict())
				.andReturn();
		MvcResult reusedKey = start(second.id(), 0, privateKey)
				.andExpect(status().isConflict())
				.andReturn();

		String problems = repeatedState.getResponse().getContentAsString()
				+ reusedKey.getResponse().getContentAsString();
		assertFalse(problems.contains(privateKey));
		assertFalse(problems.contains(privateRawText));
		assertFalse(output.getAll().contains(privateKey));
		assertFalse(output.getAll().contains(privateRawText));
		assertEquals(1, turnCount());
		assertEquals(InquiryStatus.CAPTURED, inquiryService.get(second.id()).status());
	}

	@Test
	void invalidProposalDoesNotConsumeTheKeyAndAValidRetrySucceeds(CapturedOutput output)
			throws Exception {
		String privateRawText = "private invalid proposal input";
		String privateKey = "private-reusable-start-key";
		Inquiry captured = inquiryService.create(privateRawText, "capture-before-invalid-proposal");
		when(proposalPort.propose(any())).thenReturn(null);

		MvcResult failed = start(captured.id(), 0, privateKey)
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn();

		assertFalse(failed.getResponse().getContentAsString().contains(privateRawText));
		assertFalse(failed.getResponse().getContentAsString().contains(privateKey));
		assertFalse(output.getAll().contains(privateRawText));
		assertFalse(output.getAll().contains(privateKey));
		assertEquals(0, turnCount());
		assertEquals(0, startRequestCount());
		assertEquals(InquiryStatus.CAPTURED, inquiryService.get(captured.id()).status());

		reset(proposalPort);
		when(proposalPort.propose(any())).thenReturn(VALID_PROPOSAL);
		start(captured.id(), 0, privateKey).andExpect(status().isCreated());
		assertEquals(1, turnCount());
	}

	@Test
	void transactionFailureRollsBackTheClaimTurnAndInquiry(CapturedOutput output)
			throws Exception {
		String privateRawText = "private transaction failure input";
		String privateKey = "private-transaction-failure-key";
		Inquiry captured = inquiryService.create(privateRawText, "capture-before-transaction-failure");
		doThrow(new IllegalStateException("simulated persistence failure"))
				.when(clarificationStore).insertTurn(any(), eq(privateKey));

		MvcResult failed = start(captured.id(), 0, privateKey)
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn();

		assertEquals(0, turnCount());
		assertEquals(0, startRequestCount());
		assertEquals(InquiryStatus.CAPTURED, inquiryService.get(captured.id()).status());
		assertEquals(0, inquiryService.get(captured.id()).version());
		assertFalse(output.getAll().contains(privateRawText));
		assertFalse(output.getAll().contains(privateKey));
		assertFalse(failed.getResponse().getContentAsString().contains(privateRawText));
		assertFalse(failed.getResponse().getContentAsString().contains(privateKey));
		assertFalse(failed.getResponse().getContentAsString().contains(VALID_PROPOSAL.question()));
		assertFalse(failed.getResponse().getContentAsString().contains(VALID_PROPOSAL.reason()));
		assertFalse(output.getAll().contains(VALID_PROPOSAL.question()));
		assertFalse(output.getAll().contains(VALID_PROPOSAL.reason()));

		reset(clarificationStore);
		start(captured.id(), 0, privateKey).andExpect(status().isCreated());
		assertEquals(1, turnCount());
	}

	@Test
	void validatesRequestsAndKeepsTurnOwnershipPrivate() throws Exception {
		Inquiry first = inquiryService.create("first", "capture-validation-first");
		Inquiry second = inquiryService.create("second", "capture-validation-second");
		MvcResult started = start(first.id(), 0, "start-validation-owner")
				.andExpect(status().isCreated())
				.andReturn();
		String turnId = objectMapper.readTree(started.getResponse().getContentAsString())
				.required("id").stringValue();

		startBody(first.id(), "missing-version", "{}")
				.andExpect(status().isBadRequest());
		startBody(first.id(), "negative-version", "{\"inquiryVersion\":-1}")
				.andExpect(status().isBadRequest());
		startBody(first.id(), "contains space", "{\"inquiryVersion\":0}")
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/inquiries/not-a-uuid/clarification-turns/current"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get(
				"/api/inquiries/{inquiryId}/clarification-turns/not-a-uuid",
				first.id()))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get(
				"/api/inquiries/{inquiryId}/clarification-turns/{turnId}",
				second.id(),
				turnId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(
				"/api/inquiries/{inquiryId}/clarification-turns/current",
				second.id()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(
				"/api/inquiries/{inquiryId}/clarification-turns/{turnId}",
				first.id(),
				UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	private org.springframework.test.web.servlet.ResultActions start(
			UUID inquiryId,
			long inquiryVersion,
			String key) throws Exception {
		return startBody(
				inquiryId,
				key,
				objectMapper.writeValueAsString(new StartBody(inquiryVersion)));
	}

	private org.springframework.test.web.servlet.ResultActions startBody(
			UUID inquiryId,
			String key,
			String body) throws Exception {
		return mockMvc.perform(post("/api/inquiries/{inquiryId}/clarification-turns", inquiryId)
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private int turnCount() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM clarification_turns", Integer.class);
	}

	private int startRequestCount() {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM clarification_start_requests",
				Integer.class);
	}

	private record StartBody(long inquiryVersion) {
	}

}
