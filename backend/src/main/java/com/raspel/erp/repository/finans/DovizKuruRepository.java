package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.DovizKuru;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DovizKuruRepository extends JpaRepository<DovizKuru, Long> {
    List<DovizKuru> findByTarihOrderByDovizKoduAsc(LocalDate tarih);
    Optional<DovizKuru> findByDovizKoduAndTarih(String dovizKodu, LocalDate tarih);
    Optional<DovizKuru> findByDovizKodu(String dovizKodu);
}
