package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.MuhasebeFisi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MuhasebeFisiRepository extends JpaRepository<MuhasebeFisi, Long> {
    List<MuhasebeFisi> findBySirketIdOrderByTarihDesc(Long sirketId);
    List<MuhasebeFisi> findBySirketIdAndTarihBetweenOrderByTarihAsc(Long sirketId, LocalDate baslangic, LocalDate bitis);
    Page<MuhasebeFisi> findBySirketIdAndTarihBetween(Long sirketId, LocalDate baslangic, LocalDate bitis, Pageable pageable);
    Optional<MuhasebeFisi> findTopBySirketIdOrderByFisNoDesc(Long sirketId);

    Optional<MuhasebeFisi> findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(
            Long sirketId, String kaynakTip, Long kaynakId, String durum);
}
