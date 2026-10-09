package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.service.finans.TahsilatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TahsilatController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class TahsilatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TahsilatService tahsilatService;

    // REDTEAM C10: TahsilatController artık yetki kodunu @PreAuthorize ifadesinde
    // referans alıyor (@yetkiKontrol.kontrol(authentication, 'FINANS_WRITE')).
    // @WebMvcTest dilimi config altındaki @Service/@Component'i otomatik yüklemediği
    // için bean sağlanmalıdır; varsayılan stub true döner.
    @MockBean
    private com.raspel.erp.config.security.YetkiKontrol yetkiKontrol;

    @MockBean
    private com.raspel.erp.service.sistem.IdempotencyService idempotencyService;

    private static final String GIRIS_JSON = """
            {"cariId":1,"tutar":500.00,"odemeYontemi":"NAKIT","kasaId":5}
            """;

    @Test
    void tahsilatGir_anahtarsizCagriCalisir() throws Exception {
        when(tahsilatService.tahsilatGir(eq(1L), eq(new BigDecimal("500.00")), eq("NAKIT"),
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(5L), eq(null)))
                .thenReturn(new java.util.LinkedHashMap<>(java.util.Map.of("hareketId", 77L)));

        mockMvc.perform(post("/api/tahsilat")
                        .contentType("application/json")
                        .content(GIRIS_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hareketId").value(77));
    }

    /**
     * Aynı idempotency anahtarıyla gelen ikinci istek reddedilir: çift tıklama
     * veya ağ titremesi mükerrer tahsilat oluşturmaz.
     */
    @Test
    void tahsilatGir_ayniAnahtarlaIkinciIstekReddedilir() throws Exception {
        when(idempotencyService.deneKilit("idem:tahsilat:0:k1")).thenReturn(false);

        mockMvc.perform(post("/api/tahsilat")
                        .header("X-Idempotency-Key", "k1")
                        .contentType("application/json")
                        .content(GIRIS_JSON))
                .andExpect(status().is4xxClientError());

        verify(tahsilatService, never()).tahsilatGir(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void tahsilatGir_basariliKayittaKilitTamamlanir() throws Exception {
        when(idempotencyService.deneKilit("idem:tahsilat:0:k2")).thenReturn(true);
        when(tahsilatService.tahsilatGir(eq(1L), eq(new BigDecimal("500.00")), eq("NAKIT"),
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(5L), eq(null)))
                .thenReturn(new java.util.LinkedHashMap<>(java.util.Map.of("hareketId", 88L)));

        mockMvc.perform(post("/api/tahsilat")
                        .header("X-Idempotency-Key", "k2")
                        .contentType("application/json")
                        .content(GIRIS_JSON))
                .andExpect(status().isCreated());

        verify(idempotencyService).tamamla("idem:tahsilat:0:k2", 88L);
    }

    @Test
    void tahsilatGir_hataDurumundaKilitSerbestBirakilir() throws Exception {
        when(idempotencyService.deneKilit("idem:tahsilat:0:k3")).thenReturn(true);
        when(tahsilatService.tahsilatGir(eq(1L), eq(new BigDecimal("500.00")), eq("NAKIT"),
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(5L), eq(null)))
                .thenThrow(new com.raspel.erp.exception.BusinessException("Bakiye yetersiz"));

        mockMvc.perform(post("/api/tahsilat")
                        .header("X-Idempotency-Key", "k3")
                        .contentType("application/json")
                        .content(GIRIS_JSON))
                .andExpect(status().is4xxClientError());

        verify(idempotencyService).serbestBirak("idem:tahsilat:0:k3");
    }

    @Test
    void ozet_donar() throws Exception {
        TahsilatDTO dto = TahsilatDTO.builder()
                .toplamAlacak(new BigDecimal("1000.00"))
                .acikFaturaSayisi(3)
                .build();
        when(tahsilatService.ozetGetir(null)).thenReturn(dto);

        mockMvc.perform(get("/api/tahsilat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamAlacak").value(1000.00))
                .andExpect(jsonPath("$.acikFaturaSayisi").value(3));
    }

    @Test
    void hatirlat_gonderilenSayiDonar() throws Exception {
        when(tahsilatService.hatirlat(eq(5L), eq(null))).thenReturn(2);

        mockMvc.perform(post("/api/tahsilat/5/hatirlat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gonderilen").value(2));
    }
}
