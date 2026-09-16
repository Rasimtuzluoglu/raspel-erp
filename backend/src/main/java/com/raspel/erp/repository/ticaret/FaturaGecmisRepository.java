package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.FaturaGecmis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FaturaGecmisRepository extends JpaRepository<FaturaGecmis, Long> {

    List<FaturaGecmis> findByFaturaIdOrderByTarihDescIdDesc(Long faturaId);

    List<FaturaGecmis> findByFaturaIdInAndOlayOrderByTarihDescIdDesc(List<Long> faturaIds, String olay);

    long countByFaturaIdAndOlay(Long faturaId, String olay);

    @Query("SELECT g FROM FaturaGecmis g WHERE g.sirketId = :sirketId " +
           "AND (:olay IS NULL OR g.olay = :olay) " +
           "AND (:kullaniciId IS NULL OR g.kullaniciId = :kullaniciId) " +
           "AND (:baslangic IS NULL OR g.tarih >= :baslangic) " +
           "AND (:bitis IS NULL OR g.tarih <= :bitis) " +
           "ORDER BY g.tarih DESC, g.id DESC")
    List<FaturaGecmis> filtreli(@Param("sirketId") Long sirketId,
                                @Param("olay") String olay,
                                @Param("kullaniciId") Long kullaniciId,
                                @Param("baslangic") LocalDateTime baslangic,
                                @Param("bitis") LocalDateTime bitis);
}
