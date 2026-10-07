package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.Recete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceteRepository extends JpaRepository<Recete, Long> {
    List<Recete> findBySirketIdOrderByAd(Long sirketId);
    Optional<Recete> findFirstBySirketIdAndUrunId(Long sirketId, Long urunId);

    /**
     * Üretimde kullanılacak AKTİF reçeteyi en yüksek revizyonla seçer.
     * Önceki `findFirstBySirketIdAndUrunId` pasif/eski revizyonu seçebiliyordu.
     */
    Optional<Recete> findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(Long sirketId, Long urunId);
}
