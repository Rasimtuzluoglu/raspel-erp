package com.raspel.erp.controller.ticaret;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ticaret.FiyatListesiDTO;
import com.raspel.erp.service.ticaret.FiyatListesiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FiyatListesiController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class FiyatListesiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FiyatListesiService fiyatListesiService;

    @Test
    void tumu_donar() throws Exception {
        FiyatListesiDTO dto = FiyatListesiDTO.builder().id(1L).stokId(10L).satisFiyat(new BigDecimal("120.00")).build();
        when(fiyatListesiService.tumunuGetir(any(), any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/fiyat-listesi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].stokId").value(10));
    }

    @Test
    void getir_donar() throws Exception {
        when(fiyatListesiService.getir(1L)).thenReturn(FiyatListesiDTO.builder().id(1L).build());

        mockMvc.perform(get("/api/fiyat-listesi/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void sil_siler() throws Exception {
        mockMvc.perform(delete("/api/fiyat-listesi/1"))
                .andExpect(status().isNoContent());
        verify(fiyatListesiService).sil(1L);
    }
}
