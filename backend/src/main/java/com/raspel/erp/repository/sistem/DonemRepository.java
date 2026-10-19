package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Donem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DonemRepository extends JpaRepository<Donem, Long> {
    Page<Donem> findBySirketIdOrderByBaslangicDesc(Long sirketId, Pageable pageable);
    List<Donem> findBySirketIdAndAktifTrue(Long sirketId);
    List<Donem> findBySirketIdAndKilitliTrue(Long sirketId);

    /**
     * Aynı şirkette verilen aralıkla kesişen dönem sayısı (kaydetme doğrulaması).
     * {@code haricId} güncellemede dönemin kendisini hariç tutmak için kullanılır.
     * Kesişim: mevcut.başlangıç <= yeni.bitiş VE mevcut.bitiş >= yeni.başlangıç.
     */
    @Query("SELECT COUNT(d) FROM Donem d WHERE d.sirketId = :sirketId " +
           "AND (:haricId IS NULL OR d.id <> :haricId) " +
           "AND d.baslangic <= :bitis AND d.bitis >= :baslangic")
    long cakisanDonemSayisi(@Param("sirketId") Long sirketId,
                            @Param("haricId") Long haricId,
                            @Param("baslangic") LocalDate baslangic,
                            @Param("bitis") LocalDate bitis);
}