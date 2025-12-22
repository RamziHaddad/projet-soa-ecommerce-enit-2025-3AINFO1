-- Attempt to drop unique index/constraint on reservations(order_id) if present (PostgreSQL)
DO $$
DECLARE
    idx RECORD;
BEGIN
    FOR idx IN
        SELECT indexname, indexdef
        FROM pg_indexes
        WHERE schemaname = 'public' AND tablename = 'reservations' AND indexdef ILIKE '%UNIQUE%' AND indexdef ILIKE '%(order_id)%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I', idx.indexname);
    END LOOP;
END$$;

-- Create a plain (non-unique) index for performance
CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON reservations(order_id);
