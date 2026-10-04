package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CariHesap;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Cari Hesap Repository
 * CariHesap entity'si için veritabanı işlemlerini yönetir.
 */
@Repository
public interface CariHesapRepository extends JpaRepository<CariHesap, Long> {
    
    /**
     * İsme göre cari hesapları ara (büyük/küçük harf duyarsız)
     */

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.sirketId = :sirketId")
    BigDecimal toplamBakiyeHesaplaBySirketId(@Param("sirketId") Long sirketId);

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.bakiye > 0 AND c.sirketId = :sirketId")
    BigDecimal toplamPozitifBakiyeBySirketId(@Param("sirketId") Long sirketId);

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.bakiye < 0 AND c.sirketId = :sirketId")
    BigDecimal toplamNegatifBakiyeBySirketId(@Param("sirketId") Long sirketId);

    Page<CariHesap> findBySirketId(Long sirketId, Pageable pageable);

    List<CariHesap> findBySirketIdAndAdContainingIgnoreCase(Long sirketId, String query);

    /**
     * Toplu seçimden gelen dışa aktarma için tenant izolasyonlu id listesi.
     * findAllById kullanmıyoruz: o yol sirketId filtresi içermez ve başka
     * şirketin cari kayıtlarını döndürebilir.
     */
    List<CariHesap> findBySirketIdAndIdIn(Long sirketId, Collection<Long> ids);

    @Query("SELECT COUNT(c) FROM CariHesap c WHERE c.sirketId = :sirketId")
    long countBySirketId(@Param("sirketId") Long sirketId);

    long countBySirketIdAndOlusturmaTarihiBetween(Long sirketId, LocalDateTime baslangic, LocalDateTime bitis);

    List<CariHesap> findBySirketIdOrderByAdAsc(Long sirketId);

    /** En yüksek borçlu 5 cari (bakiye en negatiften başlayarak). */
    List<CariHesap> findTop5BySirketIdAndBakiyeLessThanOrderByBakiyeAsc(Long sirketId, BigDecimal bakiye);

    /** En yüksek alacaklı 5 cari (bakiye en yüksekten başlayarak). */
    List<CariHesap> findTop5BySirketIdAndBakiyeGreaterThanOrderByBakiyeDesc(Long sirketId, BigDecimal bakiye);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CariHesap c WHERE c.id = :id")
    Optional<CariHesap> findByIdForUpdate(@Param("id") Long id);

    /**
     * Bakiyeyi ATOMIK olarak arttirir/azaltir (okuma-degistirme-yazma yok). Es zamanli
     * tahsilat/satis islemlerinde @Version cakismasi olusmaz.
     *
     * <p><b>flushAutomatically/clearAutomatically:</b> JPQL bulk UPDATE'i
     * Hibernate'in birinci seviye (persistence context) cache'ini GUNCELLEMEZ.
     * Ayni transaction icinde daha once yuklenmis {@code CariHesap} nesnesi
     * eski bakiyesiyle bellekte kalir ve transaction sonunda flush edilirse
     * DUZENLENMEMIS bir bakiye veritabanina yazilir (kayip guncelleme). Bu
     * yuzden degisiklikten once bekleyen SQL yazilir (flush), sonra context
     * temizlenir (clear) ve sonraki okumalar guncel degeri gorur.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE CariHesap c SET c.bakiye = COALESCE(c.bakiye, 0) + :tutar WHERE c.id = :id")
    int bakiyeArttir(@Param("id") Long id, @Param("tutar") BigDecimal tutar);

    /**
     * Sunucu tarafında aranmış, filtrelenmiş ve sayfalanmış cari listesi.
     *
     * <p>Bakiye işareti kuralı: <b>negatif = cari bize borçlu</b>. Buna göre
     * {@code bakiyeYonu='alacak'} negatif (tahsil edilecek), {@code 'borc'} pozitif
     * (ödenecek) bakiyeleri seçer. Önceden işaretler ters uygulanıyordu:
     * "Alacaklı" filtresi bakiyesi pozitif olan carileri, yani bizim borçlarımızı
     * getiriyordu.
     *
     * <p><b>ESCAPE '\\':</b> {@code AramaTemizleyici.like(...)} joker karakterleri
     * (% ve _) ters çizgiyle kaçışlar. Kaçış dizisinin çalışması için her
     * LIKE ifadesinde ESCAPE bildirimi zorunludur; aksi halde kullanıcı
     * "%" yazarak tüm tabloyu getirebilirdi.
     */
    @Query("SELECT c FROM CariHesap c WHERE c.sirketId = :sirketId " +
            "AND (:q IS NULL OR lower(c.ad) LIKE :q ESCAPE '\\' " +
            "            OR lower(c.vergiNumarasi) LIKE :q ESCAPE '\\' " +
            "            OR lower(c.telefon) LIKE :q ESCAPE '\\') " +
            "AND (:tur IS NULL OR c.tur = :tur OR c.tur = 'Her Ikisi') " +
            "AND (:bakiyeYonu IS NULL OR (:bakiyeYonu = 'alacak' AND c.bakiye < 0) OR (:bakiyeYonu = 'borc' AND c.bakiye > 0))")
    Page<CariHesap> filtreli(@Param("sirketId") Long sirketId,
                             @Param("q") String q,
                             @Param("tur") String tur,
                             @Param("bakiyeYonu") String bakiyeYonu,
                             Pageable pageable);
}
