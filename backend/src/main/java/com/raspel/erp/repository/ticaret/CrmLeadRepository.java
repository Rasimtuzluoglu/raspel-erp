package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.CrmLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmLeadRepository extends JpaRepository<CrmLead, Long> {

    Page<CrmLead> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId, Pageable pageable);

    List<CrmLead> findBySirketIdAndDurumOrderBySkorDesc(Long sirketId, String durum);

    long countBySirketIdAndKampanyaId(Long sirketId, Long kampanyaId);
}
