package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AjandaGorev;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AjandaGorevRepository extends JpaRepository<AjandaGorev, Long> {
    List<AjandaGorev> findByKullaniciIdOrderByBitisTarihiAsc(Long kullaniciId);
    List<AjandaGorev> findByKullaniciIdAndBitisTarihiBetweenOrderByBitisTarihiAsc(
            Long kullaniciId, LocalDate baslangic, LocalDate bitis);
}
