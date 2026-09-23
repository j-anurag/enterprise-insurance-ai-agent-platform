CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_number VARCHAR(50) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customers_customer_number UNIQUE (customer_number),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE INDEX idx_customers_customer_number ON customers (customer_number);
CREATE INDEX idx_customers_email ON customers (email);
