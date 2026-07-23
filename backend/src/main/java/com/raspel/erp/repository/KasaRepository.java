package com.raspel.erp.repository;

import com.raspel.erp.entity.Kasa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KasaRepository extends JpaRepository<Kasa, Long> {
    List<Kasa> findBySirketId(Long sirketId);
}
