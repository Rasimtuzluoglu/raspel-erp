package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Sube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubeRepository extends JpaRepository<Sube, Long> {
    Page<Sube> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Sube> findBySirketIdAndAktifTrue(Long sirketId);

    long countBySirketId(Long sirketId);
    long countBySirketIdAndAktifTrue(Long sirketId);
    long countBySirketIdAndAktifFalse(Long sirketId);

    /** Aynı şirkette aynı adlı (büyük/küçük harf duyarsız) şube var mı? */
    boolean existsBySirketIdAndAdIgnoreCase(Long sirketId, String ad);

    /** Güncelleme sırasında kaydın kendisi hariç ad çakışması kontrolü. */
    boolean existsBySirketIdAndAdIgnoreCaseAndIdNot(Long sirketId, String ad, Long id);
}
