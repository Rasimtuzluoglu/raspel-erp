package com.raspel.erp.repository;

import com.raspel.erp.entity.SatinalmaSiparis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SatinalmaSiparisRepository extends JpaRepository<SatinalmaSiparis, Long> {
    List<SatinalmaSiparis> findBySirketIdOrderByTarihDesc(Long sirketId);
    List<SatinalmaSiparis> findByCariHesapId(Long cariHesapId);
}
