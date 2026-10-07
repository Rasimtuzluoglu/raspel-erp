package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Kullanici;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {
    Optional<Kullanici> findByUsername(String username);
    List<Kullanici> findBySirketIdAndRole(Long sirketId, String role);
    org.springframework.data.domain.Page<Kullanici> findBySirketId(Long sirketId, org.springframework.data.domain.Pageable pageable);

    /**
     * Faz 0.2: JWT'deki sirketId claim'inin kullanicinin gercek uyeligiyle
     * eslestigini dogrulamak icin. Kullanici-şirket iliskisi ManyToMany
     * (kullanici_sirket) tablosundadir; lazy koleksiyon yerine DB'de sayim
     * yapilir (JwtAuthFilter'da oturum olmadigi icin).
     */
    @Query("SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END " +
           "FROM Kullanici k JOIN k.sirketler s WHERE k.username = :username AND s.id = :sirketId")
    boolean sirketUyeligiVarMi(@Param("username") String username, @Param("sirketId") Long sirketId);

    /**
     * Faz 0.11: TOTP replay koruması icin atomik karsilastir-ve-yaz. Ayni zaman
     * adimi icin iki es zamanli istek gelirse yalnizca BIRI 1 satir gunceller;
     * digeri 0 alir (replay). Boylece check-then-act yarisi kapanir.
     */
    @Modifying
    @Query("UPDATE Kullanici k SET k.twoFactorLastCounter = :counter " +
           "WHERE k.id = :id AND (k.twoFactorLastCounter IS NULL OR k.twoFactorLastCounter < :counter)")
    int totpCounterGuncelle(@Param("id") Long id, @Param("counter") long counter);
}