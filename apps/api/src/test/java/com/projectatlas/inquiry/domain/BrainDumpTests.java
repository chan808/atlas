package com.projectatlas.inquiry.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrainDumpTests {

	@Test
	void preservesTheDecodedStringWithoutNormalization() {
		String rawText = "  Kafka\r\n한글\t😀e\u0301  ";

		BrainDump brainDump = new BrainDump(rawText);

		assertEquals(rawText, brainDump.value());
		assertFalse(brainDump.value().equals("  Kafka\n한글\t😀é  "));
	}

	@Test
	void acceptsOneAndTenThousandUnicodeCodePoints() {
		assertEquals("궁", new BrainDump("궁").value());

		String emojiBoundary = "😀".repeat(BrainDump.MAX_CODE_POINTS);
		assertEquals(BrainDump.MAX_CODE_POINTS,
				new BrainDump(emojiBoundary).value().codePointCount(0, emojiBoundary.length()));
	}

	@Test
	void rejectsMoreThanTenThousandUnicodeCodePoints() {
		String tooLong = "😀".repeat(BrainDump.MAX_CODE_POINTS + 1);

		assertThrows(InvalidBrainDumpException.class, () -> new BrainDump(tooLong));
	}

	@Test
	void rejectsNullOrWhitespaceOnlyValues() {
		assertThrows(InvalidBrainDumpException.class, () -> new BrainDump(null));
		assertThrows(InvalidBrainDumpException.class, () -> new BrainDump(" \t\r\n\u00a0"));
	}

	@Test
	void rejectsPostgresNullAndMalformedUtf16() {
		assertThrows(InvalidBrainDumpException.class, () -> new BrainDump("before\0after"));
		assertThrows(InvalidBrainDumpException.class,
				() -> new BrainDump(String.valueOf('\ud800')));
		assertThrows(InvalidBrainDumpException.class,
				() -> new BrainDump(String.valueOf('\udc00')));
	}

	@Test
	void doesNotExposeRawTextThroughToString() {
		assertEquals("BrainDump[redacted]", new BrainDump("private thought").toString());
	}

}
