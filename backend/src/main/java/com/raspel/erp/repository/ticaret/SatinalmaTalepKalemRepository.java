package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.SatinalmaTalepKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SatinalmaTalepKalemRepository extends JpaRepository<SatinalmaTalepKalem, Long> {
    List<SatinalmaTalepKalem> findByTalepId(Long talepId);
    void deleteByTalepId(Long talepId);
}