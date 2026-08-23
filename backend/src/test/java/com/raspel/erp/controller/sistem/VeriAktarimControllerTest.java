package com.raspel.erp.controller.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.VeriAktarimDTO;
import com.raspel.erp.dto.sistem.VeriAktarimSonucDTO;
import com.raspel.erp.service.sistem.VeriAktarimService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeriAktarimController.class)
@AutoConfigureMockMvc(addFilters = false)
class VeriAktarimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VeriAktarimService veriAktarimService;

    @Test
    void shouldAktarimYap() throws Exception {
        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .build();
        VeriAktarimSonucDTO sonuc = VeriAktarimSonucDTO.builder()
                .aktarilanStokSayisi(5)
                .aktarilanCariSayisi(3)
                .build();
        
        when(veriAktarimService.aktarimYap(any(VeriAktarimDTO.class))).thenReturn(sonuc);

        mockMvc.perform(post("/api/veri-aktarim/sirketler-arasi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aktarilanStokSayisi").value(5))
                .andExpect(jsonPath("$.aktarilanCariSayisi").value(3));
    }

    @Test
    void shouldOnizleme() throws Exception {
        VeriAktarimSonucDTO sonuc = VeriAktarimSonucDTO.builder()
                .aktarilanStokSayisi(10)
                .aktarilanCariSayisi(2)
                .build();
        
        when(veriAktarimService.onizleme(1L, 2L)).thenReturn(sonuc);

        mockMvc.perform(get("/api/veri-aktarim/onizleme")
                .param("kaynakSirketId", "1")
                .param("hedefSirketId", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aktarilanStokSayisi").value(10))
                .andExpect(jsonPath("$.aktarilanCariSayisi").value(2));
    }
}
