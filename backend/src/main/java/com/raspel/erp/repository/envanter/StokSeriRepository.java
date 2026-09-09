package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSeri;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StokSeriRepository extends JpaRepository<StokSeri, Long> {
    List<StokSeri> findByStokId(Long stokId);
    List<StokSeri> findBySeriNo(String seriNo);
    Page<StokSeri> findByStokSirketId(Long sirketId, Pageable pageable);

    @Query("SELECT s FROM StokSeri s WHERE s.stok.sirketId = :sirketId AND s.sonKullanmaTarihi IS NOT NULL " +
            "AND s.sonKullanmaTarihi <= :tarih ORDER BY s.sonKullanmaTarihi ASC")
    List<StokSeri> sonKullanmaYaklasan(@Param("sirketId") Long sirketId, @Param("tarih") LocalDate tarih);
}
