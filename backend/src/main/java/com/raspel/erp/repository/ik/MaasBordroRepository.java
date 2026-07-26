package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.MaasBordro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaasBordroRepository extends JpaRepository<MaasBordro, Long> {
    Page<MaasBordro> findBySirketIdOrderByYilDescAyDesc(Long sirketId, Pageable pageable);
    List<MaasBordro> findByPersonelIdOrderByYilDescAyDesc(Long personelId);
}
