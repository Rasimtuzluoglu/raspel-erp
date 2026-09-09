package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetOdaUye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SohbetOdaUyeRepository extends JpaRepository<SohbetOdaUye, Long> {
    List<SohbetOdaUye> findByOdaId(Long odaId);
    List<SohbetOdaUye> findByKullaniciId(Long kullaniciId);
    boolean existsByOdaIdAndKullaniciId(Long odaId, Long kullaniciId);
    void deleteByOdaId(Long odaId);
    void deleteByOdaIdAndKullaniciId(Long odaId, Long kullaniciId);
    long countByOdaId(Long odaId);
}
