CREATE TABLE inquiries (
    id UUID PRIMARY KEY,
    raw_text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    creation_idempotency_key VARCHAR(128) NOT NULL,

    CONSTRAINT uq_inquiries_creation_idempotency_key
        UNIQUE (creation_idempotency_key),
    CONSTRAINT ck_inquiries_raw_text_length
        CHECK (char_length(raw_text) BETWEEN 1 AND 10000),
    CONSTRAINT ck_inquiries_status
        CHECK (status = 'CAPTURED'),
    CONSTRAINT ck_inquiries_creation_idempotency_key
        CHECK (creation_idempotency_key ~ '^[!-~]{1,128}$')
);
