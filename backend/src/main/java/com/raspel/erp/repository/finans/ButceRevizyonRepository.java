package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.ButceRevizyon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ButceRevizyonRepository extends JpaRepository<ButceRevizyon, Long> {
    List<ButceRevizyon> findByButceIdOrderByOlusturmaTarihiAscIdAsc(Long butceId);
}
