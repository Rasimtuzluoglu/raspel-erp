package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.FiyatListesi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiyatListesiRepository extends JpaRepository<FiyatListesi, Long> {
    List<FiyatListesi> findBySirketId(Long sirketId);
    List<FiyatListesi> findByStokIdOrderByGecerliBaslangicDesc(Long stokId);
}
