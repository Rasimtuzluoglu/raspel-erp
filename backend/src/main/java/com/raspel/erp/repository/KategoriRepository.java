package com.raspel.erp.repository;

import com.raspel.erp.entity.GelirGiderKategori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KategoriRepository extends JpaRepository<GelirGiderKategori, Long> {
    List<GelirGiderKategori> findByTurOrderByAd(String tur);
    List<GelirGiderKategori> findAllByOrderByAd();
    List<GelirGiderKategori> findBySirketId(Long sirketId);
}
