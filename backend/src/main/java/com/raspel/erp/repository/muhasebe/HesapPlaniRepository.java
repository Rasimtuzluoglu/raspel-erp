package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.HesapPlani;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HesapPlaniRepository extends JpaRepository<HesapPlani, Long> {
    List<HesapPlani> findBySirketIdOrderByKodAsc(Long sirketId);
    Optional<HesapPlani> findBySirketIdAndKod(Long sirketId, String kod);
    List<HesapPlani> findBySirketIdAndTip(Long sirketId, String tip);
}
