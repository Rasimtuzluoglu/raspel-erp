package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.FaturaKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FaturaKalemRepository extends JpaRepository<FaturaKalem, Long> {
    List<FaturaKalem> findByFaturaId(Long faturaId);
}