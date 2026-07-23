package com.raspel.erp.repository;

import com.raspel.erp.entity.Personel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelRepository extends JpaRepository<Personel, Long> {
    List<Personel> findBySirketIdOrderByAdAsc(Long sirketId);
    List<Personel> findByAktifTrueAndSirketId(Long sirketId);
    long countByAktifTrue();
    long countByIseGirisTarihiBetween(LocalDate baslangic, LocalDate bitis);
}
