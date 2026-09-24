package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Teslimat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeslimatRepository extends JpaRepository<Teslimat, Long> {
    List<Teslimat> findBySirketIdAndDriverIdOrderByOlusturmaTarihiDesc(Long sirketId, Long driverId);
    List<Teslimat> findBySirketId(Long sirketId);
    long countBySirketIdAndDriverIdAndDurumIn(Long sirketId, Long driverId, List<String> durumlar);
    List<Teslimat> findByDurumInAndBeklenenTeslimTarihiBeforeAndGecikmeBildirildiFalse(List<String> durumlar, LocalDate tarih);
    List<Teslimat> findBySirketIdAndSiparisId(Long sirketId, Long siparisId);

    List<Teslimat> findBySirketIdAndSiparisIdIn(Long sirketId, java.util.Collection<Long> siparisIdler);
}
