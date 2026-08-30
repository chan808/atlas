package com.projectatlas.inquiry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class FlywayV2UpgradeTests {

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:18-alpine"));

	@Test
	void upgradesPopulatedV1WithoutChangingCapturedInquiryData() throws SQLException {
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.target(MigrationVersion.fromVersion("1"))
				.load()
				.migrate();

		UUID inquiryId = UUID.fromString("f65d30b7-d778-4709-a3c1-b4ead4778d8f");
		String rawText = "  V1 원문은 그대로여야 해요.\nemoji: 🧭  ";
		String idempotencyKey = "v1-existing-capture-key";
		Instant createdAt = Instant.parse("2026-08-29T23:45:12.123456Z");
		insertV1Inquiry(inquiryId, rawText, createdAt, idempotencyKey);

		CapturedRow before = readCapturedRow(inquiryId);

		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.load()
				.migrate();

		CapturedRow after = readCapturedRow(inquiryId);
		assertEquals(before, after);
		assertEquals(0L, readVersion(inquiryId));
		assertEquals(0L, countRows("clarification_start_requests"));
		assertEquals(0L, countRows("clarification_turns"));
		assertTurnCannotReferenceAnotherRequestsInquiry(inquiryId);
	}

	private void insertV1Inquiry(
			UUID inquiryId,
			String rawText,
			Instant createdAt,
			String idempotencyKey) throws SQLException {
		try (Connection connection = connection();
				PreparedStatement statement = connection.prepareStatement("""
						INSERT INTO inquiries (
								id, raw_text, status, created_at, creation_idempotency_key
						) VALUES (?, ?, 'CAPTURED', ?, ?)
						""")) {
			statement.setObject(1, inquiryId);
			statement.setString(2, rawText);
			statement.setObject(3, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
			statement.setString(4, idempotencyKey);
			statement.executeUpdate();
		}
	}

	private CapturedRow readCapturedRow(UUID inquiryId) throws SQLException {
		try (Connection connection = connection();
				PreparedStatement statement = connection.prepareStatement("""
						SELECT id, raw_text, status, created_at, creation_idempotency_key
						FROM inquiries
						WHERE id = ?
						""")) {
			statement.setObject(1, inquiryId);
			try (ResultSet result = statement.executeQuery()) {
				result.next();
				return new CapturedRow(
						result.getObject("id", UUID.class),
						result.getString("raw_text"),
						result.getString("status"),
						result.getObject("created_at", OffsetDateTime.class).toInstant(),
						result.getString("creation_idempotency_key"));
			}
		}
	}

	private long readVersion(UUID inquiryId) throws SQLException {
		try (Connection connection = connection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT version FROM inquiries WHERE id = ?")) {
			statement.setObject(1, inquiryId);
			try (ResultSet result = statement.executeQuery()) {
				result.next();
				return result.getLong(1);
			}
		}
	}

	private void assertTurnCannotReferenceAnotherRequestsInquiry(UUID requestInquiryId)
			throws SQLException {
		UUID otherInquiryId = UUID.fromString("7cf18702-00a7-4816-8ac9-c1b19def032a");
		String startKey = "owner-bound-start-key";
		try (Connection connection = connection()) {
			try (PreparedStatement inquiry = connection.prepareStatement("""
					INSERT INTO inquiries (
							id, raw_text, status, created_at, creation_idempotency_key
					) VALUES (?, 'other', 'CAPTURED', ?, 'other-capture-key')
					""")) {
				inquiry.setObject(1, otherInquiryId);
				inquiry.setObject(2, OffsetDateTime.now(ZoneOffset.UTC));
				inquiry.executeUpdate();
			}
			try (PreparedStatement start = connection.prepareStatement("""
					INSERT INTO clarification_start_requests (
							idempotency_key, inquiry_id, inquiry_version, created_at
					) VALUES (?, ?, 0, ?)
					""")) {
				start.setString(1, startKey);
				start.setObject(2, requestInquiryId);
				start.setObject(3, OffsetDateTime.now(ZoneOffset.UTC));
				start.executeUpdate();
			}
		}

		assertThrows(SQLException.class, () -> {
			try (Connection connection = connection();
					PreparedStatement turn = connection.prepareStatement("""
							INSERT INTO clarification_turns (
									id, inquiry_id, sequence_number, question, reason,
									proposal_source, schema_version, created_at,
									resulting_inquiry_version, start_idempotency_key
							) VALUES (?, ?, 1, 'question', 'reason', 'source-v1',
									'schema-v1', ?, 1, ?)
							""")) {
				turn.setObject(1, UUID.randomUUID());
				turn.setObject(2, otherInquiryId);
				turn.setObject(3, OffsetDateTime.now(ZoneOffset.UTC));
				turn.setString(4, startKey);
				turn.executeUpdate();
			}
		});
	}

	private long countRows(String tableName) throws SQLException {
		String query = switch (tableName) {
			case "clarification_start_requests" -> "SELECT count(*) FROM clarification_start_requests";
			case "clarification_turns" -> "SELECT count(*) FROM clarification_turns";
			default -> throw new IllegalArgumentException("unexpected table");
		};
		try (Connection connection = connection();
				PreparedStatement statement = connection.prepareStatement(query);
				ResultSet result = statement.executeQuery()) {
			result.next();
			return result.getLong(1);
		}
	}

	private Connection connection() throws SQLException {
		return DriverManager.getConnection(
				POSTGRES.getJdbcUrl(),
				POSTGRES.getUsername(),
				POSTGRES.getPassword());
	}

	private record CapturedRow(
			UUID id,
			String rawText,
			String status,
			Instant createdAt,
			String idempotencyKey) {
	}

}
