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
import com.raspel.erp.entity.finans.CariHesap;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findAllByOrderByTarihDesc();

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    Page<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findAll();

    long count();

    @Query("SELECT f.faturaNumarasi FROM Fatura f WHERE f.faturaNumarasi LIKE :prefix% AND f.sirketId = :sirketId")
    List<String> findFaturaNumarasiByPrefix(@Param("prefix") String prefix, @Param("sirketId") Long sirketId);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findByTurAndDurumAndOdemeDurumuNotIn(Fatura.FaturaTur tur, Fatura.FaturaDurum durum, java.util.List<String> odemeDurumlari);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findByTurAndOdemeDurumuNotIn(Fatura.FaturaTur tur, java.util.List<String> odemeDurumlari);

    @EntityGraph(attributePaths = {"cariHesap", "kalemler"})
    List<Fatura> findBySirketIdAndDurumNotAndOdemeDurumuNotIn(Long sirketId, Fatura.FaturaDurum durum, java.util.List<String> odemeDurumlari);
}