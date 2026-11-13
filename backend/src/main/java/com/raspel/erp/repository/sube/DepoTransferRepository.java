package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.DepoTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DepoTransferRepository extends JpaRepository<DepoTransfer, Long> {
    List<DepoTransfer> findBySirketIdOrderByIdDesc(Long sirketId);
    List<DepoTransfer> findBySirketIdAndDurumOrderByIdAsc(Long sirketId, String durum);

    /** KPI özeti: belirtilen durumdaki transfer talebi sayısı. */
    long countBySirketIdAndDurum(Long sirketId, String durum);

    /** Operasyon listesi: depo (kaynak/hedef), durum ve tarih araligi. */
    @Query("SELECT t FROM DepoTransfer t WHERE t.sirketId = :sirketId " +
           "AND (:depoId IS NULL OR t.kaynakDepoId = :depoId OR t.hedefDepoId = :depoId) " +
           "AND (:durum IS NULL OR t.durum = :durum) " +
           "AND (:bas IS NULL OR t.olusturmaTarihi >= :bas) " +
           "AND (:bit IS NULL OR t.olusturmaTarihi < :bit) " +
           "ORDER BY t.id DESC")
    Page<DepoTransfer> filtreli(@Param("sirketId") Long sirketId,
                                @Param("depoId") Long depoId,
                                @Param("durum") String durum,
                                @Param("bas") LocalDateTime bas,
                                @Param("bit") LocalDateTime bit,
                                Pageable pageable);
}
