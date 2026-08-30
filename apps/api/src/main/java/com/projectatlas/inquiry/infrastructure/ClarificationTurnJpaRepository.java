package com.projectatlas.inquiry.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClarificationTurnJpaRepository extends JpaRepository<ClarificationTurnJpaEntity, UUID> {

	Optional<ClarificationTurnJpaEntity> findByStartIdempotencyKey(String startIdempotencyKey);

	Optional<ClarificationTurnJpaEntity> findByIdAndInquiryId(UUID id, UUID inquiryId);

	Optional<ClarificationTurnJpaEntity> findByInquiryIdAndSequence(UUID inquiryId, int sequence);

}
