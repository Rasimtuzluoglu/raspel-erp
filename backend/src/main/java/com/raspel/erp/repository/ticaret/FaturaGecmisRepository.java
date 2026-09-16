package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.FaturaGecmis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaturaGecmisRepository extends JpaRepository<FaturaGecmis, Long> {

    List<FaturaGecmis> findByFaturaIdOrderByTarihDescIdDesc(Long faturaId);

    List<FaturaGecmis> findByFaturaIdInAndOlayOrderByTarihDescIdDesc(List<Long> faturaIds, String olay);

    long countByFaturaIdAndOlay(Long faturaId, String olay);
}
