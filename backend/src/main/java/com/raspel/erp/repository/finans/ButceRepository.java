package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Butce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ButceRepository extends JpaRepository<Butce, Long> {
    Page<Butce> findBySirketIdOrderByYilDescAyDesc(Long sirketId, Pageable pageable);
}
