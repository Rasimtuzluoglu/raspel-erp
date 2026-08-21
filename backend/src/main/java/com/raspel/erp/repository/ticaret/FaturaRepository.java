package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Fatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.raspel.erp.entity.finans.CariHesap;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findAllByOrderByTarihDesc();

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    Page<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findBySirketIdAndTarihBetween(Long sirketId, java.time.LocalDate baslangic, java.time.LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    Optional<Fatura> findTopByCariHesapIdAndSirketIdOrderByTarihDescIdDesc(Long cariHesapId, Long sirketId);

    @Override
    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findAll();

    long count();

    long countByCariHesapId(Long cariHesapId);

    @Query("SELECT f.faturaNumarasi FROM Fatura f WHERE f.faturaNumarasi LIKE :prefix% AND f.sirketId = :sirketId")
    List<String> findFaturaNumarasiByPrefix(@Param("prefix") String prefix, @Param("sirketId") Long sirketId);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findByTurAndDurumAndOdemeDurumuNotIn(Fatura.FaturaTur tur, Fatura.FaturaDurum durum, java.util.List<String> odemeDurumlari);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findByTurAndOdemeDurumuNotIn(Fatura.FaturaTur tur, java.util.List<String> odemeDurumlari);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findBySirketIdAndDurumNotAndOdemeDurumuNotIn(Long sirketId, Fatura.FaturaDurum durum, java.util.List<String> odemeDurumlari);

    /**
     * Vadesi yaklaşan (bugün + ileriye dönük) ve kalan tutarı olan faturalar.
     */
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.durum = :durum " +
            "AND f.odemeDurumu NOT IN :odemeDurumlari AND f.kalanTutar > 0 " +
            "AND f.vadeTarihi BETWEEN :baslangic AND :bitis " +
            "ORDER BY f.vadeTarihi ASC")
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findVadesiYaklasan(@Param("sirketId") Long sirketId,
                                     @Param("durum") Fatura.FaturaDurum durum,
                                     @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                     @Param("baslangic") java.time.LocalDate baslangic,
                                     @Param("bitis") java.time.LocalDate bitis);

    /**
     * Vadesi geçmiş ve kalan tutarı olan faturalar.
     */
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.durum = :durum " +
            "AND f.odemeDurumu NOT IN :odemeDurumlari AND f.kalanTutar > 0 " +
            "AND f.vadeTarihi < :bugun " +
            "ORDER BY f.vadeTarihi ASC")
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findVadesiGecen(@Param("sirketId") Long sirketId,
                                  @Param("durum") Fatura.FaturaDurum durum,
                                  @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                  @Param("bugun") java.time.LocalDate bugun);
}