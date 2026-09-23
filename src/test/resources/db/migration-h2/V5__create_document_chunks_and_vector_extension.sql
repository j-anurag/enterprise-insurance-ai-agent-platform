CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding GEOMETRY NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES policy_documents(id) ON DELETE RESTRICT
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
