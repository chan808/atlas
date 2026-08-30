package com.projectatlas.inquiry.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectatlas.inquiry.domain.ClarificationProposal;
import com.projectatlas.inquiry.domain.ClarificationTurn;
import com.projectatlas.inquiry.domain.Inquiry;
import com.projectatlas.inquiry.domain.InvalidInquiryTransitionException;

@Service
class ClarificationStartTransaction {

	private final ClarificationStore clarificationStore;

	ClarificationStartTransaction(ClarificationStore clarificationStore) {
		this.clarificationStore = clarificationStore;
	}

	@Transactional
	ClarificationTurn start(
			Inquiry snapshot,
			long expectedVersion,
			String idempotencyKey,
			ClarificationProposal proposal,
			UUID turnId,
			Instant createdAt) {
		boolean claimed = clarificationStore.claimStart(
				idempotencyKey,
				snapshot.id(),
				expectedVersion,
				createdAt);
		if (!claimed) {
			return replay(idempotencyKey, snapshot.id(), expectedVersion);
		}

		Inquiry transitioned;
		try {
			transitioned = snapshot.startClarification(expectedVersion);
		}
		catch (InvalidInquiryTransitionException exception) {
			throw new ClarificationConflictException();
		}
		if (!clarificationStore.transitionToClarifying(snapshot.id(), expectedVersion)) {
			throw new ClarificationConflictException();
		}

		ClarificationTurn turn = new ClarificationTurn(
				turnId,
				snapshot.id(),
				1,
				proposal,
				createdAt,
				transitioned.version());
		clarificationStore.insertTurn(turn, idempotencyKey);
		return clarificationStore.findTurn(snapshot.id(), turnId)
				.orElseThrow(IllegalStateException::new);
	}

	private ClarificationTurn replay(
			String idempotencyKey,
			UUID inquiryId,
			long inquiryVersion) {
		ClarificationStartRecord existing = clarificationStore
				.findByIdempotencyKey(idempotencyKey)
				.orElseThrow(IllegalStateException::new);
		if (!existing.matches(inquiryId, inquiryVersion)) {
			throw new ClarificationConflictException();
		}
		return existing.turn();
	}

}
