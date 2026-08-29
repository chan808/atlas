package com.projectatlas.inquiry.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.projectatlas.inquiry.application.InquiryStore;
import com.projectatlas.inquiry.domain.BrainDump;
import com.projectatlas.inquiry.domain.Inquiry;

@Repository
class PostgresInquiryStore implements InquiryStore {

	private final InquiryJpaRepository repository;

	PostgresInquiryStore(InquiryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public void insertIfAbsent(Inquiry inquiry, String creationIdempotencyKey) {
		repository.insertIfAbsent(
				inquiry.id(),
				inquiry.brainDump().value(),
				inquiry.createdAt(),
				creationIdempotencyKey);
	}

	@Override
	public Optional<Inquiry> findByCreationIdempotencyKey(String creationIdempotencyKey) {
		return repository.findByCreationIdempotencyKey(creationIdempotencyKey).map(this::toDomain);
	}

	@Override
	public Optional<Inquiry> findById(UUID id) {
		return repository.findById(id).map(this::toDomain);
	}

	private Inquiry toDomain(InquiryJpaEntity entity) {
		return new Inquiry(
				entity.id(),
				new BrainDump(entity.rawText()),
				entity.status(),
				entity.createdAt());
	}

}
