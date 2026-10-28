package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.sistem.OnayController;
import com.raspel.erp.dto.sistem.OnayBekleyenDTO;
import com.raspel.erp.dto.sistem.OnayKararDTO;
import com.raspel.erp.dto.sistem.OnayKararRequest;
import com.raspel.erp.dto.sistem.OnayOzetDTO;
import com.raspel.erp.dto.sistem.OnayTopluRequest;
import com.raspel.erp.dto.sistem.OnayTopluSonucDTO;
import com.raspel.erp.service.sistem.OnayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnayController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityMocks.class, OnayControllerTest.MethodSecurityConfig.class})
class OnayControllerTest {

    /** @PreAuthorize kurallarının dilim testinde de uygulanması için. */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OnayService onayService;

    @BeforeEach
    void varsayilanKimlik() {
        // addFilters=false olduğu için güvenlik bağlamı elle kurulur (metot güvenliği okur).
        kimlik("ADMIN");
    }

    @AfterEach
    void kimlikTemizle() {
        SecurityContextHolder.clearContext();
    }

    private void kimlik(String... roller) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-kullanici", "n/a",
                        Arrays.stream(roller)
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList()));
    }

    @Test
    void ozet_200Doner() throws Exception {
        when(onayService.ozet(1L)).thenReturn(OnayOzetDTO.builder()
                .izin(1).masraf(2).satinalma(3).siparis(4).toplam(10).build());

        mockMvc.perform(get("/api/onaylar/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(10))
                .andExpect(jsonPath("$.satinalma").value(3));
    }

    @Test
    void bekleyen_sayfalamaParametreleriniIletir() throws Exception {
        var dto = OnayBekleyenDTO.builder()
                .tur("IZIN").kaynakId(5L).baslik("Ayşe Kaya").altBaslik("YILLIK")
                .tarih(LocalDate.now()).durum("BEKLEMEDE").olusturmaTarihi(LocalDateTime.now())
                .meta(Map.of("gunSayisi", 3)).build();
        when(onayService.bekleyen(eq(1L), eq("IZIN"), eq("ayşe"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(50), 1));

        mockMvc.perform(get("/api/onaylar/bekleyen")
                        .requestAttr("sirketId", 1L)
                        .param("tur", "IZIN")
                        .param("q", "ayşe")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("IZIN"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(onayService).bekleyen(eq(1L), eq("IZIN"), eq("ayşe"), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void bekleyen_gecersizSayfalamaDegerleri_sinirlanir() throws Exception {
        when(onayService.bekleyen(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/onaylar/bekleyen")
                        .requestAttr("sirketId", 1L)
                        .param("page", "-3")
                        .param("size", "5000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(onayService).bekleyen(eq(1L), isNull(), isNull(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(200, captor.getValue().getPageSize());
    }

    @Test
    void karar_tenantVeKullaniciBilgisiniBaglamdanAlir() throws Exception {
        when(onayService.karar(eq(1L), eq(7L), eq("Ali Veli"), any(OnayKararRequest.class)))
                .thenReturn(OnayKararDTO.builder().id(1L).tur("IZIN").kaynakId(5L)
                        .karar("ONAY").yeniDurum("ONAYLANDI").build());

        mockMvc.perform(post("/api/onaylar/karar")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .requestAttr("displayName", "Ali Veli")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OnayKararRequest.builder()
                                .tur("IZIN").kaynakId(5L).karar("ONAY").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yeniDurum").value("ONAYLANDI"));

        verify(onayService).karar(eq(1L), eq(7L), eq("Ali Veli"), any(OnayKararRequest.class));
    }

    @Test
    void karar_eksikGovde_400Doner() throws Exception {
        mockMvc.perform(post("/api/onaylar/karar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tur\":\"IZIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toplu_200DonerVeSonucuAktarir() throws Exception {
        when(onayService.toplu(eq(1L), eq(7L), eq("ali"),
                any(OnayTopluRequest.class))).thenReturn(OnayTopluSonucDTO.builder()
                .basarili(2)
                .atlanan(List.of(OnayTopluSonucDTO.Atlanan.builder()
                        .kaynakId(9L).mesaj("İzin kaydı bulunamadı").build()))
                .build());

        mockMvc.perform(post("/api/onaylar/toplu")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .requestAttr("username", "ali")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OnayTopluRequest.builder()
                                .tur("IZIN").kaynakIds(List.of(1L, 2L, 9L)).karar("ONAY").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili").value(2))
                .andExpect(jsonPath("$.atlanan[0].kaynakId").value(9));

        // displayName yoksa username kullanılır.
        verify(onayService).toplu(eq(1L), eq(7L), eq("ali"), any(OnayTopluRequest.class));
    }

    @Test
    void gecmis_tarihAraliginiVeSayfalamayiIletir() throws Exception {
        when(onayService.gecmis(eq(1L), eq("MASRAF"), eq("RED"), eq("belge"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(OnayKararDTO.builder()
                        .id(1L).tur("MASRAF").kaynakId(3L).karar("RED")
                        .gerekce("Belge eksik").build())));

        mockMvc.perform(get("/api/onaylar/gecmis")
                        .requestAttr("sirketId", 1L)
                        .param("tur", "MASRAF")
                        .param("karar", "RED")
                        .param("q", "belge")
                        .param("baslangic", "2026-01-01")
                        .param("bitis", "2026-01-31")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].karar").value("RED"));
    }

    @Test
    void yetkisizRol_403Doner() throws Exception {
        kimlik("USER");

        mockMvc.perform(get("/api/onaylar/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void muhasebeRolu_200Doner() throws Exception {
        kimlik("MUHASEBE");
        when(onayService.ozet(1L)).thenReturn(OnayOzetDTO.builder().toplam(0).build());

        mockMvc.perform(get("/api/onaylar/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void bekleyen_bosSonuc_200Doner() throws Exception {
        when(onayService.bekleyen(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/onaylar/bekleyen").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void bekleyen_tutarAlaniniJsonaYazar() throws Exception {
        when(onayService.bekleyen(eq(1L), eq("MASRAF"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(OnayBekleyenDTO.builder()
                        .tur("MASRAF").kaynakId(3L).tutar(new BigDecimal("125.50"))
                        .durum("BEKLEMEDE").build())));

        mockMvc.perform(get("/api/onaylar/bekleyen")
                        .requestAttr("sirketId", 1L)
                        .param("tur", "MASRAF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tutar").value(125.50));
    }
}
