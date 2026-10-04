package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.VeriAktarimDTO;
import com.raspel.erp.dto.sistem.VeriAktarimSonucDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeriAktarimServiceTest {

    @Mock
    private StokRepository stokRepository;

    @Mock
    private CariHesapRepository cariHesapRepository;

    @Mock
    private SirketRepository sirketRepository;

    @Mock
    private com.raspel.erp.config.TenantChecker tenantChecker;

    @Mock
    private com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;

    @InjectMocks
    private VeriAktarimService veriAktarimService;

    private Sirket kaynakSirket;
    private Sirket hedefSirket;
    private org.springframework.security.core.Authentication eskiAuth;

    @BeforeEach
    void setUp() {
        kaynakSirket = Sirket.builder().id(1L).ad("Kaynak Sirket").build();
        hedefSirket = Sirket.builder().id(2L).ad("Hedef Sirket").build();

        // REDTEAM H-3 düzeltmesi: tenantDogrula artık (a) şirket bağlamı olmazsa
        // hata veriyor ve (b) çağıran kullanıcının KAYNAK ve HEDEF şirketin ikisine
        // de üye olduğunu doğruluyor. Testler bu yeni sözleşmeye göre kurulur.
        eskiAuth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(tenantChecker.getCurrentSirketId()).thenReturn(1L);
        lenient().when(kullaniciRepository.findByUsername("admin")).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder()
                        .id(1L).username("admin").role("ADMIN").sirketId(1L).build()));
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(eskiAuth);
    }

    /** REDTEAM H-3 regresyonu: üye olmadığı şirketin verisi okunamamalı. */
    @Test
    void onizleme_uyesiOlmadigiSirketiReddeder() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user_b", null, java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(kullaniciRepository.findByUsername("user_b")).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder()
                        .id(2L).username("user_b").role("USER").sirketId(2L).build()));

        BusinessException hata = assertThrows(BusinessException.class,
                () -> veriAktarimService.onizleme(1L, 2L));
        assertTrue(hata.getMessage().contains("Kaynak"));
        verify(sirketRepository, never()).findById(any());
    }

    /** REDTEAM H-3 regresyonu: aktif şirket taraf değilse reddedilmeli. */
    @Test
    void aktarimYap_aktifSirketTarafDegilseReddeder() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user", null, java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(kullaniciRepository.findByUsername("user")).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder()
                        .id(3L).username("user").role("USER").sirketId(3L).build()));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .build();

        assertThrows(BusinessException.class, () -> veriAktarimService.aktarimYap(dto));
        verify(sirketRepository, never()).findById(any());
    }

    /** REDTEAM H-3 regresyonu: şirket bağlamı yoksa fail-open olmamalı. */
    @Test
    void aktarimYap_sirketBaglamiYoksaReddeder() {
        when(tenantChecker.getCurrentSirketId()).thenReturn(null);

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .build();

        assertThrows(BusinessException.class, () -> veriAktarimService.aktarimYap(dto));
        verify(sirketRepository, never()).findById(any());
    }

    @Test
    void aktarimYap_stoklariAktar_kopyalar() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));

        Stok stok = Stok.builder().id(100L).ad("Stok 1").stokKodu("S01").miktar(BigDecimal.TEN).build();
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stok)));
        when(stokRepository.findBySirketIdOrderByAd(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .stoklariAktar(true)
                .build();

        VeriAktarimSonucDTO sonuc = veriAktarimService.aktarimYap(dto);

        assertEquals(1, sonuc.getAktarilanStokSayisi());
        assertEquals(0, sonuc.getAtlananStokSayisi());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Stok>> stokCaptor = ArgumentCaptor.forClass(List.class);
        verify(stokRepository).saveAll(stokCaptor.capture());
        Stok savedStok = stokCaptor.getValue().get(0);
        assertEquals("Stok 1", savedStok.getAd());
        assertEquals("S01", savedStok.getStokKodu());
        assertEquals(BigDecimal.ZERO, savedStok.getMiktar());
        assertEquals(2L, savedStok.getSirketId());
    }

    @Test
    void aktarimYap_carileriAktar_kopyalar() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));

        CariHesap cari = CariHesap.builder().id(200L).ad("Cari 1").vergiNumarasi("12345").bakiye(BigDecimal.TEN).build();
        when(cariHesapRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cari)));
        when(cariHesapRepository.findBySirketId(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .carileriAktar(true)
                .bakiyeleriSifirla(true)
                .build();

        VeriAktarimSonucDTO sonuc = veriAktarimService.aktarimYap(dto);

        assertEquals(1, sonuc.getAktarilanCariSayisi());
        assertEquals(0, sonuc.getAtlananCariSayisi());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CariHesap>> cariCaptor = ArgumentCaptor.forClass(List.class);
        verify(cariHesapRepository).saveAll(cariCaptor.capture());
        CariHesap savedCari = cariCaptor.getValue().get(0);
        assertEquals("Cari 1", savedCari.getAd());
        assertEquals("12345", savedCari.getVergiNumarasi());
        assertEquals(BigDecimal.ZERO, savedCari.getBakiye());
        assertEquals(2L, savedCari.getSirketId());
    }

    @Test
    void aktarimYap_mukerrerStokAtlar() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));

        Stok stok = Stok.builder().id(100L).ad("Stok 1").stokKodu("S01").build();
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stok)));
        when(stokRepository.findBySirketIdOrderByAd(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Stok.builder().id(999L).ad("Mevcut").stokKodu("S01").build())));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .stoklariAktar(true)
                .build();

        VeriAktarimSonucDTO sonuc = veriAktarimService.aktarimYap(dto);

        assertEquals(0, sonuc.getAktarilanStokSayisi());
        assertEquals(1, sonuc.getAtlananStokSayisi());
        verify(stokRepository, never()).saveAll(any());
    }

    @Test
    void aktarimYap_ayniSirketHata() {
        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(1L)
                .build();

        assertThrows(BusinessException.class, () -> veriAktarimService.aktarimYap(dto));
        verify(sirketRepository, never()).findById(any());
    }

    @Test
    void aktarimYap_baskaSirketAktariminiReddeder() {
        // REDTEAM H-3: Aktif şirket (3) ne kaynak (1) ne hedef (2); kullanıcı da
        // bu şirketlere üye değil -> aktarım başlamadan reddedilmeli.
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user_3", null, java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(tenantChecker.getCurrentSirketId()).thenReturn(3L);
        when(kullaniciRepository.findByUsername("user_3")).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder()
                        .id(3L).username("user_3").role("USER").sirketId(3L).build()));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .build();

        assertThrows(BusinessException.class, () -> veriAktarimService.aktarimYap(dto));
        verify(sirketRepository, never()).findById(any());
    }

    @Test
    void onizleme_dogru_sayilar_doner() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));
        when(stokRepository.countBySirketId(1L)).thenReturn(5L);
        when(cariHesapRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new CariHesap(), new CariHesap(), new CariHesap())));

        VeriAktarimSonucDTO sonuc = veriAktarimService.onizleme(1L, 2L);

        assertEquals(5, sonuc.getAktarilanStokSayisi());
        assertEquals(3, sonuc.getAktarilanCariSayisi());
        assertEquals("Kaynak Sirket", sonuc.getKaynakSirketAdi());
        assertEquals("Hedef Sirket", sonuc.getHedefSirketAdi());
    }
}
