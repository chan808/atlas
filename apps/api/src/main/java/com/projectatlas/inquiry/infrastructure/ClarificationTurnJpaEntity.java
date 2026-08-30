package com.projectatlas.inquiry.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clarification_turns")
class ClarificationTurnJpaEntity {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "inquiry_id", nullable = false, updatable = false)
	private UUID inquiryId;

	@Column(name = "sequence_number", nullable = false, updatable = false)
	private int sequence;

	@Column(name = "question", nullable = false, updatable = false, columnDefinition = "text")
	private String question;

	@Column(name = "reason", nullable = false, updatable = false, columnDefinition = "text")
	private String reason;

	@Column(name = "proposal_source", nullable = false, updatable = false, length = 64)
	private String proposalSource;

	@Column(name = "schema_version", nullable = false, updatable = false, length = 64)
	private String schemaVersion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "resulting_inquiry_version", nullable = false, updatable = false)
	private long resultingInquiryVersion;

	@Column(name = "start_idempotency_key", nullable = false, updatable = false, length = 128)
	private String startIdempotencyKey;

	protected ClarificationTurnJpaEntity() {
	}

	ClarificationTurnJpaEntity(
			UUID id,
			UUID inquiryId,
			int sequence,
			String question,
			String reason,
			String proposalSource,
			String schemaVersion,
			Instant createdAt,
			long resultingInquiryVersion,
			String startIdempotencyKey) {
		this.id = id;
		this.inquiryId = inquiryId;
		this.sequence = sequence;
		this.question = question;
		this.reason = reason;
		this.proposalSource = proposalSource;
		this.schemaVersion = schemaVersion;
		this.createdAt = createdAt;
		this.resultingInquiryVersion = resultingInquiryVersion;
		this.startIdempotencyKey = startIdempotencyKey;
	}

	UUID id() {
		return id;
	}

	UUID inquiryId() {
		return inquiryId;
	}

	int sequence() {
		return sequence;
	}

	String question() {
		return question;
	}

	String reason() {
		return reason;
	}

	String proposalSource() {
		return proposalSource;
	}

	String schemaVersion() {
		return schemaVersion;
	}

	Instant createdAt() {
		return createdAt;
	}

	long resultingInquiryVersion() {
		return resultingInquiryVersion;
	}

}
