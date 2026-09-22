package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.IpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IpWhitelistRepository extends JpaRepository<IpWhitelist, Long> {
    List<IpWhitelist> findBySirketIdOrderByIdAsc(Long sirketId);
}
