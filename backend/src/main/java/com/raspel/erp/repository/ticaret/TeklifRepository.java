package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Teklif;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeklifRepository extends JpaRepository<Teklif, Long> {

    Page<Teklif> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    List<Teklif> findBySirketIdAndCariHesapId(Long sirketId, Long cariHesapId);

    @Query("SELECT t.teklifNo FROM Teklif t WHERE t.teklifNo LIKE :prefix% AND (:sirketId IS NULL OR t.sirketId = :sirketId)")
    List<String> findTeklifNoByPrefix(@Param("prefix") String prefix, @Param("sirketId") Long sirketId);

    long countBySirketIdAndDurum(Long sirketId, String durum);
}
