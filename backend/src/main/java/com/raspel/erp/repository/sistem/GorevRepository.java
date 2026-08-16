package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Gorev;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GorevRepository extends JpaRepository<Gorev, Long> {
    List<Gorev> findByProjeIdOrderByBaslangicAsc(Long projeId);

    @Query("SELECT g FROM Gorev g, Proje p WHERE g.projeId = p.id AND p.sirketId = :sirketId " +
           "AND g.baslangic <= :bitis AND (g.bitis IS NULL OR g.bitis >= :baslangic) " +
           "ORDER BY g.baslangic ASC")
    List<Gorev> sirketGorevleri(@Param("sirketId") Long sirketId,
                                @Param("baslangic") LocalDate baslangic,
                                @Param("bitis") LocalDate bitis);
}
