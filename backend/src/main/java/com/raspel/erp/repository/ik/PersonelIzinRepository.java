package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.PersonelIzin;
import com.raspel.erp.entity.ik.Personel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelIzinRepository extends JpaRepository<PersonelIzin, Long> {
    List<PersonelIzin> findByPersonelIdOrderByBaslangicDesc(Long personelId);
    Page<PersonelIzin> findByPersonelIdIn(List<Long> personelIds, Pageable pageable);

    /**
     * İzin arama/filtreleme: durum, personel, tarih aralığı ve serbest metin
     * (personel adı, izin türü, açıklama). Tarih aralığı kesişimine göre çalışır.
     */
    @Query("SELECT i FROM PersonelIzin i JOIN Personel p ON i.personelId = p.id " +
            "WHERE p.sirketId = :sirketId " +
            "AND (:durum IS NULL OR i.durum = :durum) " +
            "AND (:personelId IS NULL OR i.personelId = :personelId) " +
            "AND (:baslangic IS NULL OR i.bitis >= :baslangic) " +
            "AND (:bitis IS NULL OR i.baslangic <= :bitis) " +
            "AND (:q IS NULL OR (LOWER(CONCAT(COALESCE(p.ad,''), ' ', COALESCE(p.soyad,''))) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(i.izinTuru) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(COALESCE(i.aciklama,'')) LIKE LOWER(CONCAT('%', :q, '%'))))")
    Page<PersonelIzin> filtrele(
            @Param("sirketId") Long sirketId,
            @Param("durum") String durum,
            @Param("personelId") Long personelId,
            @Param("baslangic") LocalDate baslangic,
            @Param("bitis") LocalDate bitis,
            @Param("q") String q,
            Pageable pageable);

    @Query("SELECT COUNT(i) FROM PersonelIzin i JOIN Personel p ON i.personelId = p.id WHERE i.durum = :durum AND p.sirketId = :sirketId")
    long countByDurumAndSirketId(String durum, Long sirketId);

    @Query("SELECT COUNT(i) FROM PersonelIzin i JOIN Personel p ON i.personelId = p.id WHERE i.durum = 'ONAYLANDI' AND i.baslangic <= :bugun AND i.bitis >= :bugun AND p.sirketId = :sirketId")
    long countBugunIzinliAndSirketId(LocalDate bugun, Long sirketId);
}
