package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSayim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StokSayimRepository extends JpaRepository<StokSayim, Long> {
    List<StokSayim> findBySirketIdOrderByTarihDesc(Long sirketId);
}
