-- Create required databases and user
CREATE DATABASE payment_db;
CREATE DATABASE order_db;

CREATE USER payment_user WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE payment_db TO payment_user;

-- Ensure payment_user can create objects in public schema of payment_db
\c payment_db
GRANT ALL PRIVILEGES ON SCHEMA public TO payment_user;
ALTER DATABASE payment_db OWNER TO payment_user;
