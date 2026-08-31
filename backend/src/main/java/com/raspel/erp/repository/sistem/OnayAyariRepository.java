package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.OnayAyari;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnayAyariRepository extends JpaRepository<OnayAyari, Long> {
    List<OnayAyari> findBySirketIdOrderByModulAsc(Long sirketId);
    Optional<OnayAyari> findBySirketIdAndModul(Long sirketId, String modul);
}
