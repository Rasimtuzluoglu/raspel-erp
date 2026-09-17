package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AdresDefteri;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdresDefteriRepository extends JpaRepository<AdresDefteri, Long> {

    Page<AdresDefteri> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);

    @Query("SELECT a FROM AdresDefteri a WHERE a.sirketId = :sirketId " +
           "AND (:tur IS NULL OR a.tur = :tur) " +
           "AND (:etiket IS NULL OR LOWER(COALESCE(a.etiketler, '')) LIKE LOWER(CONCAT('%', :etiket, '%'))) " +
           "AND (:q IS NULL OR LOWER(a.ad) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(a.telefon, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(a.email, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(a.adres, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(COALESCE(a.etiketler, '')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY a.ad ASC")
    Page<AdresDefteri> filtreli(@Param("sirketId") Long sirketId,
                                @Param("q") String q,
                                @Param("tur") String tur,
                                @Param("etiket") String etiket,
                                Pageable pageable);

    @Query("SELECT DISTINCT a.tur FROM AdresDefteri a WHERE a.sirketId = :sirketId " +
           "AND a.tur IS NOT NULL AND a.tur <> '' ORDER BY a.tur")
    List<String> turListesi(@Param("sirketId") Long sirketId);

    @Query("SELECT a.etiketler FROM AdresDefteri a WHERE a.sirketId = :sirketId " +
           "AND a.etiketler IS NOT NULL AND a.etiketler <> ''")
    List<String> etiketSatirlari(@Param("sirketId") Long sirketId);
}
