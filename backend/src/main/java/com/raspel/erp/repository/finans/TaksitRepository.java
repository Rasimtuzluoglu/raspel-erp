package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Taksit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaksitRepository extends JpaRepository<Taksit, Long>, JpaSpecificationExecutor<Taksit> {

    Optional<Taksit> findByIdAndSirketId(Long id, Long sirketId);

    List<Taksit> findBySirketIdAndOdemeDurumuNot(Long sirketId, String odemeDurumu);

    List<Taksit> findBySirketIdAndOdemeDurumuNotAndVadeTarihiBetweenOrderByVadeTarihiAsc(
            Long sirketId, String odemeDurumu, LocalDate baslangic, LocalDate bitis);

    List<Taksit> findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(
            Long sirketId, LocalDate baslangic, LocalDate bitis);

    List<Taksit> findBySirketIdAndCariHesapIdOrderByVadeTarihiAsc(Long sirketId, Long cariHesapId);

    void deleteByPlanNoAndSirketId(String planNo, Long sirketId);
}
