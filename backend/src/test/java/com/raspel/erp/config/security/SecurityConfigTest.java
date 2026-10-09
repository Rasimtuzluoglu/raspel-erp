package com.raspel.erp.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Test DB'sinde sistem.rol / sistem.yetki seed'i yok ve YetkiKontrol
    // fail-closed oldugu icin gercek kod cozumlemesi kurulamaz. Mock ile
    // yalnizca "hangi uc hangi yetki KODUNU istiyor" baglantisi dogrulanir.
    @MockBean
    private YetkiKontrol yetkiKontrol;

    @BeforeEach
    void yetkiKontrolTemizle() {
        // Varsayilan: hicbir yetki kodu taninmasin. Boylece her test kendi
        // stub'unu acikca kurar; testler birbirine sizmasin.
        given(yetkiKontrol.kontrol(any(), anyString())).willReturn(false);
    }

    @Test
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void loginEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/api/kullanicilar/giris"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void preflightRequest_isPermitted() throws Exception {
        mockMvc.perform(options("/api/kullanicilar")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedEndpoint_returnsUnauthorizedWhenNoToken() throws Exception {
        mockMvc.perform(get("/api/kullanicilar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedEndpoint_returnsUnauthorizedWhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/kullanicilar")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsConfiguration_blocksDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/kullanicilar")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://evil.com"))
                .andExpect(status().isForbidden());
    }

    // --- HTTP katmanı yetki (method security, filtreler acik) ---

    @Test
    void adminOnlyDelete_forbiddenForUserRole() throws Exception {
        mockMvc.perform(delete("/api/stoklar/1").with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyDelete_forbiddenForDriverRole() throws Exception {
        mockMvc.perform(delete("/api/stoklar/1").with(user("test-driver").roles("DRIVER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyDelete_allowedForAdminRole() throws Exception {
        // Yetki kapisi gecilir; sonuc (404/200) onemli degil, 401/403 OLMAMALI.
        mockMvc.perform(delete("/api/stoklar/1").with(user("test-admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- PDF rapor uclari ---
    //
    // DENETIM "her USER her faturayi basabiliyor" diyordu. Gozden gecirildi:
    // sinif kurali zaten hasAnyRole('ADMIN','USER') idi, yani dusuk yetkili
    // roller (DRIVER/SAHA) PDF uclarina zaten 403 aliyordu. USER'in
    // FATURA_READ yetkisi oldugu icin zaten goruntuleyebildigi faturayi basmasi
    // ayrica bir yetki yukseltmesi DEGILDIR. Yani aktif bir acik yoktu.
    //
    // Gercek bulgu: MUHASEBE rolu sinif kuralinda yer almIYORDU. Muhasebeci
    // kendi yetkileri tam olmasina ragmen (FATURA_READ/IRSALIYE_READ/WRITE)
    // fatura ve irsaliye PDF'i basamiyordu. Asil duzeltme bu.

    @Test
    void pdfFatura_durumSahiForbidden() throws Exception {
        mockMvc.perform(get("/api/rapor/fatura/1").with(user("test-saha").roles("SAHA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pdfIrsaliye_soforForbidden() throws Exception {
        mockMvc.perform(get("/api/rapor/irsaliye/1").with(user("test-driver").roles("DRIVER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pdfFatura_adminIcinYetkiKapisiGecilmeli() throws Exception {
        mockMvc.perform(get("/api/rapor/fatura/1").with(user("test-admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void pdfFatura_muhasebeArtikErisebilir() throws Exception {
        mockMvc.perform(get("/api/rapor/fatura/1").with(user("test-muhasebe").roles("MUHASEBE")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void pdfSiparis_muhasebeArtikErisebilir() throws Exception {
        mockMvc.perform(get("/api/rapor/siparis/1").with(user("test-muhasebe").roles("MUHASEBE")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void pdfFatura_userErisimiKorundu() throws Exception {
        // Regresyon guardi: USER fatura basabilmeye devam etmeli.
        mockMvc.perform(get("/api/rapor/fatura/1").with(user("test-user").roles("USER")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- CSV import uclari (Wave 2.4) ---
    //
    // DENETIM "USER toplu stok/cari/hareket/fatura yazabiliyor" diyordu ve
    // "IMPORT yetki kodu yok" tespiti DOGRUYDU: sistem.yetki tablosunda hicbir
    // IMPORT/* kodu bulunmuyor. Yanlis olan, bunu bir acik saymamiz: import,
    // yazmanin bir alternatifi oldugu icin ayri bir yetki kodu uretilmedi,
    // onun yerine her ucun yazdigi ALANIN mevcut yetki kodu kullanildi.
    //
    // Gercek duzeltme: once uc sadece ROLE'e bakiyordu; yonetici bir
    // kullanicidan CARI_WRITE'i geri aldiginda o kullanici toplu cari yazmaya
    // DEVAM ediyordu (yan etki olarak). Artik alan bazli kapi var.
    //
    // Ikinci bulgu: MUHASEBE rolu sinif kuralinda yoktu; kendi
    // CARI/FATURA/FINANS_WRITE yetkileri varken toplu aktaramiyordu.
    //
    // NOT: Test DB'sinde sistem.rol/sistem.yetki seed'i YOKTUR (schema.sql
    // yalnizca sistem.sirket + ip_whitelist olusturur) ve YetkiKontrol
    // fail-closed oldugu icin seed'siz pozitif yetki senaryosu kurulamaz.
    // Bu yuzden YetkiKontrol mock'lanir: test, yetki KODUNUN hangi uca
    // baglandigini dogrular (kod bazli esleme), gercek DB eslesmesini degil.

    @Test
    void importStok_stokWriteYetkisiYoksaForbidden() throws Exception {
        // ROLE USER olsa bile STOK_WRITE cagrisi false donerse 403 olmali.
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("STOK_WRITE"))).willReturn(false);
        mockMvc.perform(multipart("/api/import/stok")
                        .file(importDosyasi())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importStok_stokWriteYetkisiVarsaYetkiKapisiGecilmeli() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("STOK_WRITE"))).willReturn(true);
        // Sonuc (400/500) onemli degil; 401/403 OLMAMALI.
        mockMvc.perform(multipart("/api/import/stok").file(importDosyasi())
                        .with(user("test-user").roles("USER")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void importCari_cariWriteYetkisiYoksaForbidden() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("CARI_WRITE"))).willReturn(false);
        mockMvc.perform(multipart("/api/import/cari").file(importDosyasi())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importHareket_finansWriteYetkisiYoksaForbidden() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("FINANS_WRITE"))).willReturn(false);
        mockMvc.perform(multipart("/api/import/hareket").file(importDosyasi())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importAlisFatura_faturaWriteYetkisiYoksaForbidden() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("FATURA_WRITE"))).willReturn(false);
        mockMvc.perform(multipart("/api/import/alis-fatura").file(importDosyasi())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importStok_yetkiKoduYanlisKodDegil() throws Exception {
        // Regresyon guardi: stok importu CARI_WRITE degil STOK_WRITE istemeli.
        // Yanlis kod baglanirsa bu test kirmizi donerdi (403 beklenirken gecer).
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("STOK_WRITE"))).willReturn(false);
        given(yetkiKontrol.kontrol(any(Authentication.class), eq("CARI_WRITE"))).willReturn(true);
        mockMvc.perform(multipart("/api/import/stok").file(importDosyasi())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importStok_adminYetkiKontroluneGerekKalmadanGecer() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), anyString())).willReturn(false);
        mockMvc.perform(multipart("/api/import/stok").file(importDosyasi())
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void importStok_durumSahiForbidden() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), anyString())).willReturn(true);
        mockMvc.perform(multipart("/api/import/stok").file(importDosyasi())
                        .with(user("test-saha").roles("SAHA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importCari_muhasebeArtikErisebilir() throws Exception {
        given(yetkiKontrol.kontrol(any(Authentication.class), anyString())).willReturn(true);
        mockMvc.perform(multipart("/api/import/cari").file(importDosyasi())
                        .with(user("test-muhasebe").roles("MUHASEBE")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    private MockMultipartFile importDosyasi() {
        return new MockMultipartFile("file", "test.csv", "text/csv",
                "ad;stokKodu\nTest Urun;TEST-1".getBytes(StandardCharsets.UTF_8));
    }
}
