package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.DashboardTercih;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Anasayfa düzeni sorguları. Tüm okumalar tenant (sirketId) ile filtrelenir;
 * yalnızca upsert sırasında kullanıcı kimliği tek başına aranır (kullanıcı
 * şirket değiştirdiğinde çakışmayı tespit etmek için).
 */
@Repository
public interface DashboardTercihRepository extends JpaRepository<DashboardTercih, Long> {

    Optional<DashboardTercih> findByKullaniciId(Long kullaniciId);

    Optional<DashboardTercih> findByKullaniciIdAndSirketId(Long kullaniciId, Long sirketId);
}
