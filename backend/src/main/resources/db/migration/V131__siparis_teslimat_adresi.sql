-- Saha siparişlerinde teslimat adresi alanı (önce istemci gönderiyordu ama sunucuda alan yoktu).
ALTER TABLE siparis.siparis ADD COLUMN IF NOT EXISTS teslimat_adresi VARCHAR(500);
