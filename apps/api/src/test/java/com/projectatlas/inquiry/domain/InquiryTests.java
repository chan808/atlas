package com.projectatlas.inquiry.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InquiryTests {

	@Test
	void startsClarificationFromTheExpectedCapturedVersion() {
		Inquiry inquiry = Inquiry.capture(
				UUID.randomUUID(),
				new BrainDump("retry가 궁금하다"),
				Instant.parse("2026-08-30T00:00:00Z"));

		Inquiry clarifying = inquiry.startClarification(0);

		assertEquals(InquiryStatus.CLARIFYING, clarifying.status());
		assertEquals(1, clarifying.version());
		assertEquals(inquiry.id(), clarifying.id());
		assertEquals(inquiry.brainDump(), clarifying.brainDump());
		assertEquals(inquiry.createdAt(), clarifying.createdAt());
	}

	@Test
	void rejectsStaleAndRepeatedTransitions() {
		Inquiry inquiry = Inquiry.capture(
				UUID.randomUUID(),
				new BrainDump("동시성을 배우고 싶다"),
				Instant.parse("2026-08-30T00:00:00Z"));

		assertThrows(InvalidInquiryTransitionException.class,
				() -> inquiry.startClarification(1));
		Inquiry clarifying = inquiry.startClarification(0);
		assertThrows(InvalidInquiryTransitionException.class,
				() -> clarifying.startClarification(1));
	}

}
