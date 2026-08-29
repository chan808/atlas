package com.projectatlas.inquiry.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InquiryJpaRepository extends JpaRepository<InquiryJpaEntity, UUID> {

	@Modifying
	@Query(value = """
			INSERT INTO inquiries (
			    id,
			    raw_text,
			    status,
			    created_at,
			    creation_idempotency_key
			)
			VALUES (:id, :rawText, 'CAPTURED', :createdAt, :creationIdempotencyKey)
			ON CONFLICT (creation_idempotency_key) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("id") UUID id,
			@Param("rawText") String rawText,
			@Param("createdAt") Instant createdAt,
			@Param("creationIdempotencyKey") String creationIdempotencyKey);

	Optional<InquiryJpaEntity> findByCreationIdempotencyKey(String creationIdempotencyKey);

}
