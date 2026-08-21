package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Masraf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MasrafRepository extends JpaRepository<Masraf, Long> {
    Page<Masraf> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Masraf> findBySirketIdAndTarihBetween(Long sirketId, LocalDate baslangic, LocalDate bitis);
}
