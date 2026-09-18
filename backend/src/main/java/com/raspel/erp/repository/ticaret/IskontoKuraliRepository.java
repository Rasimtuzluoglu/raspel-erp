package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.IskontoKurali;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IskontoKuraliRepository extends JpaRepository<IskontoKurali, Long> {

    Page<IskontoKurali> findBySirketIdOrderByOncelikAscIdDesc(Long sirketId, Pageable pageable);

    List<IskontoKurali> findBySirketIdAndAktifTrue(Long sirketId);
}
