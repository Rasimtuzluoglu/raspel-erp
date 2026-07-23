package com.raspel.erp.repository;

import com.raspel.erp.entity.PersonelIzin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelIzinRepository extends JpaRepository<PersonelIzin, Long> {
    List<PersonelIzin> findByPersonelIdOrderByBaslangicDesc(Long personelId);
    List<PersonelIzin> findByDurum(String durum);
    @Query("SELECT COUNT(i) FROM PersonelIzin i WHERE i.durum = 'ONAYLANDI' AND i.baslangic <= :bugun AND i.bitis >= :bugun")
    long countBugunIzinli(LocalDate bugun);
}
