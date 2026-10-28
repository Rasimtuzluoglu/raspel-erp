package com.raspel.erp.controller;

import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.service.finans.CekSenetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.raspel.erp.exception.ResourceNotFoundException;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.finans.CekSenetController;

@WebMvcTest(CekSenetController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class CekSenetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private CekSenetService cekSenetService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(CekSenetDTO.builder().id(1L).tur("CEK").cekNo("12345").durum("PORTFOY").build());
        when(cekSenetService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cek-senet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("CEK"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(CekSenetDTO.builder().id(1L).tur("CEK").build());
        when(cekSenetService.tumunuGetir(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cek-senet").param("sirketId", "1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("CEK"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).tur("SENET").tutar(BigDecimal.valueOf(5000)).build();
        when(cekSenetService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/cek-senet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tur").value("SENET"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(cekSenetService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Çek/Senet bulunamadı: 999"));

        mockMvc.perform(get("/api/cek-senet/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).tur("CEK").cekNo("67890").vadeTarihi(LocalDate.now().plusMonths(3)).tutar(BigDecimal.valueOf(10000)).build();
        when(cekSenetService.olustur(any(CekSenetDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/cek-senet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tur").value("CEK"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).durum("TAHSIL_EDILDI").build();
        when(cekSenetService.durumGuncelle(eq(1L), anyString(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/cek-senet/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "TAHSIL_EDILDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TAHSIL_EDILDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(cekSenetService).sil(1L);

        mockMvc.perform(delete("/api/cek-senet/1"))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // Filtre / özet / takvim
    // ------------------------------------------------------------------

    @Test
    void shouldListFiltered() throws Exception {
        var list = List.of(CekSenetDTO.builder().id(1L).tur("CEK").durum("PORTFOY").build());
        when(cekSenetService.filtreli(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cek-senet/filtreli")
                        .param("q", "123")
                        .param("tur", "CEK")
                        .param("durum", "PORTFOY")
                        .param("vadeAralik", "BU_AY")
                        .param("minTutar", "1000")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].durum").value("PORTFOY"));

        verify(cekSenetService).filtreli(eq(1L), eq("123"), eq("CEK"), eq("PORTFOY"), isNull(),
                isNull(), isNull(), eq("BU_AY"), any(BigDecimal.class), isNull(), any(Pageable.class));
    }

    @Test
    void shouldReturnOzet() throws Exception {
        var ozet = com.raspel.erp.dto.finans.CekSenetOzetDTO.builder()
                .portfoy(3).teminatta(2).ciroEdilen(1).tahsilEdilen(4).protesto(1)
                .vadesiGecen(5).buAyVadesiGelen(6).toplamTutar(BigDecimal.valueOf(1000)).build();
        when(cekSenetService.ozet(eq(1L), any(), any())).thenReturn(ozet);

        mockMvc.perform(get("/api/cek-senet/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfoy").value(3))
                .andExpect(jsonPath("$.vadesiGecen").value(5))
                .andExpect(jsonPath("$.buAyVadesiGelen").value(6));
    }

    @Test
    void shouldReturnTakvim() throws Exception {
        var gun = com.raspel.erp.dto.finans.CekSenetTakvimDTO.builder()
                .tarih(LocalDate.of(2026, 3, 2)).adet(1).tutar(BigDecimal.TEN)
                .kayitlar(List.of(CekSenetDTO.builder().id(1L).tur("CEK").build())).build();
        when(cekSenetService.takvim(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(gun));

        mockMvc.perform(get("/api/cek-senet/takvim")
                        .param("baslangic", "2026-03-01")
                        .param("bitis", "2026-03-03")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adet").value(1))
                .andExpect(jsonPath("$[0].kayitlar[0].tur").value("CEK"));
    }

    @Test
    void shouldReturnBadRequestWhenTakvimAralikAsilir() throws Exception {
        when(cekSenetService.takvim(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new com.raspel.erp.exception.BusinessException("Takvim aralığı en fazla 62 gün olabilir"));

        mockMvc.perform(get("/api/cek-senet/takvim")
                        .param("baslangic", "2026-01-01")
                        .param("bitis", "2026-06-30")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Aksiyonlar
    // ------------------------------------------------------------------

    @Test
    void shouldTahsilIptal() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).durum("PORTFOY").build();
        when(cekSenetService.tahsilIptal(eq(1L), eq("Hatalı tahsil"))).thenReturn(dto);

        mockMvc.perform(post("/api/cek-senet/1/tahsil-iptal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gerekce", "Hatalı tahsil"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("PORTFOY"));
    }

    @Test
    void shouldRejectShortGerekceOnTahsilIptal() throws Exception {
        mockMvc.perform(post("/api/cek-senet/1/tahsil-iptal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gerekce", "ab"))))
                .andExpect(status().isBadRequest());

        verify(cekSenetService, never()).tahsilIptal(anyLong(), anyString());
    }

    @Test
    void shouldCiro() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).durum("CIRO_EDILDI").ciroCariId(5L).build();
        when(cekSenetService.ciroEt(eq(1L), eq(5L), eq("Borca karşılık"))).thenReturn(dto);

        mockMvc.perform(post("/api/cek-senet/1/ciro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cariId", 5, "not", "Borca karşılık"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("CIRO_EDILDI"))
                .andExpect(jsonPath("$.ciroCariId").value(5));
    }

    @Test
    void shouldRejectCiroWithoutCari() throws Exception {
        mockMvc.perform(post("/api/cek-senet/1/ciro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(cekSenetService, never()).ciroEt(anyLong(), any(), any());
    }

    @Test
    void shouldCiroIadeVeIadeAl() throws Exception {
        when(cekSenetService.ciroIade(1L)).thenReturn(CekSenetDTO.builder().id(1L).durum("IADE").build());
        when(cekSenetService.iadeAl(1L)).thenReturn(CekSenetDTO.builder().id(1L).durum("PORTFOY").build());

        mockMvc.perform(post("/api/cek-senet/1/ciro-iade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("IADE"));

        mockMvc.perform(post("/api/cek-senet/1/iade-al"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("PORTFOY"));
    }

    @Test
    void shouldTeminatVeTeminatIade() throws Exception {
        when(cekSenetService.teminatVer(eq(1L), eq("Banka teminatı")))
                .thenReturn(CekSenetDTO.builder().id(1L).durum("TEMINATTA").build());
        when(cekSenetService.teminatIade(1L)).thenReturn(CekSenetDTO.builder().id(1L).durum("PORTFOY").build());

        mockMvc.perform(post("/api/cek-senet/1/teminat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("aciklama", "Banka teminatı"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TEMINATTA"));

        mockMvc.perform(post("/api/cek-senet/1/teminat-iade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("PORTFOY"));
    }

    @Test
    void shouldProtesto() throws Exception {
        when(cekSenetService.protesto(eq(1L), eq("Karşılıksız"))).thenReturn(
                CekSenetDTO.builder().id(1L).durum("PROTESTO").build());

        mockMvc.perform(post("/api/cek-senet/1/protesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("aciklama", "Karşılıksız"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("PROTESTO"));
    }

    @Test
    void shouldReturnBadRequestWhenGecersizGecis() throws Exception {
        when(cekSenetService.ciroIade(1L))
                .thenThrow(new com.raspel.erp.exception.BusinessException(
                        "Geçersiz durum geçişi: PORTFOY -> IADE"));

        mockMvc.perform(post("/api/cek-senet/1/ciro-iade"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnDetailWithGecmis() throws Exception {
        var gecmis = List.of(com.raspel.erp.dto.finans.CekSenetDurumLogDTO.builder()
                .id(1L).islem("OLUSTUR").yeniDurum("PORTFOY").build());
        var dto = CekSenetDTO.builder().id(1L).tur("CEK").faturaNo("F-1").ciroCariAd("Tedarikci")
                .gecmis(gecmis).build();
        when(cekSenetService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/cek-senet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturaNo").value("F-1"))
                .andExpect(jsonPath("$.ciroCariAd").value("Tedarikci"))
                .andExpect(jsonPath("$.gecmis[0].islem").value("OLUSTUR"));
    }
}





