package com.raspel.erp.controller;

import com.raspel.erp.service.sistem.PdfRaporService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.sistem.PdfRaporController;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.entity.ticaret.Siparis;

@WebMvcTest(PdfRaporController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PdfRaporControllerTest {

    @Autowired
    private MockMvc mockMvc;

    
    @MockBean
    private PdfRaporService pdfRaporService;

    @Test
    void shouldGetSiparisRaporu() throws Exception {
        byte[] pdfBytes = "fake-pdf-content".getBytes();
        when(pdfRaporService.siparisRaporu(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/rapor/siparis/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=siparis_1.pdf"))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void shouldGetIrsaliyeRaporu() throws Exception {
        byte[] pdfBytes = "fake-pdf-content-2".getBytes();
        when(pdfRaporService.irsaliyeRaporu(2L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/rapor/irsaliye/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=irsaliye_2.pdf"))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void shouldReturnNotFoundWhenSiparisRaporu() throws Exception {
        when(pdfRaporService.siparisRaporu(anyLong()))
                .thenThrow(new com.raspel.erp.exception.ResourceNotFoundException("Siparis", 999L));

        mockMvc.perform(get("/api/rapor/siparis/999"))
                .andExpect(status().isNotFound());
    }
}




