package com.projectatlas.inquiry.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectatlas.inquiry.application.InquiryService;
import com.projectatlas.inquiry.domain.Inquiry;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

	private final InquiryService inquiryService;

	public InquiryController(InquiryService inquiryService) {
		this.inquiryService = inquiryService;
	}

	@PostMapping
	ResponseEntity<InquiryResponse> create(
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestBody CreateInquiryRequest request) {
		Inquiry inquiry = inquiryService.create(request.rawText(), idempotencyKey);
		URI location = URI.create("/api/inquiries/" + inquiry.id());
		return ResponseEntity.created(location).body(InquiryResponse.from(inquiry));
	}

	@GetMapping("/{id}")
	InquiryResponse get(@PathVariable UUID id) {
		return InquiryResponse.from(inquiryService.get(id));
	}

}
