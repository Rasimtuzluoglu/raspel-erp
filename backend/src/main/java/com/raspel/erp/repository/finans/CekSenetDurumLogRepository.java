package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CekSenetDurumLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CekSenetDurumLogRepository extends JpaRepository<CekSenetDurumLog, Long> {

    /** Bir çek/senedin zaman çizelgesi (en eski önce). */
    List<CekSenetDurumLog> findByCekSenetIdOrderByOlusturmaTarihiAscIdAsc(Long cekSenetId);

    /** Şirket geneli işlem geçmişi (en yeni önce). */
    List<CekSenetDurumLog> findBySirketIdOrderByOlusturmaTarihiDescIdDesc(Long sirketId);

    long countByCekSenetId(Long cekSenetId);
}
