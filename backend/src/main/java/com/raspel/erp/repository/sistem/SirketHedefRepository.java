package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.SirketHedef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SirketHedefRepository extends JpaRepository<SirketHedef, Long> {
    Optional<SirketHedef> findBySirketIdAndYilAndAy(Long sirketId, Integer yil, Integer ay);
}
