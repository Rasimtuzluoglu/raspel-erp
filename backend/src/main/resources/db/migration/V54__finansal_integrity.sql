-- V54: Finansal bütünlük — hareket↔fatura bağlantısı, benzersizlik ve FK kısıtları

-- 1) cari.hareket.fatura_id: tahsilat/ödeme hareketi isteğe bağlı bir faturaya bağlanabilir
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS fatura_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_hareket_fatura ON cari.hareket(fatura_id);

-- 2) Cari hesap vergi numarası: boş değerler hariç şirket bazında benzersiz
-- (mevcut boş/çift kayıtların migration'ı kırmaması için kısmi unique index)
CREATE UNIQUE INDEX IF NOT EXISTS uk_cari_vergi_no_sirket
    ON cari.cari_hesap(sirket_id, vergi_numarasi)
    WHERE vergi_numarasi IS NOT NULL AND vergi_numarasi <> '';

-- 3) ticaret.iade.fatura_id -> fatura.fatura(id) FK (NOT VALID: mevcut satırlar doğrulanmaz,
-- yeni yazımlarda bütünlük sağlanır)
ALTER TABLE ticaret.iade ADD CONSTRAINT fk_iade_fatura
    FOREIGN KEY (fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
