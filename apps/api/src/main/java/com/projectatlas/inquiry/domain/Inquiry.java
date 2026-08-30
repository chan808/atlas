package com.projectatlas.inquiry.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Inquiry(UUID id, BrainDump brainDump, InquiryStatus status, Instant createdAt, long version) {

	public Inquiry {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(brainDump, "brainDump must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Inquiry version must not be negative");
		}
		if (status == InquiryStatus.CLARIFYING && version == 0) {
			throw new IllegalArgumentException("A clarifying Inquiry must have a transitioned version");
		}
	}

	public static Inquiry capture(UUID id, BrainDump brainDump, Instant createdAt) {
		return new Inquiry(id, brainDump, InquiryStatus.CAPTURED, createdAt, 0);
	}

	public Inquiry startClarification(long expectedVersion) {
		if (status != InquiryStatus.CAPTURED || version != expectedVersion) {
			throw new InvalidInquiryTransitionException();
		}
		return new Inquiry(id, brainDump, InquiryStatus.CLARIFYING, createdAt, version + 1);
	}

}
