package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.CariFirsat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CariFirsatRepository extends JpaRepository<CariFirsat, Long> {
    List<CariFirsat> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<CariFirsat> findBySirketIdAndDurum(Long sirketId, String durum);
}
