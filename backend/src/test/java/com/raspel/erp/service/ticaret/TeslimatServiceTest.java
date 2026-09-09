package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.SurucuDTO;
import com.raspel.erp.dto.ticaret.TeslimatDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeslimatServiceTest {

    @Mock private TeslimatRepository teslimatRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private FaturaRepository faturaRepository;
    @InjectMocks private TeslimatService teslimatService;

    @Test
    void suruculer_driverOlmayanKullaniciIcinTumSuruculeriDoner() {
        Kullanici s1 = Kullanici.builder().id(1L).displayName("Ali").role("DRIVER").build();
        Kullanici s2 = Kullanici.builder().id(2L).displayName("Veli").role("DRIVER").build();
        when(kullaniciRepository.findBySirketIdAndRole(1L, "DRIVER")).thenReturn(List.of(s1, s2));
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurumIn(eq(1L), eq(1L), anyList())).thenReturn(2L);
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurumIn(eq(1L), eq(2L), anyList())).thenReturn(0L);

        List<SurucuDTO> sonuc = teslimatService.suruculer(1L, 99L);

        assertEquals(2, sonuc.size());
        assertEquals(2L, sonuc.get(0).getBekleyenTeslimatSayisi());
    }

    @Test
    void suruculer_driverKendiDisindakiSurucuyuGoremez() {
        Kullanici s1 = Kullanici.builder().id(1L).displayName("Ali").role("DRIVER").build();
        when(kullaniciRepository.findBySirketIdAndRole(1L, "DRIVER")).thenReturn(List.of(s1));
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(Kullanici.builder().id(1L).role("DRIVER").build()));
        when(teslimatRepository.countBySirketIdAndDriverIdAndDurumIn(1L, 1L, List.of("BEKLEMEDE", "YOLDA"))).thenReturn(1L);

        List<SurucuDTO> sonuc = teslimatService.suruculer(1L, 1L);

        assertEquals(1, sonuc.size());
        assertEquals(1L, sonuc.get(0).getId());
    }

    @Test
    void olustur_surucuOlmayanKullaniciHataVerir() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(Kullanici.builder().id(5L).role("USER").build()));

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(5L).teslimatAdresi("Adres").build();
        assertThrows(BusinessException.class, () -> teslimatService.olustur(dto, 1L));
    }

    @Test
    void olustur_basariliKayitOlusturur() {
        Fatura f = Fatura.builder().id(10L).sirketId(1L).faturaNumarasi("F-1").build();
        Kullanici surucu = Kullanici.builder().id(5L).displayName("Ali").role("DRIVER").build();
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(f));
        when(kullaniciRepository.findById(5L)).thenReturn(Optional.of(surucu));
        when(teslimatRepository.save(any(Teslimat.class))).thenAnswer(inv -> {
            Teslimat t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TeslimatDTO dto = TeslimatDTO.builder().faturaId(10L).driverId(5L).teslimatAdresi("Adres").build();
        TeslimatDTO sonuc = teslimatService.olustur(dto, 1L);

        assertEquals(1L, sonuc.getId());
        assertEquals("F-1", sonuc.getFaturaNumarasi());
        assertEquals("BEKLEMEDE", sonuc.getDurum());
    }
}
