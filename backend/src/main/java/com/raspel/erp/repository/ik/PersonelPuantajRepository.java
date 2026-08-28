package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.PersonelPuantaj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelPuantajRepository extends JpaRepository<PersonelPuantaj, Long> {
    List<PersonelPuantaj> findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(Long sirketId, Long personelId, LocalDate baslangic, LocalDate bitis);
    List<PersonelPuantaj> findByTarih(LocalDate tarih);
}