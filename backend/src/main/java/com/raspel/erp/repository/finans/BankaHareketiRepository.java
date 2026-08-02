package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.BankaHareketi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankaHareketiRepository extends JpaRepository<BankaHareketi, Long> {
    List<BankaHareketi> findByBankaIdOrderByTarihDesc(Long bankaId);
    List<BankaHareketi> findByBankaIdAndEslestirildiFalse(Long bankaId);
}
