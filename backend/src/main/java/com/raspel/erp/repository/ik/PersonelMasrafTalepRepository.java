package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.PersonelMasrafTalep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonelMasrafTalepRepository extends JpaRepository<PersonelMasrafTalep, Long> {
    Page<PersonelMasrafTalep> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<PersonelMasrafTalep> findBySirketIdAndDurumOrderByTarihDesc(Long sirketId, String durum);
    List<PersonelMasrafTalep> findByPersonelIdOrderByTarihDesc(Long personelId);
    List<PersonelMasrafTalep> findByKullaniciIdOrderByTarihDesc(Long kullaniciId);
    long countBySirketIdAndDurum(Long sirketId, String durum);
}
