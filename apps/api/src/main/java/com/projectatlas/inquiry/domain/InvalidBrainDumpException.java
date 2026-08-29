package com.projectatlas.inquiry.domain;

public final class InvalidBrainDumpException extends RuntimeException {

	public InvalidBrainDumpException(String reason) {
		super(reason);
	}

}
