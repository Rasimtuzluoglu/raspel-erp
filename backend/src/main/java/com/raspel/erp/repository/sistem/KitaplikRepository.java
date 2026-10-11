package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Kitaplik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitaplikRepository extends JpaRepository<Kitaplik, Long> {

    /**
     * Gorunur kitaplik kayitlari. GORUNURLUK KURALI SORGU SEVIYESINDE uygulanir:
     *   - Kendi KISISEL kayitlari
     *   - GLOBAL + TUMU (herkes)
     *   - admin=true ise TUM GLOBAL kayitlar (SAHIS dahil)
     *
     * <p>`:q` HAM terim DEGIL, `AramaTemizleyici.like(q)` ile uretilen
     * kucuk-harfli/joker-kacisli `%terim%` DESENIDIR. (PostgreSQL'de
     * `LOWER(CONCAT('%', :q, '%'))` lower(bytea) hatasi verdigi icin
     * `LIKE :q ESCAPE '\\'` kullanilir.)
     */
    @Query("SELECT k FROM Kitaplik k WHERE k.sirketId = :sirketId "
            + "AND (:tur IS NULL OR k.tur = :tur) "
            + "AND (:aktif IS NULL OR k.aktif = :aktif) "
            + "AND (:kategori IS NULL OR k.kategori = :kategori) "
            + "AND (:q IS NULL OR LOWER(k.baslik) LIKE :q ESCAPE '\\' "
            + "     OR LOWER(COALESCE(k.url, '')) LIKE :q ESCAPE '\\' "
            + "     OR LOWER(COALESCE(k.icerik, '')) LIKE :q ESCAPE '\\') "
            + "AND ( (k.kapsam = 'KISISEL' AND k.kullaniciId = :kullaniciId) "
            + "   OR (k.kapsam = 'GLOBAL' AND :admin = true) "
            + "   OR (k.kapsam = 'GLOBAL' AND k.gorunurluk = 'TUMU') ) "
            + "ORDER BY k.baslik ASC")
    List<Kitaplik> gorunurKayitlar(@Param("sirketId") Long sirketId,
                                   @Param("kullaniciId") Long kullaniciId,
                                   @Param("admin") boolean admin,
                                   @Param("tur") String tur,
                                   @Param("aktif") Boolean aktif,
                                   @Param("kategori") String kategori,
                                   @Param("q") String q);

    /** Arsiv/sayim icin basit tenant sorgusu. */
    long countBySirketIdAndAktifFalse(Long sirketId);

    /** KISISEL ayni baslik kontrolu (kullanici bazinda). */
    boolean existsBySirketIdAndKullaniciIdAndTurAndBaslikIgnoreCaseAndAktifTrue(
            Long sirketId, Long kullaniciId, String tur, String baslik);

    /** GLOBAL ayni baslik kontrolu (sirket geneli). */
    boolean existsBySirketIdAndKapsamAndTurAndBaslikIgnoreCaseAndAktifTrue(
            Long sirketId, String kapsam, String tur, String baslik);
}