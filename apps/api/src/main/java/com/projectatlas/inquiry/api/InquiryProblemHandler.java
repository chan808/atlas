package com.projectatlas.inquiry.api;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.projectatlas.inquiry.application.ClarificationConflictException;
import com.projectatlas.inquiry.application.ClarificationTurnNotFoundException;
import com.projectatlas.inquiry.application.IdempotencyConflictException;
import com.projectatlas.inquiry.application.InquiryNotFoundException;
import com.projectatlas.inquiry.application.InvalidIdempotencyKeyException;
import com.projectatlas.inquiry.application.InvalidInquiryVersionException;
import com.projectatlas.inquiry.domain.InvalidClarificationProposalException;
import com.projectatlas.inquiry.domain.InvalidBrainDumpException;

@RestControllerAdvice(assignableTypes = InquiryController.class)
class InquiryProblemHandler {

	@ExceptionHandler(InvalidBrainDumpException.class)
	ProblemDetail invalidBrainDump() {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid inquiry request",
				"The Brain Dump does not satisfy the capture contract.",
				"urn:project-atlas:problem:invalid-inquiry-request",
				"rawText");
	}

	@ExceptionHandler(InvalidIdempotencyKeyException.class)
	ProblemDetail invalidIdempotencyKey() {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid inquiry request",
				"The request metadata does not satisfy the inquiry contract.",
				"urn:project-atlas:problem:invalid-inquiry-request",
				"Idempotency-Key");
	}

	@ExceptionHandler(InvalidInquiryVersionException.class)
	ProblemDetail invalidInquiryVersion() {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid clarification request",
				"The Inquiry version does not satisfy the clarification contract.",
				"urn:project-atlas:problem:invalid-clarification-request",
				"inquiryVersion");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableRequest() {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid inquiry request",
				"The request body could not be read.",
				"urn:project-atlas:problem:invalid-inquiry-request",
				"request");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail invalidId() {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid inquiry identifier",
				"The inquiry identifier is not a valid UUID.",
				"urn:project-atlas:problem:invalid-inquiry-id",
				"id");
	}

	@ExceptionHandler(InquiryNotFoundException.class)
	ProblemDetail inquiryNotFound() {
		return problem(
				HttpStatus.NOT_FOUND,
				"Inquiry not found",
				"No Inquiry exists for the requested identifier.",
				"urn:project-atlas:problem:inquiry-not-found",
				null);
	}

	@ExceptionHandler(ClarificationTurnNotFoundException.class)
	ProblemDetail clarificationTurnNotFound() {
		return problem(
				HttpStatus.NOT_FOUND,
				"Clarification turn not found",
				"No clarification turn exists for the requested identifiers.",
				"urn:project-atlas:problem:clarification-turn-not-found",
				null);
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	ProblemDetail idempotencyConflict() {
		return problem(
				HttpStatus.CONFLICT,
				"Idempotency conflict",
				"The creation key has already been used for another request.",
				"urn:project-atlas:problem:idempotency-conflict",
				"Idempotency-Key");
	}

	@ExceptionHandler(ClarificationConflictException.class)
	ProblemDetail clarificationConflict() {
		return problem(
				HttpStatus.CONFLICT,
				"Clarification conflict",
				"Clarification cannot start from the requested Inquiry state.",
				"urn:project-atlas:problem:clarification-conflict",
				null);
	}

	@ExceptionHandler(InvalidClarificationProposalException.class)
	ProblemDetail invalidClarificationProposal() {
		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Clarification unavailable",
				"The clarification question could not be prepared.",
				"urn:project-atlas:problem:clarification-unavailable",
				null);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail unexpectedFailure() {
		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Inquiry operation unavailable",
				"The inquiry operation could not be completed.",
				"urn:project-atlas:problem:inquiry-operation-unavailable",
				null);
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			String type,
			String field) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create(type));
		if (field != null) {
			problem.setProperty("field", field);
		}
		return problem;
	}

}
