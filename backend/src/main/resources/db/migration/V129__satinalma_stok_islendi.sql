-- Satınalma siparişi "Teslim Al" gerçek stok girişi işler; çift girişi önlemek
-- için stok_islendi bayrağı tutulur.
ALTER TABLE satinalma.satinalma_siparis ADD COLUMN IF NOT EXISTS stok_islendi BOOLEAN NOT NULL DEFAULT false;
