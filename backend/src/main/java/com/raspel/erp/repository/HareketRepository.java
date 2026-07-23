package com.raspel.erp.repository;

import com.raspel.erp.entity.Hareket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * Hareket Repository
 * Hareket entity'si için veritabanı işlemlerini yönetir.
 */
@Repository
public interface HareketRepository extends JpaRepository<Hareket, Long> {
    
    /**
     * Belirli bir cari hesaba ait hareketleri getir
     */
    List<Hareket> findByCariHesapIdOrderByHareketTarihiDesc(Long cariHesapId);

    List<Hareket> findBySirketIdOrderByHareketTarihiDesc(Long sirketId);
    
    /**
     * Son n hareketi getir
     */
    List<Hareket> findAllByOrderByHareketTarihiDescOlusturmaTarihiDesc(Pageable pageable);

    /**
     * Tüm hareketleri tarihe göre sıralı getir
     */
    List<Hareket> findAllByOrderByHareketTarihiDesc();

    /**
     * Belirli bir cari hesaba ait hareket sayısı
     */
    long countByCariHesapId(Long cariHesapId);

    /**
     * Tarih aralığına göre hareketleri getir
     */
    List<Hareket> findByHareketTarihiBetweenOrderByHareketTarihiDesc(LocalDate baslangic, LocalDate bitis);

    /**
     * Cari hesap ve tarih aralığına göre hareketleri getir
     */
    List<Hareket> findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiDesc(Long cariHesapId, LocalDate baslangic, LocalDate bitis);
    List<Hareket> findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(Long cariHesapId, LocalDate baslangic, LocalDate bitis);
    List<Hareket> findByHareketTarihiBetweenOrderByHareketTarihiAsc(LocalDate baslangic, LocalDate bitis);
}
