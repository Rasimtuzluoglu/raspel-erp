package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.PersonelMasrafTalep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonelMasrafTalepRepository extends JpaRepository<PersonelMasrafTalep, Long> {
    Page<PersonelMasrafTalep> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<PersonelMasrafTalep> findBySirketIdAndDurumOrderByTarihDesc(Long sirketId, String durum);
    List<PersonelMasrafTalep> findByKullaniciIdOrderByTarihDesc(Long kullaniciId);
    long countBySirketIdAndDurum(Long sirketId, String durum);

    /**
     * Birleşik onay ekranı: bekleyen masraf/avans talepleri (şirket + durum).
     * {@code q} ham terim değil,
     * {@link com.raspel.erp.util.AramaTemizleyici#like(String)} desenidir.
     */
    @Query("SELECT t FROM PersonelMasrafTalep t WHERE t.sirketId = :sirketId AND t.durum = :durum " +
            "AND (:q IS NULL OR (" +
            "     LOWER(COALESCE(t.aciklama,'')) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(t.tur) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(COALESCE(t.kategori,'')) LIKE :q ESCAPE '\\'))")
    Page<PersonelMasrafTalep> onayBekleyenler(
            @Param("sirketId") Long sirketId,
            @Param("durum") String durum,
            @Param("q") String q,
            Pageable pageable);
}
