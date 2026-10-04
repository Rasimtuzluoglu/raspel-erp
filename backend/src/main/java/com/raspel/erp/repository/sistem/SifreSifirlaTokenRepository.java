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

    /**
     * Kullanıcının tüm şifre sıfırlama token'larını geçersiz kılar.
     *
     * <p><b>clearAutomatically:</b> bulk UPDATE persistence context'i
     * güncellemez. Aynı transaction içinde token okunmuşsa ve sonra flush
     * olursa {@code kullanildi=false} geri yazılır, yani TOKEN YENİDEN
     * KULLANILABİLİR hale gelirdi (şifre sıfırlama tek kullanımlı olmalı).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE SifreSifirlaToken t SET t.kullanildi = true WHERE t.kullaniciId = :kullaniciId AND t.kullanildi = false")
    int kullaniciTokenlariniGecersizKil(@Param("kullaniciId") Long kullaniciId);
}
