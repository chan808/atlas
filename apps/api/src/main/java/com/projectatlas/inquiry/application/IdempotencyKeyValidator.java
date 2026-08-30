package com.projectatlas.inquiry.application;

final class IdempotencyKeyValidator {

	private static final int MAX_LENGTH = 128;

	private IdempotencyKeyValidator() {
	}

	static void validate(String key) {
		if (key == null || key.isEmpty() || key.length() > MAX_LENGTH
				|| key.chars().anyMatch(character -> character < '!' || character > '~')) {
			throw new InvalidIdempotencyKeyException();
		}
	}

}
