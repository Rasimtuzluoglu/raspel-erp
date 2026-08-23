package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSeri;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StokSeriRepository extends JpaRepository<StokSeri, Long> {
    List<StokSeri> findByStokId(Long stokId);
    List<StokSeri> findBySeriNo(String seriNo);
    Page<StokSeri> findByStokSirketId(Long sirketId, Pageable pageable);
}
