package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.TekrarlayanFaturaDTO;
import com.raspel.erp.entity.ticaret.TekrarlayanFatura;
import com.raspel.erp.entity.ticaret.TekrarlayanFaturaKalem;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.TekrarlayanFaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TekrarlayanFaturaServiceTest {

    @Mock
    private TekrarlayanFaturaRepository repository;

    @Mock
    private FaturaService faturaService;

    @Mock
    private CariHesapRepository cariHesapRepository;

    @InjectMocks
    private TekrarlayanFaturaService tekrarlayanFaturaService;

    private TekrarlayanFatura ornekTekrarlayanFatura() {
        TekrarlayanFatura tf = TekrarlayanFatura.builder()
                .id(1L).sirketId(4L).cariHesapId(3L)
                .tur("SATIS").periyot("AYLIK")
                .baslangicTarihi(LocalDate.of(2026, 9, 1))
                .sonrakiCalistirma(LocalDate.of(2026, 9, 1))
                .aktif(true)
                .kalemler(new ArrayList<>())
                .build();
        tf.getKalemler().add(TekrarlayanFaturaKalem.builder()
                .id(1L).tekrarlayanFatura(tf)
                .aciklama("Bakim ucreti").adet(1).birimFiyat(BigDecimal.valueOf(1500))
                .kdvOrani(BigDecimal.valueOf(20)).build());
        return tf;
    }

    @Test
    void faturaUret_faturaOlustururVeTarihiIlerletir() {
        TekrarlayanFatura tf = ornekTekrarlayanFatura();
        when(repository.findById(1L)).thenReturn(Optional.of(tf));
        when(repository.save(any(TekrarlayanFatura.class))).thenAnswer(inv -> inv.getArgument(0));

        tekrarlayanFaturaService.faturaUret(1L);

        verify(faturaService).faturaOlustur(any(), eq(4L), isNull(), eq("Tekrarlayan Fatura"));
        assertEquals(LocalDate.of(2026, 10, 1), tf.getSonrakiCalistirma());
        verify(repository).save(tf);
    }

    @Test
    void faturaUret_bitisTarihiGecinceAktifligiKapatir() {
        TekrarlayanFatura tf = ornekTekrarlayanFatura();
        tf.setBitisTarihi(LocalDate.of(2026, 8, 1));
        tf.setSonrakiCalistirma(LocalDate.of(2026, 9, 1));
        when(repository.findById(1L)).thenReturn(Optional.of(tf));
        when(repository.save(any(TekrarlayanFatura.class))).thenAnswer(inv -> inv.getArgument(0));

        tekrarlayanFaturaService.faturaUret(1L);

        assertFalse(tf.getAktif());
        verify(faturaService, never()).faturaOlustur(any(), any(), any(), any());
    }

    @Test
    void olustur_kalemleriIleKaydeder() {
        TekrarlayanFaturaDTO.TekrarlayanFaturaKalemDTO kalem = TekrarlayanFaturaDTO.TekrarlayanFaturaKalemDTO.builder()
                .aciklama("Test").adet(1).birimFiyat(BigDecimal.valueOf(100)).build();
        TekrarlayanFaturaDTO dto = TekrarlayanFaturaDTO.builder()
                .cariHesapId(3L).tur("SATIS").periyot("AYLIK")
                .baslangicTarihi(LocalDate.of(2026, 9, 1))
                .kalemler(List.of(kalem)).build();
        when(repository.save(any(TekrarlayanFatura.class))).thenAnswer(inv -> inv.getArgument(0));

        TekrarlayanFaturaDTO sonuc = tekrarlayanFaturaService.olustur(dto, 4L);

        assertNotNull(sonuc);
        assertEquals("AYLIK", sonuc.getPeriyot());
        assertEquals(1, sonuc.getKalemler().size());
    }
}
