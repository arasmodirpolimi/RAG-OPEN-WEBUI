CREATE TABLE documents (
    id            UUID          PRIMARY KEY,
    collection    VARCHAR(63)   NOT NULL,
    file_name     VARCHAR(500)  NOT NULL,
    mime_type     VARCHAR(200),
    sha256        CHAR(64)      NOT NULL,
    storage_key   VARCHAR(1000) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMPTZ   NOT NULL,
    indexed_at    TIMESTAMPTZ,
    CONSTRAINT uq_documents_collection_sha256 UNIQUE (collection, sha256)
);

CREATE INDEX idx_documents_collection ON documents (collection, created_at DESC);
