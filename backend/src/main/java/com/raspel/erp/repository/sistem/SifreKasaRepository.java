package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SifreKasa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SifreKasaRepository extends JpaRepository<SifreKasa, Long> {

    /**
     * Kasa listesi. Tenant filtresi SORGU SEVIYESINDE uygulanir: servis
     * katmanindaki kontrol atlanirsa bile baska sirketin kaydi listelenmez
     * (defense in depth).
     *
     * @param kapsam null ise tum kapsamlar
     * @param aktif true=aktif, false=arsiv, null=tumu
     */
    @Query("SELECT k FROM SifreKasa k WHERE k.sirketId = :sirketId "
            + "AND (:kapsam IS NULL OR k.kapsam = :kapsam) "
            + "AND (:aktif IS NULL OR k.aktif = :aktif) "
            + "AND (:kategori IS NULL OR k.kategori = :kategori) "
            + "AND (:q IS NULL OR LOWER(k.baslik) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "     OR LOWER(COALESCE(k.kullaniciAdi, '')) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "     OR LOWER(COALESCE(k.url, '')) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY k.baslik ASC")
    List<SifreKasa> filtreli(@Param("sirketId") Long sirketId,
                            @Param("kapsam") String kapsam,
                            @Param("aktif") Boolean aktif,
                            @Param("kategori") String kategori,
                            @Param("q") String q);

    /**
     * Kullanicinin KISISEL kayitlari. Diger kullanicilarin kisisel
     * kayitlari ASLA donmez.
     */
    @Query("SELECT k FROM SifreKasa k WHERE k.sirketId = :sirketId "
            + "AND k.kullaniciId = :kullaniciId "
            + "AND (:aktif IS NULL OR k.aktif = :aktif) "
            + "ORDER BY k.baslik ASC")
    List<SifreKasa> kisiselKayitlar(@Param("sirketId") Long sirketId,
                                    @Param("kullaniciId") Long kullaniciId,
                                    @Param("aktif") Boolean aktif);

    // --- Ozet sayaclar (aktif kayitlar; arsiv haric) ---

    long countBySirketIdAndAktifTrueAndKapsam(Long sirketId, String kapsam);

    long countBySirketIdAndAktifTrueAndKapsamAndSifreGorunurlugu(Long sirketId,
                                                                 String kapsam,
                                                                 String sifreGorunurlugu);

    long countBySirketIdAndAktifFalse(Long sirketId);

    /**
     * Suresi dolmus aktif kayitlar: gecerlilik_gun dolu ve degisim tarihi
     * eski. NULL tarihli kayitlar sayilmaz (referans noktasi yoksa
     * "dolmus" demek yaniltici olur).
     */
    @Query("SELECT count(k) FROM SifreKasa k WHERE k.sirketId = :sirketId "
            + "AND k.aktif = true "
            + "AND k.gecerlilikGun IS NOT NULL AND k.gecerlilikGun > 0 "
            + "AND k.sifreDegisimTarihi IS NOT NULL "
            + "AND k.sifreDegisimTarihi <= :sureDolmaEsigi")
    long countSuresiBitti(@Param("sirketId") Long sirketId,
                          @Param("sureDolmaEsigi") LocalDateTime sureDolmaEsigi);

    /** Uyarilacak kayitlar: 14 gun veya daha az kalan (dolmus dahil). */
    @Query("SELECT count(k) FROM SifreKasa k WHERE k.sirketId = :sirketId "
            + "AND k.aktif = true "
            + "AND k.gecerlilikGun IS NOT NULL AND k.gecerlilikGun > 0 "
            + "AND k.sifreDegisimTarihi IS NOT NULL "
            + "AND k.sifreDegisimTarihi <= :uyariEsigi")
    long countUyari(@Param("sirketId") Long sirketId,
                    @Param("uyariEsigi") LocalDateTime uyariEsigi);

    /** Benzersizlik: ayni sirket icinde ayni baslikli aktif kayitlar. */
    boolean existsBySirketIdAndBaslikIgnoreCaseAndAktifTrueAndIdNot(Long sirketId,
                                                                    String baslik,
                                                                    Long id);

    boolean existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(Long sirketId, String baslik);

    List<SifreKasa> findByIdAndSirketId(Long id, Long sirketId);
}