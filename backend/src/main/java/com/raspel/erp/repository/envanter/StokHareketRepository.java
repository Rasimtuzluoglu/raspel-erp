package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokHareket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import com.raspel.erp.entity.envanter.Stok;

@Repository
public interface StokHareketRepository extends JpaRepository<StokHareket, Long> {
    List<StokHareket> findByStokIdOrderByHareketTarihiDesc(Long stokId);
    List<StokHareket> findAllByOrderByHareketTarihiDesc();
    long countByStokId(Long stokId);
    long countByTur(String tur);
    @Query("SELECT new map(h.stok.ad as stokAd, h.stok.stokKodu as stokKodu, SUM(h.miktar) as satisMiktari) FROM StokHareket h WHERE h.tur = 'CIKIS' GROUP BY h.stok.ad, h.stok.stokKodu ORDER BY SUM(h.miktar) DESC")
    List<Map<String, Object>> enCokSatanlar();
}