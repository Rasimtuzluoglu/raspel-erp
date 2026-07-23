package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Masraf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasrafRepository extends JpaRepository<Masraf, Long> {
    List<Masraf> findBySirketIdOrderByTarihDesc(Long sirketId);
}
