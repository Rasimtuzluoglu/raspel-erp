package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.UretimEmri;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UretimEmriRepository extends JpaRepository<UretimEmri, Long> {
    List<UretimEmri> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<UretimEmri> findBySirketIdAndSiparisId(Long sirketId, Long siparisId);

    List<UretimEmri> findBySirketIdAndSiparisIdIn(Long sirketId, java.util.Collection<Long> siparisIdler);

    /**
     * Borçlu kilitleme (SELECT ... FOR UPDATE). Üretim emrini tamamlama tek atomik
     * işlem olmalı: durum kontrolü, hammadde tüketimi ve mamul üretimi birlikte.
     * Kilit olmadan iki eşzamanlı istek ikisi de "TAMAMLANDI" kontrolünü geçip
     * hammaddeyi iki kez tüketiyor ve mamulü iki kez üretiyordu.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM UretimEmri e WHERE e.id = :id")
    Optional<UretimEmri> findByIdForUpdate(@Param("id") Long id);
}
