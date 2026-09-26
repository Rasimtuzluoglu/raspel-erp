package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Bildirim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BildirimRepository extends JpaRepository<Bildirim, Long> {
    List<Bildirim> findTop50BySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    long countBySirketIdAndOkunduFalse(Long sirketId);

    /** Kullanıcının kendi bildirimleri + şirkete ait genel bildirimler (kullanici_id NULL). */
    @Query("SELECT b FROM Bildirim b WHERE b.sirketId = :sirketId " +
           "AND (b.kullaniciId IS NULL OR b.kullaniciId = :kullaniciId) " +
           "ORDER BY b.olusturmaTarihi DESC")
    List<Bildirim> kullaniciBildirimleri(@Param("sirketId") Long sirketId,
                                         @Param("kullaniciId") Long kullaniciId);

    @Query("SELECT COUNT(b) FROM Bildirim b WHERE b.sirketId = :sirketId AND b.okundu = false " +
           "AND (b.kullaniciId IS NULL OR b.kullaniciId = :kullaniciId)")
    long kullaniciOkunmamisSayisi(@Param("sirketId") Long sirketId,
                                  @Param("kullaniciId") Long kullaniciId);
}
