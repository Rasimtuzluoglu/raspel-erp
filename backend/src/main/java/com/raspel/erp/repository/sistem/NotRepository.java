package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotRepository extends JpaRepository<Not, Long> {
    Page<Not> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId, Pageable pageable);
    List<Not> findBySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(Long sirketId, Long kullaniciId);
    List<Not> findByCariHesapIdOrderByOlusturmaTarihiDesc(Long cariHesapId);
}