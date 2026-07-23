package com.raspel.erp.repository;

import com.raspel.erp.entity.Banka;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BankaRepository extends JpaRepository<Banka, Long> {
    List<Banka> findBySirketId(Long sirketId);
}
