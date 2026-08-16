package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Belge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BelgeRepository extends JpaRepository<Belge, Long> {
    List<Belge> findByEntityAdiAndEntityIdOrderByOlusturmaTarihiDesc(String entityAdi, Long entityId);
    List<Belge> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    void deleteByEntityAdiAndEntityId(String entityAdi, Long entityId);
}