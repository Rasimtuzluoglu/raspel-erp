package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SifreSifirlaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SifreSifirlaTokenRepository extends JpaRepository<SifreSifirlaToken, Long> {

    Optional<SifreSifirlaToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM SifreSifirlaToken t WHERE t.sonKullanma < :esik OR t.kullanildi = true")
    int eskiTokenlariTemizle(@Param("esik") LocalDateTime esik);

    @Modifying
    @Query("UPDATE SifreSifirlaToken t SET t.kullanildi = true WHERE t.kullaniciId = :kullaniciId AND t.kullanildi = false")
    int kullaniciTokenlariniGecersizKil(@Param("kullaniciId") Long kullaniciId);
}
