package com.raspel.erp.repository;

import com.raspel.erp.entity.Banka;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankaRepository extends JpaRepository<Banka, Long> {
    Page<Banka> findBySirketId(Long sirketId, Pageable pageable);
}
