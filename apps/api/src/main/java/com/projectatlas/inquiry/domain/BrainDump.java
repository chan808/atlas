package com.projectatlas.inquiry.domain;

public record BrainDump(String value) {

	public static final int MAX_CODE_POINTS = 10_000;

	public BrainDump {
		if (value == null) {
			throw new InvalidBrainDumpException("Brain Dump is required");
		}

		validateUtf16(value);

		int codePointCount = value.codePointCount(0, value.length());
		if (codePointCount > MAX_CODE_POINTS) {
			throw new InvalidBrainDumpException("Brain Dump exceeds the maximum length");
		}

		boolean hasNonWhitespace = value.codePoints()
				.anyMatch(codePoint -> !Character.isWhitespace(codePoint)
						&& !Character.isSpaceChar(codePoint));
		if (!hasNonWhitespace) {
			throw new InvalidBrainDumpException("Brain Dump must contain non-whitespace text");
		}
	}

	private static void validateUtf16(String value) {
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (current == '\0') {
				throw new InvalidBrainDumpException("Brain Dump contains an unsupported character");
			}

			if (Character.isHighSurrogate(current)) {
				if (index + 1 >= value.length()
						|| !Character.isLowSurrogate(value.charAt(index + 1))) {
					throw new InvalidBrainDumpException("Brain Dump contains malformed Unicode");
				}
				index++;
			}
			else if (Character.isLowSurrogate(current)) {
				throw new InvalidBrainDumpException("Brain Dump contains malformed Unicode");
			}
		}
	}

	@Override
	public String toString() {
		return "BrainDump[redacted]";
	}

}
