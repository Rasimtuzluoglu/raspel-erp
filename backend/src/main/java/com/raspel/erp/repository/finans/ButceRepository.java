package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Butce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ButceRepository extends JpaRepository<Butce, Long> {
    List<Butce> findBySirketIdOrderByYilDescAyDesc(Long sirketId);
}
