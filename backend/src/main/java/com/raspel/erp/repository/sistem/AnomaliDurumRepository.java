package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AnomaliDurum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnomaliDurumRepository extends JpaRepository<AnomaliDurum, Long> {

    List<AnomaliDurum> findBySirketId(Long sirketId);

    Optional<AnomaliDurum> findBySirketIdAndAnahtar(Long sirketId, String anahtar);

    void deleteBySirketIdAndAnahtar(Long sirketId, String anahtar);
}
