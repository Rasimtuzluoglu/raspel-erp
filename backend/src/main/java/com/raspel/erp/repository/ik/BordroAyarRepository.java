package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.BordroAyar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BordroAyarRepository extends JpaRepository<BordroAyar, Long> {
    Optional<BordroAyar> findBySirketIdAndYil(Long sirketId, Integer yil);
}
