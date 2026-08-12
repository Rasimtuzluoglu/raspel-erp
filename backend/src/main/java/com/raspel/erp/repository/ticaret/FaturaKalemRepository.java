package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.FaturaKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FaturaKalemRepository extends JpaRepository<FaturaKalem, Long> {
    List<FaturaKalem> findByFaturaId(Long faturaId);

    /**
     * Bir cari hesabin son aldigi urunleri, en son alis tarihine gore
     * benzersiz (stokId) olarak dondurur. Sadece SATIS + KESILDI faturalar.
     */
    @Query("SELECT k.stokId AS stokId, MAX(f.tarih) AS sonAlisTarihi, " +
           "MAX(k.birimFiyat) AS sonBirimFiyat, COUNT(k) AS adet " +
           "FROM FaturaKalem k JOIN k.fatura f " +
           "WHERE f.cariHesap.id = :cariId AND f.sirketId = :sirketId " +
           "AND f.tur = :tur AND f.durum = :durum " +
           "AND k.stokId IS NOT NULL " +
           "GROUP BY k.stokId ORDER BY MAX(f.tarih) DESC")
    List<CariSonUrunProjeksiyon> cariSonUrunler(@Param("cariId") Long cariId,
                                                @Param("sirketId") Long sirketId,
                                                @Param("tur") com.raspel.erp.entity.ticaret.Fatura.FaturaTur tur,
                                                @Param("durum") com.raspel.erp.entity.ticaret.Fatura.FaturaDurum durum);
}