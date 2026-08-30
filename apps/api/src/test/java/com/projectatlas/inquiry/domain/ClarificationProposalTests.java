package com.projectatlas.inquiry.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClarificationProposalTests {

	@Test
	void preservesValidProposalValues() {
		String question = "  원리를 설명할까요?\r\n예제로 확인할까요? 😀e\u0301  ";
		String reason = "답에 따라 다음 방향과 증거가 달라집니다.";

		ClarificationProposal proposal = new ClarificationProposal(
				question,
				reason,
				"deterministic-fake-v1",
				"clarification-question-v1");

		assertEquals(question, proposal.question());
		assertEquals(reason, proposal.reason());
	}

	@Test
	void rejectsBlankAndOversizedText() {
		assertInvalid(" \t\n", "valid reason", "source", "schema");
		assertInvalid("valid question", "\u3000\u00a0", "source", "schema");
		assertInvalid("😀".repeat(501), "valid reason", "source", "schema");
	}

	@Test
	void rejectsNulAndMalformedUtf16() {
		assertInvalid("question\0", "valid reason", "source", "schema");
		assertInvalid("\uD83D", "valid reason", "source", "schema");
		assertInvalid("valid question", "\uDE00", "source", "schema");
	}

	@Test
	void rejectsInvalidMetadata() {
		assertInvalid("valid question", "valid reason", "contains space", "schema");
		assertInvalid("valid question", "valid reason", "source", "한글-schema");
		assertInvalid("valid question", "valid reason", "x".repeat(65), "schema");
	}

	private static void assertInvalid(
			String question,
			String reason,
			String source,
			String schemaVersion) {
		assertThrows(
				InvalidClarificationProposalException.class,
				() -> new ClarificationProposal(question, reason, source, schemaVersion));
	}

}
