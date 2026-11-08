package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Iade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface IadeRepository extends JpaRepository<Iade, Long> {
    Page<Iade> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    List<Iade> findByFaturaIdInAndSirketId(Collection<Long> faturaIds, Long sirketId);

    /** Cari 360: iade adet/toplam agregasyonu (iptal hariç). */
    @Query("SELECT new com.raspel.erp.dto.finans.CariTutarOzet(COUNT(i), COALESCE(SUM(i.tutar), 0)) " +
            "FROM Iade i WHERE i.cariHesapId = :cariId AND i.sirketId = :sirketId " +
            "AND (i.durum IS NULL OR UPPER(i.durum) <> 'IPTAL')")
    com.raspel.erp.dto.finans.CariTutarOzet cariIadeOzet(@Param("cariId") Long cariId,
                                                         @Param("sirketId") Long sirketId);

    List<Iade> findTop5ByCariHesapIdAndSirketIdOrderByTarihDescIdDesc(Long cariHesapId, Long sirketId);

    List<Iade> findBySirketIdAndTurAndDurumAndTarihBetween(Long sirketId, String tur, String durum,
                                                           LocalDate bas, LocalDate bit);
}
