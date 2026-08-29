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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.projectatlas.TestcontainersConfiguration;
import com.projectatlas.inquiry.application.IdempotencyConflictException;
import com.projectatlas.inquiry.application.InquiryService;
import com.projectatlas.inquiry.domain.Inquiry;
import com.projectatlas.inquiry.domain.InvalidBrainDumpException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class InquiryCaptureIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private InquiryService inquiryService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ExecutorService executor;

	@BeforeEach
	void clearInquiries() {
		jdbcTemplate.update("DELETE FROM inquiries");
	}

	@AfterEach
	void stopExecutor() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	@Test
	void createsAndRetrievesTheExactDecodedString() throws Exception {
		String rawText = "  Kafka\r\n한글\t😀e\u0301  ";

		MvcResult created = create(rawText, "capture-round-trip")
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();

		JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
		String id = createdBody.required("id").stringValue();
		assertEquals(rawText, createdBody.required("rawText").stringValue());
		assertEquals("CAPTURED", createdBody.required("status").stringValue());
		assertEquals("/api/inquiries/" + id, created.getResponse().getHeader("Location"));

		MvcResult retrieved = mockMvc.perform(get("/api/inquiries/{id}", id))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode retrievedBody = objectMapper.readTree(retrieved.getResponse().getContentAsString());
		assertEquals(createdBody, retrievedBody);
	}

	@Test
	void sequentialReplayReturnsTheOriginalRepresentation() throws Exception {
		MvcResult first = create("retry를 안전하게 이해하고 싶다", "sequential-retry")
				.andExpect(status().isCreated())
				.andReturn();

		MvcResult replay = create("retry를 안전하게 이해하고 싶다", "sequential-retry")
				.andExpect(status().isCreated())
				.andReturn();

		assertEquals(first.getResponse().getHeader("Location"), replay.getResponse().getHeader("Location"));
		assertEquals(first.getResponse().getContentAsString(), replay.getResponse().getContentAsString());
		assertEquals(1, inquiryCount());
	}

	@Test
	void sameKeyWithDifferentTextConflictsWithoutChangingTheOriginal(CapturedOutput output)
			throws Exception {
		String key = "private-conflict-key";
		String original = "private-original-thought";
		String conflicting = "private-conflicting-thought";
		MvcResult first = create(original, key).andExpect(status().isCreated()).andReturn();

		MvcResult conflict = create(conflicting, key)
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn();

		String problem = conflict.getResponse().getContentAsString();
		assertFalse(problem.contains(key));
		assertFalse(problem.contains(original));
		assertFalse(problem.contains(conflicting));
		assertFalse(output.getAll().contains(key));
		assertFalse(output.getAll().contains(original));
		assertFalse(output.getAll().contains(conflicting));
		assertEquals(1, inquiryCount());

		String id = objectMapper.readTree(first.getResponse().getContentAsString())
				.required("id").stringValue();
		MvcResult retrieved = mockMvc.perform(get("/api/inquiries/{id}", id))
				.andExpect(status().isOk())
				.andReturn();
		assertEquals(original,
				objectMapper.readTree(retrieved.getResponse().getContentAsString())
						.required("rawText").stringValue());
	}

	@Test
	void differentKeysMayCaptureTheSameTextTwice() {
		Inquiry first = inquiryService.create("같은 생각", "same-text-one");
		Inquiry second = inquiryService.create("같은 생각", "same-text-two");

		assertNotEquals(first.id(), second.id());
		assertEquals(2, inquiryCount());
	}

	@Test
	void persistsTheTenThousandCodePointBoundary() {
		String rawText = "😀".repeat(10_000);

		Inquiry captured = inquiryService.create(rawText, "maximum-code-point-boundary");

		assertEquals(rawText, inquiryService.get(captured.id()).brainDump().value());
	}

	@Test
	void concurrentReplayCommitsOneInquiry() throws Exception {
		int requestCount = 8;
		executor = Executors.newFixedThreadPool(requestCount);
		CountDownLatch ready = new CountDownLatch(requestCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Inquiry>> futures = new ArrayList<>();

		for (int index = 0; index < requestCount; index++) {
			futures.add(executor.submit(() -> {
				ready.countDown();
				assertTrue(start.await(10, TimeUnit.SECONDS));
				return inquiryService.create("동시 retry를 배우고 싶다", "concurrent-retry");
			}));
		}

		assertTrue(ready.await(10, TimeUnit.SECONDS));
		start.countDown();

		Set<Inquiry> results = new HashSet<>();
		for (Future<Inquiry> future : futures) {
			results.add(future.get(20, TimeUnit.SECONDS));
		}

		assertEquals(1, results.size());
		assertEquals(1, inquiryCount());
	}

	@Test
	void validationDoesNotConsumeTheCreationKey() {
		assertThrows(InvalidBrainDumpException.class,
				() -> inquiryService.create(" \t\n", "reusable-after-validation"));

		Inquiry captured = inquiryService.create("이제 유효한 생각", "reusable-after-validation");

		assertEquals("이제 유효한 생각", captured.brainDump().value());
		assertEquals(1, inquiryCount());
	}

	@Test
	void invalidRequestsAndMissingInquiriesUseRedactedProblemDetails(CapturedOutput output)
			throws Exception {
		String privateRawText = "private-invalid-content";
		String privateKey = "private-invalid-key";

		MvcResult invalidText = create(" \t\n", privateKey)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn();
		MvcResult missingKey = mockMvc.perform(post("/api/inquiries")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new RequestBody(privateRawText))))
				.andExpect(status().isBadRequest())
				.andReturn();
		MvcResult malformedId = mockMvc.perform(get("/api/inquiries/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andReturn();
		MvcResult missing = mockMvc.perform(get("/api/inquiries/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andReturn();

		String errors = invalidText.getResponse().getContentAsString()
				+ missingKey.getResponse().getContentAsString()
				+ malformedId.getResponse().getContentAsString()
				+ missing.getResponse().getContentAsString();
		assertFalse(errors.contains(privateRawText));
		assertFalse(errors.contains(privateKey));
		assertFalse(output.getAll().contains(privateRawText));
		assertFalse(output.getAll().contains(privateKey));
	}

	@Test
	void invalidIdempotencyKeysAreRejectedWithoutBeingEchoed() throws Exception {
		for (String invalidKey : List.of("contains space", "한글-key", "x".repeat(129))) {
			MvcResult result = create("유효한 원문", invalidKey)
					.andExpect(status().isBadRequest())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andReturn();

			assertFalse(result.getResponse().getContentAsString().contains(invalidKey));
		}
		assertEquals(0, inquiryCount());
	}

	private org.springframework.test.web.servlet.ResultActions create(String rawText, String key)
			throws Exception {
		return mockMvc.perform(post("/api/inquiries")
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RequestBody(rawText))));
	}

	private int inquiryCount() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM inquiries", Integer.class);
	}

	private record RequestBody(String rawText) {
	}

}
