package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Sirket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SirketRepository extends JpaRepository<Sirket, Long> {
    List<Sirket> findByAktifTrue();
    Sirket findFirstByAktifTrueOrderByIdAsc();
    java.util.Optional<Sirket> findByKodIgnoreCase(String kod);
    boolean existsByKodIgnoreCase(String kod);
    boolean existsByKodIgnoreCaseAndIdNot(String kod, Long id);
}