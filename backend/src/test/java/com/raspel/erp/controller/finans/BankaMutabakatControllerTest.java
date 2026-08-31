package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.BankaHareketiDTO;
import com.raspel.erp.service.finans.BankaMutabakatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankaMutabakatController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class BankaMutabakatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankaMutabakatService bankaMutabakatService;

    @Test
    void listele_donar() throws Exception {
        when(bankaMutabakatService.listele(1L)).thenReturn(List.of(
                BankaHareketiDTO.builder().id(1L).aciklama("Hareket").build()));

        mockMvc.perform(get("/api/bankalar/1/mutabakat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aciklama").value("Hareket"));
    }

    @Test
    void otomatikEslestir_donar() throws Exception {
        when(bankaMutabakatService.otomatikEslestir(any(), any())).thenReturn(List.of(
                BankaHareketiDTO.builder().id(1L).eslestirildi(true).build()));

        mockMvc.perform(post("/api/bankalar/1/mutabakat/otomatik-eslestir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eslestirildi").value(true));
    }

    @Test
    void sil_temizler() throws Exception {
        mockMvc.perform(delete("/api/bankalar/1/mutabakat"))
                .andExpect(status().isNoContent());
    }
}
