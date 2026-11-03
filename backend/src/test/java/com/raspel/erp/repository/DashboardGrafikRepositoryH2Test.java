package com.raspel.erp.repository;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anasayfa finans masası grafiklerinin yeni JPQL sorgularını H2 üzerinde
 * doğrular: sipariş durum dağılımı ve kategori bazlı stok değeri.
 *
 * <p>Bütçe/gerçekleşen hesabı mevcut repository metotlarını kullandığı için
 * ayrıca sorgu testi gerektirmez.
 */
@SpringBootTest
class DashboardGrafikRepositoryH2Test {

    @Autowired private SiparisRepository siparisRepository;
    @Autowired private StokRepository stokRepository;

    @Test
    void durumDagilimiTenantBazliGruplarVeAdetAzalanDoner() {
        Long sirket = 191L;
        siparis(sirket, "H2-SIP-1", "TEKLIF");
        siparis(sirket, "H2-SIP-2", "TEKLIF");
        siparis(sirket, "H2-SIP-3", "TESLIM_EDILDI");
        // Başka şirketin siparişi sayıma girmemeli.
        siparis(192L, "H2-SIP-4", "TEKLIF");

        List<Object[]> satirlar = siparisRepository.durumDagilimi(sirket);

        assertEquals(2, satirlar.size());
        assertEquals("TEKLIF", satirlar.get(0)[0]);
        assertEquals(2L, uzun(satirlar.get(0)[1]));
        assertEquals("TESLIM_EDILDI", satirlar.get(1)[0]);
        assertEquals(1L, uzun(satirlar.get(1)[1]));
    }

    @Test
    void kategoriDegerDagilimiDegerAzalanVeLimitliDoner() {
        Long sirket = 193L;
        stok(sirket, "H2-STK-A", "Gıda", 10, 100);        // 1.000
        stok(sirket, "H2-STK-B", "Gıda", 5, 100);         // 500 -> Gıda 1.500 / 2 ürün
        stok(sirket, "H2-STK-C", "Kırtasiye", 1, 2000);   // 2.000
        stok(sirket, "H2-STK-D", "Elektronik", 1, 900);
        stok(sirket, "H2-STK-E", "Mobilya", 1, 800);
        stok(sirket, "H2-STK-F", "Tekstil", 1, 700);
        stok(sirket, "H2-STK-G", "Kozmetik", 1, 600);
        stok(sirket, "H2-STK-H", "Hobi", 1, 50);          // 7. kategori -> limite takılır
        stok(sirket, "H2-STK-I", null, 1, 50000);         // kategorisiz -> dağılıma girmez
        // Başka şirketin stoğu toplama ve dağılıma girmemeli.
        stok(194L, "H2-STK-X", "Gıda", 100, 1000);

        List<Object[]> satirlar = stokRepository.kategoriDegerDagilimi(sirket, PageRequest.of(0, 6));

        assertEquals(6, satirlar.size());
        assertEquals("Kırtasiye", satirlar.get(0)[0]);
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(bigDecimal(satirlar.get(0)[1])));
        assertEquals(1L, uzun(satirlar.get(0)[2]));

        assertEquals("Gıda", satirlar.get(1)[0]);
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(bigDecimal(satirlar.get(1)[1])));
        assertEquals(2L, uzun(satirlar.get(1)[2]));

        assertTrue(satirlar.stream().noneMatch(s -> "Hobi".equals(s[0])));
    }

    @Test
    void toplamStokDegeriTenantBazliHesaplanir() {
        Long sirket = 195L;
        stok(sirket, "H2-STK-T1", "Gıda", 3, 100);
        stok(sirket, "H2-STK-T2", null, 2, 50);
        stok(196L, "H2-STK-T3", "Gıda", 100, 1000);

        BigDecimal toplam = stokRepository.toplamStokDegeriBySirketId(sirket);

        assertEquals(0, BigDecimal.valueOf(400).compareTo(toplam));
    }

    private void siparis(Long sirketId, String siparisNo, String durum) {
        siparisRepository.save(Siparis.builder()
                .sirketId(sirketId)
                .siparisNo(siparisNo)
                .tarih(LocalDate.now())
                .durum(durum)
                .build());
    }

    private void stok(Long sirketId, String stokKodu, String kategori, long miktar, long fiyat) {
        stokRepository.save(Stok.builder()
                .sirketId(sirketId)
                .stokKodu(stokKodu)
                .ad(stokKodu + " ürün")
                .kategori(kategori)
                .miktar(BigDecimal.valueOf(miktar))
                .fiyat(BigDecimal.valueOf(fiyat))
                .build());
    }

    private BigDecimal bigDecimal(Object deger) {
        return deger instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(deger));
    }

    private long uzun(Object deger) {
        return deger instanceof Number sayi ? sayi.longValue() : 0L;
    }
}
