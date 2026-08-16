package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.GelirGiderKategori;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KategoriRepository extends JpaRepository<GelirGiderKategori, Long> {
    List<GelirGiderKategori> findByTurOrderByAd(String tur);
    List<GelirGiderKategori> findBySirketIdAndTurOrderByAd(Long sirketId, String tur);
    List<GelirGiderKategori> findAllByOrderByAd();
    Page<GelirGiderKategori> findBySirketId(Long sirketId, Pageable pageable);
}