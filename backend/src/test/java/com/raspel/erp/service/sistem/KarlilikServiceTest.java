package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.service.envanter.MaliyetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KarlilikServiceTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private FaturaKalemRepository faturaKalemRepository;
    @Mock private StokRepository stokRepository;
    @Mock private IadeRepository iadeRepository;
    @Mock private IadeKalemRepository iadeKalemRepository;
    @Mock private MaliyetService maliyetService;
    @InjectMocks private KarlilikService service;

    private static final Long SIRKET = 1L;
    private final LocalDate BAS = LocalDate.of(2026, 9, 1);
    private final LocalDate BIT = LocalDate.of(2026, 9, 30);

    private Fatura satis(Long id) {
        return Fatura.builder().id(id).tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI).tarih(LocalDate.of(2026, 9, 10)).sirketId(SIRKET).build();
    }

    private FaturaKalem kalem(Long stokId, String adet, String birimFiyat, String birimMaliyet) {
        return FaturaKalem.builder().stokId(stokId)
                .adet(new BigDecimal(adet)).birimFiyat(new BigDecimal(birimFiyat))
                .iskontoOrani(BigDecimal.ZERO)
                .birimMaliyet(birimMaliyet != null ? new BigDecimal(birimMaliyet) : null)
                .build();
    }

    private Stok stok(Long id, String kategori) {
        return Stok.builder().id(id).ad("Ürün" + id).kategori(kategori).sirketId(SIRKET)
                .ortalamaMaliyet(new BigDecimal("40")).build();
    }

    private void bosIade() {
        when(iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(anyLong(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void kategoriKirilimi_veOzetHesaplanir() {
        when(faturaRepository.findBySirketIdAndTarihBetween(SIRKET, BAS, BIT)).thenReturn(List.of(satis(1L)));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(
                kalem(10L, "2", "100", "40"),
                kalem(11L, "1", "200", "150")));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok(10L, "Elektronik")));
        when(stokRepository.findById(11L)).thenReturn(Optional.of(stok(11L, "Gıda")));
        bosIade();

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, "KATEGORI");

        assertEquals("KATEGORI", r.getGrup());
        // ciro = 200 + 200 = 400 ; maliyet = 80 + 150 = 230
        assertEquals(0, r.getOzet().getCiro().compareTo(new BigDecimal("400.00")));
        assertEquals(0, r.getOzet().getMaliyet().compareTo(new BigDecimal("230.00")));
        assertEquals(0, r.getOzet().getBrutKar().compareTo(new BigDecimal("170.00")));
        assertEquals(2, r.getKirilim().size());
        assertEquals(1, r.getAylikTrend().size());
        assertEquals("2026-09", r.getAylikTrend().get(0).getAy());
    }

    @Test
    void snapshotMaliyetOrtalamadanOnceKullanilir() {
        when(faturaRepository.findBySirketIdAndTarihBetween(SIRKET, BAS, BIT)).thenReturn(List.of(satis(1L)));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(kalem(10L, "1", "100", "25")));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok(10L, "X")));
        bosIade();

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, "KATEGORI");

        assertEquals(0, r.getOzet().getMaliyet().compareTo(new BigDecimal("25.00")));
    }

    @Test
    void iadeCiroVeMaliyetiDuser() {
        when(faturaRepository.findBySirketIdAndTarihBetween(SIRKET, BAS, BIT)).thenReturn(List.of(satis(1L)));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(kalem(10L, "2", "100", "40")));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok(10L, "Elektronik")));
        when(maliyetService.ortalamaMaliyet(any(Stok.class))).thenReturn(new BigDecimal("40"));
        Iade iade = Iade.builder().id(5L).tur("SATIS").durum("TAMAMLANDI")
                .tarih(LocalDate.of(2026, 9, 12)).sirketId(SIRKET).build();
        when(iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(SIRKET, "SATIS", "TAMAMLANDI", BAS, BIT))
                .thenReturn(List.of(iade));
        when(iadeKalemRepository.findByIadeId(5L)).thenReturn(List.of(
                IadeKalem.builder().id(1L).iadeId(5L).stokId(10L).miktar(BigDecimal.ONE)
                        .birimFiyat(new BigDecimal("100")).build()));

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, "KATEGORI");

        // ciro = 200 - 100 = 100 ; maliyet = 80 - 40 = 40
        assertEquals(0, r.getOzet().getCiro().compareTo(new BigDecimal("100.00")));
        assertEquals(0, r.getOzet().getMaliyet().compareTo(new BigDecimal("40.00")));
        assertEquals(0, r.getOzet().getIadeTutari().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void negatifMarjliKalemlerListelenir() {
        when(faturaRepository.findBySirketIdAndTarihBetween(SIRKET, BAS, BIT)).thenReturn(List.of(satis(1L)));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(
                kalem(10L, "1", "50", "80"),
                kalem(11L, "1", "200", "150")));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok(10L, "Zarar")));
        when(stokRepository.findById(11L)).thenReturn(Optional.of(stok(11L, "Kar")));
        bosIade();

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, "KATEGORI");

        assertEquals(1, r.getNegatifMarjli().size());
        assertEquals("Zarar", r.getNegatifMarjli().get(0).getAd());
        assertEquals(1, r.getOzet().getNegatifMarjliAdet());
    }

    @Test
    void urunGruplamaStokAdiniKullanir() {
        when(faturaRepository.findBySirketIdAndTarihBetween(SIRKET, BAS, BIT)).thenReturn(List.of(satis(1L)));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(kalem(10L, "1", "100", "40")));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok(10L, "X")));
        bosIade();

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, "URUN");

        assertEquals("Ürün10", r.getKirilim().get(0).getAd());
        assertEquals(10L, r.getKirilim().get(0).getId());
    }

    @Test
    void kesilmeyenVeAlisFaturalarYoksayilir() {
        Fatura taslak = Fatura.builder().id(2L).tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.TASLAK).tarih(LocalDate.of(2026, 9, 10)).sirketId(SIRKET).build();
        Fatura alis = Fatura.builder().id(3L).tur(Fatura.FaturaTur.ALIS)
                .durum(Fatura.FaturaDurum.KESILDI).tarih(LocalDate.of(2026, 9, 10)).sirketId(SIRKET).build();
        when(faturaRepository.findBySirketIdAndTarihBetween(SIRKET, BAS, BIT)).thenReturn(List.of(taslak, alis));
        bosIade();

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, null);

        assertEquals(0, r.getOzet().getCiro().compareTo(BigDecimal.ZERO));
        assertTrue(r.getKirilim().isEmpty());
        assertEquals("KATEGORI", r.getGrup());
    }

    @Test
    void bosVeriSifirDoner() {
        when(faturaRepository.findBySirketIdAndTarihBetween(anyLong(), any(), any())).thenReturn(List.of());
        bosIade();

        var r = service.karlilikAnalizi(SIRKET, BAS, BIT, "KATEGORI");

        assertNotNull(r.getOzet());
        assertEquals(0, r.getOzet().getBrutKar().compareTo(BigDecimal.ZERO));
        assertTrue(r.getAylikTrend().isEmpty());
    }
}
