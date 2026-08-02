package com.raspel.erp.service;

import com.raspel.erp.dto.FaturaDTO;
import com.raspel.erp.dto.FaturaKalemDTO;
import com.raspel.erp.entity.*;
import com.raspel.erp.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturaServiceTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private SeriNoServisi seriNoServisi;
    @Mock private BildirimService bildirimService;
    @Mock private EmailService emailService;
    @Mock private PdfRaporService pdfRaporService;
    @Mock private com.raspel.erp.repository.SirketRepository sirketRepository;
    @InjectMocks private FaturaService faturaService;

    private CariHesap createCariHesap() {
        CariHesap c = new CariHesap();
        c.setId(1L);
        c.setAd("Test Cari");
        c.setBakiye(BigDecimal.ZERO);
        c.setOlusturmaTarihi(LocalDateTime.now());
        c.setGuncellemeTarihi(LocalDateTime.now());
        return c;
    }

    private Stok createStok() {
        Stok s = new Stok();
        s.setId(1L);
        s.setAd("Test Stok");
        s.setStokKodu("STK001");
        s.setMiktar(BigDecimal.valueOf(100));
        s.setFiyat(BigDecimal.valueOf(50));
        s.setOlusturmaTarihi(LocalDateTime.now());
        return s;
    }

    private Fatura createFatura(Long id) {
        CariHesap c = createCariHesap();
        Fatura f = new Fatura();
        f.setId(id);
        f.setFaturaNumarasi("FTR-202607-0001");
        f.setTarih(LocalDate.now());
        f.setTur(Fatura.FaturaTur.SATIS);
        f.setDurum(Fatura.FaturaDurum.TASLAK);
        f.setCariHesap(c);
        f.setAraToplam(BigDecimal.valueOf(100));
        f.setKdv(BigDecimal.valueOf(20));
        f.setGenelToplam(BigDecimal.valueOf(120));
        f.setOlusturmaTarihi(LocalDateTime.now());
        f.setKalemler(new ArrayList<>());
        return f;
    }

    @Test
    void tumFaturalariGetir_returnsAll() {
        when(faturaRepository.findBySirketIdOrderByTarihDesc(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createFatura(1L))));
        var result = faturaService.tumFaturalariGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void faturaGetir_returnsFatura() {
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(createFatura(1L)));
        var result = faturaService.faturaGetir(1L);
        assertNotNull(result);
        assertEquals("FTR-202607-0001", result.getFaturaNumarasi());
    }

    @Test
    void faturaGetir_throwsWhenNotFound() {
        when(faturaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> faturaService.faturaGetir(99L));
    }

    @Test
    void faturaOlustur_creates() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Kalem 1").adet(2)
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        Fatura saved = createFatura(1L);
        saved.setDurum(Fatura.FaturaDurum.KESILDI);
        when(faturaRepository.save(any(Fatura.class))).thenReturn(saved);
        var result = faturaService.faturaOlustur(dto, 1L);
        assertNotNull(result);
    }

    @Test
    void faturaOlustur_throwsWhenInvalidTur() {
        FaturaDTO dto = FaturaDTO.builder().tur("INVALID").tarih(LocalDate.now()).kalemler(List.of()).build();
        assertThrows(RuntimeException.class, () -> faturaService.faturaOlustur(dto, 1L));
    }

    @Test
    void faturaDurumGuncelle_updatesToKesildi() {
        Fatura fatura = createFatura(1L);
        Stok stok = createStok();
        FaturaKalem kalem = FaturaKalem.builder().id(1L).fatura(fatura).aciklama("K").adet(2)
                .birimFiyat(BigDecimal.valueOf(100)).kdvOrani(BigDecimal.valueOf(20))
                .tutar(BigDecimal.valueOf(240)).stokId(1L).build();
        fatura.getKalemler().add(kalem);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);
        var result = faturaService.faturaDurumGuncelle(1L, "KESILDI");
        assertEquals("KESILDI", result.getDurum());
    }

    @Test
    void faturaDurumGuncelle_throwsWhenIptalFatura() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.IPTAL);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaDurumGuncelle(1L, "KESILDI"));
    }

    @Test
    void faturaDurumGuncelle_throwsWhenInvalidDurum() {
        Fatura fatura = createFatura(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaDurumGuncelle(1L, "INVALID"));
    }

    @Test
    void faturaGuncelle_updatesDraft() {
        Fatura fatura = createFatura(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari));
        FaturaKalemDTO kalem = FaturaKalemDTO.builder().aciklama("Yeni Kalem").adet(1)
                .birimFiyat(BigDecimal.valueOf(200)).kdvOrani(BigDecimal.valueOf(20)).build();
        FaturaDTO dto = FaturaDTO.builder().tur("SATIS").tarih(LocalDate.now())
                .cariHesapId(1L).kalemler(List.of(kalem)).build();
        when(faturaRepository.save(any(Fatura.class))).thenReturn(fatura);
        var result = faturaService.faturaGuncelle(1L, dto);
        assertNotNull(result);
    }

    @Test
    void faturaGuncelle_throwsWhenNotDraft() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaGuncelle(1L, new FaturaDTO()));
    }

    @Test
    void faturaSil_deletesDraft() {
        Fatura fatura = createFatura(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        faturaService.faturaSil(1L);
        verify(faturaRepository).deleteById(1L);
    }

    @Test
    void faturaSil_throwsWhenKesildi() {
        Fatura fatura = createFatura(1L);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        assertThrows(RuntimeException.class, () -> faturaService.faturaSil(1L));
    }

    @Test
    void faturaSil_throwsWhenNotFound() {
        when(faturaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> faturaService.faturaSil(99L));
    }
}
