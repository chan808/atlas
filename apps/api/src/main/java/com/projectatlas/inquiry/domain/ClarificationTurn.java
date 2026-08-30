package com.projectatlas.inquiry.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ClarificationTurn(
		UUID id,
		UUID inquiryId,
		int sequence,
		ClarificationProposal proposal,
		Instant createdAt,
		long inquiryVersion) {

	public ClarificationTurn {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(inquiryId, "inquiryId must not be null");
		Objects.requireNonNull(proposal, "proposal must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		if (sequence != 1) {
			throw new IllegalArgumentException("M1.2 supports only the first clarification turn");
		}
		if (inquiryVersion < 1) {
			throw new IllegalArgumentException("Clarification turn must reference a transitioned version");
		}
	}

}
