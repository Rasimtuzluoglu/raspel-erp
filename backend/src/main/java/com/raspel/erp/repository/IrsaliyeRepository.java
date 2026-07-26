package com.raspel.erp.repository;

import com.raspel.erp.entity.Irsaliye;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IrsaliyeRepository extends JpaRepository<Irsaliye, Long> {
    Page<Irsaliye> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Irsaliye> findByFaturaId(Long faturaId);
}
