package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.TeklifKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeklifKalemRepository extends JpaRepository<TeklifKalem, Long> {
    List<TeklifKalem> findByTeklifId(Long teklifId);
    List<TeklifKalem> findByTeklifIdIn(java.util.Collection<Long> teklifIdler);
    void deleteByTeklifId(Long teklifId);
}
