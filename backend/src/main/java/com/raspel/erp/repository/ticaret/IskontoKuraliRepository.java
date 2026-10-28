package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.IskontoKurali;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IskontoKuraliRepository extends JpaRepository<IskontoKurali, Long> {

    Page<IskontoKurali> findBySirketIdOrderByOncelikAscIdDesc(Long sirketId, Pageable pageable);

    List<IskontoKurali> findBySirketIdAndAktifTrue(Long sirketId);

    // ---------- Kategori registry entegrasyonu ----------

    long countByKategoriId(Long kategoriId);

    @Query("SELECT i.kategoriId, COUNT(i) FROM IskontoKurali i "
            + "WHERE i.sirketId = :sirketId AND i.kategoriId IS NOT NULL GROUP BY i.kategoriId")
    List<Object[]> kategoriKullanimSayilari(@Param("sirketId") Long sirketId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE IskontoKurali i SET i.kategori = :ad WHERE i.kategoriId = :kategoriId")
    int kategoriAdGuncelle(@Param("kategoriId") Long kategoriId, @Param("ad") String ad);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE IskontoKurali i SET i.kategoriId = :hedefId, i.kategori = :hedefAd "
            + "WHERE i.kategoriId = :kaynakId")
    int kategoriBirlestir(@Param("kaynakId") Long kaynakId,
                          @Param("hedefId") Long hedefId,
                          @Param("hedefAd") String hedefAd);
}
