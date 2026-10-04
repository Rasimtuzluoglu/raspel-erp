package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.service.sistem.DosyaDepolamaService;
import com.raspel.erp.config.TenantChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DosyaDepolamaService dosyaDepolama;

    @MockBean
    private TenantChecker tenantChecker;

    @MockBean
    private com.raspel.erp.service.sistem.ResimIslemeService resimIslemeService;

    @Test
    void uploadAvatar_bosDosyaReddedilir() throws Exception {
        MockMultipartFile bos = new MockMultipartFile("file", "bos.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/upload/avatar").file(bos))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFoto_gecersizTipReddedilir() throws Exception {
        MockMultipartFile dosya = new MockMultipartFile("file", "dosya.exe", "application/octet-stream", "x".getBytes());
        mockMvc.perform(multipart("/api/upload/foto").file(dosya))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvatar_olmayanDosya404Doner() throws Exception {
        when(dosyaDepolama.getir(any(), any())).thenReturn(null);
        mockMvc.perform(get("/api/uploads/avatars/bulunmayan.png"))
                .andExpect(status().isNotFound());
    }

    /**
     * C7: sohbet eki yalnızca tenant klasöründen okunur. Önceden düz `sohbet/`
     * klasörü de denendiği için UUID'yi bilen bir kullanıcı başka şirketin
     * sohbet dosyasını indirebiliyordu.
     */
    @Test
    void getSohbet_yalnizcaTenantKlasorundenOkur() throws Exception {
        when(dosyaDepolama.getir("sohbet/s4", "a.png"))
                .thenReturn(new DosyaDepolamaService.DepolananDosya("x".getBytes(), "image/png"));

        when(tenantChecker.getCurrentSirketId()).thenReturn(4L);
        mockMvc.perform(get("/api/uploads/sohbet/a.png"))
                .andExpect(status().isOk());
    }

    /** C7: düz `sohbet/` klasörüne düşme kapatıldı (cross-tenant okuma yok). */
    @Test
    void getSohbet_duzKlasorDenenmez() throws Exception {
        // Yalnızca düz klasörde duran (eski) dosya artık sunulmaz.
        when(dosyaDepolama.getir("sohbet", "eski.png"))
                .thenReturn(new DosyaDepolamaService.DepolananDosya("x".getBytes(), "image/png"));

        when(tenantChecker.getCurrentSirketId()).thenReturn(4L);
        mockMvc.perform(get("/api/uploads/sohbet/eski.png"))
                .andExpect(status().isNotFound());
        verify(dosyaDepolama, never()).getir(eq("sohbet"), anyString());
    }

    /** C7: başka şirketin klasörüne düşmez. */
    @Test
    void getSohbet_baskaSirketKlasoruneDusmez() throws Exception {
        when(tenantChecker.getCurrentSirketId()).thenReturn(4L);
        when(dosyaDepolama.getir(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/uploads/sohbet/a.png"))
                .andExpect(status().isNotFound());

        verify(dosyaDepolama).getir(eq("sohbet/s4"), eq("a.png"));
        verify(dosyaDepolama, never()).getir(eq("sohbet/s9"), anyString());
        verify(dosyaDepolama, never()).getir(eq("sohbet"), anyString());
    }

    /** C7: sirket baglami yoksa sohbet dosyasi sunulmaz (klasor tahmini yapilmaz). */
    @Test
    void getSohbet_sirketBaglamiYoksa404() throws Exception {
        when(tenantChecker.getCurrentSirketId()).thenReturn(null);
        when(dosyaDepolama.getir(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/uploads/sohbet/a.png"))
                .andExpect(status().isNotFound());
        verify(dosyaDepolama, never()).getir(eq("sohbet"), anyString());
    }
}
