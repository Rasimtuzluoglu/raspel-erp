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

    @InjectMocks
    private DepoTransferService depoTransferService;

    @BeforeEach
    void setUp() {
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
        when(transferRepository.save(any(DepoTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        DepoTransferDTO sonuc = depoTransferService.talepOlustur(dto, 4L, 1L);

        assertNotNull(sonuc);
        assertEquals("BEKLIYOR", sonuc.getDurum());
        verify(transferRepository).save(any(DepoTransfer.class));
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
}
