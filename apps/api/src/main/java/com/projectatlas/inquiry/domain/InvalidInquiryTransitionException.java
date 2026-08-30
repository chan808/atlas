package com.projectatlas.inquiry.domain;

public final class InvalidInquiryTransitionException extends RuntimeException {

	public InvalidInquiryTransitionException() {
		super("Inquiry cannot start clarification from the requested state");
	}

}
