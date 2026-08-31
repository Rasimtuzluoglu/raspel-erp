package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.sistem.GorevRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjandaServiceTest {

    @Mock
    private GorevRepository gorevRepository;
    @Mock
    private FaturaRepository faturaRepository;

    @InjectMocks
    private AjandaService ajandaService;

    @Test
    void olaylar_gorevleriVeVadeleriBirlesitiripTariheGoreSiralar() {
        LocalDate bas = LocalDate.of(2026, 8, 1);
        LocalDate bit = LocalDate.of(2026, 8, 31);

        Gorev g1 = Gorev.builder().id(1L).ad("Rapor hazırla").durum("YAPILACAK").atanan("Ali").baslangic(LocalDate.of(2026, 8, 10)).build();
        Gorev g2 = Gorev.builder().id(2L).ad("Stok sayımı").durum("TAMAMLANDI").baslangic(LocalDate.of(2026, 8, 3)).build();
        when(gorevRepository.sirketGorevleri(1L, bas, bit)).thenReturn(List.of(g1, g2));

        Fatura f1 = new Fatura();
        f1.setFaturaNumarasi("FTR-1-2026-000001");
        f1.setVadeTarihi(LocalDate.of(2026, 8, 5));
        f1.setKalanTutar(new java.math.BigDecimal("1500.00"));
        when(faturaRepository.findVadesiYaklasan(eq(1L), eq(Fatura.FaturaDurum.KESILDI), any(), eq(bas), eq(bit)))
                .thenReturn(List.of(f1));

        List<AjandaOlayDTO> olaylar = ajandaService.olaylar(1L, bas, bit);

        assertEquals(3, olaylar.size());
        assertEquals("GOREV", olaylar.get(0).getTip());
        assertEquals("Stok sayımı", olaylar.get(0).getBaslik());
        assertEquals("VADE", olaylar.get(1).getTip());
        assertEquals("GOREV", olaylar.get(2).getTip());
        assertTrue(olaylar.get(0).getTarih().isBefore(olaylar.get(2).getTarih()));
    }

    @Test
    void olaylar_bosSonucBosListeDoner() {
        LocalDate bas = LocalDate.of(2026, 8, 1);
        LocalDate bit = LocalDate.of(2026, 8, 31);
        when(gorevRepository.sirketGorevleri(1L, bas, bit)).thenReturn(List.of());
        when(faturaRepository.findVadesiYaklasan(eq(1L), any(), any(), eq(bas), eq(bit))).thenReturn(List.of());

        List<AjandaOlayDTO> olaylar = ajandaService.olaylar(1L, bas, bit);

        assertTrue(olaylar.isEmpty());
    }

    @Test
    void olaylar_gorevBaslangicNullIseBaslangicTarihiniKullanir() {
        LocalDate bas = LocalDate.of(2026, 8, 1);
        LocalDate bit = LocalDate.of(2026, 8, 31);
        Gorev g = Gorev.builder().id(1L).ad("Görev").durum("YAPILACAK").build();
        when(gorevRepository.sirketGorevleri(1L, bas, bit)).thenReturn(List.of(g));
        when(faturaRepository.findVadesiYaklasan(eq(1L), any(), any(), eq(bas), eq(bit))).thenReturn(List.of());

        List<AjandaOlayDTO> olaylar = ajandaService.olaylar(1L, bas, bit);

        assertEquals(1, olaylar.size());
        assertEquals(bas, olaylar.get(0).getTarih());
    }
}
