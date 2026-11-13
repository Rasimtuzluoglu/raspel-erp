package com.raspel.erp.service.sube;

import com.raspel.erp.dto.sube.DepoTransferDTO;
import com.raspel.erp.entity.sube.DepoTransfer;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoTransferRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepoTransferServiceTest {

    @Mock
    private DepoTransferRepository transferRepository;

    @Mock
    private DepoService depoService;

    @Mock
    private DepoRepository depoRepository;

    @Mock
    private StokRepository stokRepository;

    @Mock
    private com.raspel.erp.repository.sube.DepoStokRepository depoStokRepository;

    @Mock
    private com.raspel.erp.config.TenantChecker tenantChecker;

    @Mock
    private com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;

    @InjectMocks
    private DepoTransferService depoTransferService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(depoTransferService, "slaSaat", 24L);
        when(depoRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(stokRepository.findById(anyLong())).thenReturn(Optional.empty());
    }

    private DepoTransfer bekleyenTransfer() {
        return DepoTransfer.builder()
                .id(1L).sirketId(4L)
                .kaynakDepoId(1L).hedefDepoId(2L)
                .stokId(10L).miktar(BigDecimal.valueOf(5))
                .durum("BEKLIYOR")
                .build();
    }

    @Test
    void talepOlustur_ayniDepoyaIzinVermez() {
        DepoTransferDTO dto = DepoTransferDTO.builder()
                .kaynakDepoId(1L).hedefDepoId(1L).stokId(10L).miktar(BigDecimal.ONE).build();

        assertThrows(BusinessException.class, () -> depoTransferService.talepOlustur(dto, 4L, 1L));
    }

    @Test
    void talepOlustur_bekliyorDurumundaKaydeder() {
        DepoTransferDTO dto = DepoTransferDTO.builder()
                .kaynakDepoId(1L).hedefDepoId(2L).stokId(10L).miktar(BigDecimal.valueOf(5)).build();
        // Talep aninda dogrulama: depolar/stok tenant'a ait ve kaynak depoda yeterli miktar olmali.
        when(depoRepository.findById(1L)).thenReturn(Optional.of(com.raspel.erp.entity.sube.Depo.builder()
                .id(1L).ad("Kaynak").sirketId(4L).build()));
        when(depoRepository.findById(2L)).thenReturn(Optional.of(com.raspel.erp.entity.sube.Depo.builder()
                .id(2L).ad("Hedef").sirketId(4L).build()));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(com.raspel.erp.entity.envanter.Stok.builder()
                .id(10L).ad("Ürün").sirketId(4L).build()));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 10L))
                .thenReturn(Optional.of(com.raspel.erp.entity.sube.DepoStok.builder()
                        .depoId(1L).stokId(10L).miktar(BigDecimal.valueOf(100)).build()));
        when(transferRepository.save(any(DepoTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        DepoTransferDTO sonuc = depoTransferService.talepOlustur(dto, 4L, 1L);

        assertNotNull(sonuc);
        assertEquals("BEKLIYOR", sonuc.getDurum());
        verify(transferRepository).save(any(DepoTransfer.class));
    }

    @Test
    void talepOlustur_yetersizKaynakStoktaHataFirlatir() {
        DepoTransferDTO dto = DepoTransferDTO.builder()
                .kaynakDepoId(1L).hedefDepoId(2L).stokId(10L).miktar(BigDecimal.valueOf(50)).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(com.raspel.erp.entity.sube.Depo.builder()
                .id(1L).ad("Kaynak").sirketId(4L).build()));
        when(depoRepository.findById(2L)).thenReturn(Optional.of(com.raspel.erp.entity.sube.Depo.builder()
                .id(2L).ad("Hedef").sirketId(4L).build()));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(com.raspel.erp.entity.envanter.Stok.builder()
                .id(10L).ad("Ürün").sirketId(4L).build()));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 10L))
                .thenReturn(Optional.of(com.raspel.erp.entity.sube.DepoStok.builder()
                        .depoId(1L).stokId(10L).miktar(BigDecimal.valueOf(5)).build()));

        assertThrows(BusinessException.class, () -> depoTransferService.talepOlustur(dto, 4L, 1L));
    }

    @Test
    void onayla_stokTransferGerceklestirir() {
        DepoTransfer t = bekleyenTransfer();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));
        when(transferRepository.save(any(DepoTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        DepoTransferDTO sonuc = depoTransferService.onayla(1L);

        assertEquals("ONAYLANDI", sonuc.getDurum());
        verify(depoService).stokTransfer(1L, 2L, 10L, BigDecimal.valueOf(5));
    }

    @Test
    void onayla_beklemeyenTransferdeHataFirlatir() {
        DepoTransfer t = bekleyenTransfer();
        t.setDurum("ONAYLANDI");
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessException.class, () -> depoTransferService.onayla(1L));
        verify(depoService, never()).stokTransfer(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void reddet_durumuReddedildiYapar() {
        DepoTransfer t = bekleyenTransfer();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));
        when(transferRepository.save(any(DepoTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        DepoTransferDTO sonuc = depoTransferService.reddet(1L);

        assertEquals("REDDEDILDI", sonuc.getDurum());
        verify(depoService, never()).stokTransfer(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void sayfali_tarihVerilmezseSentinelAralikKullanir() {
        when(depoRepository.findBySirketIdOrderByAdAsc(eq(1L), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(transferRepository.filtreli(eq(1L), isNull(), isNull(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        var bas = org.mockito.ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        var bit = org.mockito.ArgumentCaptor.forClass(java.time.LocalDateTime.class);

        depoTransferService.sayfali(1L, null, null, null, null,
                org.springframework.data.domain.Pageable.unpaged());

        verify(transferRepository).filtreli(eq(1L), isNull(), isNull(), bas.capture(), bit.capture(), any());
        assertNotNull(bas.getValue());
        assertNotNull(bit.getValue());
        assertTrue(bas.getValue().getYear() <= 1970);
        assertTrue(bit.getValue().getYear() >= 9999);
    }

    @Test
    void slaDurumu_kovalariDogruHesaplar() {
        java.time.LocalDateTime simdi = java.time.LocalDateTime.now();
        assertEquals("NORMAL", depoTransferService.slaDurumu(simdi.minusHours(2)));
        assertEquals("YAKLASIYOR", depoTransferService.slaDurumu(simdi.minusHours(6)));
        assertEquals("GECIKTI", depoTransferService.slaDurumu(simdi.minusHours(30)));
        assertEquals("NORMAL", depoTransferService.slaDurumu(null));
    }

    @Test
    void onayla_adminOlmayanKullaniciyiReddeder() {
        DepoTransfer t = bekleyenTransfer();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(9L)).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder().id(9L).role("USER").build()));

        assertThrows(BusinessException.class, () -> depoTransferService.onayla(1L, 9L));
        verify(depoService, never()).stokTransfer(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void onayla_onaylayanKullaniciyiKaydeder() {
        DepoTransfer t = bekleyenTransfer();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));
        when(transferRepository.save(any(DepoTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kullaniciRepository.findById(9L)).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder().id(9L).role("ADMIN").build()));

        DepoTransferDTO sonuc = depoTransferService.onayla(1L, 9L);

        assertEquals(9L, sonuc.getOnaylayanKullaniciId());
        assertNotNull(sonuc.getOnayTarihi());
    }

    @Test
    void reddet_kisaGerekceyiReddeder() {
        DepoTransfer t = bekleyenTransfer();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(9L)).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder().id(9L).role("ADMIN").build()));

        assertThrows(BusinessException.class, () -> depoTransferService.reddet(1L, 9L, "ab"));
    }

    @Test
    void reddet_gerekceVeDenetimBilgisiniKaydeder() {
        DepoTransfer t = bekleyenTransfer();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(t));
        when(transferRepository.save(any(DepoTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kullaniciRepository.findById(9L)).thenReturn(Optional.of(
                com.raspel.erp.entity.sistem.Kullanici.builder().id(9L).role("ADMIN").build()));

        DepoTransferDTO sonuc = depoTransferService.reddet(1L, 9L, "Stok hasarlı");

        assertEquals("REDDEDILDI", sonuc.getDurum());
        assertEquals("Stok hasarlı", sonuc.getRedSebebi());
        assertNotNull(sonuc.getRedTarihi());
        assertEquals(9L, sonuc.getOnaylayanKullaniciId());
    }
}
