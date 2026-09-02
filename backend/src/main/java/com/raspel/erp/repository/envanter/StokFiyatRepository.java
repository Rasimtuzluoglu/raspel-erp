package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokFiyat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StokFiyatRepository extends JpaRepository<StokFiyat, Long> {
    List<StokFiyat> findByStokIdOrderByFiyatAsc(Long stokId);
    List<StokFiyat> findByStokIdInOrderByFiyatAsc(Collection<Long> stokIds);
    void deleteByStokId(Long stokId);
}
