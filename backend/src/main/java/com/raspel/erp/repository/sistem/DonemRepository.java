package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Donem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonemRepository extends JpaRepository<Donem, Long> {
    Page<Donem> findBySirketIdOrderByBaslangicDesc(Long sirketId, Pageable pageable);
    List<Donem> findBySirketIdAndAktifTrue(Long sirketId);
}