package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.ReceteDTO;
import com.raspel.erp.dto.envanter.UretimEmriDTO;
import com.raspel.erp.entity.envanter.Recete;
import com.raspel.erp.entity.envanter.ReceteKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.UretimEmri;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.ReceteKalemRepository;
import com.raspel.erp.repository.envanter.ReceteRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UretimServiceTest {

    @Mock private ReceteRepository receteRepository;
    @Mock private ReceteKalemRepository receteKalemRepository;
    @Mock private UretimEmriRepository uretimEmriRepository;
    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @InjectMocks private UretimService uretimService;

    private Stok stok(Long id, String ad, String miktar) {
        Stok s = new Stok();
        s.setId(id);
        s.setAd(ad);
        s.setMiktar(new BigDecimal(miktar));
        return s;
    }

    @Test
    void receteOlustur_bosAdHataVerir() {
        assertThrows(BusinessException.class,
                () -> uretimService.receteOlustur(ReceteDTO.builder().ad(" ").urunId(1L).build(), 1L));
    }

    @Test
    void uretimEmriTamamla_hammaddeleriTuketirVeMamulUretir() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(new BigDecimal("2")).durum("TASLAK").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "MDF", "10");
        Stok mamul = stok(10L, "Masa", "0");

        when(uretimEmriRepository.findById(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunId(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mamul));

        uretimService.uretimEmriTamamla(1L, 1L);

        assertEquals(0, hammadde.getMiktar().compareTo(new BigDecimal("4")));  // 10 - 3*2
        assertEquals(0, mamul.getMiktar().compareTo(new BigDecimal("2")));
        assertEquals("TAMAMLANDI", emri.getDurum());
        verify(stokHareketRepository, times(2)).save(any());
    }

    @Test
    void uretimEmriTamamla_yetersizHammaddeHataVerir() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(new BigDecimal("5")).durum("TASLAK").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "MDF", "10");

        when(uretimEmriRepository.findById(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunId(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));

        assertThrows(BusinessException.class, () -> uretimService.uretimEmriTamamla(1L, 1L));
    }

    @Test
    void uretimEmriOlustur_gecersizMiktarHataVerir() {
        assertThrows(BusinessException.class,
                () -> uretimService.uretimEmriOlustur(UretimEmriDTO.builder().urunId(1L).miktar(BigDecimal.ZERO).build(), 1L));
    }
}
