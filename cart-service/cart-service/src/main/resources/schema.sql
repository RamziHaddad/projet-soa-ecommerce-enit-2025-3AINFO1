-- Create carts table
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT,
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    total_amount DECIMAL (19,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()

);

-- Create cart_items table
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(id),
    product_id BIGINT NOT NULL,
    name VARCHAR(255),
    unit_price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create orders_outbox table
CREATE TABLE orders_outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

