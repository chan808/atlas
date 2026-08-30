package com.projectatlas.inquiry.api;

import java.time.Instant;
import java.util.UUID;

import com.projectatlas.inquiry.domain.Inquiry;
import com.projectatlas.inquiry.domain.InquiryStatus;

public record InquiryResponse(
		UUID id,
		String rawText,
		InquiryStatus status,
		long version,
		Instant createdAt) {

	static InquiryResponse from(Inquiry inquiry) {
		return new InquiryResponse(
				inquiry.id(),
				inquiry.brainDump().value(),
				inquiry.status(),
				inquiry.version(),
				inquiry.createdAt());
	}

}
