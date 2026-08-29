package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.TekrarlayanFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TekrarlayanFaturaRepository extends JpaRepository<TekrarlayanFatura, Long> {
    List<TekrarlayanFatura> findBySirketIdOrderByIdDesc(Long sirketId);

    List<TekrarlayanFatura> findByAktifTrueAndSonrakiCalistirmaLessThanEqual(LocalDate tarih);
}
