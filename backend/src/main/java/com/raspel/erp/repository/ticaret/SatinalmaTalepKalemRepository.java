package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.SatinalmaTalepKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SatinalmaTalepKalemRepository extends JpaRepository<SatinalmaTalepKalem, Long> {
    List<SatinalmaTalepKalem> findByTalepId(Long talepId);
    void deleteByTalepId(Long talepId);

    /**
     * Onay listesinde talebin tahmini toplam tutarını göstermek için
     * talep bazında miktar × birim fiyat toplamı (tek sorgu, N+1 önlenir).
     *
     * @return her satır: {@code [talepId, toplamTutar]}
     */
    @Query("SELECT k.talepId, SUM(k.miktar * k.tahminiBirimFiyat) FROM SatinalmaTalepKalem k " +
            "WHERE k.talepId IN :talepIds GROUP BY k.talepId")
    List<Object[]> toplamTutarByTalepIds(@Param("talepIds") Collection<Long> talepIds);
}
