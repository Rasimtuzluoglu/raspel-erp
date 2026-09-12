package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.IadeKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IadeKalemRepository extends JpaRepository<IadeKalem, Long> {
    List<IadeKalem> findByIadeId(Long iadeId);
    void deleteByIadeId(Long iadeId);

    /**
     * Bir stogun tarih aralığındaki tamamlanmış (TAMAMLANDI) iade kalemlerini
     * ürün maliyet-kârlılık analizi için düz satır olarak döndürür.
     * Cari bilgisi bağlantılı fatura üzerinden LEFT JOIN ile alınır.
     */
    @Query("SELECT i.id AS iadeId, i.tarih AS iadeTarihi, i.tur AS iadeTuru, " +
           "f.cariHesap.id AS cariHesapId, f.cariHesap.ad AS cariHesapAd, " +
           "k.miktar AS miktar, k.birimFiyat AS birimFiyat " +
           "FROM IadeKalem k JOIN Iade i ON i.id = k.iadeId " +
           "LEFT JOIN Fatura f ON f.id = i.faturaId " +
           "WHERE k.stokId = :stokId AND i.sirketId = :sirketId " +
           "AND i.tur = :tur AND i.durum = :durum " +
           "AND i.tarih BETWEEN :baslangic AND :bitis " +
           "ORDER BY i.tarih ASC")
    List<IadeAnalizSatirProjeksiyon> analizSatirlari(@Param("stokId") Long stokId,
                                                     @Param("sirketId") Long sirketId,
                                                     @Param("tur") String tur,
                                                     @Param("durum") String durum,
                                                     @Param("baslangic") LocalDate baslangic,
                                                     @Param("bitis") LocalDate bitis);
}
