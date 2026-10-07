package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CariAdresDTO;
import com.raspel.erp.entity.finans.CariAdres;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariAdresRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.finans.CariAdresService;
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
class CariAdresServiceTest {

    @Mock private CariAdresRepository cariAdresRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private CariAdresService cariAdresService;

    private CariHesap cari() {
        return CariHesap.builder().id(1L).ad("Cari").sirketId(1L).bakiye(BigDecimal.ZERO).build();
    }

    @Test
    void listele_cariAdresleriniDoner() {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari()));
        when(cariAdresRepository.findBySirketIdAndCariHesapIdOrderByVarsayilanDescIdAsc(1L, 1L))
                .thenReturn(List.of(CariAdres.builder().id(10L).cariHesapId(1L).sirketId(1L)
                        .adres("Adres 1").varsayilan(true).build()));

        var sonuc = cariAdresService.listele(1L, 1L);

        assertEquals(1, sonuc.size());
        assertEquals("Adres 1", sonuc.get(0).getAdres());
        assertTrue(sonuc.get(0).getVarsayilan());
    }

    @Test
    void ekle_ilkKayitOtomatikVarsayilanOlur() {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari()));
        when(cariAdresRepository.countBySirketIdAndCariHesapId(1L, 1L)).thenReturn(0L);
        when(cariAdresRepository.save(any(CariAdres.class))).thenAnswer(inv -> {
            CariAdres a = inv.getArgument(0);
            a.setId(5L);
            return a;
        });
        when(cariAdresRepository.findBySirketIdAndCariHesapIdOrderByVarsayilanDescIdAsc(1L, 1L))
                .thenReturn(List.of(CariAdres.builder().id(5L).cariHesapId(1L).sirketId(1L)
                        .adres("Yeni").varsayilan(true).build()));

        CariAdresDTO dto = CariAdresDTO.builder().adres("Yeni").build();
        var sonuc = cariAdresService.ekle(1L, dto, 1L);

        assertTrue(sonuc.getVarsayilan());
    }

    @Test
    void sil_tenantDogrular() {
        when(cariAdresRepository.findById(9L)).thenReturn(Optional.of(
                CariAdres.builder().id(9L).cariHesapId(1L).sirketId(1L).build()));

        cariAdresService.sil(9L, 1L);

        verify(tenantChecker).check(1L, "CariAdres");
        verify(cariAdresRepository).deleteById(9L);
    }
}
