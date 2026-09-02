package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CariFiyat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CariFiyatRepository extends JpaRepository<CariFiyat, Long> {
    List<CariFiyat> findByCariHesapIdOrderByStokId(Long cariHesapId);
    Optional<CariFiyat> findByCariHesapIdAndStokId(Long cariHesapId, Long stokId);
    void deleteByCariHesapId(Long cariHesapId);
}
