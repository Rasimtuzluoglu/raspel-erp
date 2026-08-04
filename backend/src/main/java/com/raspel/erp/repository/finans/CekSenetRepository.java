package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CekSenet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CekSenetRepository extends JpaRepository<CekSenet, Long> {
    Page<CekSenet> findBySirketIdOrderByVadeTarihiAsc(Long sirketId, Pageable pageable);
    List<CekSenet> findByDurumAndVadeTarihiBefore(String durum, LocalDate tarih);
}