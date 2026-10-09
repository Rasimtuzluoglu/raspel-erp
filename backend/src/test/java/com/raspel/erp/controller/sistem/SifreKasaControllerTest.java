package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.SifreKasaDTO;
import com.raspel.erp.dto.sistem.SifreKasaOzetDTO;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.service.sistem.SifreKasaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SifreKasaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
@ContextConfiguration(classes = {SifreKasaController.class,
        com.raspel.erp.config.GlobalExceptionHandler.class, TestSecurityMocks.class})
class SifreKasaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SifreKasaService sifreKasaService;

    private SifreKasaDTO ornek() {
        return SifreKasaDTO.builder()
                .id(1L).baslik("Netsis").kullaniciAdi("muhasebe")
                .kapsam("KISISEL").sifreGorunurlugu("SAHIS").durum("GECERLI")
                .build();
    }

    @Test
    void listele_sifreAlaniIcermez() throws Exception {
        given(sifreKasaService.listele(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of(ornek()));

        mockMvc.perform(get("/api/sifre-kasa").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baslik").value("Netsis"))
                // Liste yanitinda sifre anahtari HIC olmamali.
                .andExpect(jsonPath("$[0].sifre").doesNotExist());
    }

    @Test
    void sifreyiAc_noStoreOnbellekBasligiDoner() throws Exception {
        given(sifreKasaService.sifreyiGoster(eq(1L), any(), any(), anyBoolean(), any()))
                .willReturn("CokGizli123!");

        mockMvc.perform(get("/api/sifre-kasa/1/sifre")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sifre").value("CokGizli123!"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void olustur_201Doner() throws Exception {
        given(sifreKasaService.olustur(any(), any(), any(), anyBoolean())).willReturn(ornek());

        mockMvc.perform(post("/api/sifre-kasa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baslik\":\"Netsis\",\"sifre\":\"Gizli123\"}")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isCreated());
    }

    @Test
    void olustur_globalYetkiYoksa400Doner() throws Exception {
        // Servis BusinessException atar -> GlobalExceptionHandler 400.
        given(sifreKasaService.olustur(any(), any(), any(), anyBoolean()))
                .willThrow(new BusinessException("Şirkete ait (global) kasaya yalnızca yönetici ekleyebilir"));

        mockMvc.perform(post("/api/sifre-kasa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baslik\":\"Global\",\"sifre\":\"Gizli123\",\"kapsam\":\"GLOBAL\"}")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void arsivle_204Doner() throws Exception {
        mockMvc.perform(delete("/api/sifre-kasa/1").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isNoContent());
    }

    @Test
    void arsivleGeriAl_204Doner() throws Exception {
        mockMvc.perform(post("/api/sifre-kasa/1/geri-al").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isNoContent());
    }

    @Test
    void ozet_sayaclariDoner() throws Exception {
        given(sifreKasaService.ozet(any(), any()))
                .willReturn(SifreKasaOzetDTO.builder().uyari(2L).suresiBitti(1L).sirketGeneli(3L).arsiv(0L).build());

        mockMvc.perform(get("/api/sifre-kasa/ozet").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uyari").value(2))
                .andExpect(jsonPath("$.suresiBitti").value(1));
    }
}