package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {
    Optional<AiConfig> findBySirketId(Long sirketId);
    void deleteBySirketId(Long sirketId);
}
