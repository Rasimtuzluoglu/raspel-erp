package com.raspel.erp.repository;

import com.raspel.erp.entity.IrsaliyeKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IrsaliyeKalemRepository extends JpaRepository<IrsaliyeKalem, Long> {
    List<IrsaliyeKalem> findByIrsaliyeId(Long irsaliyeId);
    void deleteByIrsaliyeId(Long irsaliyeId);
}
