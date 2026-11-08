package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.CariFirsat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CariFirsatRepository extends JpaRepository<CariFirsat, Long> {
    List<CariFirsat> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<CariFirsat> findBySirketIdAndDurum(Long sirketId, String durum);
    List<CariFirsat> findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(Long sirketId, Long cariHesapId);

    /** Cari 360: fırsatları sayfalı getirir (lazy sekme). */
    Page<CariFirsat> findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(
            Long sirketId, Long cariHesapId, Pageable pageable);

    long countBySirketIdAndCariHesapId(Long sirketId, Long cariHesapId);

    long countByCariHesapId(Long cariHesapId);
    void deleteByCariHesapId(Long cariHesapId);
}
