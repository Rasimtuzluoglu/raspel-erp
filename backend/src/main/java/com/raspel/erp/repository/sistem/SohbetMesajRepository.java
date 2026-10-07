package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetMesaj;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SohbetMesajRepository extends JpaRepository<SohbetMesaj, Long> {
    List<SohbetMesaj> findTop50BySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<SohbetMesaj> findTop100ByOdaIdOrderByOlusturmaTarihiDesc(Long odaId);

    /** Faz 3.3: en yeni N oda mesajı (id azalan). */
    List<SohbetMesaj> findByOdaIdOrderByIdDesc(Long odaId, Pageable pageable);

    /** Faz 3.3: cursor'dan eski N mesaj (id < cursor, id azalan). */
    List<SohbetMesaj> findByOdaIdAndIdLessThanOrderByIdDesc(Long odaId, Long cursor, Pageable pageable);

    long countByOdaIdAndOlusturmaTarihiGreaterThan(Long odaId, Instant tarih);
}
