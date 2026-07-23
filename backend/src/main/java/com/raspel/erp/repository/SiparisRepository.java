package com.raspel.erp.repository;

import com.raspel.erp.entity.Siparis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SiparisRepository extends JpaRepository<Siparis, Long> {
    List<Siparis> findBySirketIdOrderByTarihDesc(Long sirketId);
    List<Siparis> findByCariHesapId(Long cariHesapId);
    long countByTarih(LocalDate tarih);
    long countByDurumNot(String durum);
}
