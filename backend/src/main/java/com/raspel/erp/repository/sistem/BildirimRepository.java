package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Bildirim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BildirimRepository extends JpaRepository<Bildirim, Long> {
    List<Bildirim> findTop50BySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    long countBySirketIdAndOkunduFalse(Long sirketId);
}
