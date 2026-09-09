package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.PosTerminali;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosTerminaliRepository extends JpaRepository<PosTerminali, Long> {
    List<PosTerminali> findBySirketIdOrderByAd(Long sirketId);
    List<PosTerminali> findBySirketIdAndAktifTrueOrderByAd(Long sirketId);
}
