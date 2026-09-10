package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.PosGunSonu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PosGunSonuRepository extends JpaRepository<PosGunSonu, Long> {
    boolean existsByPosIdAndTarih(Long posId, LocalDate tarih);
    List<PosGunSonu> findTop100BySirketIdOrderByTarihDesc(Long sirketId);
}
