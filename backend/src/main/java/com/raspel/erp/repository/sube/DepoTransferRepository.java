package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.DepoTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepoTransferRepository extends JpaRepository<DepoTransfer, Long> {
    List<DepoTransfer> findBySirketIdOrderByIdDesc(Long sirketId);
    List<DepoTransfer> findBySirketIdAndDurumOrderByIdAsc(Long sirketId, String durum);
}
