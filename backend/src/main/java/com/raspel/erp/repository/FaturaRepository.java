package com.raspel.erp.repository;

import com.raspel.erp.entity.Fatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    List<Fatura> findAllByOrderByTarihDesc();
    Page<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    long count();

    @Query("SELECT f.faturaNumarasi FROM Fatura f WHERE f.faturaNumarasi LIKE :prefix%")
    List<String> findFaturaNumarasiByPrefix(@Param("prefix") String prefix);
}
