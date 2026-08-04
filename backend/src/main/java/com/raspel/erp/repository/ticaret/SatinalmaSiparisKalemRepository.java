package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.SatinalmaSiparisKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SatinalmaSiparisKalemRepository extends JpaRepository<SatinalmaSiparisKalem, Long> {
    List<SatinalmaSiparisKalem> findBySiparisId(Long siparisId);
    void deleteBySiparisId(Long siparisId);
}