package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.ReceteKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceteKalemRepository extends JpaRepository<ReceteKalem, Long> {
    List<ReceteKalem> findByReceteId(Long receteId);
    void deleteByReceteId(Long receteId);
}
