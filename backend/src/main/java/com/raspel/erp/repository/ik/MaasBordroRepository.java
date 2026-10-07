package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.MaasBordro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaasBordroRepository extends JpaRepository<MaasBordro, Long> {

    /**
     * REDTEAM/Faz1.7 (N+1 düzeltmesi): İlişki PERSONEL LAZY yüklendiği ve
     * servis DTO dönüşümünde {@code b.getPersonel().getAd()} çağrıldığı için
     * her bordro satırı için ayrı bir SELECT atılıyordu.
     *
     * <p>Frontend {@code size=500} istediğinden tek HTTP isteği içinde
     * 1× COUNT + 1× SELECT + <b>500× SELECT personel ≈ 502 SQL sorgusu</b>
     * oluşuyordu. Bu yüzden "Maaş ve Bordro" ekranı yavaş açılıyordu.
     *
     * <p>{@code @EntityGraph} ile personel JOIN'li tek sorguda gelir:
     * <b>502 → 2 sorgu</b>. Projede aynı desen
     * {@code PersonelPuantajService} ve {@code CariHesapService}'de kullanılıyor.
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"personel"})
    Page<MaasBordro> findBySirketIdOrderByYilDescAyDesc(Long sirketId, Pageable pageable);

    /**
     * Borçlu kilitleme (SELECT ... FOR UPDATE). Bordro ödemesinde "zaten ödendi mi"
     * kontrolünün kilit ALINDIKTAN SONRA yapılması gerekiyor; aksi halde iki
     * eşzamanlı ödeme isteği ikisini de geçip kasadan çift düşüyordu.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM MaasBordro b WHERE b.id = :id")
    Optional<MaasBordro> findByIdForUpdate(@Param("id") Long id);

    /** Toplu üretimde aynı personel/ay için mükerrer bordro engeli. */
    boolean existsBySirketIdAndYilAndAyAndPersonelId(Long sirketId, Integer yil, Integer ay, Long personelId);

    /** Yıl içinde ÖNCEKİ aylarda oluşan gelir vergisi matrahı (kümülatif dilim için). */
    @Query("SELECT COALESCE(SUM(b.gelirVergisiMatrahi), 0) FROM MaasBordro b " +
           "WHERE b.sirketId = :sirketId AND b.personel.id = :personelId " +
           "AND b.yil = :yil AND b.ay < :ay")
    java.math.BigDecimal kumulatifMatrah(@Param("sirketId") Long sirketId,
                                         @Param("personelId") Long personelId,
                                         @Param("yil") Integer yil,
                                         @Param("ay") Integer ay);

    /** Sunucu taraflı filtre + sayfalama (personel adı, dönem, durum, ödeme durumu). */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"personel"})
    @Query("SELECT b FROM MaasBordro b JOIN b.personel p WHERE b.sirketId = :sirketId " +
           "AND (:yil IS NULL OR b.yil = :yil) " +
           "AND (:ay IS NULL OR b.ay = :ay) " +
           "AND (:durum IS NULL OR b.durum = :durum) " +
           "AND (:odemeDurumu IS NULL OR b.odemeDurumu = :odemeDurumu) " +
           "AND (:q IS NULL OR LOWER(CONCAT(COALESCE(p.ad,''), ' ', COALESCE(p.soyad,''))) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY b.yil DESC, b.ay DESC, p.ad")
    Page<MaasBordro> filtreliGetir(@Param("sirketId") Long sirketId,
                                   @Param("yil") Integer yil,
                                   @Param("ay") Integer ay,
                                   @Param("durum") String durum,
                                   @Param("odemeDurumu") String odemeDurumu,
                                   @Param("q") String q,
                                   Pageable pageable);

    /** Filtreli KPI özeti: [toplamBrut, toplamKesinti, toplamNet, odenenNet, adet]. */
    @Query("SELECT COALESCE(SUM(b.brutMaas),0), COALESCE(SUM(b.kesintiler),0), COALESCE(SUM(b.netMaas),0), " +
           "COALESCE(SUM(CASE WHEN b.odemeDurumu = 'ODENDI' THEN b.netMaas ELSE 0 END),0), COUNT(b) " +
           "FROM MaasBordro b JOIN b.personel p WHERE b.sirketId = :sirketId " +
           "AND (:yil IS NULL OR b.yil = :yil) " +
           "AND (:ay IS NULL OR b.ay = :ay) " +
           "AND (:durum IS NULL OR b.durum = :durum) " +
           "AND (:odemeDurumu IS NULL OR b.odemeDurumu = :odemeDurumu) " +
           "AND (:q IS NULL OR LOWER(CONCAT(COALESCE(p.ad,''), ' ', COALESCE(p.soyad,''))) LIKE LOWER(CONCAT('%', :q, '%')))")
    Object[] ozet(@Param("sirketId") Long sirketId,
                  @Param("yil") Integer yil,
                  @Param("ay") Integer ay,
                  @Param("durum") String durum,
                  @Param("odemeDurumu") String odemeDurumu,
                  @Param("q") String q);
}
