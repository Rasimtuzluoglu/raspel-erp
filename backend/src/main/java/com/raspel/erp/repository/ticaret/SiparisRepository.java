package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Siparis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SiparisRepository extends JpaRepository<Siparis, Long> {
    Page<Siparis> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Siparis> findByCariHesapId(Long cariHesapId);
    long countByTarih(LocalDate tarih);
    long countByDurumNot(String durum);
    long countBySirketIdAndTarih(Long sirketId, LocalDate tarih);
    long countBySirketIdAndDurumNot(Long sirketId, String durum);

    @Query("SELECT s.siparisNo FROM Siparis s WHERE s.siparisNo LIKE :prefix% AND s.sirketId = :sirketId")
    List<String> findSiparisNoByPrefix(@Param("prefix") String prefix, @Param("sirketId") Long sirketId);
}