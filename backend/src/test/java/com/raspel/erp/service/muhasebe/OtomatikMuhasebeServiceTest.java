package com.raspel.erp.service.muhasebe;

import com.raspel.erp.dto.muhasebe.MuhasebeFisKalemDTO;
import com.raspel.erp.dto.muhasebe.MuhasebeFisiDTO;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.muhasebe.MuhasebeFisi;
import com.raspel.erp.repository.muhasebe.HesapPlaniRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtomatikMuhasebeServiceTest {

    @Mock private MuhasebeService muhasebeService;
    @Mock private MuhasebeFisiRepository muhasebeFisiRepository;
    @Mock private HesapPlaniRepository hesapPlaniRepository;
    @InjectMocks private OtomatikMuhasebeService otomatikMuhasebeService;

    private MaasBordro bordro() {
        Personel p = new Personel();
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        return MaasBordro.builder()
                .id(5L).sirketId(1L)
                .brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00"))
                .netMaas(new BigDecimal("21000.00"))
                .yil(2026).ay(7)
                .odemeTarihi(LocalDate.of(2026, 7, 30))
                .personel(p)
                .build();
    }

    @Test
    void bordroIsle_denkFisOlusturur() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());

        otomatikMuhasebeService.bordroIsle(bordro());

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("BORDRO", dto.getKaynakTip());
        assertEquals(5L, dto.getKaynakId());

        BigDecimal borc = dto.getKalemler().stream().map(MuhasebeFisKalemDTO::getBorc).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alacak = dto.getKalemler().stream().map(MuhasebeFisKalemDTO::getAlacak).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, borc.compareTo(new BigDecimal("30000.00")));
        assertEquals(0, alacak.compareTo(new BigDecimal("30000.00")));
        // 770 borç, 335 alacak net, 360 alacak kesinti
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "770".equals(k.getHesapKodu())));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "335".equals(k.getHesapKodu())
                && k.getAlacak().compareTo(new BigDecimal("21000.00")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "360".equals(k.getHesapKodu())
                && k.getAlacak().compareTo(new BigDecimal("9000.00")) == 0));
    }

    @Test
    void bordroIsle_idempotent() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.of(new MuhasebeFisi()));

        otomatikMuhasebeService.bordroIsle(bordro());

        verify(muhasebeService, never()).fisOlustur(any());
    }

    @Test
    void bordroIsle_hataDurumundaBloklamaz() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(muhasebeService.fisOlustur(any())).thenThrow(new RuntimeException("hesap yok"));

        assertDoesNotThrow(() -> otomatikMuhasebeService.bordroIsle(bordro()));
    }
}
