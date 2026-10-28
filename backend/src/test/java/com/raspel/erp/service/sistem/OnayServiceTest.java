package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.OnayBekleyenDTO;
import com.raspel.erp.dto.sistem.OnayKararDTO;
import com.raspel.erp.dto.sistem.OnayKararRequest;
import com.raspel.erp.dto.sistem.OnayOzetDTO;
import com.raspel.erp.dto.sistem.OnayTopluRequest;
import com.raspel.erp.dto.sistem.OnayTopluSonucDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.PersonelIzin;
import com.raspel.erp.entity.ik.PersonelMasrafTalep;
import com.raspel.erp.entity.sistem.OnayKarar;
import com.raspel.erp.entity.ticaret.SatinalmaTalep;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.OnayKararRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepKalemRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.service.ik.PersonelIzinService;
import com.raspel.erp.service.ik.PersonelMasrafTalepService;
import com.raspel.erp.service.ticaret.SatinalmaTalepService;
import com.raspel.erp.service.ticaret.SiparisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnayServiceTest {

    @Mock private OnayKararRepository onayKararRepository;
    @Mock private PersonelIzinRepository personelIzinRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private PersonelMasrafTalepRepository personelMasrafTalepRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private SatinalmaTalepRepository satinalmaTalepRepository;
    @Mock private SatinalmaTalepKalemRepository satinalmaTalepKalemRepository;
    @Mock private SiparisRepository siparisRepository;
    @Mock private CariHesapRepository cariHesapRepository;

    @Mock private PersonelIzinService personelIzinService;
    @Mock private PersonelMasrafTalepService personelMasrafTalepService;
    @Mock private SatinalmaTalepService satinalmaTalepService;
    @Mock private SiparisService siparisService;

    @InjectMocks
    private OnayService onayService;

    private void kayitCevabi() {
        when(onayKararRepository.save(any(OnayKarar.class))).thenAnswer(inv -> {
            OnayKarar k = inv.getArgument(0);
            if (k.getId() == null) k.setId(99L);
            return k;
        });
    }

    // ------------------------------------------------------------------
    // Tür eşleme + karar kaydı
    // ------------------------------------------------------------------

    @Test
    void karar_izinOnay_modulServisiniCagirirVeKararYazar() {
        kayitCevabi();
        when(personelIzinRepository.findById(5L)).thenReturn(Optional.of(
                PersonelIzin.builder().id(5L).personelId(3L).durum("BEKLEMEDE").build()));

        OnayKararDTO dto = onayService.karar(1L, 7L, "Ali Veli",
                OnayKararRequest.builder().tur("IZIN").kaynakId(5L).karar("ONAY").build());

        verify(personelIzinService).durumGuncelle(5L, "ONAYLANDI", "Ali Veli");
        assertEquals("BEKLEMEDE", dto.getOncekiDurum());
        assertEquals("ONAYLANDI", dto.getYeniDurum());
        assertEquals("IZIN", dto.getTur());
        assertEquals("ONAY", dto.getKarar());
        assertEquals(7L, dto.getKararVerenId());
        assertEquals("Ali Veli", dto.getKararVerenAd());
        assertEquals(99L, dto.getId());
    }

    @Test
    void karar_masrafRed_reddetCagirirVeGerekceyiSaklar() {
        kayitCevabi();
        when(personelMasrafTalepRepository.findById(3L)).thenReturn(Optional.of(
                PersonelMasrafTalep.builder().id(3L).durum("BEKLEMEDE").build()));

        OnayKararDTO dto = onayService.karar(1L, 7L, "Ali",
                OnayKararRequest.builder().tur("MASRAF").kaynakId(3L).karar("RED")
                        .gerekce("Belge eksik").build());

        verify(personelMasrafTalepService).reddet(3L, "Ali", "Belge eksik");
        verify(personelMasrafTalepService, never()).onayla(any(), any(), any());
        assertEquals("REDDEDILDI", dto.getYeniDurum());
        assertEquals("Belge eksik", dto.getGerekce());
    }

    @Test
    void karar_satinalmaOnay_durumGuncelleCagirir() {
        kayitCevabi();
        when(satinalmaTalepRepository.findById(8L)).thenReturn(Optional.of(
                SatinalmaTalep.builder().id(8L).durum("TASLAK").build()));

        OnayKararDTO dto = onayService.karar(1L, 7L, "Ali",
                OnayKararRequest.builder().tur("SATINALMA").kaynakId(8L).karar("ONAY").build());

        verify(satinalmaTalepService).durumGuncelle(8L, "ONAYLANDI");
        assertEquals("TASLAK", dto.getOncekiDurum());
        assertEquals("ONAYLANDI", dto.getYeniDurum());
    }

    @Test
    void karar_siparisRed_iptalEder() {
        kayitCevabi();
        when(siparisRepository.findById(9L)).thenReturn(Optional.of(
                Siparis.builder().id(9L).durum("TEKLIF").build()));

        OnayKararDTO dto = onayService.karar(1L, 7L, "Ali",
                OnayKararRequest.builder().tur("SIPARIS").kaynakId(9L).karar("RED")
                        .gerekce("Müşteri vazgeçti").build());

        verify(siparisService).durumGuncelle(9L, "IPTAL");
        assertEquals("TEKLIF", dto.getOncekiDurum());
        assertEquals("IPTAL", dto.getYeniDurum());
    }

    @Test
    void karar_redGerekcesiKisa_hataFirlatirVeModulCagrilmaz() {
        BusinessException hata = assertThrows(BusinessException.class, () ->
                onayService.karar(1L, 7L, "Ali",
                        OnayKararRequest.builder().tur("IZIN").kaynakId(5L).karar("RED")
                                .gerekce("ab").build()));

        assertTrue(hata.getMessage().contains("gerekçe"));
        verifyNoInteractions(personelIzinService, personelMasrafTalepService,
                satinalmaTalepService, siparisService);
        verify(onayKararRepository, never()).save(any());
    }

    @Test
    void karar_modulHatasi_kararKaydiYazilmaz() {
        when(personelIzinRepository.findById(5L)).thenReturn(Optional.of(
                PersonelIzin.builder().id(5L).durum("BEKLEMEDE").build()));
        doThrow(new BusinessException("Bu izin zaten onaylanmış"))
                .when(personelIzinService).durumGuncelle(5L, "ONAYLANDI", "Ali");

        assertThrows(BusinessException.class, () -> onayService.karar(1L, 7L, "Ali",
                OnayKararRequest.builder().tur("IZIN").kaynakId(5L).karar("ONAY").build()));

        verify(onayKararRepository, never()).save(any());
    }

    @Test
    void karar_gecersizTur_hataFirlatir() {
        assertThrows(BusinessException.class, () -> onayService.karar(1L, 7L, "Ali",
                OnayKararRequest.builder().tur("BILINMEYEN").kaynakId(1L).karar("ONAY").build()));
    }

    // ------------------------------------------------------------------
    // Özet
    // ------------------------------------------------------------------

    @Test
    void ozet_turBazindaSayarlariToplar() {
        when(personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", 1L)).thenReturn(2L);
        when(personelMasrafTalepRepository.countBySirketIdAndDurum(1L, "BEKLEMEDE")).thenReturn(3L);
        when(satinalmaTalepRepository.countBySirketIdAndDurum(1L, "TASLAK")).thenReturn(4L);
        when(siparisRepository.countBySirketIdAndDurumIn(eq(1L), anyCollection())).thenReturn(5L);

        OnayOzetDTO ozet = onayService.ozet(1L);

        assertEquals(2L, ozet.getIzin());
        assertEquals(3L, ozet.getMasraf());
        assertEquals(4L, ozet.getSatinalma());
        assertEquals(5L, ozet.getSiparis());
        assertEquals(14L, ozet.getToplam());
    }

    // ------------------------------------------------------------------
    // Bekleyenler
    // ------------------------------------------------------------------

    @Test
    void bekleyen_turVerilinceYalnizOTuruEsler() {
        LocalDate bugun = LocalDate.now();
        when(personelIzinRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(PersonelIzin.builder()
                        .id(2L).personelId(3L).izinTuru("YILLIK")
                        .baslangic(bugun).bitis(bugun.plusDays(2)).gunSayisi(3)
                        .durum("BEKLEMEDE").aciklama("Yıllık izin")
                        .olusturmaTarihi(LocalDateTime.now()).build())));
        when(personelRepository.findAllById(any())).thenReturn(List.of(
                Personel.builder().id(3L).ad("Ayşe").soyad("Kaya").build()));

        Page<OnayBekleyenDTO> sayfa = onayService.bekleyen(1L, "IZIN", null, PageRequest.of(0, 20));

        assertEquals(1, sayfa.getTotalElements());
        OnayBekleyenDTO dto = sayfa.getContent().get(0);
        assertEquals("IZIN", dto.getTur());
        assertEquals(2L, dto.getKaynakId());
        assertEquals("Ayşe Kaya", dto.getBaslik());
        assertEquals("YILLIK", dto.getAltBaslik());
        assertEquals(bugun, dto.getTarih());
        assertEquals(3, dto.getMeta().get("gunSayisi"));
        assertEquals(bugun.plusDays(2), dto.getMeta().get("bitisTarihi"));
        // Diğer tür sorguları çalıştırılmamalı.
        verifyNoInteractions(personelMasrafTalepRepository, satinalmaTalepRepository, siparisRepository);
    }

    @Test
    void bekleyen_tumTurler_birlesikVeTariheGoreSiralanir() {
        LocalDateTime simdi = LocalDateTime.now();
        when(personelIzinRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(PersonelIzin.builder()
                        .id(1L).personelId(3L).izinTuru("YILLIK").baslangic(LocalDate.now())
                        .durum("BEKLEMEDE").olusturmaTarihi(simdi.minusDays(1)).build())));
        when(personelMasrafTalepRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(PersonelMasrafTalep.builder()
                        .id(2L).tur("MASRAF").tutar(new BigDecimal("100"))
                        .tarih(LocalDate.now()).durum("BEKLEMEDE")
                        .olusturmaTarihi(simdi).build())));
        when(satinalmaTalepRepository.onayBekleyenler(eq(1L), eq("TASLAK"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(SatinalmaTalep.builder()
                        .id(3L).talepNo("ST-1").tarih(LocalDate.now()).durum("TASLAK")
                        .olusturmaTarihi(simdi.minusDays(2)).build())));
        when(siparisRepository.onayBekleyenler(eq(1L), anyCollection(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(Siparis.builder()
                        .id(4L).siparisNo("SP-1").tarih(LocalDate.now()).durum("TEKLIF")
                        .genelToplam(new BigDecimal("500"))
                        .olusturmaTarihi(simdi.minusDays(3)).build())));
        when(personelRepository.findAllById(any())).thenReturn(List.of());
        when(kullaniciRepository.findAllById(any())).thenReturn(List.of());
        when(cariHesapRepository.findAllById(any())).thenReturn(List.of());
        when(satinalmaTalepKalemRepository.toplamTutarByTalepIds(any())).thenReturn(List.of());

        Page<OnayBekleyenDTO> sayfa = onayService.bekleyen(1L, null, null, PageRequest.of(0, 20));

        assertEquals(4, sayfa.getTotalElements());
        // En yeni oluşturulan (MASRAF) ilk sırada.
        assertEquals("MASRAF", sayfa.getContent().get(0).getTur());
        assertEquals("SIPARIS", sayfa.getContent().get(3).getTur());
    }

    @Test
    void bekleyen_tumuGorunumundeSayfalamaUygulanmaz() {
        when(personelIzinRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(PersonelIzin.builder()
                        .id(1L).personelId(3L).izinTuru("YILLIK").baslangic(LocalDate.now())
                        .durum("BEKLEMEDE").olusturmaTarihi(LocalDateTime.now()).build())));
        when(personelMasrafTalepRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(PersonelMasrafTalep.builder()
                        .id(2L).tur("MASRAF").tutar(BigDecimal.TEN).tarih(LocalDate.now())
                        .durum("BEKLEMEDE").olusturmaTarihi(LocalDateTime.now().minusHours(1)).build())));
        when(satinalmaTalepRepository.onayBekleyenler(eq(1L), eq("TASLAK"), isNull(), any()))
                .thenReturn(Page.empty());
        when(siparisRepository.onayBekleyenler(eq(1L), anyCollection(), isNull(), any()))
                .thenReturn(Page.empty());
        when(personelRepository.findAllById(any())).thenReturn(List.of());
        when(kullaniciRepository.findAllById(any())).thenReturn(List.of());

        // Sayfa boyutu 1 istense de "Tümü" birleşik liste döner (dokümante sınır).
        Page<OnayBekleyenDTO> sayfa = onayService.bekleyen(1L, null, null, PageRequest.of(0, 1));

        assertEquals(2, sayfa.getContent().size());
        assertEquals(2, sayfa.getTotalElements());
    }

    @Test
    void bekleyen_aramaDeseniniSorguyaIletir() {
        when(personelMasrafTalepRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), eq("%yakıt%"), any()))
                .thenReturn(Page.empty());

        onayService.bekleyen(1L, "MASRAF", "yakıt", PageRequest.of(0, 20));

        verify(personelMasrafTalepRepository).onayBekleyenler(
                eq(1L), eq("BEKLEMEDE"), eq("%yakıt%"), any());
    }

    @Test
    void bekleyen_gecersizTur_hataFirlatir() {
        assertThrows(BusinessException.class,
                () -> onayService.bekleyen(1L, "GECERSIZ", null, PageRequest.of(0, 20)));
    }

    // ------------------------------------------------------------------
    // Toplu
    // ------------------------------------------------------------------

    @Test
    void toplu_kismiHata_digerKayitlariIslerVeAtlananlariDoner() {
        kayitCevabi();
        when(personelIzinRepository.findById(1L)).thenReturn(Optional.of(
                PersonelIzin.builder().id(1L).durum("BEKLEMEDE").build()));
        when(personelIzinRepository.findById(2L)).thenReturn(Optional.of(
                PersonelIzin.builder().id(2L).durum("BEKLEMEDE").build()));
        doThrow(new BusinessException("İzin kaydı bulunamadı"))
                .when(personelIzinService).durumGuncelle(eq(2L), any(), any());

        OnayTopluSonucDTO sonuc = onayService.toplu(1L, 7L, "Ali",
                OnayTopluRequest.builder().tur("IZIN").kaynakIds(List.of(1L, 2L))
                        .karar("ONAY").build());

        assertEquals(1, sonuc.getBasarili());
        assertEquals(1, sonuc.getAtlanan().size());
        assertEquals(2L, sonuc.getAtlanan().get(0).getKaynakId());
        assertEquals("İzin kaydı bulunamadı", sonuc.getAtlanan().get(0).getMesaj());
        verify(onayKararRepository, times(1)).save(any(OnayKarar.class));
    }

    @Test
    void toplu_redGerekcesiYoksaHataFirlatir() {
        assertThrows(BusinessException.class, () -> onayService.toplu(1L, 7L, "Ali",
                OnayTopluRequest.builder().tur("MASRAF").kaynakIds(List.of(1L))
                        .karar("RED").build()));

        verifyNoInteractions(personelMasrafTalepService);
    }

    // ------------------------------------------------------------------
    // Geçmiş
    // ------------------------------------------------------------------

    @Test
    void gecmis_filtreleriVeTarihAraliginiSorguyaGecirir() {
        when(onayKararRepository.filtrele(eq(1L), eq("IZIN"), eq("RED"), isNull(),
                any(), any(), any())).thenReturn(new PageImpl<>(List.of(OnayKarar.builder()
                        .id(1L).tur("IZIN").kaynakId(5L).karar("RED")
                        .gerekce("Belge eksik").oncekiDurum("BEKLEMEDE").yeniDurum("REDDEDILDI")
                        .kararVerenId(7L).kararVerenAd("Ali")
                        .olusturmaTarihi(LocalDateTime.now()).build())));

        Page<OnayKararDTO> sayfa = onayService.gecmis(1L, "IZIN", "RED", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PageRequest.of(0, 10));

        assertEquals(1, sayfa.getTotalElements());
        assertEquals("RED", sayfa.getContent().get(0).getKarar());
        verify(onayKararRepository).filtrele(eq(1L), eq("IZIN"), eq("RED"), isNull(),
                eq(LocalDateTime.of(2026, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 2, 1, 0, 0)), any());
    }

    @Test
    void excel_bekleyenleriListeOlarakDoner() {
        when(personelIzinRepository.onayBekleyenler(eq(1L), eq("BEKLEMEDE"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(PersonelIzin.builder()
                        .id(1L).personelId(3L).izinTuru("YILLIK").baslangic(LocalDate.now())
                        .durum("BEKLEMEDE").olusturmaTarihi(LocalDateTime.now()).build())));
        when(personelRepository.findAllById(any())).thenReturn(List.of(
                Personel.builder().id(3L).ad("Ayşe").soyad("Kaya").build()));

        List<OnayBekleyenDTO> liste = onayService.excel(1L, "IZIN", null);

        assertEquals(1, liste.size());
        assertEquals("IZIN", liste.get(0).getTur());
    }
}
