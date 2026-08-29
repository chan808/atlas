package com.projectatlas.inquiry.api;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.projectatlas.inquiry.application.IdempotencyConflictException;
import com.projectatlas.inquiry.application.InquiryNotFoundException;
import com.projectatlas.inquiry.application.InvalidIdempotencyKeyException;
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
				"The creation metadata does not satisfy the capture contract.",
				"urn:project-atlas:problem:invalid-inquiry-request",
				"Idempotency-Key");
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

	@ExceptionHandler(IdempotencyConflictException.class)
	ProblemDetail idempotencyConflict() {
		return problem(
				HttpStatus.CONFLICT,
				"Idempotency conflict",
				"The creation key has already been used for another request.",
				"urn:project-atlas:problem:idempotency-conflict",
				"Idempotency-Key");
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
