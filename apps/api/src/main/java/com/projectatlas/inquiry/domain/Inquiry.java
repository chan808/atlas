package com.projectatlas.inquiry.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Inquiry(UUID id, BrainDump brainDump, InquiryStatus status, Instant createdAt) {

	public Inquiry {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(brainDump, "brainDump must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		if (status != InquiryStatus.CAPTURED) {
			throw new IllegalArgumentException("A new Inquiry must be captured");
		}
	}

	public static Inquiry capture(UUID id, BrainDump brainDump, Instant createdAt) {
		return new Inquiry(id, brainDump, InquiryStatus.CAPTURED, createdAt);
	}

}
