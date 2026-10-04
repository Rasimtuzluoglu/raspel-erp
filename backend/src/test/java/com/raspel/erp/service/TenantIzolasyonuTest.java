package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariFiyatRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.NotRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.ik.PersonelIzinService;
import com.raspel.erp.service.sistem.NotService;
import com.raspel.erp.service.sube.DepoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Cross-tenant (IDOR/BOLA) regresyon testleri.
 *
 * <p>Denetimde dört okuma yolunda {@code TenantChecker} çağrısı eksikti:
 * {@code NotService.cariNotlari}, {@code CariHesapService.cariFiyatlari},
 * {@code DepoService.depoStoklari}, {@code PersonelIzinService.personelIzinleri}.
 * Yazma yollarının hepsinde kontrol vardı; okuma yolları atlanıyordu. Sonuç:
 * {@code USER} rolündeki herhangi bir kullanıcı başka şirketin cari fiyatlarını,
 * cari notlarını, depo stoklarını ve personel izinlerini okuyabiliyordu.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantIzolasyonuTest {

    private static final Long SIRKET_A = 1L;
    private static final Long SIRKET_B = 2L;

    @Mock private NotRepository notRepository;
    @Mock private TenantChecker tenantChecker;

    @Mock private CariFiyatRepository cariFiyatRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;

    @Mock private DepoRepository depoRepository;
    @Mock private DepoStokRepository depoStokRepository;

    @Mock private PersonelIzinRepository izinRepository;
    @Mock private PersonelRepository personelRepository;

    @InjectMocks private NotService notService;
    @InjectMocks private CariHesapService cariHesapService;
    @InjectMocks private DepoService depoService;
    @InjectMocks private PersonelIzinService personelIzinService;

    @BeforeEach
    void setUp() {
        // Bu testin konusu tenant izolasyonu; TenantChecker gerçek davranışıyla
        // çalışsın diye repository çağrılarını taklit ediyoruz.
        when(tenantChecker.getCurrentSirketId()).thenAnswer(inv -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return (Long) ((ServletRequestAttributes) attrs).getRequest().getAttribute("sirketId");
        });
        doAnswer(inv -> {
            Long current = tenantChecker.getCurrentSirketId();
            if (current == null) return null;
            Long entitySirket = inv.getArgument(0);
            if (entitySirket == null || !current.equals(entitySirket)) {
                throw new ResourceNotFoundException(inv.getArgument(1) + " bu sirkete ait degil");
            }
            return null;
        }).when(tenantChecker).check(any(), any());
    }

    @AfterEach
    void temizle() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void sirketBaglami(Long sirketId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("sirketId", sirketId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    // ---------- NotService.cariNotlari ----------

    @Test
    void cariNotlari_tenantFiltresiUygular() {
        sirketBaglami(SIRKET_A);
        when(notRepository.findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(SIRKET_A, 100L))
                .thenReturn(List.of());

        notService.cariNotlari(100L);

        verify(notRepository).findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(SIRKET_A, 100L);
        verify(notRepository, never()).findByCariHesapIdOrderByOlusturmaTarihiDesc(anyLong());
    }

    @Test
    void cariNotlari_dahiliCagriTenantFiltresiUygulamaz() {
        // Request bağlamı yok (scheduler/iç çağrı): eski davranış korunur.
        when(notRepository.findByCariHesapIdOrderByOlusturmaTarihiDesc(100L)).thenReturn(List.of());

        notService.cariNotlari(100L);

        verify(notRepository).findByCariHesapIdOrderByOlusturmaTarihiDesc(100L);
    }

    // ---------- CariHesapService.cariFiyatlari ----------

    @Test
    void cariFiyatlari_tenantFiltresiUygular() {
        sirketBaglami(SIRKET_A);
        when(cariFiyatRepository.findBySirketIdAndCariHesapIdOrderByStokId(SIRKET_A, 55L))
                .thenReturn(List.of());

        assertTrue(cariHesapService.cariFiyatlari(55L).isEmpty());

        verify(cariFiyatRepository).findBySirketIdAndCariHesapIdOrderByStokId(SIRKET_A, 55L);
        verify(cariFiyatRepository, never()).findByCariHesapIdOrderByStokId(anyLong());
    }

    @Test
    void cariFiyatlari_bosSonucIcinStokSorgusuYapmaz() {
        sirketBaglami(SIRKET_A);
        when(cariFiyatRepository.findBySirketIdAndCariHesapIdOrderByStokId(SIRKET_A, 55L))
                .thenReturn(List.of());

        cariHesapService.cariFiyatlari(55L);

        verify(stokRepository, never()).findAllById(any());
    }

    // ---------- DepoService.depoStoklari ----------

    @Test
    void depoStoklari_baskaSirketinDeposuReddedilir() {
        sirketBaglami(SIRKET_A);
        Depo yabanciDepo = Depo.builder().id(9L).ad("B Depo").sirketId(SIRKET_B).build();
        when(depoRepository.findById(9L)).thenReturn(java.util.Optional.of(yabanciDepo));

        assertThrows(ResourceNotFoundException.class, () -> depoService.depoStoklari(9L));

        verify(depoStokRepository, never()).findByDepoId(anyLong());
    }

    @Test
    void depoStoklari_ayniSirketinDeposunaIzinVerir() {
        sirketBaglami(SIRKET_A);
        Depo kendiDepo = Depo.builder().id(9L).ad("A Depo").sirketId(SIRKET_A).build();
        when(depoRepository.findById(9L)).thenReturn(java.util.Optional.of(kendiDepo));
        when(stokRepository.findBySirketIdOrderByAd(SIRKET_A)).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(9L)).thenReturn(List.of());

        assertTrue(depoService.depoStoklari(9L).isEmpty());

        verify(depoStokRepository).findByDepoId(9L);
    }

    // ---------- PersonelIzinService.personelIzinleri ----------

    @Test
    void personelIzinleri_baskaSirketinPersoneliReddedilir() {
        sirketBaglami(SIRKET_A);
        Personel yabanci = Personel.builder().id(42L).ad("Ali").soyad("V").sirketId(SIRKET_B).build();
        when(personelRepository.findById(42L)).thenReturn(java.util.Optional.of(yabanci));

        assertThrows(ResourceNotFoundException.class, () -> personelIzinService.personelIzınleri(42L));

        verify(izinRepository, never()).findByPersonelIdOrderByBaslangicDesc(anyLong());
    }

    @Test
    void personelIzinleri_ayniSirketinPersoneliIcinKayitDoner() {
        sirketBaglami(SIRKET_A);
        Personel kendiPersonel = Personel.builder().id(42L).ad("Ali").soyad("V").sirketId(SIRKET_A).build();
        when(personelRepository.findById(42L)).thenReturn(java.util.Optional.of(kendiPersonel));
        when(izinRepository.findByPersonelIdOrderByBaslangicDesc(42L)).thenReturn(List.of());

        assertNotNull(personelIzinService.personelIzınleri(42L));
        verify(izinRepository).findByPersonelIdOrderByBaslangicDesc(42L);
    }
}