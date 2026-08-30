package com.projectatlas.inquiry.api;

import java.time.Instant;
import java.util.UUID;

import com.projectatlas.inquiry.domain.ClarificationTurn;

record ClarificationTurnResponse(
		UUID id,
		UUID inquiryId,
		int sequence,
		String question,
		String reason,
		long inquiryVersion,
		Instant createdAt) {

	static ClarificationTurnResponse from(ClarificationTurn turn) {
		return new ClarificationTurnResponse(
				turn.id(),
				turn.inquiryId(),
				turn.sequence(),
				turn.proposal().question(),
				turn.proposal().reason(),
				turn.inquiryVersion(),
				turn.createdAt());
	}

}
