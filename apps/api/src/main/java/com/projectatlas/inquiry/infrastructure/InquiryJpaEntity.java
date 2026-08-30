package com.projectatlas.inquiry.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.projectatlas.inquiry.domain.InquiryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inquiries")
class InquiryJpaEntity {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "raw_text", nullable = false, updatable = false, columnDefinition = "text")
	private String rawText;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private InquiryStatus status;

	@Column(name = "version", nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "creation_idempotency_key", nullable = false, updatable = false, length = 128)
	private String creationIdempotencyKey;

	protected InquiryJpaEntity() {
	}

	UUID id() {
		return id;
	}

	String rawText() {
		return rawText;
	}

	InquiryStatus status() {
		return status;
	}

	Instant createdAt() {
		return createdAt;
	}

	long version() {
		return version;
	}

}
