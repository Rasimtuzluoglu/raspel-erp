-- V108__crm_lead_aktivite_kampanya.sql
-- CRM genisletmesi: Lead (potansiyel musteri), Aktivite (gorusme/arama/toplanti)
-- ve Kampanya yonetimi. Lead'ler nitelikli hale geldiginde cari hesaba
-- veya firsata donusturulebilir.

CREATE TABLE IF NOT EXISTS ticaret.crm_lead (
    id               BIGSERIAL PRIMARY KEY,
    sirket_id        BIGINT NOT NULL,
    ad               VARCHAR(250) NOT NULL,
    firma            VARCHAR(250),
    email            VARCHAR(150),
    telefon          VARCHAR(30),
    kaynak           VARCHAR(50),
    durum            VARCHAR(30) NOT NULL DEFAULT 'YENI',  -- YENI, NITELIKLI, DONUSTURULDU, KAYBEDILDI
    skor             INTEGER NOT NULL DEFAULT 0,
    tahmini_deger    NUMERIC(19, 2),
    cari_hesap_id    BIGINT,
    firsat_id        BIGINT,
    kampanya_id      BIGINT,
    aciklama         VARCHAR(1000),
    kullanici_id     BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    guncelleme_tarihi TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_lead_sirket ON ticaret.crm_lead(sirket_id, durum);
CREATE INDEX IF NOT EXISTS idx_crm_lead_kampanya ON ticaret.crm_lead(kampanya_id);

CREATE TABLE IF NOT EXISTS ticaret.crm_aktivite (
    id               BIGSERIAL PRIMARY KEY,
    sirket_id        BIGINT NOT NULL,
    tur              VARCHAR(30) NOT NULL,  -- ARAMA, TOPLANTI, EMAIL, NOT, GOREV
    baslik           VARCHAR(250) NOT NULL,
    aciklama         VARCHAR(2000),
    cari_hesap_id    BIGINT,
    firsat_id        BIGINT,
    lead_id          BIGINT,
    planlanan_tarih  TIMESTAMP,
    tamamlandi       BOOLEAN NOT NULL DEFAULT false,
    tamamlanma_tarihi TIMESTAMP,
    kullanici_id     BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_crm_aktivite_sirket ON ticaret.crm_aktivite(sirket_id, tamamlandi);
CREATE INDEX IF NOT EXISTS idx_crm_aktivite_cari ON ticaret.crm_aktivite(cari_hesap_id);
CREATE INDEX IF NOT EXISTS idx_crm_aktivite_lead ON ticaret.crm_aktivite(lead_id);

CREATE TABLE IF NOT EXISTS ticaret.crm_kampanya (
    id               BIGSERIAL PRIMARY KEY,
    sirket_id        BIGINT NOT NULL,
    ad               VARCHAR(250) NOT NULL,
    tur              VARCHAR(50),           -- EMAIL, SMS, SOSYAL, ETKINLIK, DIGER
    durum            VARCHAR(30) NOT NULL DEFAULT 'PLANLANDI', -- PLANLANDI, AKTIF, TAMAMLANDI, IPTAL
    baslangic        DATE,
    bitis            DATE,
    butce            NUMERIC(19, 2),
    harcama          NUMERIC(19, 2),
    hedef_kisi       INTEGER,
    aciklama         VARCHAR(1000),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_crm_kampanya_sirket ON ticaret.crm_kampanya(sirket_id, durum);
