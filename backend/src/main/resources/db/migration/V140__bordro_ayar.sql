-- Bordro hesaplama parametreleri (yıl bazlı): asgari ücret, SGK/işsizlik oranları,
-- damga vergisi oranı ve gelir vergisi dilimleri (JSON). Oranlar yıl başında güncellenir.
CREATE TABLE IF NOT EXISTS ik.bordro_ayar (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    yil INTEGER NOT NULL,
    asgari_ucret NUMERIC(19,2) NOT NULL DEFAULT 0,
    sgk_isci_orani NUMERIC(5,2) NOT NULL DEFAULT 14,
    issizlik_isci_orani NUMERIC(5,2) NOT NULL DEFAULT 1,
    sgk_isveren_orani NUMERIC(5,2) NOT NULL DEFAULT 20.50,
    issizlik_isveren_orani NUMERIC(5,2) NOT NULL DEFAULT 2,
    damga_orani NUMERIC(5,3) NOT NULL DEFAULT 0.759,
    gelir_vergisi_dilimleri TEXT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    CONSTRAINT uq_bordro_ayar_sirket_yil UNIQUE (sirket_id, yil)
);
