-- =====================================================================
-- V154: Aynı şirkette mükerrer vergi numarasını engelle (Faz 1.2)
--
-- DENETIM BULGUSU:
--   `cari.cari_hesap.vergi_numarasi` üzerinde benzersizlik kısıtı yoktu.
--   Aynı vergi numarasıyla birden çok cari açılabiliyordu (mükerrer kayıt,
--   yanlış cari eşleşmesi, finansal karışıklık).
--
-- COZUM:
--   1) Mevcut mükerrerleri temizle: aynı (sirket_id, lower(vergi_numarasi))
--      grubunda en eski kayıt (MIN id) korunur, diğerlerinin vergi_numarasi
--      NULL'lanır. Boş/null değerler kapsam dışıdır.
--   2) Kısmi UNIQUE index: yalnızca dolu vergi numaraları için.
--
-- Idempotent: index IF NOT EXISTS ile; tekrar çalıştırılabilir.
-- =====================================================================

DO $$
DECLARE
    v_temizlenen integer;
BEGIN
    UPDATE cari.cari_hesap c
    SET vergi_numarasi = NULL
    WHERE c.vergi_numarasi IS NOT NULL
      AND c.vergi_numarasi <> ''
      AND c.id NOT IN (
          SELECT MIN(c2.id)
          FROM cari.cari_hesap c2
          WHERE c2.vergi_numarasi IS NOT NULL
            AND c2.vergi_numarasi <> ''
          GROUP BY c2.sirket_id, lower(c2.vergi_numarasi)
      );
    GET DIAGNOSTICS v_temizlenen = ROW_COUNT;
    RAISE NOTICE 'V154: % mükerrer vergi numarası NULL olarak temizlendi', v_temizlenen;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_cari_hesap_sirket_vkn
    ON cari.cari_hesap (sirket_id, lower(vergi_numarasi))
    WHERE vergi_numarasi IS NOT NULL AND vergi_numarasi <> '';
