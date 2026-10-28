package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.SatinalmaTalep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SatinalmaTalepRepository extends JpaRepository<SatinalmaTalep, Long> {
    Page<SatinalmaTalep> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    long countBySirketIdAndDurum(Long sirketId, String durum);

    /**
     * Birleşik onay ekranı: bekleyen satınalma talepleri (şirket + durum).
     * {@code q} ham terim değil,
     * {@link com.raspel.erp.util.AramaTemizleyici#like(String)} desenidir.
     */
    @Query("SELECT t FROM SatinalmaTalep t WHERE t.sirketId = :sirketId AND t.durum = :durum " +
            "AND (:q IS NULL OR (" +
            "     LOWER(COALESCE(t.talepNo,'')) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(COALESCE(t.talepEden,'')) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(COALESCE(t.departman,'')) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(COALESCE(t.aciklama,'')) LIKE :q ESCAPE '\\'))")
    Page<SatinalmaTalep> onayBekleyenler(
            @Param("sirketId") Long sirketId,
            @Param("durum") String durum,
            @Param("q") String q,
            Pageable pageable);
}
