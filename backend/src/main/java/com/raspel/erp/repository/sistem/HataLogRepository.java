package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.HataLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HataLogRepository extends JpaRepository<HataLog, Long> {
    List<HataLog> findTop50ByOrderByOlusturmaTarihiDesc();
}
