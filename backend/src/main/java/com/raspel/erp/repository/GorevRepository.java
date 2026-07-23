package com.raspel.erp.repository;

import com.raspel.erp.entity.Gorev;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GorevRepository extends JpaRepository<Gorev, Long> {
    List<Gorev> findByProjeIdOrderByBaslangicAsc(Long projeId);
}
