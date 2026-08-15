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

    /**
     * Bir stogun son alis fiyatlarini (ALIS + KESILDI faturalardan) tarihe gore dondurur.
     */
    @Query("SELECT k.birimFiyat AS birimFiyat, f.tarih AS tarih, f.faturaNumarasi AS faturaNumarasi " +
           "FROM FaturaKalem k JOIN k.fatura f " +
           "WHERE k.stokId = :stokId AND f.sirketId = :sirketId " +
           "AND f.tur = :tur AND f.durum = :durum " +
           "ORDER BY f.tarih DESC")
    List<StokFiyatGecmisiProjeksiyon> stokFiyatGecmisi(@Param("stokId") Long stokId,
                                                       @Param("sirketId") Long sirketId,
                                                       @Param("tur") com.raspel.erp.entity.ticaret.Fatura.FaturaTur tur,
                                                       @Param("durum") com.raspel.erp.entity.ticaret.Fatura.FaturaDurum durum);

    /**
     * Tedarikçi bazlı ürün raporu: hangi tedarikçiden hangi ürün, toplam miktar, son fiyat ve son tarih.
     * Yalnızca ALIS + KESILDI faturalar dikkate alınır.
     */
    @Query("SELECT f.cariHesap.id AS cariHesapId, f.cariHesap.ad AS cariHesapAd, " +
           "k.stokId AS stokId, SUM(k.adet) AS toplamMiktar, " +
           "MAX(k.birimFiyat) AS sonBirimFiyat, MAX(f.tarih) AS sonTarih " +
           "FROM FaturaKalem k JOIN k.fatura f " +
           "WHERE f.sirketId = :sirketId AND f.tur = :tur AND f.durum = :durum " +
           "AND k.stokId IS NOT NULL AND f.cariHesap IS NOT NULL " +
           "GROUP BY f.cariHesap.id, f.cariHesap.ad, k.stokId " +
           "ORDER BY f.cariHesap.ad ASC, MAX(f.tarih) DESC")
    List<TedarikciUrunProjeksiyon> tedarikciUrunler(@Param("sirketId") Long sirketId,
                                                    @Param("tur") com.raspel.erp.entity.ticaret.Fatura.FaturaTur tur,
                                                    @Param("durum") com.raspel.erp.entity.ticaret.Fatura.FaturaDurum durum);
}