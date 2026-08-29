package com.projectatlas.inquiry.application;

public final class InquiryNotFoundException extends RuntimeException {

	public InquiryNotFoundException() {
		super("Inquiry was not found");
	}

}
