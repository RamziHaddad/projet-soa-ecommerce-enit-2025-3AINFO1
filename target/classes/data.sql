CREATE TABLE delivery (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- identifiant interne
    order_id INT NOT NULL,
    customer_id INT NOT NULL,           -- nouveau champ
    address VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    tracking_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


