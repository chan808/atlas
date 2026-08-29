package com.projectatlas.inquiry.application;

import java.util.Optional;
import java.util.UUID;

import com.projectatlas.inquiry.domain.Inquiry;

public interface InquiryStore {

	void insertIfAbsent(Inquiry inquiry, String creationIdempotencyKey);

	Optional<Inquiry> findByCreationIdempotencyKey(String creationIdempotencyKey);

	Optional<Inquiry> findById(UUID id);

}
