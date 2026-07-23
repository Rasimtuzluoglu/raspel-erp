package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSeri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StokSeriRepository extends JpaRepository<StokSeri, Long> {
    List<StokSeri> findByStokId(Long stokId);
    List<StokSeri> findBySeriNo(String seriNo);
}
