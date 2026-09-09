package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AjandaHatirlatici;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AjandaHatirlaticiRepository extends JpaRepository<AjandaHatirlatici, Long> {
    List<AjandaHatirlatici> findByKullaniciIdOrderByHatirlatmaZamaniAsc(Long kullaniciId);
    List<AjandaHatirlatici> findByBildirildiFalseAndHatirlatmaZamaniLessThanEqual(LocalDateTime zaman);
}
