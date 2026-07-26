package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.IadeKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IadeKalemRepository extends JpaRepository<IadeKalem, Long> {
    List<IadeKalem> findByIadeId(Long iadeId);
    void deleteByIadeId(Long iadeId);
}
