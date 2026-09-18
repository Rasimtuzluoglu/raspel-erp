package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.CrmKampanya;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrmKampanyaRepository extends JpaRepository<CrmKampanya, Long> {

    Page<CrmKampanya> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId, Pageable pageable);

    long countBySirketIdAndDurum(Long sirketId, String durum);
}
