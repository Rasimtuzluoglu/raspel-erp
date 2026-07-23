package com.raspel.erp.repository;

import com.raspel.erp.entity.SatinalmaTalep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SatinalmaTalepRepository extends JpaRepository<SatinalmaTalep, Long> {
    List<SatinalmaTalep> findBySirketIdOrderByTarihDesc(Long sirketId);
    List<SatinalmaTalep> findByDurumAndSirketId(String durum, Long sirketId);
}
