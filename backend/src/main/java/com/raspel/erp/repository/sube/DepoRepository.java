package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Depo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepoRepository extends JpaRepository<Depo, Long> {
    Page<Depo> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Depo> findBySirketIdAndAktifTrue(Long sirketId);
    boolean existsBySirketIdAndAdIgnoreCase(Long sirketId, String ad);

    /** KPI özeti: şirketin toplam depo sayısı. */
    long countBySirketId(Long sirketId);

    /** KPI özeti: şirketin aktif depo sayısı. */
    long countBySirketIdAndAktifTrue(Long sirketId);

    /** Bir şubeye bağlı depo var mı? (silme guard'ı) */
    boolean existsBySubeId(Long subeId);

    /** Bir şubeye bağlı depolar (alfabetik). */
    List<Depo> findBySubeIdAndSirketIdOrderByAdAsc(Long subeId, Long sirketId);

    /**
     * Operasyon listesi: arama, sube, aktiflik ve kritik stok filtresi.
     * {@code q} alt/ust harf duyarsizdir; {@code kritik=true} yalnizca en az bir
     * stogu min miktarin altinda olan depoları getirir.
     */
    @Query("SELECT d FROM Depo d WHERE d.sirketId = :sirketId " +
            "AND (:subeId IS NULL OR d.subeId = :subeId) " +
            "AND (:aktif IS NULL OR d.aktif = :aktif) " +
            "AND (:q IS NULL OR LOWER(d.ad) LIKE :q OR LOWER(d.yetkili) LIKE :q) " +
            "AND (:kritik = FALSE OR EXISTS (" +
            "  SELECT 1 FROM DepoStok ds, Stok s WHERE ds.depoId = d.id " +
            "  AND s.id = ds.stokId AND s.minMiktar IS NOT NULL AND ds.miktar <= s.minMiktar)) " +
            "ORDER BY d.ad ASC")
    Page<Depo> filtreli(@Param("sirketId") Long sirketId,
                        @Param("q") String q,
                        @Param("subeId") Long subeId,
                        @Param("aktif") Boolean aktif,
                        @Param("kritik") Boolean kritik,
                        Pageable pageable);
}
