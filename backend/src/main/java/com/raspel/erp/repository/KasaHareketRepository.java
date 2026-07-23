package com.raspel.erp.repository;

import com.raspel.erp.entity.KasaHareket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KasaHareketRepository extends JpaRepository<KasaHareket, Long> {
    List<KasaHareket> findByKasaIdOrderByHareketTarihiDesc(Long kasaId);
    long countByKasaId(Long kasaId);
}
