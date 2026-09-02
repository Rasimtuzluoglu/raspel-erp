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

    List<Stok> findAllByOrderByAd();
    Page<Stok> findBySirketIdOrderByAd(Long sirketId, Pageable pageable);
    List<Stok> findBySirketIdOrderByAd(Long sirketId);
    List<Stok> findByAdContainingIgnoreCase(String q);
    List<Stok> findByBarkod(String barkod);
    List<Stok> findByBarkodContainingIgnoreCase(String barkod);
    List<Stok> findBySirketIdAndAdContainingIgnoreCase(Long sirketId, String q);
    List<Stok> findBySirketIdAndBarkod(Long sirketId, String barkod);
    List<Stok> findBySirketIdAndBarkodContainingIgnoreCase(Long sirketId, String barkod);
    Optional<Stok> findBySirketIdAndStokKodu(Long sirketId, String stokKodu);

    long countBySirketId(Long sirketId);

    @Query("SELECT SUM(s.miktar) FROM Stok s")
    BigDecimal toplamMiktar();

    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId AND s.minMiktar IS NOT NULL AND s.miktar <= s.minMiktar ORDER BY s.miktar ASC")
    List<Stok> kritikStoklar(Long sirketId);

    @Query("SELECT COUNT(s) FROM Stok s WHERE s.sirketId = :sirketId AND s.minMiktar IS NOT NULL AND s.miktar <= s.minMiktar")
    long countKritikStokBySirketId(@Param("sirketId") Long sirketId);

    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId " +
            "AND (:q IS NULL OR lower(s.ad) LIKE lower(concat('%', :q, '%')) OR lower(s.stokKodu) LIKE lower(concat('%', :q, '%')) OR lower(s.barkod) LIKE lower(concat('%', :q, '%'))) " +
            "AND (:kategori IS NULL OR s.kategori = :kategori) " +
            "AND (:marka IS NULL OR lower(s.marka) LIKE lower(concat('%', :marka, '%'))) " +
            "AND (:stokGrubu IS NULL OR s.stokGrubu = :stokGrubu) " +
            "AND (:minFiyat IS NULL OR s.satisFiyati >= :minFiyat OR s.fiyat >= :minFiyat) " +
            "AND (:maxFiyat IS NULL OR s.satisFiyati <= :maxFiyat OR s.fiyat <= :maxFiyat)")
    Page<Stok> filtreli(@Param("sirketId") Long sirketId,
                        @Param("q") String q,
                        @Param("kategori") String kategori,
                        @Param("marka") String marka,
                        @Param("stokGrubu") String stokGrubu,
                        @Param("minFiyat") BigDecimal minFiyat,
                        @Param("maxFiyat") BigDecimal maxFiyat,
                        Pageable pageable);
}