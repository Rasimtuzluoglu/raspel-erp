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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                eq(null), eq(null), eq(null), eq(null), eq(5L), eq(null), eq(null)))
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
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void tahsilatGir_basariliKayittaKilitTamamlanir() throws Exception {
        when(idempotencyService.deneKilit("idem:tahsilat:0:k2")).thenReturn(true);
        when(tahsilatService.tahsilatGir(eq(1L), eq(new BigDecimal("500.00")), eq("NAKIT"),
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(5L), eq(null), eq(null)))
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
                eq(null), eq(null), eq(null), eq(null), eq(5L), eq(null), eq(null)))
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
        when(tahsilatService.ozetGetir(isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(dto);

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

    @Test
    void ozet_filtreleriServiseIletir() throws Exception {
        when(tahsilatService.ozetGetir(eq(1L), eq("acme"), eq("0-30"), eq(true),
                eq(new BigDecimal("100.00"))))
                .thenReturn(TahsilatDTO.builder().cariler(java.util.List.of()).build());

        mockMvc.perform(get("/api/tahsilat")
                        .requestAttr("sirketId", 1L)
                        .param("q", "acme")
                        .param("aralik", "0-30")
                        .param("sadeceGecikmis", "true")
                        .param("minTutar", "100.00"))
                .andExpect(status().isOk());

        verify(tahsilatService).ozetGetir(eq(1L), eq("acme"), eq("0-30"), eq(true),
                eq(new BigDecimal("100.00")));
    }

    @Test
    void gecmis_islemBazliDoner() throws Exception {
        var dto = com.raspel.erp.dto.finans.TahsilatIslemDTO.builder()
                .id(11L).durum("AKTIF").tutar(new BigDecimal("500.00")).satirSayisi(2)
                .dagilimlar(java.util.List.of()).build();
        when(tahsilatService.gecmisFiltreli(eq(1L), eq("acme"), eq(3L), eq("NAKIT"), eq("AKTIF"),
                eq(4L), eq(5L), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(dto)));

        mockMvc.perform(get("/api/tahsilat/gecmis")
                        .requestAttr("sirketId", 1L)
                        .param("q", "acme")
                        .param("cariId", "3")
                        .param("odemeYontemi", "NAKIT")
                        .param("durum", "AKTIF")
                        .param("kasaId", "4")
                        .param("bankaId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(11))
                .andExpect(jsonPath("$.content[0].durum").value("AKTIF"));
    }

    @Test
    void iptal_gerekceyleCalisir() throws Exception {
        var dto = com.raspel.erp.dto.finans.TahsilatIslemDTO.builder()
                .id(5L).durum("IPTAL").iptalGerekce("müşteri vazgeçti").build();
        when(tahsilatService.iptalEt(eq(5L), eq("müşteri vazgeçti"), eq(9L))).thenReturn(dto);

        mockMvc.perform(post("/api/tahsilat/islem/5/iptal")
                        .requestAttr("kullaniciId", 9L)
                        .contentType("application/json")
                        .content("{\"gerekce\":\"müşteri vazgeçti\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("IPTAL"))
                .andExpect(jsonPath("$.iptalGerekce").value("müşteri vazgeçti"));
    }

    @Test
    void iptal_kisaGerekce400Doner() throws Exception {
        mockMvc.perform(post("/api/tahsilat/islem/5/iptal")
                        .contentType("application/json")
                        .content("{\"gerekce\":\"ab\"}"))
                .andExpect(status().isBadRequest());

        verify(tahsilatService, never()).iptalEt(any(), any(), any());
    }

    @Test
    void hedef_upsertSonucuDoner() throws Exception {
        when(tahsilatService.hedefKaydet(eq(1L), eq(2026), eq(10), eq(new BigDecimal("100000.00")), eq(9L)))
                .thenReturn(com.raspel.erp.dto.finans.TahsilatHedefDTO.builder()
                        .yil(2026).ay(10).tutar(new BigDecimal("100000.00")).build());

        mockMvc.perform(put("/api/tahsilat/hedef")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 9L)
                        .contentType("application/json")
                        .content("{\"yil\":2026,\"ay\":10,\"tutar\":100000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutar").value(100000.00));
    }

    @Test
    void performans_donar() throws Exception {
        when(tahsilatService.performans(1L, 2026, 10))
                .thenReturn(com.raspel.erp.dto.finans.TahsilatPerformansDTO.builder()
                        .hedef(new BigDecimal("1000.00"))
                        .tahsilEdilen(new BigDecimal("750.00"))
                        .gerceklesmeYuzde(new BigDecimal("75.00"))
                        .tahsilatSayisi(3)
                        .vadesiGecmisToplam(new BigDecimal("200.00"))
                        .trend(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/tahsilat/performans")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("ay", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gerceklesmeYuzde").value(75.00))
                .andExpect(jsonPath("$.tahsilatSayisi").value(3));
    }

    @Test
    void topluHatirlat_sonucDonar() throws Exception {
        when(tahsilatService.topluHatirlat(eq(1L), eq(java.util.List.of(1L, 2L))))
                .thenReturn(com.raspel.erp.dto.finans.TopluHatirlatmaSonuc.builder()
                        .gonderilen(1).atlanan(1).hatalar(java.util.List.of()).build());

        mockMvc.perform(post("/api/tahsilat/toplu-hatirlat")
                        .requestAttr("sirketId", 1L)
                        .contentType("application/json")
                        .content("{\"cariIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gonderilen").value(1))
                .andExpect(jsonPath("$.atlanan").value(1));
    }

    @Test
    void topluHatirlat_bosSecim400Doner() throws Exception {
        mockMvc.perform(post("/api/tahsilat/toplu-hatirlat")
                        .requestAttr("sirketId", 1L)
                        .contentType("application/json")
                        .content("{\"cariIds\":[]}"))
                .andExpect(status().isBadRequest());

        verify(tahsilatService, never()).topluHatirlat(any(), any());
    }
}
