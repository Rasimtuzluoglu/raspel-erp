package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.UretimEmri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UretimEmriRepository extends JpaRepository<UretimEmri, Long> {
    List<UretimEmri> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
}
