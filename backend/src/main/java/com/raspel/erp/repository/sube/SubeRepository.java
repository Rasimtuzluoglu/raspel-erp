package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Sube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubeRepository extends JpaRepository<Sube, Long> {
    Page<Sube> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Sube> findBySirketIdAndAktifTrue(Long sirketId);
}
