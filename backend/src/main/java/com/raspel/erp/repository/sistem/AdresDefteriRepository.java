package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AdresDefteri;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdresDefteriRepository extends JpaRepository<AdresDefteri, Long> {

    Page<AdresDefteri> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);

    // LDESTEK: arama parametreleri artik HAM terim DEGIL, `AramaTemizleyici.like(q)`
    // ile uretilen kucuk-harfli/joker-kacisli `%terim%` DESENIDIR.
    //
    // ONCEKI HAL `LOWER(CONCAT('%', :q, '%'))` idi ve PostgreSQL'de
    // "function lower(bytea) does not exist" hatasi veriyordu: parametre tipi
    // cozulemeyip bytea'ya dusuyordu (q null olsa bile). H2 bunu tolere ettigi
    // icin testlerde gorunmuyordu. FaturaRepository.ara ile ayni kanitli kaliba
    // gecirildi: `LOWER(sutun) LIKE :q ESCAPE '\\'`.
    @Query("SELECT a FROM AdresDefteri a WHERE a.sirketId = :sirketId " +
           "AND (:tur IS NULL OR a.tur = :tur) " +
           "AND (:etiket IS NULL OR LOWER(COALESCE(a.etiketler, '')) LIKE :etiket ESCAPE '\\') " +
           "AND (:q IS NULL OR LOWER(a.ad) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(COALESCE(a.telefon, '')) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(COALESCE(a.email, '')) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(COALESCE(a.adres, '')) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(COALESCE(a.etiketler, '')) LIKE :q ESCAPE '\\') " +
           "ORDER BY a.ad ASC")
    Page<AdresDefteri> filtreli(@Param("sirketId") Long sirketId,
                                @Param("q") String q,
                                @Param("tur") String tur,
                                @Param("etiket") String etiket,
                                Pageable pageable);

    @Query("SELECT DISTINCT a.tur FROM AdresDefteri a WHERE a.sirketId = :sirketId " +
           "AND a.tur IS NOT NULL AND a.tur <> '' ORDER BY a.tur")
    List<String> turListesi(@Param("sirketId") Long sirketId);

    @Query("SELECT a.etiketler FROM AdresDefteri a WHERE a.sirketId = :sirketId " +
           "AND a.etiketler IS NOT NULL AND a.etiketler <> ''")
    List<String> etiketSatirlari(@Param("sirketId") Long sirketId);
}
