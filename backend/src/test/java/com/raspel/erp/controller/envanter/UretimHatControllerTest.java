package com.raspel.erp.controller.envanter;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.envanter.UretimHatDTO;
import com.raspel.erp.service.envanter.UretimHatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UretimHatController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class UretimHatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UretimHatService hatService;

    @Test
    void listele_dolulukIleDoner() throws Exception {
        when(hatService.listele(1L)).thenReturn(List.of(UretimHatDTO.builder()
                .id(1L).ad("Hat A").kapasite(4).aktif(true).aktifEmir(2).gecikenEmir(1).dolulukYuzde(50).build()));

        mockMvc.perform(get("/api/uretim/hatlar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Hat A"))
                .andExpect(jsonPath("$[0].dolulukYuzde").value(50))
                .andExpect(jsonPath("$[0].gecikenEmir").value(1));
    }

    @Test
    void olustur_201Doner() throws Exception {
        when(hatService.olustur(any(), eq(1L))).thenReturn(UretimHatDTO.builder()
                .id(2L).ad("Hat B").kapasite(5).aktif(true).build());

        mockMvc.perform(post("/api/uretim/hatlar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"Hat B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void guncelle_veSil_calisir() throws Exception {
        when(hatService.guncelle(eq(2L), any(), eq(1L))).thenReturn(UretimHatDTO.builder()
                .id(2L).ad("Hat B2").kapasite(6).aktif(false).build());

        mockMvc.perform(put("/api/uretim/hatlar/2")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"Hat B2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Hat B2"));

        mockMvc.perform(delete("/api/uretim/hatlar/2").requestAttr("sirketId", 1L))
                .andExpect(status().isNoContent());
        verify(hatService).sil(2L, 1L);
    }
}
