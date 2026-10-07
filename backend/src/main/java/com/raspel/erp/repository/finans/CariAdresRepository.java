package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CariAdres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CariAdresRepository extends JpaRepository<CariAdres, Long> {

    List<CariAdres> findBySirketIdAndCariHesapIdOrderByVarsayilanDescIdAsc(Long sirketId, Long cariHesapId);

    Optional<CariAdres> findByIdAndSirketId(Long id, Long sirketId);

    long countBySirketIdAndCariHesapId(Long sirketId, Long cariHesapId);

    void deleteByCariHesapId(Long cariHesapId);
}
