package com.raspel.erp.repository;

import com.raspel.erp.entity.Donem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonemRepository extends JpaRepository<Donem, Long> {
    List<Donem> findBySirketIdOrderByBaslangicDesc(Long sirketId);
    List<Donem> findBySirketIdAndAktifTrue(Long sirketId);
}
