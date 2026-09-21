package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.FaturaGecmis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// Dinamik filtreleme Specification ile yapilir: opsiyonel (null) parametreler
// sorguya hic eklenmez, boylece PostgreSQL'in "could not determine data type of
// parameter" hatasi olusmaz.
public interface FaturaGecmisRepository extends JpaRepository<FaturaGecmis, Long>,
        JpaSpecificationExecutor<FaturaGecmis> {

    List<FaturaGecmis> findByFaturaIdOrderByTarihDescIdDesc(Long faturaId);

    List<FaturaGecmis> findByFaturaIdInAndOlayOrderByTarihDescIdDesc(List<Long> faturaIds, String olay);

    long countByFaturaIdAndOlay(Long faturaId, String olay);
}
