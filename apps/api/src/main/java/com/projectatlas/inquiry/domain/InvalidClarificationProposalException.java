package com.projectatlas.inquiry.domain;

public final class InvalidClarificationProposalException extends RuntimeException {

	public InvalidClarificationProposalException() {
		super("Clarification proposal does not satisfy the contract");
	}

}
