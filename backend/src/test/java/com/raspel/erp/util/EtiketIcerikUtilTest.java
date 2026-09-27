package com.raspel.erp.util;

import com.raspel.erp.entity.envanter.Stok;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EtiketIcerikUtilTest {

    private Stok stok(String barkod, String stokKodu) {
        return Stok.builder().id(7L).ad("Ürün").barkod(barkod).stokKodu(stokKodu).build();
    }

    @Test
    void gosterim_barkodOncelikli() {
        assertEquals("8690002", EtiketIcerikUtil.gosterim(stok("8690002", "PVC-B01")));
    }

    @Test
    void gosterim_barkodYoksaStokKodu() {
        assertEquals("MDF-18", EtiketIcerikUtil.gosterim(stok(null, "MDF-18")));
        assertEquals("MDF-18", EtiketIcerikUtil.gosterim(stok("  ", "MDF-18")));
    }

    @Test
    void gosterim_ikisiDeYoksaStkId() {
        assertEquals("STK7", EtiketIcerikUtil.gosterim(stok(null, null)));
    }

    @Test
    void qrIcerik_etiketKoduylaAyni() {
        assertEquals("MDF-18", EtiketIcerikUtil.qrIcerik(stok(null, "MDF-18")));
    }

    @Test
    void asciiSadelestir_turkceKarakterleriTranslitereEder() {
        assertEquals("URUN-SISE 5L", EtiketIcerikUtil.asciiSadelestir("ÜRÜN-ŞİŞE 5L"));
        assertEquals("Igdir Cagri", EtiketIcerikUtil.asciiSadelestir("Iğdır Çağrı"));
    }

    @Test
    void asciiSadelestir_asciiDisiKalanlariAtar() {
        assertEquals("ABC", EtiketIcerikUtil.asciiSadelestir("A\u20ACB\u00A0C"));
    }

    @Test
    void barkodIcerik_turkceBarkoduSadelestirir() {
        assertEquals("URUN-1", EtiketIcerikUtil.barkodIcerik(stok("ÜRÜN-1", null)));
    }

    @Test
    void barkodIcerik_barkodsuzStokKoduKodlar() {
        assertEquals("MDF-18", EtiketIcerikUtil.barkodIcerik(stok(null, "MDF-18")));
    }

    @Test
    void barkodIcerik_tamamenBosIcerikteStkId() {
        assertEquals("STK7", EtiketIcerikUtil.barkodIcerik(stok("€€", null)));
    }
}
