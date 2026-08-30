ALTER TABLE inquiries
    DROP CONSTRAINT ck_inquiries_status;

ALTER TABLE inquiries
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_inquiries_version
        CHECK (version >= 0),
    ADD CONSTRAINT ck_inquiries_status
        CHECK (status IN ('CAPTURED', 'CLARIFYING'));

CREATE TABLE clarification_start_requests (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    inquiry_id UUID NOT NULL REFERENCES inquiries (id),
    inquiry_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_clarification_start_key_inquiry
        UNIQUE (idempotency_key, inquiry_id),
    CONSTRAINT ck_clarification_start_key
        CHECK (idempotency_key ~ '^[!-~]{1,128}$'),
    CONSTRAINT ck_clarification_start_version
        CHECK (inquiry_version >= 0)
);

CREATE TABLE clarification_turns (
    id UUID PRIMARY KEY,
    inquiry_id UUID NOT NULL REFERENCES inquiries (id),
    sequence_number INTEGER NOT NULL,
    question TEXT NOT NULL,
    reason TEXT NOT NULL,
    proposal_source VARCHAR(64) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resulting_inquiry_version BIGINT NOT NULL,
    start_idempotency_key VARCHAR(128) NOT NULL,

    CONSTRAINT uq_clarification_turn_inquiry_sequence
        UNIQUE (inquiry_id, sequence_number),
    CONSTRAINT uq_clarification_turn_start_request
        UNIQUE (start_idempotency_key),
    CONSTRAINT fk_clarification_turn_start_inquiry
        FOREIGN KEY (start_idempotency_key, inquiry_id)
        REFERENCES clarification_start_requests (idempotency_key, inquiry_id),
    CONSTRAINT ck_clarification_turn_sequence
        CHECK (sequence_number = 1),
    CONSTRAINT ck_clarification_turn_question_length
        CHECK (char_length(question) BETWEEN 1 AND 500),
    CONSTRAINT ck_clarification_turn_reason_length
        CHECK (char_length(reason) BETWEEN 1 AND 500),
    CONSTRAINT ck_clarification_turn_source
        CHECK (proposal_source ~ '^[A-Za-z0-9._-]{1,64}$'),
    CONSTRAINT ck_clarification_turn_schema_version
        CHECK (schema_version ~ '^[A-Za-z0-9._-]{1,64}$'),
    CONSTRAINT ck_clarification_turn_resulting_version
        CHECK (resulting_inquiry_version >= 1)
);
