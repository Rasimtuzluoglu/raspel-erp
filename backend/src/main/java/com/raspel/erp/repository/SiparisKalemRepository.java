package com.raspel.erp.repository;

import com.raspel.erp.entity.SiparisKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiparisKalemRepository extends JpaRepository<SiparisKalem, Long> {
    List<SiparisKalem> findBySiparisId(Long siparisId);
    void deleteBySiparisId(Long siparisId);
}
