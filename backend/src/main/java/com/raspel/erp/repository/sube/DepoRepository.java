package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Depo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepoRepository extends JpaRepository<Depo, Long> {
    List<Depo> findBySirketIdOrderByAdAsc(Long sirketId);
    List<Depo> findBySubeId(Long subeId);
    List<Depo> findBySirketIdAndAktifTrue(Long sirketId);
}
