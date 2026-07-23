package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Iade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IadeRepository extends JpaRepository<Iade, Long> {
    List<Iade> findBySirketIdOrderByTarihDesc(Long sirketId);
}
