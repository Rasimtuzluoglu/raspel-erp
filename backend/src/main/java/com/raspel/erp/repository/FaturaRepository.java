package com.raspel.erp.repository;

import com.raspel.erp.entity.Fatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    List<Fatura> findAllByOrderByTarihDesc();
    List<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId);
    long count();
}
