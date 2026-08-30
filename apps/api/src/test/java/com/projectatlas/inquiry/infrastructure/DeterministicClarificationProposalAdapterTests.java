package com.projectatlas.inquiry.infrastructure;

import org.junit.jupiter.api.Test;

import com.projectatlas.inquiry.domain.BrainDump;
import com.projectatlas.inquiry.domain.ClarificationProposal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeterministicClarificationProposalAdapterTests {

	@Test
	void returnsTheVersionedReviewedTemplateWithoutCopyingTheBrainDump() {
		String privateRawText = "private raw curiosity";

		ClarificationProposal proposal = new DeterministicClarificationProposalAdapter()
				.propose(new BrainDump(privateRawText));

		assertEquals("deterministic-fake-v1", proposal.source());
		assertEquals("clarification-question-v1", proposal.schemaVersion());
		assertFalse(proposal.question().contains(privateRawText));
		assertFalse(proposal.reason().contains(privateRawText));
	}

}
