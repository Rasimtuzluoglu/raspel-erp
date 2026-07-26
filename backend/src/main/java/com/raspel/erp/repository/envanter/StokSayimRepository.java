package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSayim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StokSayimRepository extends JpaRepository<StokSayim, Long> {
    Page<StokSayim> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
}
