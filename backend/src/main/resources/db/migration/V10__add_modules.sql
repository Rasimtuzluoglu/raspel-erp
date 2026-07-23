CREATE SCHEMA IF NOT EXISTS finans;
CREATE SCHEMA IF NOT EXISTS ticaret;
CREATE SCHEMA IF NOT EXISTS envanter;
CREATE SCHEMA IF NOT EXISTS ik;

CREATE TABLE finans.butce (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    yil INTEGER NOT NULL,
    ay INTEGER NOT NULL,
    tutar NUMERIC(19,2) NOT NULL,
    tur VARCHAR(10) NOT NULL CHECK(tur IN ('GELIR','GIDER')),
    kategori VARCHAR(100),
    sirket_id BIGINT NOT NULL,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE finans.masraf (
    id BIGSERIAL PRIMARY KEY,
    tarih DATE NOT NULL,
    tutar NUMERIC(19,2) NOT NULL,
    aciklama VARCHAR(500),
    kategori VARCHAR(100),
    cari_hesap_id BIGINT,
    belge_no VARCHAR(50),
    sirket_id BIGINT NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE ticaret.fiyat_listesi (
    id BIGSERIAL PRIMARY KEY,
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id),
    alis_fiyat NUMERIC(19,2),
    satis_fiyat NUMERIC(19,2) NOT NULL,
    gecerli_baslangic DATE,
    gecerli_bitis DATE,
    sirket_id BIGINT NOT NULL,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE ticaret.iade (
    id BIGSERIAL PRIMARY KEY,
    fatura_id BIGINT,
    tarih DATE NOT NULL,
    tutar NUMERIC(19,2) NOT NULL,
    aciklama VARCHAR(500),
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    sirket_id BIGINT NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE envanter.stok_seri (
    id BIGSERIAL PRIMARY KEY,
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id),
    seri_no VARCHAR(100) NOT NULL,
    lot_no VARCHAR(100),
    son_kullanma_tarihi DATE,
    stok_hareket_id BIGINT REFERENCES stok.stok_hareket(id),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE envanter.stok_sayim (
    id BIGSERIAL PRIMARY KEY,
    tarih DATE NOT NULL,
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id),
    beklenen_miktar NUMERIC(19,2) NOT NULL DEFAULT 0,
    sayilan_miktar NUMERIC(19,2) NOT NULL DEFAULT 0,
    fark NUMERIC(19,2),
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    sirket_id BIGINT NOT NULL,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE ik.maas_bordro (
    id BIGSERIAL PRIMARY KEY,
    personel_id BIGINT NOT NULL REFERENCES personel.personel(id),
    yil INTEGER NOT NULL,
    ay INTEGER NOT NULL,
    brut_maas NUMERIC(19,2) NOT NULL,
    kesintiler NUMERIC(19,2) NOT NULL DEFAULT 0,
    net_maas NUMERIC(19,2) NOT NULL,
    odeme_tarihi DATE,
    sirket_id BIGINT NOT NULL,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE ik.vardiya (
    id BIGSERIAL PRIMARY KEY,
    personel_id BIGINT NOT NULL REFERENCES personel.personel(id),
    tarih DATE NOT NULL,
    baslangic TIME NOT NULL,
    bitis TIME NOT NULL,
    tur VARCHAR(10) NOT NULL CHECK(tur IN ('SABAH','AKSAM','GECE')),
    sirket_id BIGINT NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL
);
