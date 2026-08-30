package com.projectatlas.inquiry.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ClarificationStartRequestJpaRepository
		extends JpaRepository<ClarificationStartRequestJpaEntity, String> {

	@Modifying
	@Query(value = """
			INSERT INTO clarification_start_requests (
			    idempotency_key,
			    inquiry_id,
			    inquiry_version,
			    created_at
			)
			VALUES (:idempotencyKey, :inquiryId, :inquiryVersion, :createdAt)
			ON CONFLICT (idempotency_key) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("idempotencyKey") String idempotencyKey,
			@Param("inquiryId") UUID inquiryId,
			@Param("inquiryVersion") long inquiryVersion,
			@Param("createdAt") Instant createdAt);

}
