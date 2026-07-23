package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Sube;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubeRepository extends JpaRepository<Sube, Long> {
    List<Sube> findBySirketIdOrderByAdAsc(Long sirketId);
    List<Sube> findBySirketIdAndAktifTrue(Long sirketId);
}
