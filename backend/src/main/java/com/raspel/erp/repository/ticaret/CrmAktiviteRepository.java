package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.CrmAktivite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmAktiviteRepository extends JpaRepository<CrmAktivite, Long> {

    Page<CrmAktivite> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId, Pageable pageable);

    List<CrmAktivite> findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(Long sirketId, Long cariHesapId);

    List<CrmAktivite> findBySirketIdAndLeadIdOrderByOlusturmaTarihiDesc(Long sirketId, Long leadId);

    long countBySirketIdAndTamamlandiFalse(Long sirketId);

    long countByCariHesapId(Long cariHesapId);
    void deleteByCariHesapId(Long cariHesapId);
}
