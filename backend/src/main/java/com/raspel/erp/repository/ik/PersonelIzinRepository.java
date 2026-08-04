package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.PersonelIzin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelIzinRepository extends JpaRepository<PersonelIzin, Long> {
    List<PersonelIzin> findByPersonelIdOrderByBaslangicDesc(Long personelId);
    List<PersonelIzin> findByDurum(String durum);
    long countByDurum(String durum);
    Page<PersonelIzin> findByPersonelIdIn(List<Long> personelIds, Pageable pageable);
    @Query("SELECT COUNT(i) FROM PersonelIzin i WHERE i.durum = 'ONAYLANDI' AND i.baslangic <= :bugun AND i.bitis >= :bugun")
    long countBugunIzinli(LocalDate bugun);
}