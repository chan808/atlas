package com.projectatlas.inquiry.domain;

import java.util.regex.Pattern;

public record ClarificationProposal(
		String question,
		String reason,
		String source,
		String schemaVersion) {

	public static final int MAX_TEXT_CODE_POINTS = 500;
	public static final int MAX_METADATA_LENGTH = 64;

	private static final Pattern METADATA_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	public ClarificationProposal {
		validateText(question);
		validateText(reason);
		validateMetadata(source);
		validateMetadata(schemaVersion);
	}

	private static void validateText(String value) {
		if (value == null) {
			throw new InvalidClarificationProposalException();
		}
		validateUtf16(value);
		if (value.codePointCount(0, value.length()) > MAX_TEXT_CODE_POINTS) {
			throw new InvalidClarificationProposalException();
		}
		boolean hasNonWhitespace = value.codePoints()
				.anyMatch(codePoint -> !Character.isWhitespace(codePoint)
						&& !Character.isSpaceChar(codePoint));
		if (!hasNonWhitespace) {
			throw new InvalidClarificationProposalException();
		}
	}

	private static void validateMetadata(String value) {
		if (value == null || !METADATA_PATTERN.matcher(value).matches()) {
			throw new InvalidClarificationProposalException();
		}
	}

	private static void validateUtf16(String value) {
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (current == '\0') {
				throw new InvalidClarificationProposalException();
			}
			if (Character.isHighSurrogate(current)) {
				if (index + 1 >= value.length()
						|| !Character.isLowSurrogate(value.charAt(index + 1))) {
					throw new InvalidClarificationProposalException();
				}
				index++;
			}
			else if (Character.isLowSurrogate(current)) {
				throw new InvalidClarificationProposalException();
			}
		}
	}

	@Override
	public String toString() {
		return "ClarificationProposal[redacted]";
	}

}
