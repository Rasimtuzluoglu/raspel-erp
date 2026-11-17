package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.NotRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SahaOzetServiceTest {

    @Mock private TeslimatRepository teslimatRepository;
    @Mock private NotRepository notRepository;
    @Mock private PersonelIzinRepository personelIzinRepository;
    @Mock private PersonelMasrafTalepRepository personelMasrafTalepRepository;
    @Mock private PersonelRepository personelRepository;
    @InjectMocks private SahaOzetService service;

    private void surucuVerisiHazirla(Long sirketId, Long kullaniciId) {
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurum(sirketId, kullaniciId, "BEKLEMEDE"))
                .thenReturn(3L);
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurum(sirketId, kullaniciId, "YOLDA"))
                .thenReturn(1L);
        when(teslimatRepository.countBySirketIdAndDriverIdAndTeslimTarihiBetweenAndDurum(
                eq(sirketId), eq(kullaniciId), any(), any(), eq("TESLIM_EDILDI")))
                .thenReturn(2L);
        when(teslimatRepository.countBySirketIdAndDriverIdAndBeklenenTeslimTarihiBeforeAndDurumNotIn(
                eq(sirketId), eq(kullaniciId), any(), any()))
                .thenReturn(1L);
        when(notRepository.countBySirketIdAndKullaniciIdAndOlusturmaTarihiBetween(
                eq(sirketId), eq(kullaniciId), any(), any()))
                .thenReturn(2L);
        when(notRepository.findTop1BySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(sirketId, kullaniciId))
                .thenReturn(Optional.of(Not.builder()
                        .id(5L)
                        .baslik("Ziyaret")
                        .olusturmaTarihi(LocalDateTime.now())
                        .build()));
        when(personelRepository.findByKullaniciId(kullaniciId))
                .thenReturn(Optional.of(Personel.builder().id(77L).sirketId(sirketId).build()));
        when(personelIzinRepository.countByPersonelIdAndDurum(77L, "BEKLEMEDE")).thenReturn(1L);
        when(personelMasrafTalepRepository.countBySirketIdAndKullaniciIdAndDurum(
                sirketId, kullaniciId, "BEKLEMEDE")).thenReturn(2L);
    }

    @Test
    void ozet_talepleriKullaniciyaGoreSayar() {
        surucuVerisiHazirla(1L, 9L);

        service.ozet(1L, 9L);

        verify(personelMasrafTalepRepository)
                .countBySirketIdAndKullaniciIdAndDurum(1L, 9L, "BEKLEMEDE");
        verify(personelIzinRepository).countByPersonelIdAndDurum(77L, "BEKLEMEDE");
        // Şirket geneli sayımlar kullanılmamalı: başka personelin talebi sızmasın.
        verify(personelIzinRepository, never()).countByDurumAndSirketId(anyString(), anyLong());
        verify(personelMasrafTalepRepository, never()).countBySirketIdAndDurum(anyLong(), anyString());
    }

    @Test
    void ozet_surucuSayilariniDondurur() {
        surucuVerisiHazirla(1L, 9L);

        var sonuc = service.ozet(1L, 9L);

        assertEquals(3L, sonuc.getTeslimat().getBekleyen());
        assertEquals(1L, sonuc.getTeslimat().getYolda());
        assertEquals(2L, sonuc.getTeslimat().getBugunTeslimEdilen());
        assertEquals(1L, sonuc.getTeslimat().getGeciken());
        assertEquals(2L, sonuc.getZiyaret().getBugun());
        assertNotNull(sonuc.getZiyaret().getSonZiyaretTarihi());
        assertEquals(1L, sonuc.getTalepler().getBekleyenIzin());
        assertEquals(2L, sonuc.getTalepler().getBekleyenMasraf());
    }

    @Test
    void ozet_yetkiliKullaniciyaGoreSorgular() {
        surucuVerisiHazirla(1L, 9L);

        service.ozet(1L, 9L);

        verify(teslimatRepository, atLeastOnce())
                .countBySirketIdAndDriverIdAndDurum(1L, 9L, "BEKLEMEDE");
        verify(teslimatRepository, never())
                .countBySirketIdAndDriverIdAndDurum(anyLong(), eq(8L), anyString());
        verify(notRepository).findTop1BySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(1L, 9L);
    }

    @Test
    void ozet_bosVerideSifirDoner() {
        var sonuc = service.ozet(1L, 9L);

        assertEquals(0L, sonuc.getTeslimat().getBekleyen());
        assertEquals(0L, sonuc.getTeslimat().getYolda());
        assertEquals(0L, sonuc.getZiyaret().getBugun());
        assertEquals(0L, sonuc.getTalepler().getBekleyenIzin());
        assertNull(sonuc.getZiyaret().getSonZiyaretTarihi());
    }

    @Test
    void ozet_bugunSinirlariniKullanir() {
        surucuVerisiHazirla(1L, 9L);

        service.ozet(1L, 9L);

        ArgumentCaptor<LocalDateTime> bas = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> bit = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(teslimatRepository).countBySirketIdAndDriverIdAndTeslimTarihiBetweenAndDurum(
                eq(1L), eq(9L), bas.capture(), bit.capture(), eq("TESLIM_EDILDI"));

        LocalDate bugun = LocalDate.now();
        assertEquals(bugun.atStartOfDay(), bas.getValue());
        assertEquals(bugun.plusDays(1).atStartOfDay(), bit.getValue());
    }

    @Test
    void ozet_kullaniciYoksaSorguYapmaz() {
        var sonuc = service.ozet(1L, null);

        assertEquals(0L, sonuc.getTeslimat().getBekleyen());
        verifyNoInteractions(teslimatRepository, notRepository);
    }
}
