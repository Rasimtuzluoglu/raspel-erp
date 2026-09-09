package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokDuzeltme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StokDuzeltmeRepository extends JpaRepository<StokDuzeltme, Long> {
    List<StokDuzeltme> findTop100BySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
}
