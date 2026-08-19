package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomaliTespitEngineTest {

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private HareketRepository hareketRepository;

    @Mock
    private StokHareketRepository stokHareketRepository;

    @InjectMocks
    private AnomaliTespitEngine anomaliTespitEngine;

    @Test
    void testAnomalileriTara_MukerrerFaturaTespitEder() {
        CariHesap cari = CariHesap.builder().id(10L).ad("Test Cari").build();
        Fatura f1 = Fatura.builder().id(1L).cariHesap(cari).genelToplam(BigDecimal.valueOf(5000)).build();
        Fatura f2 = Fatura.builder().id(2L).cariHesap(cari).genelToplam(BigDecimal.valueOf(5000)).build();

        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f1, f2)));
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<AnomaliDTO> list = anomaliTespitEngine.anomalileriTara(100L);

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(a -> a.getTur().equals("MUKERRER_FATURA")));
    }

    @Test
    void testAnomalileriTara_AnormalYuksekTutarTespitEder() {
        CariHesap cari = CariHesap.builder().id(10L).ad("Test Cari").build();
        Fatura f1 = Fatura.builder().id(1L).cariHesap(cari).faturaNumarasi("FTR-999").genelToplam(BigDecimal.valueOf(75000)).build();

        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f1)));
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<AnomaliDTO> list = anomaliTespitEngine.anomalileriTara(100L);

        assertNotNull(list);
        assertTrue(list.stream().anyMatch(a -> a.getTur().equals("ANORMAL_MASRAF")));
        assertTrue(list.stream().anyMatch(a -> a.getTur().equals("GUVENLIK_IP")));
    }

    @Test
    void testIpWhitelist_crudOperations() {
        var list = anomaliTespitEngine.getIpWhitelist();
        assertNotNull(list);
        int initialSize = list.size();

        var added = anomaliTespitEngine.addIpWhitelist(java.util.Map.of(
                "ipAdresi", "10.0.0.1",
                "aciklama", "Test VPN"
        ));
        assertEquals(initialSize + 1, added.size());

        String id = String.valueOf(added.get(added.size() - 1).get("id"));
        var afterDelete = anomaliTespitEngine.deleteIpWhitelist(id);
        assertEquals(initialSize, afterDelete.size());
    }
}
