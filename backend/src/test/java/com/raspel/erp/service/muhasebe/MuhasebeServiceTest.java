package com.raspel.erp.service.muhasebe;

import com.raspel.erp.dto.muhasebe.*;
import com.raspel.erp.entity.muhasebe.HesapPlani;
import com.raspel.erp.entity.muhasebe.MuhasebeFisKalem;
import com.raspel.erp.entity.muhasebe.MuhasebeFisi;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.DuplicateResourceException;
import com.raspel.erp.repository.muhasebe.HesapPlaniRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisKalemRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisiRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MuhasebeServiceTest {

    @Mock
    private HesapPlaniRepository hesapPlaniRepository;
    @Mock
    private MuhasebeFisiRepository muhasebeFisiRepository;
    @Mock
    private MuhasebeFisKalemRepository muhasebeFisKalemRepository;

    @InjectMocks
    private MuhasebeService muhasebeService;

    private HesapPlani kasaHesabi;

    @BeforeEach
    void setUp() {
        kasaHesabi = HesapPlani.builder().id(1L).kod("100").ad("Kasa").tip("AKTIF").sirketId(1L).aktif(true).build();
    }

    @Test
    void testHesapPlaniniGetir_BosIseVarsayilanlariOlusturur() {
        when(hesapPlaniRepository.findBySirketIdOrderByKodAsc(1L)).thenReturn(List.of(), List.of(kasaHesabi));

        List<HesapPlaniDTO> result = muhasebeService.hesapPlaniniGetir(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(hesapPlaniRepository, times(1)).saveAll(any());
    }

    @Test
    void testHesapOlustur_MukerrerKodHatasiFirlatir() {
        when(hesapPlaniRepository.findBySirketIdAndKod(1L, "100")).thenReturn(Optional.of(kasaHesabi));

        HesapPlaniDTO dto = HesapPlaniDTO.builder().kod("100").ad("Kasa").tip("AKTIF").sirketId(1L).build();

        assertThrows(DuplicateResourceException.class, () -> muhasebeService.hesapOlustur(dto));
    }

    @Test
    void testFisOlustur_DengesizFisteHataFirlatir() {
        MuhasebeFisKalemDTO k1 = MuhasebeFisKalemDTO.builder().hesapKodu("100").borc(BigDecimal.valueOf(100)).alacak(BigDecimal.ZERO).build();
        MuhasebeFisKalemDTO k2 = MuhasebeFisKalemDTO.builder().hesapKodu("320").borc(BigDecimal.ZERO).alacak(BigDecimal.valueOf(50)).build();

        MuhasebeFisiDTO dto = MuhasebeFisiDTO.builder()
                .tarih(LocalDate.now()).sirketId(1L)
                .kalemler(List.of(k1, k2)).build();

        assertThrows(BusinessException.class, () -> muhasebeService.fisOlustur(dto));
    }

    @Test
    void testFisOlustur_DengeliFisBasariylaKaydedilir() {
        MuhasebeFisKalemDTO k1 = MuhasebeFisKalemDTO.builder().hesapKodu("100").borc(BigDecimal.valueOf(100)).alacak(BigDecimal.ZERO).build();
        MuhasebeFisKalemDTO k2 = MuhasebeFisKalemDTO.builder().hesapKodu("320").borc(BigDecimal.ZERO).alacak(BigDecimal.valueOf(100)).build();

        when(hesapPlaniRepository.findBySirketIdAndKod(1L, "100")).thenReturn(Optional.of(kasaHesabi));
        when(hesapPlaniRepository.findBySirketIdAndKod(1L, "320")).thenReturn(Optional.of(kasaHesabi));
        when(muhasebeFisiRepository.findTopBySirketIdOrderByFisNoDesc(1L)).thenReturn(Optional.empty());
        when(muhasebeFisiRepository.save(any(MuhasebeFisi.class))).thenAnswer(inv -> {
            MuhasebeFisi f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });
        when(muhasebeFisKalemRepository.findByFisIdOrderByIdAsc(1L))
                .thenReturn(List.of(
                        MuhasebeFisKalem.builder().hesapKodu("100").borc(BigDecimal.valueOf(100)).alacak(BigDecimal.ZERO).build(),
                        MuhasebeFisKalem.builder().hesapKodu("320").borc(BigDecimal.ZERO).alacak(BigDecimal.valueOf(100)).build()
                ));

        MuhasebeFisiDTO dto = MuhasebeFisiDTO.builder()
                .tarih(LocalDate.now()).sirketId(1L)
                .kalemler(List.of(k1, k2)).build();

        MuhasebeFisiDTO result = muhasebeService.fisOlustur(dto);

        assertNotNull(result);
        assertTrue(result.getFisNo().startsWith("MUH-"));
        assertEquals(0, result.getToplamBorc().subtract(result.getToplamAlacak()).intValue());
        verify(muhasebeFisiRepository, times(1)).save(any(MuhasebeFisi.class));
    }

    @Test
    void testMizan_HesapBazliBakiyeHesaplar() {
        MuhasebeFisKalem borc = MuhasebeFisKalem.builder().hesapKodu("100").hesapAdi("Kasa").borc(BigDecimal.valueOf(250)).alacak(BigDecimal.ZERO).build();
        MuhasebeFisKalem alacak = MuhasebeFisKalem.builder().hesapKodu("100").hesapAdi("Kasa").borc(BigDecimal.ZERO).alacak(BigDecimal.valueOf(100)).build();
        when(muhasebeFisKalemRepository.findBySirketId(1L)).thenReturn(List.of(borc, alacak));

        List<MizanSatiriDTO> mizan = muhasebeService.mizanGetir(1L, LocalDate.now().minusMonths(1), LocalDate.now());

        assertEquals(1, mizan.size());
        assertEquals(BigDecimal.valueOf(150), mizan.get(0).getBorcBakiye());
    }

    @Test
    void testFisIptal_OnayliFisIptalEdilemez() {
        MuhasebeFisi onayliFis = MuhasebeFisi.builder().id(1L).durum("ONAYLANDI").build();
        when(muhasebeFisiRepository.findById(1L)).thenReturn(Optional.of(onayliFis));

        assertThrows(BusinessException.class, () -> muhasebeService.fisIptalEt(1L));
    }
}
