package com.projectatlas.inquiry.application;

public final class ClarificationConflictException extends RuntimeException {

	public ClarificationConflictException() {
		super("Clarification cannot start from the requested state");
	}

}
