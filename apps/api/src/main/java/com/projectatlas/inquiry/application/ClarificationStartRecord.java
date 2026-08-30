package com.projectatlas.inquiry.application;

import java.util.Objects;
import java.util.UUID;

import com.projectatlas.inquiry.domain.ClarificationTurn;

public record ClarificationStartRecord(
		UUID inquiryId,
		long requestedInquiryVersion,
		ClarificationTurn turn) {

	public ClarificationStartRecord {
		Objects.requireNonNull(inquiryId, "inquiryId must not be null");
		Objects.requireNonNull(turn, "turn must not be null");
		if (requestedInquiryVersion < 0) {
			throw new IllegalArgumentException("requestedInquiryVersion must not be negative");
		}
	}

	public boolean matches(UUID requestedInquiryId, long requestedVersion) {
		return inquiryId.equals(requestedInquiryId)
				&& requestedInquiryVersion == requestedVersion;
	}

}
