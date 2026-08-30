package com.projectatlas.inquiry.application;

public final class ClarificationTurnNotFoundException extends RuntimeException {

	public ClarificationTurnNotFoundException() {
		super("Clarification turn not found");
	}

}
