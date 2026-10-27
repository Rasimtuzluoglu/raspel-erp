package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.UretimHat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UretimHatRepository extends JpaRepository<UretimHat, Long> {
    List<UretimHat> findBySirketIdOrderByAdAsc(Long sirketId);

    Optional<UretimHat> findByIdAndSirketId(Long id, Long sirketId);

    boolean existsBySirketIdAndAdIgnoreCase(Long sirketId, String ad);
}
