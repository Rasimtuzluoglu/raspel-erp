package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokMaliyetHareket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StokMaliyetHareketRepository extends JpaRepository<StokMaliyetHareket, Long> {

    List<StokMaliyetHareket> findByStokIdOrderByTarihAscIdAsc(Long stokId);

    @Query("SELECT m FROM StokMaliyetHareket m WHERE m.stokId = :stokId ORDER BY m.tarih DESC, m.id DESC")
    List<StokMaliyetHareket> sonHareketler(@Param("stokId") Long stokId);
}
