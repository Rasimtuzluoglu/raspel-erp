package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.MuhasebeFisKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MuhasebeFisKalemRepository extends JpaRepository<MuhasebeFisKalem, Long> {
    List<MuhasebeFisKalem> findByFisIdOrderByIdAsc(Long fisId);

    @Query("SELECT k FROM MuhasebeFisKalem k WHERE k.fisId IN (SELECT f.id FROM MuhasebeFisi f WHERE f.sirketId = :sirketId) ORDER BY k.hesapKodu, k.id")
    List<MuhasebeFisKalem> findBySirketId(@Param("sirketId") Long sirketId);

    void deleteByFisId(Long fisId);
}
