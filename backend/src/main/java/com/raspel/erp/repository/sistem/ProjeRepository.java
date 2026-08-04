package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Proje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjeRepository extends JpaRepository<Proje, Long> {
    Page<Proje> findBySirketIdOrderByBaslangicDesc(Long sirketId, Pageable pageable);
}