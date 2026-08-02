package com.raspel.erp.repository;

import com.raspel.erp.entity.Sirket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SirketRepository extends JpaRepository<Sirket, Long> {
    List<Sirket> findByAktifTrue();
    Sirket findFirstByAktifTrueOrderByIdAsc();
}
