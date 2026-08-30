package com.projectatlas.inquiry.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clarification_start_requests")
class ClarificationStartRequestJpaEntity {

	@Id
	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
	private String idempotencyKey;

	@Column(name = "inquiry_id", nullable = false, updatable = false)
	private UUID inquiryId;

	@Column(name = "inquiry_version", nullable = false, updatable = false)
	private long inquiryVersion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ClarificationStartRequestJpaEntity() {
	}

	String idempotencyKey() {
		return idempotencyKey;
	}

	UUID inquiryId() {
		return inquiryId;
	}

	long inquiryVersion() {
		return inquiryVersion;
	}

}
