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
    Page<StokSeri> findByStokSirketId(Long sirketId, Pageable pageable);

    @Query("SELECT s FROM StokSeri s WHERE s.stok.sirketId = :sirketId AND s.sonKullanmaTarihi IS NOT NULL " +
            "AND s.sonKullanmaTarihi <= :tarih ORDER BY s.sonKullanmaTarihi ASC")
    List<StokSeri> sonKullanmaYaklasan(@Param("sirketId") Long sirketId, @Param("tarih") LocalDate tarih);

    /** FEFO: SKT en yakin once, SKT yoksa giris sirasina gore stoktaki seriler. */
    @Query("SELECT s FROM StokSeri s WHERE s.stok.id = :stokId " +
            "AND s.durum = 'STOKTA' AND s.kalanMiktar > 0 " +
            "AND (:depoId IS NULL OR s.depoId = :depoId) " +
            "ORDER BY s.sonKullanmaTarihi ASC NULLS LAST, s.girisTarihi ASC, s.id ASC")
    List<StokSeri> fefoUygun(@Param("stokId") Long stokId, @Param("depoId") Long depoId);
}
