package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.Stok;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.raspel.erp.entity.sistem.Not;

@Repository
public interface StokRepository extends JpaRepository<Stok, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stok s WHERE s.id = :id")
    Optional<Stok> findByIdForUpdate(@Param("id") Long id);

    Page<Stok> findBySirketIdOrderByAd(Long sirketId, Pageable pageable);
    List<Stok> findBySirketIdOrderByAd(Long sirketId);    List<Stok> findBySirketIdAndAdContainingIgnoreCase(Long sirketId, String q);
    List<Stok> findBySirketIdAndBarkod(Long sirketId, String barkod);
    Optional<Stok> findBySirketIdAndStokKodu(Long sirketId, String stokKodu);

    /**
     * Satış ekranı (POS / yeni satış) için yazarken ürün önerisi.
     *
     * <p>Önceki {@code findBySirketIdAndAdContainingIgnoreCase} kullanılamaz:
     * {@code LIKE '%q%'} indeks kullanamaz ve LİMİT YOKTUR — "a" gibi tek harf
     * binlerce satır döndürür. Burada önek eşleşmesi (prefix) de denenir; ilk
     * harfler indeksli olduğu için büyük katalogda da hızlıdır. LIMIT
     * {@link Pageable} ile gelir (native sorguda {@code LIMIT :param} yerine
     * burada yazılması hem taşınabilir hem testte H2'de çalışır).
     *
     * <p>Sıralama önceliği (tam eşleşme → ad öneki → içinde geçen) servis
     * tarafında uygulanır; sonuç zaten en fazla 50 kayıttır.
     */
    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId AND ("
            + "  s.barkod = :q"
            + "  OR s.stokKodu = :q"
            + "  OR LOWER(s.ad) LIKE :oneki"
            + "  OR LOWER(s.stokKodu) LIKE :oneki"
            + "  OR LOWER(s.ad) LIKE :icerideki"
            + ") ORDER BY s.ad ASC")
    List<Stok> satisOnerileri(@Param("sirketId") Long sirketId,
                              @Param("q") String q,
                              @Param("oneki") String oneki,
                              @Param("icerideki") String icerideki,
                              Pageable pageable);

    /** En cok satanlar gibi toplu akislarda tek sorguda stok kodu eslesmesi. */
    List<Stok> findBySirketIdAndStokKoduIn(Long sirketId, java.util.Collection<String> stokKodlari);
    boolean existsBySirketIdAndBarkod(Long sirketId, String barkod);

    /** Otomatik barkod uretimi icin sirketteki en buyuk 869'lu EAN-13 barkod. */
    @Query("SELECT MAX(s.barkod) FROM Stok s WHERE s.sirketId = :sirketId " +
            "AND s.barkod LIKE '869%' AND LENGTH(s.barkod) = 13")
    String maxEan13Barkod(@Param("sirketId") Long sirketId);

    long countBySirketId(Long sirketId);

    @Query("SELECT SUM(s.miktar) FROM Stok s WHERE s.sirketId = :sirketId")
    BigDecimal toplamMiktarBySirketId(@Param("sirketId") Long sirketId);

    /**
     * Stok degeri (miktar x birim fiyat) tek sorguda hesaplanir. Tum stoklarin
     * entity olarak yuklenmesini (bellek + N+1) onler.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(s.miktar, 0) * COALESCE(s.fiyat, 0)), 0) " +
           "FROM Stok s WHERE s.sirketId = :sirketId")
    BigDecimal toplamStokDegeriBySirketId(@Param("sirketId") Long sirketId);

    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId AND s.minMiktar IS NOT NULL AND s.miktar <= s.minMiktar ORDER BY s.miktar ASC")
    List<Stok> kritikStoklar(Long sirketId);

    /** Kritik stoklar, DB'de sayfalanarak doner (tumunu cekip Java'da kesmek yerine). */
    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId AND s.minMiktar IS NOT NULL AND s.miktar <= s.minMiktar ORDER BY s.miktar ASC")
    List<Stok> kritikStoklar(Long sirketId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Stok s WHERE s.sirketId = :sirketId AND s.minMiktar IS NOT NULL AND s.miktar <= s.minMiktar")
    long countKritikStokBySirketId(@Param("sirketId") Long sirketId);

    /**
     * Sunucu tarafı stok listesi (arama + kategori/marka/üretim tipi + fiyat aralığı).
     *
     * <p>`kategori` ve `stokGrubu` karşılaştırmaları `lower(...)` ile yapılır.
     * Önceden birebir `=` idi ve büyük/küçük harfe duyarlıydı; aynı kavramın
     * farklı yazımları ("Gıda" / "gıda") kullanıcıya SESSİZCE boş sonuç
     * döndürüyordu. V146 ile eklenen fonksiyonel indeksler bu karşılaştırmayı
     * destekler.
     *
     * <p><b>ESCAPE '\\':</b> {@code AramaTemizleyici} joker karakterleri (% ve _)
     * ters çizgiyle kaçışlar; kaçışın çalışması için ESCAPE bildirimi zorunludur.
     */
    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId " +
            "AND (:q IS NULL OR lower(s.ad) LIKE :q ESCAPE '\\' " +
            "            OR lower(s.stokKodu) LIKE :q ESCAPE '\\' " +
            "            OR lower(s.barkod) LIKE :q ESCAPE '\\') " +
            "AND (:kategori IS NULL OR lower(s.kategori) = :kategori) " +
            "AND (:marka IS NULL OR lower(s.marka) LIKE :marka ESCAPE '\\') " +
            "AND (:stokGrubu IS NULL OR lower(s.stokGrubu) = :stokGrubu) " +
            "AND (:minFiyat IS NULL OR s.satisFiyati >= :minFiyat OR s.fiyat >= :minFiyat) " +
            "AND (:maxFiyat IS NULL OR s.satisFiyati <= :maxFiyat OR s.fiyat <= :maxFiyat) " +
            "AND (:depoId IS NULL OR EXISTS (SELECT ds FROM DepoStok ds WHERE ds.stokId = s.id AND ds.depoId = :depoId))")
    Page<Stok> filtreli(@Param("sirketId") Long sirketId,
                        @Param("q") String q,
                        @Param("kategori") String kategori,
                        @Param("marka") String marka,
                        @Param("stokGrubu") String stokGrubu,
                        @Param("minFiyat") BigDecimal minFiyat,
                        @Param("maxFiyat") BigDecimal maxFiyat,
                        @Param("depoId") Long depoId,
                        Pageable pageable);

    /**
     * Sınıflandırma değerleri ve ürün sayıları (şirket geneli).
     *
     * <p>Stoklar ekranındaki grup çipleri ve toplu fiyat hedef listesi ÖNCEDEN
     * yalnızca yüklenmiş SAYFA satırlarından hesaplanıyordu: çipler sayfa
     * değişince kayboluyor, grup başlığındaki sayaç yanlış oluyor ve 7. sayfadaki
     * bir gruba hiçbir zaman ulaşılamıyordu. Bu sorgu tüm katalogdan döner.
     */
    @Query("SELECT COALESCE(kategori, '') AS deger, count(*) AS adet "
            + "FROM Stok s WHERE s.sirketId = :sirketId AND s.kategori IS NOT NULL "
            + "GROUP BY s.kategori ORDER BY s.kategori ASC")
    List<Object[]> kategoriDagilimi(@Param("sirketId") Long sirketId);

    @Query("SELECT COALESCE(stokGrubu, '') AS deger, count(*) AS adet "
            + "FROM Stok s WHERE s.sirketId = :sirketId AND s.stokGrubu IS NOT NULL "
            + "GROUP BY s.stokGrubu ORDER BY s.stokGrubu ASC")
    List<Object[]> stokGrubuDagilimi(@Param("sirketId") Long sirketId);
}
