package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.Personel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonelRepository extends JpaRepository<Personel, Long> {
    Page<Personel> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Personel> findBySirketIdAndRolOrderByAdAsc(Long sirketId, String rol);
    List<Personel> findBySirketIdAndRolAndAktifTrue(Long sirketId, String rol);
    Optional<Personel> findByKullaniciId(Long kullaniciId);
    long countByAktifTrueAndSirketId(Long sirketId);
    long countBySirketIdAndIseGirisTarihiBetween(Long sirketId, LocalDate baslangic, LocalDate bitis);
}
