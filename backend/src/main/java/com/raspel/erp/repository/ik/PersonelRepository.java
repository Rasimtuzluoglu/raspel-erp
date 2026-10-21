package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.Personel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonelRepository extends JpaRepository<Personel, Long>, JpaSpecificationExecutor<Personel> {
    Page<Personel> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Personel> findBySirketIdAndRolOrderByAdAsc(Long sirketId, String rol);
    List<Personel> findBySirketIdAndRolAndAktifTrue(Long sirketId, String rol);
    List<Personel> findBySirketIdAndAktifTrue(Long sirketId);
    Optional<Personel> findByKullaniciId(Long kullaniciId);
    long countByAktifTrueAndSirketId(Long sirketId);
    long countBySirketId(Long sirketId);
    long countBySirketIdAndAktifFalse(Long sirketId);
    long countBySirketIdAndIseGirisTarihiBetween(Long sirketId, LocalDate baslangic, LocalDate bitis);

    /** Filtre seçenekleri: şirketteki farklı departmanlar. */
    @Query("SELECT DISTINCT p.departman FROM Personel p " +
            "WHERE p.sirketId = :sirketId AND p.departman IS NOT NULL AND p.departman <> '' " +
            "ORDER BY p.departman")
    List<String> findDistinctDepartmanBySirketId(@Param("sirketId") Long sirketId);

    /** TC kimlik tekilliği (global). */
    boolean existsByTcKimlik(String tcKimlik);

    /** Güncellemede kaydın kendisi hariç TC tekilliği. */
    boolean existsByTcKimlikAndIdNot(String tcKimlik, Long id);
}
