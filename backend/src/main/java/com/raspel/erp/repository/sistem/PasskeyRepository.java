package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Passkey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyRepository extends JpaRepository<Passkey, Long> {

    Optional<Passkey> findByCredentialId(String credentialId);

    List<Passkey> findByKullaniciIdOrderByOlusturmaTarihiDesc(Long kullaniciId);

    Optional<Passkey> findByIdAndKullaniciId(Long id, Long kullaniciId);

    long deleteByKullaniciId(Long kullaniciId);
}
