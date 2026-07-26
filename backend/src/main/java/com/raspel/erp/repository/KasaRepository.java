package com.raspel.erp.repository;

import com.raspel.erp.entity.Kasa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KasaRepository extends JpaRepository<Kasa, Long> {
    Page<Kasa> findBySirketId(Long sirketId, Pageable pageable);
}
