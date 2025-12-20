-- Inventory Service Database Schema
-- Version 1: Initial Setup

-- Create inventory table with two-counter pattern
CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(200),
    category VARCHAR(100),
    available_quantity INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    unit_price DECIMAL(10, 2),
    version INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create reservations table
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id UUID PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    product_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES inventory(product_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON reservations(order_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservations_product_id ON reservations(product_id);

-- Insert sample inventory data
INSERT INTO inventory (product_id, product_name, category, available_quantity, reserved_quantity, unit_price, version) 
VALUES 
    ('PROD-001', 'Laptop Dell XPS 15', 'Electronics', 100, 0, 1299.99, 0),
    ('PROD-002', 'iPhone 15 Pro', 'Electronics', 50, 0, 999.99, 0),
    ('PROD-003', 'Nike Air Max Shoes', 'Fashion', 200, 0, 129.99, 0),
    ('PROD-004', 'Samsung Galaxy Watch', 'Electronics', 75, 0, 299.99, 0),
    ('PROD-005', 'Adidas Running Shirt', 'Fashion', 150, 0, 49.99, 0)
ON CONFLICT (product_id) DO NOTHING;
