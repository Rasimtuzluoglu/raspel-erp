package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.Irsaliye;
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
public interface IrsaliyeRepository extends JpaRepository<Irsaliye, Long> {
    Page<Irsaliye> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Irsaliye> findByFaturaId(Long faturaId);
    List<Irsaliye> findBySirketIdAndSiparisId(Long sirketId, Long siparisId);

    List<Irsaliye> findBySirketIdAndSiparisIdIn(Long sirketId, java.util.Collection<Long> siparisIdler);

    long countByCariHesapId(Long cariHesapId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Irsaliye i WHERE i.id = :id")
    Optional<Irsaliye> findByIdForUpdate(@Param("id") Long id);
}