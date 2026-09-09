package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.TeslimatDurumLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeslimatDurumLogRepository extends JpaRepository<TeslimatDurumLog, Long> {
    List<TeslimatDurumLog> findByTeslimatIdOrderByOlusturmaTarihiDesc(Long teslimatId);
}
