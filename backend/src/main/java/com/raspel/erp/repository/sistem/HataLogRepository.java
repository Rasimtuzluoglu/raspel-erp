package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.HataLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HataLogRepository extends JpaRepository<HataLog, Long> {
    List<HataLog> findTop50ByOrderByOlusturmaTarihiDesc();

    @Modifying
    @Query("DELETE FROM HataLog h WHERE h.olusturmaTarihi < :tarih")
    int deleteOlderThan(@Param("tarih") LocalDateTime tarih);
}
