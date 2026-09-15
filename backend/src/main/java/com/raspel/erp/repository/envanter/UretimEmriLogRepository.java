package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.UretimEmriLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UretimEmriLogRepository extends JpaRepository<UretimEmriLog, Long> {
    List<UretimEmriLog> findByUretimEmriIdOrderByOlusturmaTarihiDesc(Long uretimEmriId);
}
