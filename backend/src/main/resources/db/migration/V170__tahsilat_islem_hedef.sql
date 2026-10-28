-- =====================================================================
-- V170: Tahsilat işlem (batch) modeli + aylık tahsilat hedefi
--
-- Amaç:
--   - Tek bir tahsilat girişinde oluşan N cari hareketi (fatura dağıtımı +
--     fazla/avans) tek bir "işlem" altında birleştirmek ve gerekçeli olarak
--     bütün halinde geri alabilmek.
--   - Şirket + ay bazında tahsilat hedefi tutmak (performans ölçümü).
--
-- Legacy backfill:
--   Mevcut her TAHSILAT hareketi tek satırlık bir işlem olarak eklenir
--   (legacy_hareket_id = hareket.id, unique). Böylece geçmiş kayıtlar yeni
--   işlem geçmişinde görünür ve tek tek geri alınabilir.
--
-- Idempotent: tablo/kolon/indeks IF NOT EXISTS, backfill NOT EXISTS
-- koşuluyla yalnızca bağlanmamış hareketler için çalışır.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Tahsilat işlem tablosu
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS finans.tahsilat_islem (
    id                        BIGSERIAL PRIMARY KEY,
    sirket_id                 BIGINT       NOT NULL,
    cari_id                   BIGINT       NOT NULL,
    tarih                     DATE         NOT NULL,
    tutar                     NUMERIC(19,2) NOT NULL,
    odeme_yontemi             VARCHAR(20),
    kasa_id                   BIGINT,
    banka_id                  BIGINT,
    durum                     VARCHAR(20)  NOT NULL DEFAULT 'AKTIF',
    iptal_gerekce             VARCHAR(500),
    iptal_eden_kullanici_id   BIGINT,
    iptal_tarihi              TIMESTAMP,
    olusturan_kullanici_id    BIGINT,
    legacy_hareket_id         BIGINT,
    olusturma_tarihi          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Aynı eski hareket iki işleme bağlanamaz.
CREATE UNIQUE INDEX IF NOT EXISTS ux_tahsilat_islem_legacy
    ON finans.tahsilat_islem (legacy_hareket_id)
    WHERE legacy_hareket_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_tahsilat_islem_sirket_tarih_durum
    ON finans.tahsilat_islem (sirket_id, tarih DESC, durum);

CREATE INDEX IF NOT EXISTS ix_tahsilat_islem_sirket_cari
    ON finans.tahsilat_islem (sirket_id, cari_id);

-- ---------------------------------------------------------------------
-- 2) cari.hareket -> işlem bağı
-- ---------------------------------------------------------------------
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS tahsilat_islem_id BIGINT;

CREATE INDEX IF NOT EXISTS ix_hareket_tahsilat_islem
    ON cari.hareket (tahsilat_islem_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_hareket_tahsilat_islem'
    ) THEN
        ALTER TABLE cari.hareket
            ADD CONSTRAINT fk_hareket_tahsilat_islem
            FOREIGN KEY (tahsilat_islem_id)
            REFERENCES finans.tahsilat_islem (id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 3) Legacy backfill (her TAHSILAT hareketi tek işlem)
--
--    Zaten iptal edilmiş (soft iptal) hareketler IPTAL durumuyla taşınır;
--    aksi halde geçmişte "aktif" gibi görünür ve ikinci kez iptal
--    edilemeyeceği için yanıltıcı olurdu.
-- ---------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
    yeni_id BIGINT;
    cozulen_sirket_id BIGINT;
BEGIN
    FOR r IN
        SELECT h.id,
               h.sirket_id,
               h.cari_hesap_id,
               h.hareket_tarihi,
               h.tutar,
               h.odeme_yontemi,
               h.iptal,
               h.iptal_tarihi,
               h.olusturma_tarihi
        FROM cari.hareket h
        WHERE h.tur = 'TAHSILAT'
          AND h.tahsilat_islem_id IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM finans.tahsilat_islem ti WHERE ti.legacy_hareket_id = h.id
          )
        ORDER BY h.id
    LOOP
        cozulen_sirket_id := COALESCE(
            r.sirket_id,
            (SELECT c.sirket_id FROM cari.cari_hesap c WHERE c.id = r.cari_hesap_id)
        );

        -- Şirketi hiç çözülemeyen eski kayıtlar yeni modele taşınamaz; atlanır.
        IF cozulen_sirket_id IS NULL THEN
            CONTINUE;
        END IF;

        INSERT INTO finans.tahsilat_islem (
            sirket_id, cari_id, tarih, tutar, odeme_yontemi,
            kasa_id, banka_id, durum, iptal_gerekce, iptal_tarihi,
            olusturan_kullanici_id, legacy_hareket_id, olusturma_tarihi
        ) VALUES (
            cozulen_sirket_id,
            r.cari_hesap_id,
            r.hareket_tarihi,
            r.tutar,
            r.odeme_yontemi,
            (SELECT kh.kasa_id FROM muhasebe.kasa_hareket kh
              WHERE kh.kaynak_tip = 'TAHSILAT' AND kh.kaynak_id = r.id
              ORDER BY kh.id LIMIT 1),
            (SELECT bh.banka_id FROM finans.banka_hareketi bh
              WHERE bh.kaynak_tip = 'TAHSILAT' AND bh.kaynak_id = r.id
              ORDER BY bh.id LIMIT 1),
            CASE WHEN COALESCE(r.iptal, false) THEN 'IPTAL' ELSE 'AKTIF' END,
            CASE WHEN COALESCE(r.iptal, false) THEN 'Geçmiş kayıt (iptal edilmiş)' ELSE NULL END,
            r.iptal_tarihi,
            NULL,
            r.id,
            COALESCE(r.olusturma_tarihi, NOW())
        )
        RETURNING id INTO yeni_id;

        UPDATE cari.hareket
           SET tahsilat_islem_id = yeni_id
         WHERE id = r.id;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- 4) Aylık tahsilat hedefi
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS finans.tahsilat_hedef (
    id                       BIGSERIAL PRIMARY KEY,
    sirket_id                BIGINT       NOT NULL,
    yil                      INTEGER      NOT NULL,
    ay                       INTEGER      NOT NULL,
    tutar                    NUMERIC(19,2) NOT NULL DEFAULT 0,
    olusturan_kullanici_id   BIGINT,
    olusturma_tarihi         TIMESTAMP    NOT NULL DEFAULT NOW(),
    guncelleme_tarihi        TIMESTAMP,
    CONSTRAINT ux_tahsilat_hedef_sirket_yil_ay UNIQUE (sirket_id, yil, ay)
);

CREATE INDEX IF NOT EXISTS ix_tahsilat_hedef_sirket_yil
    ON finans.tahsilat_hedef (sirket_id, yil);
