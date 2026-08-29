package com.projectatlas.inquiry.application;

public final class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException() {
		super("Idempotency-Key was already used for another request");
	}

}
