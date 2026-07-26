package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Depo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepoRepository extends JpaRepository<Depo, Long> {
    Page<Depo> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Depo> findBySubeId(Long subeId);
    List<Depo> findBySirketIdAndAktifTrue(Long sirketId);
}
