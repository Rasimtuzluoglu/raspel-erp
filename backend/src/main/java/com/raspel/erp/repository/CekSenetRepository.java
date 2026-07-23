package com.raspel.erp.repository;

import com.raspel.erp.entity.CekSenet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CekSenetRepository extends JpaRepository<CekSenet, Long> {
    List<CekSenet> findBySirketIdOrderByVadeTarihiAsc(Long sirketId);
    List<CekSenet> findByDurumAndVadeTarihiBefore(String durum, LocalDate tarih);
}
