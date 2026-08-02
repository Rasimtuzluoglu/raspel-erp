package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.MuhasebeFisi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MuhasebeFisiRepository extends JpaRepository<MuhasebeFisi, Long> {
    List<MuhasebeFisi> findBySirketIdOrderByTarihDesc(Long sirketId);
    List<MuhasebeFisi> findBySirketIdAndTarihBetweenOrderByTarihAsc(Long sirketId, LocalDate baslangic, LocalDate bitis);
    Optional<MuhasebeFisi> findTopBySirketIdOrderByFisNoDesc(Long sirketId);
}
