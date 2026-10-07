package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CariFiyat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CariFiyatRepository extends JpaRepository<CariFiyat, Long> {
    List<CariFiyat> findByCariHesapIdOrderByStokId(Long cariHesapId);

    /**
     * Tenant-scoped cari fiyat listesi. `findByCariHesapIdOrderByStokId` yalnızca
     * cari id'sine baktığı için başka şirketin cariye özel fiyatlarını
     * döndürüyordu; okuma yolunda tenant kontrolü atlandığında veri sızıyordu.
     */
    List<CariFiyat> findBySirketIdAndCariHesapIdOrderByStokId(Long sirketId, Long cariHesapId);

    Optional<CariFiyat> findByCariHesapIdAndStokId(Long cariHesapId, Long stokId);

    /** Faz 1.3/1.4: Tenant filtreli tek kayıt araması (defense-in-depth). */
    Optional<CariFiyat> findBySirketIdAndCariHesapIdAndStokId(Long sirketId, Long cariHesapId, Long stokId);

    long countByCariHesapId(Long cariHesapId);
    void deleteByCariHesapId(Long cariHesapId);
}
