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

    long countByCariHesap_Id(Long cariHesapId);

    /**
     * REDTEAM/Faz1.6: Plan toptan silinmeden önce ödenmiş kalem var mı?
     * {@code deleteByPlanNoAndSirketId} bu kontrolü atlayarak ödenmiş kalemleri
     * sessizce siliyordu.
     */
    long countByPlanNoAndSirketIdAndOdemeDurumu(String planNo, Long sirketId, String odemeDurumu);

    /** REDTEAM/Faz1.6: Tahsilat hareketine bağlı kalem sayısı (denetim izi). */
    long countByPlanNoAndSirketIdAndHareketIdIsNotNull(String planNo, Long sirketId);
}
