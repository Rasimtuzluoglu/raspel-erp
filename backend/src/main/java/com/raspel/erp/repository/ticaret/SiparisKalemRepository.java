package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.SiparisKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiparisKalemRepository extends JpaRepository<SiparisKalem, Long> {
    List<SiparisKalem> findBySiparisId(Long siparisId);
    void deleteBySiparisId(Long siparisId);
}