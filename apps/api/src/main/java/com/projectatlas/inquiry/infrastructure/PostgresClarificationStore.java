package com.projectatlas.inquiry.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.projectatlas.inquiry.application.ClarificationStartRecord;
import com.projectatlas.inquiry.application.ClarificationStore;
import com.projectatlas.inquiry.domain.ClarificationProposal;
import com.projectatlas.inquiry.domain.ClarificationTurn;

import jakarta.persistence.EntityManager;

@Repository
class PostgresClarificationStore implements ClarificationStore {

	private final InquiryJpaRepository inquiryRepository;
	private final ClarificationStartRequestJpaRepository startRequestRepository;
	private final ClarificationTurnJpaRepository turnRepository;
	private final EntityManager entityManager;

	PostgresClarificationStore(
			InquiryJpaRepository inquiryRepository,
			ClarificationStartRequestJpaRepository startRequestRepository,
			ClarificationTurnJpaRepository turnRepository,
			EntityManager entityManager) {
		this.inquiryRepository = inquiryRepository;
		this.startRequestRepository = startRequestRepository;
		this.turnRepository = turnRepository;
		this.entityManager = entityManager;
	}

	@Override
	public Optional<ClarificationStartRecord> findByIdempotencyKey(String idempotencyKey) {
		return startRequestRepository.findById(idempotencyKey)
				.map(request -> new ClarificationStartRecord(
						request.inquiryId(),
						request.inquiryVersion(),
						turnRepository.findByStartIdempotencyKey(request.idempotencyKey())
								.map(this::toDomain)
								.orElseThrow(IllegalStateException::new)));
	}

	@Override
	public boolean claimStart(
			String idempotencyKey,
			UUID inquiryId,
			long inquiryVersion,
			Instant createdAt) {
		return startRequestRepository.insertIfAbsent(
				idempotencyKey,
				inquiryId,
				inquiryVersion,
				createdAt) == 1;
	}

	@Override
	public boolean transitionToClarifying(UUID inquiryId, long expectedVersion) {
		return inquiryRepository.transitionToClarifying(inquiryId, expectedVersion) == 1;
	}

	@Override
	public void insertTurn(ClarificationTurn turn, String idempotencyKey) {
		turnRepository.saveAndFlush(new ClarificationTurnJpaEntity(
				turn.id(),
				turn.inquiryId(),
				turn.sequence(),
				turn.proposal().question(),
				turn.proposal().reason(),
				turn.proposal().source(),
				turn.proposal().schemaVersion(),
				turn.createdAt(),
				turn.inquiryVersion(),
				idempotencyKey));
		entityManager.clear();
	}

	@Override
	public Optional<ClarificationTurn> findTurn(UUID inquiryId, UUID turnId) {
		return turnRepository.findByIdAndInquiryId(turnId, inquiryId).map(this::toDomain);
	}

	@Override
	public Optional<ClarificationTurn> findCurrentTurn(UUID inquiryId) {
		return turnRepository.findByInquiryIdAndSequence(inquiryId, 1).map(this::toDomain);
	}

	private ClarificationTurn toDomain(ClarificationTurnJpaEntity entity) {
		return new ClarificationTurn(
				entity.id(),
				entity.inquiryId(),
				entity.sequence(),
				new ClarificationProposal(
						entity.question(),
						entity.reason(),
						entity.proposalSource(),
						entity.schemaVersion()),
				entity.createdAt(),
				entity.resultingInquiryVersion());
	}

}
