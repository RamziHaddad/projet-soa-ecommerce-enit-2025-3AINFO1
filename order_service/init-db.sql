-- Configuration initiale de la base de données Order Service
-- Ce script est exécuté automatiquement lors de la création du conteneur PostgreSQL

-- Créer les extensions nécessaires
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Créer les tables principales
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Créer la table des articles de commande
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);

-- Créer la table de l'état des sagas
CREATE TABLE IF NOT EXISTS saga_states (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    saga_id UUID DEFAULT uuid_generate_v4(),
    current_step VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    compensation_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id)
);

-- Créer les index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_saga_states_order_id ON saga_states(order_id);
CREATE INDEX IF NOT EXISTS idx_saga_states_status ON saga_states(status);

-- Insérer quelques données de test
INSERT INTO orders (order_number, customer_id, status, total_amount, shipping_address) VALUES
    ('ORD-TEST-001', 123, 'PENDING', 109.97, '123 Rue Test, 75001 Paris'),
    ('ORD-TEST-002', 456, 'COMPLETED', 89.99, '456 Avenue Test, 69001 Lyon')
ON CONFLICT (order_number) DO NOTHING;

-- Insérer des articles de commande de test
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) 
SELECT o.id, 1, 2, 29.99, 59.98 FROM orders o WHERE o.order_number = 'ORD-TEST-001'
UNION ALL
SELECT o.id, 2, 1, 49.99, 49.99 FROM orders o WHERE o.order_number = 'ORD-TEST-001'
ON CONFLICT DO NOTHING;

-- Insérer des états de saga de test
INSERT INTO saga_states (order_id, current_step, status) 
SELECT id, 'INITIALIZED', 'IN_PROGRESS' FROM orders WHERE order_number = 'ORD-TEST-001'
ON CONFLICT (order_id) DO NOTHING;

-- Fonction pour mettre à jour automatiquement updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Créer les triggers pour updated_at
CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_saga_states_updated_at BEFORE UPDATE ON saga_states
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
