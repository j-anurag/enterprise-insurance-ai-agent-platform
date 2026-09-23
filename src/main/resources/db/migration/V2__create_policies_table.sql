CREATE TABLE policies (
    id BIGSERIAL PRIMARY KEY,
    policy_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_policies_policy_number UNIQUE (policy_number),
    CONSTRAINT fk_policies_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT
);

CREATE INDEX idx_policies_customer_id ON policies (customer_id);
CREATE INDEX idx_policies_policy_number ON policies (policy_number);
