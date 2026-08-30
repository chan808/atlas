package com.projectatlas.inquiry.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.projectatlas.inquiry.domain.ClarificationProposal;
import com.projectatlas.inquiry.domain.ClarificationTurn;
import com.projectatlas.inquiry.domain.Inquiry;
import com.projectatlas.inquiry.domain.InvalidClarificationProposalException;
import com.projectatlas.inquiry.domain.InvalidInquiryTransitionException;

@Service
public class ClarificationService {

	private final InquiryService inquiryService;
	private final ClarificationStore clarificationStore;
	private final ClarificationProposalPort proposalPort;
	private final ClarificationStartTransaction startTransaction;
	private final Clock clock;

	public ClarificationService(
			InquiryService inquiryService,
			ClarificationStore clarificationStore,
			ClarificationProposalPort proposalPort,
			ClarificationStartTransaction startTransaction,
			Clock clock) {
		this.inquiryService = inquiryService;
		this.clarificationStore = clarificationStore;
		this.proposalPort = proposalPort;
		this.startTransaction = startTransaction;
		this.clock = clock;
	}

	public ClarificationTurn start(
			UUID inquiryId,
			Long inquiryVersion,
			String idempotencyKey) {
		IdempotencyKeyValidator.validate(idempotencyKey);
		long requestedVersion = validateVersion(inquiryVersion);

		ClarificationStartRecord existing = clarificationStore
				.findByIdempotencyKey(idempotencyKey)
				.orElse(null);
		if (existing != null) {
			if (!existing.matches(inquiryId, requestedVersion)) {
				throw new ClarificationConflictException();
			}
			return existing.turn();
		}

		Inquiry inquiry = inquiryService.get(inquiryId);
		try {
			inquiry.startClarification(requestedVersion);
		}
		catch (InvalidInquiryTransitionException exception) {
			throw new ClarificationConflictException();
		}

		ClarificationProposal proposal = proposalPort.propose(inquiry.brainDump());
		if (proposal == null) {
			throw new InvalidClarificationProposalException();
		}
		return startTransaction.start(
				inquiry,
				requestedVersion,
				idempotencyKey,
				proposal,
				UUID.randomUUID(),
				Instant.now(clock));
	}

	public ClarificationTurn get(UUID inquiryId, UUID turnId) {
		return clarificationStore.findTurn(inquiryId, turnId)
				.orElseThrow(ClarificationTurnNotFoundException::new);
	}

	public ClarificationTurn getCurrent(UUID inquiryId) {
		return clarificationStore.findCurrentTurn(inquiryId)
				.orElseThrow(ClarificationTurnNotFoundException::new);
	}

	private static long validateVersion(Long version) {
		if (version == null || version < 0) {
			throw new InvalidInquiryVersionException();
		}
		return version;
	}

}
