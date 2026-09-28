package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.BankaHareketi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankaHareketiRepository extends JpaRepository<BankaHareketi, Long> {
    List<BankaHareketi> findByBankaIdOrderByTarihDesc(Long bankaId);

    Page<BankaHareketi> findByBankaId(Long bankaId, Pageable pageable);

    Page<BankaHareketi> findByBankaIdAndSirketId(Long bankaId, Long sirketId, Pageable pageable);
    List<BankaHareketi> findByBankaIdAndEslestirildiFalse(Long bankaId);
    long countByBankaId(Long bankaId);
    List<BankaHareketi> findByKaynakFaturaId(Long kaynakFaturaId);
    List<BankaHareketi> findByKaynakTipAndKaynakId(String kaynakTip, Long kaynakId);
}
