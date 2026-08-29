package com.projectatlas.inquiry.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectatlas.inquiry.domain.BrainDump;
import com.projectatlas.inquiry.domain.Inquiry;

@Service
public class InquiryService {

	private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

	private final InquiryStore inquiryStore;
	private final Clock clock;

	public InquiryService(InquiryStore inquiryStore, Clock clock) {
		this.inquiryStore = inquiryStore;
		this.clock = clock;
	}

	@Transactional
	public Inquiry create(String rawText, String creationIdempotencyKey) {
		BrainDump brainDump = new BrainDump(rawText);
		validateIdempotencyKey(creationIdempotencyKey);

		Inquiry candidate = Inquiry.capture(UUID.randomUUID(), brainDump, Instant.now(clock));
		inquiryStore.insertIfAbsent(candidate, creationIdempotencyKey);

		Inquiry stored = inquiryStore.findByCreationIdempotencyKey(creationIdempotencyKey)
				.orElseThrow(IllegalStateException::new);
		if (!stored.brainDump().value().equals(brainDump.value())) {
			throw new IdempotencyConflictException();
		}

		return stored;
	}

	@Transactional(readOnly = true)
	public Inquiry get(UUID id) {
		return inquiryStore.findById(id).orElseThrow(InquiryNotFoundException::new);
	}

	private static void validateIdempotencyKey(String key) {
		if (key == null || key.isEmpty() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH
				|| key.chars().anyMatch(character -> character < '!' || character > '~')) {
			throw new InvalidIdempotencyKeyException();
		}
	}

}
