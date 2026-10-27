package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.IrsaliyeDurumLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IrsaliyeDurumLogRepository extends JpaRepository<IrsaliyeDurumLog, Long> {
    List<IrsaliyeDurumLog> findByIrsaliyeIdOrderByOlusturmaTarihiAscIdAsc(Long irsaliyeId);
}
