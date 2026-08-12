package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.Personel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelRepository extends JpaRepository<Personel, Long> {
    Page<Personel> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Personel> findByAktifTrueAndSirketId(Long sirketId);
    long countByAktifTrue();
    long countByAktifTrueAndSirketId(Long sirketId);
    long countByIseGirisTarihiBetween(LocalDate baslangic, LocalDate bitis);
    long countBySirketIdAndIseGirisTarihiBetween(Long sirketId, LocalDate baslangic, LocalDate bitis);
}