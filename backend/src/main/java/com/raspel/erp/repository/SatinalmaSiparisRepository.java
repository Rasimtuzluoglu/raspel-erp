package com.raspel.erp.repository;

import com.raspel.erp.entity.SatinalmaSiparis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SatinalmaSiparisRepository extends JpaRepository<SatinalmaSiparis, Long> {
    Page<SatinalmaSiparis> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<SatinalmaSiparis> findByCariHesapId(Long cariHesapId);
}
