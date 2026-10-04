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

    /**
     * Tenant-scoped cari notları. `findByCariHesapId...` yalnızca cari id'sine
     * baktığı için başka şirketin carisine ait notları döndürüyordu (cross-tenant
     * IDOR). Sorgu seviyesinde filtrelemek, servis katmanındaki kontrolün
     * atlanması hâlinde de sızıntıyı engeller (defense in depth).
     */
    List<Not> findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(Long sirketId, Long cariHesapId);

    /** Kullanıcının kişisel notları: cari notlarını hariç tutar (gizlilik + kirlilik önleme). */
    Page<Not> findBySirketIdAndKullaniciIdAndCariHesapIdIsNullOrderByOlusturmaTarihiDesc(
            Long sirketId, Long kullaniciId, Pageable pageable);

    long countByCariHesapId(Long cariHesapId);
    void deleteByCariHesapId(Long cariHesapId);
}