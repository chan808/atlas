package com.projectatlas.inquiry.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.projectatlas.inquiry.domain.ClarificationTurn;

public interface ClarificationStore {

	Optional<ClarificationStartRecord> findByIdempotencyKey(String idempotencyKey);

	boolean claimStart(
			String idempotencyKey,
			UUID inquiryId,
			long inquiryVersion,
			Instant createdAt);

	boolean transitionToClarifying(UUID inquiryId, long expectedVersion);

	void insertTurn(ClarificationTurn turn, String idempotencyKey);

	Optional<ClarificationTurn> findTurn(UUID inquiryId, UUID turnId);

	Optional<ClarificationTurn> findCurrentTurn(UUID inquiryId);

}
