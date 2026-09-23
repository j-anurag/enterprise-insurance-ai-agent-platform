CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) NOT NULL,
    policy_id BIGINT NOT NULL,
    description TEXT NOT NULL,
    incident_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_claims_claim_number UNIQUE (claim_number),
    CONSTRAINT fk_claims_policy FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE RESTRICT
);

CREATE INDEX idx_claims_policy_id ON claims (policy_id);
CREATE INDEX idx_claims_claim_number ON claims (claim_number);
