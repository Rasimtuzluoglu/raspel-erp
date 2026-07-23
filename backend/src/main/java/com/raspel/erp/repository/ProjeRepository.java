package com.raspel.erp.repository;

import com.raspel.erp.entity.Proje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjeRepository extends JpaRepository<Proje, Long> {
    List<Proje> findBySirketIdOrderByBaslangicDesc(Long sirketId);
}
