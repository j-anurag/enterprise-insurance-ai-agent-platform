CREATE TABLE policy_documents (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    storage_reference VARCHAR(500) NOT NULL,
    extracted_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_documents_policy FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE RESTRICT
);

CREATE INDEX idx_policy_documents_policy_id ON policy_documents(policy_id);
