package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.PushAbonelik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushAbonelikRepository extends JpaRepository<PushAbonelik, Long> {

    Optional<PushAbonelik> findByEndpoint(String endpoint);

    List<PushAbonelik> findBySirketId(Long sirketId);

    List<PushAbonelik> findByKullaniciId(Long kullaniciId);

    void deleteByEndpoint(String endpoint);
}
