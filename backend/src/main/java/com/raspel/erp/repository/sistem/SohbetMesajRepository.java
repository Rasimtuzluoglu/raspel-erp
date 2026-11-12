package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetMesaj;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SohbetMesajRepository extends JpaRepository<SohbetMesaj, Long> {
    List<SohbetMesaj> findTop50BySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<SohbetMesaj> findTop100ByOdaIdOrderByOlusturmaTarihiDesc(Long odaId);

    /** Faz 3.3: en yeni N oda mesaji (id azalan). */
    List<SohbetMesaj> findByOdaIdOrderByIdDesc(Long odaId, Pageable pageable);

    /** Faz 3.3: cursor'dan eski N mesaj (id < cursor, id azalan). */
    List<SohbetMesaj> findByOdaIdAndIdLessThanOrderByIdDesc(Long odaId, Long cursor, Pageable pageable);

    long countByOdaIdAndOlusturmaTarihiGreaterThan(Long odaId, Instant tarih);

    // --- Faz 8: genel sohbet (oda_id NULL) cursor sayfalamasi -----------------

    List<SohbetMesaj> findBySirketIdAndOdaIdIsNullOrderByIdDesc(Long sirketId, Pageable pageable);

    List<SohbetMesaj> findBySirketIdAndOdaIdIsNullAndIdLessThanOrderByIdDesc(Long sirketId, Long cursor, Pageable pageable);

    long countBySirketIdAndOdaIdIsNullAndIdGreaterThan(Long sirketId, Long id);

    /**
     * Oda silinirken mesajlari da temizler. Onceden oda silinse bile mesajlar
     * `oda_id` ile oksuz kaliyordu (FK yok), zamanla cop veri birikiyordu.
     */
    @Modifying
    @Query("delete from SohbetMesaj m where m.odaId = :odaId")
    int deleteByOdaId(@Param("odaId") Long odaId);
}
