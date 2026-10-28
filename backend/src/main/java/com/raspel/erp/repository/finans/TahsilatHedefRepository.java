package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.TahsilatHedef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TahsilatHedefRepository extends JpaRepository<TahsilatHedef, Long> {

    Optional<TahsilatHedef> findBySirketIdAndYilAndAy(Long sirketId, Integer yil, Integer ay);
}
