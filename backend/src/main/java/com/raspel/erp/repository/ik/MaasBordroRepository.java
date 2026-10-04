package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.MaasBordro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaasBordroRepository extends JpaRepository<MaasBordro, Long> {
    Page<MaasBordro> findBySirketIdOrderByYilDescAyDesc(Long sirketId, Pageable pageable);

    /**
     * Borçlu kilitleme (SELECT ... FOR UPDATE). Bordro ödemesinde "zaten ödendi mi"
     * kontrolünün kilit ALINDIKTAN SONRA yapılması gerekiyor; aksi halde iki
     * eşzamanlı ödeme isteği ikisini de geçip kasadan çift düşüyordu.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM MaasBordro b WHERE b.id = :id")
    Optional<MaasBordro> findByIdForUpdate(@Param("id") Long id);

    /** Toplu üretimde aynı personel/ay için mükerrer bordro engeli. */
    boolean existsBySirketIdAndYilAndAyAndPersonelId(Long sirketId, Integer yil, Integer ay, Long personelId);
}
