package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.service.sistem.AuditLogService;
import com.raspel.erp.service.sistem.ExcelExportService;
import com.raspel.erp.service.finans.BankaService;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.finans.KasaService;
import com.raspel.erp.service.ik.PersonelService;
import com.raspel.erp.service.envanter.StokService;
import com.raspel.erp.service.muhasebe.MuhasebeService;
import com.raspel.erp.service.ticaret.FaturaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExcelExportController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class ExcelExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExcelExportService excelService;
    @MockBean
    private CariHesapService cariHesapService;
    @MockBean
    private FaturaService faturaService;
    @MockBean
    private HareketService hareketService;
    @MockBean
    private StokService stokService;
    @MockBean
    private PersonelService personelService;
    @MockBean
    private BankaService bankaService;
    @MockBean
    private KasaService kasaService;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private MuhasebeService muhasebeService;

    @Test
    void cariHesaplar_excelDoner() throws Exception {
        CariHesapDTO c = CariHesapDTO.builder().id(1L).ad("Müşteri").vergiNumarasi("123").build();
        when(cariHesapService.tumCariHesaplariGetir(any(), any())).thenReturn(new PageImpl<>(List.of(c)));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/cari-hesaplar"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }
}
