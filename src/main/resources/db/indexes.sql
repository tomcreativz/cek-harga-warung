-- Jalankan sekali di PostgreSQL untuk mempercepat pencarian.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE UNIQUE INDEX IF NOT EXISTS idx_m_products_barcode ON m_products (barcode) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_m_products_name_trgm ON m_products USING gin (upper(name) gin_trgm_ops);
