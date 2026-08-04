package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.SatinalmaTalep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SatinalmaTalepRepository extends JpaRepository<SatinalmaTalep, Long> {
    Page<SatinalmaTalep> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<SatinalmaTalep> findByDurumAndSirketId(String durum, Long sirketId);
}