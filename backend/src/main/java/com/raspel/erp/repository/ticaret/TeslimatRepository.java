package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Teslimat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeslimatRepository extends JpaRepository<Teslimat, Long> {
    List<Teslimat> findBySirketIdAndDriverIdOrderByOlusturmaTarihiDesc(Long sirketId, Long driverId);
    long countBySirketIdAndDriverIdAndDurumIn(Long sirketId, Long driverId, List<String> durumlar);
}
