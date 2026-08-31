package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.service.sistem.AjandaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AjandaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class AjandaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AjandaService ajandaService;

    @Test
    void olaylar_donar() throws Exception {
        when(ajandaService.olaylar(any(), any(), any())).thenReturn(List.of(
                AjandaOlayDTO.builder().tarih(LocalDate.of(2026, 8, 1)).tip("GOREV").baslik("Görev").build()));

        mockMvc.perform(get("/api/ajanda")
                        .param("baslangic", "2026-08-01")
                        .param("bitis", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tip").value("GOREV"));
    }
}
