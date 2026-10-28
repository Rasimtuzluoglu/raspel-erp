package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Kategori;
import com.raspel.erp.entity.sistem.KategoriKapsam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KategoriRepository extends JpaRepository<Kategori, Long> {

    /* ---------------- Kapsam bazlı okumalar ---------------- */

    /** Ağaç kurulumu: üst kategori tek sorguda gelir (N+1 yok). */
    @EntityGraph(attributePaths = "ustKategori")
    List<Kategori> findBySirketIdAndKapsamOrderBySiraAscAdAsc(Long sirketId, KategoriKapsam kapsam);

    @EntityGraph(attributePaths = "ustKategori")
    List<Kategori> findBySirketIdOrderByKapsamAscSiraAscAdAsc(Long sirketId);

    /** Get-or-create: aynı kapsamda aynı ad (case-insensitive) tek satırdır. */
    Optional<Kategori> findBySirketIdAndKapsamAndAdIgnoreCase(Long sirketId, KategoriKapsam kapsam, String ad);

    boolean existsBySirketIdAndKapsamAndAdIgnoreCase(Long sirketId, KategoriKapsam kapsam, String ad);

    Optional<Kategori> findBySirketIdAndKapsamAndAdIgnoreCaseAndIdNot(
            Long sirketId, KategoriKapsam kapsam, String ad, Long id);

    List<Kategori> findByUstKategoriId(Long ustKategoriId);

    long countByUstKategoriIdAndAktifTrue(Long ustKategoriId);

    /* ---------------- Sayımlar ---------------- */

    long countBySirketId(Long sirketId);

    long countBySirketIdAndKapsam(Long sirketId, KategoriKapsam kapsam);

    long countBySirketIdAndKapsamAndAktif(Long sirketId, KategoriKapsam kapsam, Boolean aktif);

    long countBySirketIdAndKapsamAndUstKategoriIdIsNotNull(Long sirketId, KategoriKapsam kapsam);

    long countBySirketIdAndUstKategoriIdIsNotNull(Long sirketId);

    /** Üst kategori başına alt kategori sayısı (tek sorgu). */
    @Query("SELECT k.ustKategori.id, COUNT(k) FROM Kategori k " +
            "WHERE k.sirketId = :sirketId AND k.ustKategori IS NOT NULL GROUP BY k.ustKategori.id")
    List<Object[]> altKategoriSayilari(@Param("sirketId") Long sirketId);

    /* ---------------- Sayfalı / filtrelenmiş liste ---------------- */

    @Query(value = "SELECT k FROM Kategori k LEFT JOIN FETCH k.ustKategori " +
            "WHERE k.sirketId = :sirketId AND k.kapsam = :kapsam " +
            "AND (:q IS NULL OR LOWER(k.ad) LIKE :q ESCAPE '\\') " +
            "AND (:aktif IS NULL OR k.aktif = :aktif) " +
            "AND (:ustKategoriId IS NULL OR k.ustKategori.id = :ustKategoriId)",
            countQuery = "SELECT COUNT(k) FROM Kategori k " +
                    "WHERE k.sirketId = :sirketId AND k.kapsam = :kapsam " +
                    "AND (:q IS NULL OR LOWER(k.ad) LIKE :q ESCAPE '\\') " +
                    "AND (:aktif IS NULL OR k.aktif = :aktif) " +
                    "AND (:ustKategoriId IS NULL OR k.ustKategori.id = :ustKategoriId)")
    Page<Kategori> listele(@Param("sirketId") Long sirketId,
                           @Param("kapsam") KategoriKapsam kapsam,
                           @Param("q") String q,
                           @Param("aktif") Boolean aktif,
                           @Param("ustKategoriId") Long ustKategoriId,
                           Pageable pageable);

    @Query(value = "SELECT k FROM Kategori k LEFT JOIN FETCH k.ustKategori " +
            "WHERE k.sirketId = :sirketId " +
            "AND (:q IS NULL OR LOWER(k.ad) LIKE :q ESCAPE '\\') " +
            "AND (:aktif IS NULL OR k.aktif = :aktif) " +
            "AND (:ustKategoriId IS NULL OR k.ustKategori.id = :ustKategoriId)",
            countQuery = "SELECT COUNT(k) FROM Kategori k " +
                    "WHERE k.sirketId = :sirketId " +
                    "AND (:q IS NULL OR LOWER(k.ad) LIKE :q ESCAPE '\\') " +
                    "AND (:aktif IS NULL OR k.aktif = :aktif) " +
                    "AND (:ustKategoriId IS NULL OR k.ustKategori.id = :ustKategoriId)")
    Page<Kategori> listeleTumKapsamlar(@Param("sirketId") Long sirketId,
                                       @Param("q") String q,
                                       @Param("aktif") Boolean aktif,
                                       @Param("ustKategoriId") Long ustKategoriId,
                                       Pageable pageable);
}
