package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.DashboardDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.sistem.DashboardAkisDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardGrafikDTO;
import com.raspel.erp.dto.sistem.DashboardIcgoruDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import com.raspel.erp.dto.sistem.DashboardTercihDTO;
import com.raspel.erp.service.sistem.DashboardService;
import com.raspel.erp.service.sistem.DashboardTercihService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.sistem.DashboardController;

@WebMvcTest(DashboardController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DashboardTercihService dashboardTercihService;

    @AfterEach
    void temizle() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetDashboard() throws Exception {
        var dto = DashboardDTO.builder()
                .toplamCariSayisi(10L)
                .toplamBakiye(BigDecimal.valueOf(100000))
                .sonHareketler(List.of(HareketDTO.builder().id(1L).tur("TAHSILAT").build()))
                .build();
        when(dashboardService.dashboardVerileriGetir(any())).thenReturn(dto);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamCariSayisi").value(10L))
                .andExpect(jsonPath("$.toplamBakiye").value(100000));
    }

    @Test
    void ozet_heroKpiVeGorevSayilariniDoner() throws Exception {
        var dto = DashboardOzetDTO.builder()
                .hero(DashboardOzetDTO.HeroDTO.builder()
                        .kullaniciAd("Ali Veli").sirketAd("Test AŞ").canli(true).build())
                .kpi(DashboardOzetDTO.KpiDTO.builder()
                        .likidite(DashboardOzetDTO.LikiditeDTO.builder()
                                .deger(BigDecimal.valueOf(1500))
                                .sparkline(List.of(BigDecimal.ZERO, BigDecimal.valueOf(60)))
                                .build())
                        .build())
                .gorevSayilari(DashboardOzetDTO.GorevSayilariDTO.builder()
                        .onayBekleyen(5L).vadesiGecen(2L).build())
                .build();
        when(dashboardService.ozetGetir(1L, 5L, "Ali Veli", "USER")).thenReturn(dto);

        mockMvc.perform(get("/api/dashboard/ozet")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 5L)
                        .requestAttr("displayName", "Ali Veli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hero.kullaniciAd").value("Ali Veli"))
                .andExpect(jsonPath("$.hero.sirketAd").value("Test AŞ"))
                .andExpect(jsonPath("$.kpi.likidite.deger").value(1500))
                .andExpect(jsonPath("$.gorevSayilari.onayBekleyen").value(5));
    }

    @Test
    void ozet_sirketYoksa400Doner() throws Exception {
        mockMvc.perform(get("/api/dashboard/ozet"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void gorevler_roluSecurityContexttenAlir() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(dashboardService.gorevlerGetir(1L, "ADMIN")).thenReturn(List.of(
                DashboardGorevDTO.builder().tur("ONAY_IZIN").adet(2L).oncelik(90).build()));

        mockMvc.perform(get("/api/dashboard/gorevler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("ONAY_IZIN"))
                .andExpect(jsonPath("$[0].adet").value(2));

        verify(dashboardService).gorevlerGetir(1L, "ADMIN");
    }

    @Test
    void gorevler_muhasebeRolunuTanimlar() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("muhasebe", null,
                        List.of(new SimpleGrantedAuthority("ROLE_MUHASEBE"))));
        when(dashboardService.gorevlerGetir(1L, "MUHASEBE")).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/gorevler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());

        verify(dashboardService).gorevlerGetir(1L, "MUHASEBE");
    }

    @Test
    void grafikler_veriyiDoner() throws Exception {
        when(dashboardService.grafiklerGetir(1L)).thenReturn(DashboardGrafikDTO.builder()
                .alacakYaslandirma(List.of(DashboardDTO.AlacakYasDTO.builder()
                        .aralik("Vadesi Geçti").tutar(BigDecimal.TEN).build()))
                .kasaBanka(DashboardGrafikDTO.KasaBankaDTO.builder()
                        .kasa(BigDecimal.valueOf(100)).banka(BigDecimal.valueOf(200)).build())
                .build());

        mockMvc.perform(get("/api/dashboard/grafikler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alacakYaslandirma[0].aralik").value("Vadesi Geçti"))
                .andExpect(jsonPath("$.kasaBanka.kasa").value(100));
    }

    @Test
    void akis_limitParametresiniIletir() throws Exception {
        when(dashboardService.akisGetir(1L, 5)).thenReturn(List.of(
                DashboardAkisDTO.builder().tur("TAHSILAT").baslik("Tahsilat")
                        .tarih(LocalDateTime.now()).build()));

        mockMvc.perform(get("/api/dashboard/akis")
                        .param("limit", "5")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("TAHSILAT"));

        verify(dashboardService).akisGetir(1L, 5);
    }

    @Test
    void icgoruler_veriyiDoner() throws Exception {
        when(dashboardService.icgorulerGetir(1L)).thenReturn(List.of(
                DashboardIcgoruDTO.builder().seviye("KRITIK").baslik("Vadesi geçen faturalar var").build()));

        mockMvc.perform(get("/api/dashboard/icgoruler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seviye").value("KRITIK"));
    }

    @Test
    void tercih_yoksaDuzenNullDoner() throws Exception {
        when(dashboardTercihService.tercihGetir(5L, 1L))
                .thenReturn(DashboardTercihDTO.builder().duzen(null).build());

        mockMvc.perform(get("/api/dashboard/tercihler")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duzen").value(nullValue()));
    }

    @Test
    void tercih_kaydeder() throws Exception {
        when(dashboardTercihService.tercihKaydet(eq(5L), eq(1L), any(DashboardTercihDTO.class)))
                .thenReturn(DashboardTercihDTO.builder()
                        .duzen(Map.of("versiyon", 1))
                        .guncellemeTarihi(LocalDateTime.now())
                        .build());

        mockMvc.perform(put("/api/dashboard/tercihler")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duzen\":{\"versiyon\":1}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duzen.versiyon").value(1));

        verify(dashboardTercihService).tercihKaydet(eq(5L), eq(1L), argThat(d ->
                d.getDuzen() != null && Integer.valueOf(1).equals(d.getDuzen().get("versiyon"))));
    }

    @Test
    void tercih_kullaniciYoksa400Doner() throws Exception {
        mockMvc.perform(get("/api/dashboard/tercihler").requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dashboardTercihService);
    }
}
