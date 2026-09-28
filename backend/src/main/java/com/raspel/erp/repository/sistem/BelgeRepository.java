package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Belge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BelgeRepository extends JpaRepository<Belge, Long> {
    List<Belge> findByEntityAdiAndEntityIdAndSirketIdOrderByOlusturmaTarihiDesc(String entityAdi, Long entityId, Long sirketId);
    List<Belge> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    Page<Belge> findBySirketId(Long sirketId, Pageable pageable);
    List<Belge> findByUrlEndingWith(String filename);
}
