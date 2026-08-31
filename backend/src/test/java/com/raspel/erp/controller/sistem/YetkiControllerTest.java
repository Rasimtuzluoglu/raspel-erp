package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.service.sistem.YetkiService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(YetkiController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class YetkiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private YetkiService yetkiService;

    @Test
    void tumYetkiler_listeler() throws Exception {
        when(yetkiService.tumYetkileriGetir()).thenReturn(List.of(
                Yetki.builder().id(1L).kod("STOK_READ").modul("Stok").build()));

        mockMvc.perform(get("/api/yetkiler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kod").value("STOK_READ"));
    }

    @Test
    void tumRoller_listeler() throws Exception {
        when(yetkiService.tumRolleriGetir()).thenReturn(List.of(
                Rol.builder().id(1L).ad("ADMIN").build()));

        mockMvc.perform(get("/api/yetkiler/roller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("ADMIN"));
    }

    @Test
    void rolYetkiGuncelle_gunceller() throws Exception {
        when(yetkiService.rolYetkileriniGuncelle(eq(1L), any(Set.class)))
                .thenReturn(Rol.builder().id(1L).ad("ADMIN").build());

        mockMvc.perform(put("/api/yetkiler/roller/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("ADMIN"));
    }
}
