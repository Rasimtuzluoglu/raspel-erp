package com.raspel.erp.repository;

import com.raspel.erp.entity.Irsaliye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IrsaliyeRepository extends JpaRepository<Irsaliye, Long> {
    List<Irsaliye> findBySirketIdOrderByTarihDesc(Long sirketId);
    List<Irsaliye> findByFaturaId(Long faturaId);
}
