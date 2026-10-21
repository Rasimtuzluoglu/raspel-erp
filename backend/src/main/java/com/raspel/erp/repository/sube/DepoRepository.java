package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.Depo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepoRepository extends JpaRepository<Depo, Long> {
    Page<Depo> findBySirketIdOrderByAdAsc(Long sirketId, Pageable pageable);
    List<Depo> findBySirketIdAndAktifTrue(Long sirketId);
    boolean existsBySirketIdAndAdIgnoreCase(Long sirketId, String ad);

    /** KPI özeti: şirketin toplam depo sayısı. */
    long countBySirketId(Long sirketId);

    /** KPI özeti: şirketin aktif depo sayısı. */
    long countBySirketIdAndAktifTrue(Long sirketId);

    /** Bir şubeye bağlı depo var mı? (silme guard'ı) */
    boolean existsBySubeId(Long subeId);

    /** Bir şubeye bağlı depolar (alfabetik). */
    List<Depo> findBySubeIdAndSirketIdOrderByAdAsc(Long subeId, Long sirketId);
}
