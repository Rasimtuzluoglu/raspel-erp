package com.raspel.erp.repository;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.service.envanter.StokService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Satış ekranı ürün önerisi (typeahead) sorgusunu uçtan uca doğrular.
 *
 * <p>JPQL ifadesi gerçekten derlenip çalışıyor mu, tenant izolasyonu
 * sızıntı yapıyor mu ve LİMİT uygulanıyor mu — bunlar Mockito birim testleriyle
 * yakalanamaz. Önceki {@code /stoklar/ara} yolunda limit yoktu ve tüm katalog
 * belleğe geliyordu; bu testin asıl amacı o regresyonu kilitlemek.
 */
@SpringBootTest
class StokSatisOnerisiEntegrasyonH2Test {

    private static final Long SIRKET = 771L;
    private static final Long BASKA_SIRKET = 772L;

    @Autowired private StokRepository stokRepository;
    @Autowired private StokService stokService;

    private Stok stok(String kod, String ad, String barkod, Long sirketId) {
        return stokRepository.save(Stok.builder()
                .stokKodu(kod).ad(ad).barkod(barkod).birim("ADET")
                .fiyat(BigDecimal.valueOf(100)).miktar(BigDecimal.valueOf(10))
                .sirketId(sirketId).build());
    }

    @Test
    void oneriler_adOnekiVeIcindeGecenBulur() {
        stok("ON-1", "Ayçiçek Yağı 5L", "8690000000011", SIRKET);
        stok("ON-2", "Karton Kutu", null, SIRKET);
        stok("ON-3", "MDF Lam", null, SIRKET);
        stok("ON-4", "Bamba Kulü", null, SIRKET);

        var onek = stokService.satisOnerileri("ay", SIRKET, 20);
        assertTrue(onek.stream().anyMatch(s -> "Ayçiçek Yağı 5L".equals(s.getAd())),
                "Kucuk harf ile ad öneki eşleşmeli");

        var iceride = stokService.satisOnerileri("kul", SIRKET, 20);
        assertTrue(iceride.stream().anyMatch(s -> "Bamba Kulü".equals(s.getAd())),
                "Ad içinde geçen ürün de bulunmalı");
    }

    @Test
    void oneriler_limitUygular() {
        for (int i = 1; i <= 12; i++) {
            stok("LIM-" + i, "Limit Ürün " + i, null, SIRKET);
        }

        var sonuc = stokService.satisOnerileri("limit", SIRKET, 5);

        assertEquals(5, sonuc.size(), "İstenen limit aşılmamalı");
    }

    @Test
    void oneriler_limitUstSiniriniKirpar() {
        for (int i = 1; i <= 60; i++) {
            stok("UST-" + i, "Üst Sınır Ürün " + i, null, SIRKET);
        }

        // 500 isteniyor ama sunucu 50'de kesmelidir.
        assertEquals(50, stokService.satisOnerileri("üst sınır", SIRKET, 500).size());
    }

    @Test
    void oneriler_barkodOkutmadaTekSonucDoner() {
        stok("BRK-1", "Barkodlu Ürün", "8690000000099", SIRKET);

        var sonuc = stokService.satisOnerileri("8690000000099", SIRKET, 20);

        assertEquals(1, sonuc.size());
        assertEquals("Barkodlu Ürün", sonuc.get(0).getAd());
    }

    @Test
    void oneriler_tenantIzolasyonuSizar() {
        stok("IZO-1", "Kendi Kataloğum", null, SIRKET);
        stok("IZO-2", "Baska Sirket Urunu", null, BASKA_SIRKET);

        var sonuc = stokService.satisOnerileri("baska", SIRKET, 20);

        assertTrue(sonuc.stream().noneMatch(s -> "Baska Sirket Urunu".equals(s.getAd())),
                "Başka şirketin ürünü sızmamalı");
    }

    @Test
    void oneriler_bosSorguVeSirketNullGuvenli() {
        assertTrue(stokService.satisOnerileri("   ", SIRKET, 20).isEmpty());
        assertTrue(stokService.satisOnerileri(null, SIRKET, 20).isEmpty());
        assertTrue(stokService.satisOnerileri("bir", null, 20).isEmpty());
    }
}
