package com.raspel.erp.controller.sistem;

import com.raspel.erp.config.GlobalExceptionHandler;
import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.KitaplikDTO;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.service.sistem.KitaplikService;
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

@WebMvcTest(controllers = KitaplikController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
@ContextConfiguration(classes = {KitaplikController.class, GlobalExceptionHandler.class, TestSecurityMocks.class})
class KitaplikControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KitaplikService kitaplikService;

    private KitaplikDTO ornek() {
        return KitaplikDTO.builder().id(1L).tur("YER_IMI").baslik("Netsis")
                .url("https://netsis.local").kapsam("KISISEL").gorunurluk("SAHIS").build();
    }

    @Test
    void listele_200Doner() throws Exception {
        given(kitaplikService.listele(any(), any(), anyBoolean(), any(), any(), any(), any()))
                .willReturn(List.of(ornek()));
        mockMvc.perform(get("/api/kitaplik").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baslik").value("Netsis"));
    }

    @Test
    void olustur_201Doner() throws Exception {
        given(kitaplikService.olustur(any(), any(), any(), anyBoolean())).willReturn(ornek());
        mockMvc.perform(post("/api/kitaplik")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tur\":\"YER_IMI\",\"baslik\":\"Netsis\",\"url\":\"https://netsis.local\"}")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isCreated());
    }

    @Test
    void olustur_globalYetkiYoksa400Doner() throws Exception {
        given(kitaplikService.olustur(any(), any(), any(), anyBoolean()))
                .willThrow(new BusinessException("Şirkete ait (global) kitaplığa yalnızca yönetici ekleyebilir"));
        mockMvc.perform(post("/api/kitaplik")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tur\":\"SNIPPET\",\"baslik\":\"G\",\"icerik\":\"kod\",\"kapsam\":\"GLOBAL\"}")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void basliksizOlustur_400Doner() throws Exception {
        mockMvc.perform(post("/api/kitaplik")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tur\":\"YER_IMI\",\"url\":\"https://x\"}")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guncelle_200Doner() throws Exception {
        given(kitaplikService.guncelle(any(), any(), any(), any(), anyBoolean())).willReturn(ornek());
        mockMvc.perform(put("/api/kitaplik/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tur\":\"YER_IMI\",\"baslik\":\"Netsis\",\"url\":\"https://netsis.local\"}")
                        .requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isOk());
    }

    @Test
    void arsivle_204Doner() throws Exception {
        mockMvc.perform(delete("/api/kitaplik/1").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isNoContent());
    }

    @Test
    void geriAl_204Doner() throws Exception {
        mockMvc.perform(post("/api/kitaplik/1/geri-al").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 10L))
                .andExpect(status().isNoContent());
    }
}