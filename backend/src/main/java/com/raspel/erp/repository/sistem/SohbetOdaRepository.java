package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetOda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SohbetOdaRepository extends JpaRepository<SohbetOda, Long> {
    List<SohbetOda> findBySirketIdOrderByAd(Long sirketId);
}
