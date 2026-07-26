package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.EFatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EFaturaRepository extends JpaRepository<EFatura, Long> {
    Page<EFatura> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId, Pageable pageable);
    Optional<EFatura> findByEttn(String ettn);
    Optional<EFatura> findByFaturaId(Long faturaId);
}
