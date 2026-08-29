package com.projectatlas.inquiry.application;

public final class InvalidIdempotencyKeyException extends RuntimeException {

	public InvalidIdempotencyKeyException() {
		super("Idempotency-Key must contain 1-128 visible ASCII characters");
	}

}
