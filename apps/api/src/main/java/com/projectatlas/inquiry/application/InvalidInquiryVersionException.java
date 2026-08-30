package com.projectatlas.inquiry.application;

public final class InvalidInquiryVersionException extends RuntimeException {

	public InvalidInquiryVersionException() {
		super("Inquiry version is required and must not be negative");
	}

}
