package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.Vardiya;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VardiyaRepository extends JpaRepository<Vardiya, Long> {
    Page<Vardiya> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Vardiya> findByPersonelIdAndTarihBetweenOrderByTarihAsc(Long personelId, LocalDate baslangic, LocalDate bitis);
}
