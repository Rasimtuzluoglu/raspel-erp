package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SohbetGenelOkuma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SohbetGenelOkumaRepository extends JpaRepository<SohbetGenelOkuma, Long> {

    Optional<SohbetGenelOkuma> findBySirketIdAndKullaniciId(Long sirketId, Long kullaniciId);
}
