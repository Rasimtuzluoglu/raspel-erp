package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Iade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IadeRepository extends JpaRepository<Iade, Long> {
    Page<Iade> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
}
