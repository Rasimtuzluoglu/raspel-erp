package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetOdaUye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SohbetOdaUyeRepository extends JpaRepository<SohbetOdaUye, Long> {
    List<SohbetOdaUye> findByOdaId(Long odaId);

    List<SohbetOdaUye> findByKullaniciId(Long kullaniciId);

    /** Faz 8: oda listesi tek sorguda tum uyelikleri ceker (N+1 onlenir). */
    List<SohbetOdaUye> findByOdaIdIn(Collection<Long> odaIdleri);

    Optional<SohbetOdaUye> findByOdaIdAndKullaniciId(Long odaId, Long kullaniciId);

    boolean existsByOdaIdAndKullaniciId(Long odaId, Long kullaniciId);

    void deleteByOdaId(Long odaId);

    void deleteByOdaIdAndKullaniciId(Long odaId, Long kullaniciId);
}
