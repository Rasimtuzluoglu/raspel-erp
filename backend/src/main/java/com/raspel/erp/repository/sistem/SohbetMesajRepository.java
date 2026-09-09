package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetMesaj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SohbetMesajRepository extends JpaRepository<SohbetMesaj, Long> {
    List<SohbetMesaj> findTop50BySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<SohbetMesaj> findTop100ByOdaIdOrderByOlusturmaTarihiDesc(Long odaId);
    long countByOdaIdAndOlusturmaTarihiGreaterThan(Long odaId, LocalDateTime tarih);
}
