package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    List<ApiToken> findByKullaniciIdOrderByOlusturmaTarihiDesc(Long kullaniciId);
    Optional<ApiToken> findByTokenHash(String tokenHash);
}
