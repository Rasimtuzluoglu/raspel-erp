package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeKalemDTO;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.muhasebe.IrsaliyeKalem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeKalemRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.service.muhasebe.IrsaliyeService;
import com.raspel.erp.repository.envanter.StokRepository;

@ExtendWith(MockitoExtension.class)
class IrsaliyeServiceTest {

    @Mock private IrsaliyeRepository irsaliyeRepository;
    @Mock private IrsaliyeKalemRepository kalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private com.raspel.erp.repository.envanter.StokHareketRepository stokHareketRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private CacheYardimci cacheYardimci;
    @Mock private com.raspel.erp.service.sube.DepoStokService depoStokService;
    @Mock private com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @Mock private com.raspel.erp.service.sistem.DonemService donemService;
    @Mock private com.raspel.erp.repository.muhasebe.IrsaliyeDurumLogRepository irsaliyeDurumLogRepository;
    @Mock private com.raspel.erp.service.ticaret.TeslimatService teslimatService;
    @Mock private com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    @Mock private com.raspel.erp.service.ticaret.FaturaService faturaService;
    @Mock private com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;
    @Mock private com.raspel.erp.repository.ticaret.SiparisRepository siparisRepository;
    @Mock private com.raspel.erp.repository.ticaret.TeslimatRepository teslimatRepository;
    @Mock private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @InjectMocks private IrsaliyeService irsaliyeService;

    private Irsaliye createIrsaliye(Long id) {
        Irsaliye i = new Irsaliye();
        i.setId(id);
        i.setIrsaliyeNo("IRS-000" + id);
        i.setTarih(LocalDate.now());
        i.setCariHesapId(1L);
        i.setDurum("TASLAK");
        i.setTur("SATIS");
        i.setSirketId(1L);
        i.setOlusturmaTarihi(LocalDateTime.now());
        return i;
    }

    @Test
    void tumunuGetir_returnsAll() {
        when(irsaliyeRepository.findBySirketIdOrderByTarihDesc(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(createIrsaliye(1L))));
        var result = irsaliyeService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getir_returnsIrsaliye() {
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(createIrsaliye(1L)));
        var result = irsaliyeService.getir(1L);
        assertEquals("IRS-0001", result.getIrsaliyeNo());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(irsaliyeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> irsaliyeService.getir(99L));
    }

    @Test
    void olustur_creates() {
        IrsaliyeKalemDTO kalem = IrsaliyeKalemDTO.builder().stokId(1L).aciklama("K").miktar(java.math.BigDecimal.valueOf(5)).birim("Adet").build();
        IrsaliyeDTO dto = IrsaliyeDTO.builder().irsaliyeNo("IRS-999").tarih(LocalDate.now())
                .cariHesapId(1L).aciklama("Test").tur("SATIS").sirketId(1L).kalemler(List.of(kalem)).build();
        Irsaliye saved = createIrsaliye(1L);
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenReturn(saved);
        var result = irsaliyeService.olustur(dto, 1L);
        assertNotNull(result);
    }

    @Test
    void olustur_kilitliDonem_reddedilir() {
        doThrow(new com.raspel.erp.exception.BusinessException("Bu tarih kilitli"))
                .when(donemService).kilitKontrol(eq(1L), any(LocalDate.class), anyString());
        IrsaliyeDTO dto = IrsaliyeDTO.builder().irsaliyeNo("IRS-999").tarih(LocalDate.now())
                .cariHesapId(1L).tur("SATIS").sirketId(1L).build();

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.olustur(dto, 1L));
        verify(irsaliyeRepository, never()).save(any());
    }

    @Test
    void durumGuncelle_kilitliDonem_reddedilir() {
        Irsaliye irsaliye = createIrsaliye(1L);
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(irsaliye));
        doThrow(new com.raspel.erp.exception.BusinessException("Bu tarih kilitli"))
                .when(donemService).kilitKontrol(eq(1L), any(LocalDate.class), anyString());

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.durumGuncelle(1L, "KESILDI"));
    }

    @Test
    void durumGuncelle_updates() {
        Irsaliye irsaliye = createIrsaliye(1L);
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(irsaliye));
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(irsaliye));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));
        var result = irsaliyeService.durumGuncelle(1L, "HAZIRLANDI");
        assertEquals("HAZIRLANDI", result.getDurum());
    }

    @Test
    void durumGuncelle_throwsWhenNotFound() {
        when(irsaliyeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> irsaliyeService.durumGuncelle(99L, "SEVK_EDILDI"));
    }

    @Test
    void sil_deletes() {
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(createIrsaliye(1L)));
        irsaliyeService.sil(1L);
        verify(kalemRepository).deleteByIrsaliyeId(1L);
        verify(irsaliyeRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenKesildi() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("KESILDI");
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));
        assertThrows(RuntimeException.class, () -> irsaliyeService.sil(1L));
    }

    // ------------------------------------------------------------------
    // REDTEAM C5 regresyonu: PUT /api/irsaliyeler/{id} ile durum mass-assignment
    //
    // CANLI KANIT: guncelle() `if (dto.getDurum() != null) i.setDurum(...)`
    // yaziyordu. Zincir:
    //   1) POST /irsaliyeler            TASLAK  -> stok etkisiz (100)
    //   2) PUT  /irsaliyeler/1          durum=KESILDI -> stok HALA 100 (etkisiz)
    //   3) PUT  /irsaliyeler/1/durum    IPTAL  -> stok 100 -> 150  (+50 BEDAVA)
    // Durum alani yalnizca ayri uctan (/durum) degistirilebilir olmali.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("C5: guncelle() DTO'daki durum alanini YAZMAZ, acikca hata verir")
    void guncelle_dtoDurumAlaniniYazmaz() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("TASLAK");
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));

        var dto = new IrsaliyeDTO();
        dto.setDurum("KESILDI");

        com.raspel.erp.exception.BusinessException hata = assertThrows(
                com.raspel.erp.exception.BusinessException.class, () -> irsaliyeService.guncelle(1L, dto));

        assertTrue(hata.getMessage().contains("/durum"),
                "Hata mesaji dogru ucu gostermeli: " + hata.getMessage());
        assertEquals("TASLAK", i.getDurum(),
                "PUT /irsaliyeler/{id} durumu degistirmemeli (canli kanit: bedava stok)");
        verify(irsaliyeRepository, never()).save(any());
    }

    @Test
    @DisplayName("C5: durum alani hic gonderilmediginde guncelle normal calisir")
    void guncelle_durumAlaniYoksaNormalCalisir() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("TASLAK");
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(java.util.List.of());
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new IrsaliyeDTO();
        dto.setAciklama("Guncellendi mi");
        var sonuc = irsaliyeService.guncelle(1L, dto);

        assertEquals("TASLAK", sonuc.getDurum());
        assertEquals("Guncellendi mi", sonuc.getAciklama());
    }

    @Test
    void olustur_taslakDurumundaIlkLoguYazar() {
        IrsaliyeDTO dto = IrsaliyeDTO.builder().irsaliyeNo("IRS-100").tarih(LocalDate.now())
                .cariHesapId(1L).tur("SATIS").kalemler(List.of()).build();
        Irsaliye saved = createIrsaliye(1L);
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenReturn(saved);

        var sonuc = irsaliyeService.olustur(dto, 1L);

        assertEquals("TASLAK", sonuc.getDurum());
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "OLUSTUR".equals(l.getIslem())
                && "TASLAK".equals(l.getYeniDurum())));
    }

    @Test
    void hazirla_taslakIrsaliyeyiHazirlarVeLogYazar() {
        Irsaliye i = createIrsaliye(1L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = irsaliyeService.hazirla(1L, 7L);

        assertEquals("HAZIRLANDI", sonuc.getDurum());
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "HAZIRLA".equals(l.getIslem())
                && "HAZIRLANDI".equals(l.getYeniDurum()) && Long.valueOf(7L).equals(l.getKullaniciId())));
    }

    @Test
    void hazirla_taslakDegilseReddeder() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.hazirla(1L, 7L));
        verify(irsaliyeRepository, never()).save(any());
    }

    @Test
    void gecmis_loglariTarihSirasiylaDoner() {
        Irsaliye i = createIrsaliye(1L);
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(irsaliyeDurumLogRepository.findByIrsaliyeIdOrderByOlusturmaTarihiAscIdAsc(1L))
                .thenReturn(List.of(com.raspel.erp.entity.muhasebe.IrsaliyeDurumLog.builder()
                        .id(1L).irsaliyeId(1L).eskiDurum(null).yeniDurum("TASLAK")
                        .islem("OLUSTUR").olusturmaTarihi(LocalDateTime.now()).build()));

        var liste = irsaliyeService.gecmis(1L);

        assertEquals(1, liste.size());
        assertEquals("OLUSTUR", liste.get(0).getIslem());
    }

    @Test
    void sevk_hazirIrsaliyedeStokDuserVeTeslimatOlusur() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("HAZIRLANDI");
        IrsaliyeKalem kalem = IrsaliyeKalem.builder().id(1L).irsaliyeId(1L).stokId(10L)
                .miktar(new java.math.BigDecimal("2")).build();
        com.raspel.erp.entity.envanter.Stok stok = com.raspel.erp.entity.envanter.Stok.builder()
                .id(10L).ad("Masa").sirketId(1L).miktar(new java.math.BigDecimal("10")).build();
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(stok));
        when(depoStokService.coz(any(), any())).thenReturn(1L);
        when(stokSeriService.fefoTuket(eq(10L), eq(1L), any())).thenReturn(List.of());
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(
                com.raspel.erp.entity.finans.CariHesap.builder().id(1L).ad("Müşteri").adres("Adres").build()));
        when(teslimatService.irsaliyedenOlustur(any()))
                .thenReturn(com.raspel.erp.dto.ticaret.TeslimatDTO.builder().id(55L).durum("YOLDA").build());
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var istek = com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek.builder()
                .soforId(7L).plaka("34ABC123").build();
        var sonuc = irsaliyeService.sevk(1L, istek, 9L);

        assertEquals("SEVK_EDILDI", sonuc.getDurum());
        assertEquals(55L, sonuc.getTeslimatId());
        assertEquals(0, stok.getMiktar().compareTo(new java.math.BigDecimal("8")));
        verify(teslimatService).irsaliyedenOlustur(any());
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "SEVK".equals(l.getIslem())));
    }

    @Test
    void sevk_soforYoksaHataVerirVeStokDegismez() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("HAZIRLANDI");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.sevk(1L,
                        com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek.builder().build(), 9L));
        verify(stokRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void sevk_ikinciKezCagrilirsaReddeder() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.sevk(1L,
                        com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek.builder().soforId(7L).build(), 9L));
        verify(stokRepository, never()).findByIdForUpdate(any());
    }

    // ------------------------------------------------------------------
    // Teslim / sevki geri alma / iptal
    // ------------------------------------------------------------------

    private IrsaliyeKalem kalem(Long stokId, String miktar) {
        return IrsaliyeKalem.builder().id(1L).irsaliyeId(1L).stokId(stokId)
                .miktar(new java.math.BigDecimal(miktar)).build();
    }

    private com.raspel.erp.entity.envanter.Stok stok(Long id, String miktar) {
        return com.raspel.erp.entity.envanter.Stok.builder()
                .id(id).ad("Masa").sirketId(1L)
                .miktar(new java.math.BigDecimal(miktar)).build();
    }

    @Test
    void teslim_sevkEdilmisiTeslimEderVeTeslimatKapatir() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        i.setTeslimatId(55L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime tarih = LocalDateTime.of(2026, 10, 20, 15, 30);
        var sonuc = irsaliyeService.teslim(1L,
                new com.raspel.erp.dto.muhasebe.IrsaliyeTeslimIstek("Ahmet Yılmaz", tarih), 7L);

        assertEquals("TESLIM_EDILDI", sonuc.getDurum());
        assertEquals("Ahmet Yılmaz", i.getTeslimAlan());
        assertEquals(tarih, i.getTeslimTarihi());
        verify(teslimatService).irsaliyeTeslimEdildi(55L, "Ahmet Yılmaz", tarih);
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "TESLIM".equals(l.getIslem())
                && "TESLIM_EDILDI".equals(l.getYeniDurum())));
    }

    @Test
    void teslim_sevkEdilmemisIrsaliyeyiTeslimEdemez() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("HAZIRLANDI");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.teslim(1L, new com.raspel.erp.dto.muhasebe.IrsaliyeTeslimIstek(), 7L));
        verify(irsaliyeRepository, never()).save(any());
    }

    @Test
    void sevkiGeriAl_stokIadesiYaparVeTeslimatiIptalEder() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        i.setTeslimatId(55L);
        com.raspel.erp.entity.envanter.Stok stok = stok(10L, "8");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of(kalem(10L, "2")));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(stok));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = irsaliyeService.sevkiGeriAl(1L, 7L);

        assertEquals("HAZIRLANDI", sonuc.getDurum());
        assertEquals(0, stok.getMiktar().compareTo(new java.math.BigDecimal("10")));
        assertNull(i.getSevkTarihi());
        verify(teslimatService).irsaliyeIptal(55L);
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "SEVKI_GERI_AL".equals(l.getIslem())));
    }

    @Test
    void sevkiGeriAl_canliFaturaVarsaReddederVeStokDegismez() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        i.setFaturaId(77L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(
                com.raspel.erp.entity.ticaret.Fatura.builder().id(77L)
                        .faturaNumarasi("FTR-77")
                        .durum(com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI).build()));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.sevkiGeriAl(1L, 7L));

        verify(stokRepository, never()).findByIdForUpdate(any());
        verify(irsaliyeRepository, never()).save(any());
    }

    @Test
    void iptal_sevkEdilmisIrsaliyedeStokIadesiYapar() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        i.setTeslimatId(55L);
        com.raspel.erp.entity.envanter.Stok stok = stok(10L, "8");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of(kalem(10L, "2")));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(stok));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = irsaliyeService.iptal(1L, "Müşteri vazgeçti", 7L);

        assertEquals("IPTAL", sonuc.getDurum());
        assertEquals(0, stok.getMiktar().compareTo(new java.math.BigDecimal("10")));
        verify(teslimatService).irsaliyeIptal(55L);
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "IPTAL".equals(l.getIslem())
                && "Müşteri vazgeçti".equals(l.getAciklama())));
    }

    @Test
    void iptal_faturasiIptalEdilmisseIadeyeIzinVerir() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("TESLIM_EDILDI");
        i.setFaturaId(77L);
        i.setTeslimatId(55L);
        com.raspel.erp.entity.envanter.Stok stok = stok(10L, "8");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(
                com.raspel.erp.entity.ticaret.Fatura.builder().id(77L)
                        .durum(com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.IPTAL).build()));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of(kalem(10L, "2")));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(stok));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = irsaliyeService.iptal(1L, null, 7L);

        assertEquals("IPTAL", sonuc.getDurum());
        assertEquals(0, stok.getMiktar().compareTo(new java.math.BigDecimal("10")));
    }

    @Test
    void iptal_taslakIrsaliyedeStokEtkisiYok() {
        Irsaliye i = createIrsaliye(1L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = irsaliyeService.iptal(1L, null, 7L);

        assertEquals("IPTAL", sonuc.getDurum());
        verify(stokRepository, never()).findByIdForUpdate(any());
        verify(teslimatService, never()).irsaliyeIptal(any());
    }

    @Test
    void iptal_canliFaturaVarsaReddeder() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        i.setFaturaId(77L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(
                com.raspel.erp.entity.ticaret.Fatura.builder().id(77L)
                        .durum(com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI).build()));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.iptal(1L, null, 7L));
        verify(stokRepository, never()).findByIdForUpdate(any());
    }

    // ------------------------------------------------------------------
    // Erken fatura + toplu fatura
    // ------------------------------------------------------------------

    @Test
    void faturayaDonustur_taslaktaOtomatikSevkEderVeStokDuser() {
        Irsaliye i = createIrsaliye(1L);
        i.setSoforId(7L);
        i.setTeslimatAdresi("Teslim Adresi");
        com.raspel.erp.entity.envanter.Stok stok = stok(10L, "10");
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of(kalem(10L, "2")));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(stok));
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok));
        when(teslimatService.irsaliyedenOlustur(any()))
                .thenReturn(com.raspel.erp.dto.ticaret.TeslimatDTO.builder().id(55L).durum("YOLDA").build());
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));
        when(faturaService.faturaOlustur(any(), any(), any(), any()))
                .thenReturn(com.raspel.erp.dto.ticaret.FaturaDTO.builder()
                        .id(88L).faturaNumarasi("FTR-88").build());

        var sonuc = irsaliyeService.faturayaDonustur(1L, 1L, 7L);

        assertEquals("SEVK_EDILDI", sonuc.getDurum());
        assertEquals(88L, sonuc.getFaturaId());
        assertEquals(0, stok.getMiktar().compareTo(new java.math.BigDecimal("8")));
        verify(faturaService).faturaOlustur(any(), eq(1L), eq(7L), any());
        verify(teslimatService).faturaBaglaByIrsaliye(1L, 88L, "FTR-88");
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "FATURA".equals(l.getIslem())));
    }

    @Test
    void faturayaDonustur_soforYoksaHataVerirVeStokDegismez() {
        Irsaliye i = createIrsaliye(1L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var hata = assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.faturayaDonustur(1L, 1L, 7L));

        assertTrue(hata.getMessage().contains("şoför"), "Anlaşılır şoför hatası beklenir: " + hata.getMessage());
        verify(stokRepository, never()).findByIdForUpdate(any());
        verify(faturaService, never()).faturaOlustur(any(), any(), any(), any());
    }

    @Test
    void faturayaDonustur_canliFaturaVarsaReddeder() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        i.setFaturaId(77L);
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(
                com.raspel.erp.entity.ticaret.Fatura.builder().id(77L)
                        .faturaNumarasi("FTR-77")
                        .durum(com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI).build()));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.faturayaDonustur(1L, 1L, 7L));
        verify(faturaService, never()).faturaOlustur(any(), any(), any(), any());
    }

    @Test
    void topluFatura_farkliCariSecilirseHicFaturaOlusturmaz() {
        Irsaliye i1 = createIrsaliye(1L);
        i1.setDurum("SEVK_EDILDI");
        i1.setCariHesapId(5L);
        Irsaliye i2 = createIrsaliye(2L);
        i2.setDurum("SEVK_EDILDI");
        i2.setCariHesapId(6L);
        when(irsaliyeRepository.findAllById(any())).thenReturn(List.of(i1, i2));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.topluFatura(List.of(1L, 2L), 1L, 7L));

        verify(faturaService, never()).faturaOlustur(any(), any(), any(), any());
    }

    @Test
    void topluFatura_ayniCarideHerIrsaliyeIcinAyriFaturaUretir() {
        Irsaliye i1 = createIrsaliye(1L);
        i1.setDurum("SEVK_EDILDI");
        i1.setCariHesapId(5L);
        Irsaliye i2 = createIrsaliye(2L);
        i2.setDurum("TESLIM_EDILDI");
        i2.setCariHesapId(5L);
        when(irsaliyeRepository.findAllById(any())).thenReturn(List.of(i1, i2));
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i1));
        when(irsaliyeRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(i2));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));
        when(faturaService.faturaOlustur(any(), any(), any(), any()))
                .thenReturn(com.raspel.erp.dto.ticaret.FaturaDTO.builder().id(101L).faturaNumarasi("F-101").build())
                .thenReturn(com.raspel.erp.dto.ticaret.FaturaDTO.builder().id(102L).faturaNumarasi("F-102").build());

        var sonuc = irsaliyeService.topluFatura(List.of(1L, 2L), 1L, 7L);

        assertEquals(2, sonuc.getBasarili().size());
        assertTrue(sonuc.getAtlanan().isEmpty());
        assertEquals(101L, sonuc.getBasarili().get(0).getFaturaId());
        assertEquals(102L, sonuc.getBasarili().get(1).getFaturaId());
        verify(faturaService, times(2)).faturaOlustur(any(), any(), any(), any());
    }

    @Test
    void topluFatura_iptalVeFaturalilariAtlar() {
        Irsaliye iptal = createIrsaliye(1L);
        iptal.setDurum("IPTAL");
        iptal.setCariHesapId(5L);
        Irsaliye faturali = createIrsaliye(2L);
        faturali.setDurum("SEVK_EDILDI");
        faturali.setCariHesapId(5L);
        faturali.setFaturaId(77L);
        when(irsaliyeRepository.findAllById(any())).thenReturn(List.of(iptal, faturali));
        when(faturaRepository.findById(77L)).thenReturn(Optional.of(
                com.raspel.erp.entity.ticaret.Fatura.builder().id(77L)
                        .durum(com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI).build()));

        var sonuc = irsaliyeService.topluFatura(List.of(1L, 2L), 1L, 7L);

        assertTrue(sonuc.getBasarili().isEmpty());
        assertEquals(2, sonuc.getAtlanan().size());
        verify(faturaService, never()).faturaOlustur(any(), any(), any(), any());
    }

    @Test
    void topluFatura_bosListeReddedilir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.topluFatura(List.of(), 1L, 7L));
    }

    // ------------------------------------------------------------------
    // Filtre / özet
    // ------------------------------------------------------------------

    @Test
    void filtreli_parametreleriIletirVeSayfaliDoner() {
        Irsaliye i = createIrsaliye(1L);
        LocalDate bas = LocalDate.of(2026, 10, 1);
        LocalDate bit = LocalDate.of(2026, 10, 31);
        when(irsaliyeRepository.filtreliGetir(eq(1L), eq("%abc%"), eq("SEVK_EDILDI"), eq("SATIS"),
                eq(5L), eq(bas), eq(bit), eq(Boolean.FALSE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(i)));

        var sonuc = irsaliyeService.filtreli(1L, "abc", "sevk_edildi", "satis", 5L, bas, bit,
                Boolean.FALSE, Pageable.unpaged());

        assertEquals(1, sonuc.getTotalElements());
        assertEquals("IRS-0001", sonuc.getContent().get(0).getIrsaliyeNo());
    }

    @Test
    void ozet_durumSayilariniVeFaturasizlariDoner() {
        when(irsaliyeRepository.countDurum(1L, "TASLAK", null, null)).thenReturn(2L);
        when(irsaliyeRepository.countDurum(1L, "HAZIRLANDI", null, null)).thenReturn(3L);
        when(irsaliyeRepository.countDurum(1L, "SEVK_EDILDI", null, null)).thenReturn(4L);
        when(irsaliyeRepository.countDurum(1L, "IPTAL", null, null)).thenReturn(1L);
        when(irsaliyeRepository.countTeslimEdilen(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(irsaliyeRepository.countFaturasiz(1L, null, null)).thenReturn(6L);

        var sonuc = irsaliyeService.ozet(1L, null, null);

        assertEquals(2L, sonuc.getTaslak());
        assertEquals(3L, sonuc.getHazirlanan());
        assertEquals(4L, sonuc.getSevkEdilen());
        assertEquals(1L, sonuc.getIptal());
        assertEquals(5L, sonuc.getTeslimEdilenBuAy());
        assertEquals(6L, sonuc.getFaturasiz());
    }

    // ------------------------------------------------------------------
    // Legacy KESILDI alias
    // ------------------------------------------------------------------

    @Test
    void durumGuncelle_kesildiAliasTaslaktaHazirlaSevkYapar() {
        Irsaliye i = createIrsaliye(1L);
        i.setSoforId(7L);
        i.setTeslimatAdresi("Teslim Adresi");
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kalemRepository.findByIrsaliyeId(1L)).thenReturn(List.of());
        when(teslimatService.irsaliyedenOlustur(any()))
                .thenReturn(com.raspel.erp.dto.ticaret.TeslimatDTO.builder().id(55L).durum("YOLDA").build());

        var sonuc = irsaliyeService.durumGuncelle(1L, "KESILDI");

        assertEquals("SEVK_EDILDI", sonuc.getDurum());
        assertEquals(55L, sonuc.getTeslimatId());
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "HAZIRLA".equals(l.getIslem())));
        verify(irsaliyeDurumLogRepository).save(argThat(l -> "SEVK".equals(l.getIslem())));
    }

    @Test
    void durumGuncelle_kesildiAliasSoforYoksaAnlasilirHataVerir() {
        Irsaliye i = createIrsaliye(1L);
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(i));
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenAnswer(inv -> inv.getArgument(0));

        var hata = assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> irsaliyeService.durumGuncelle(1L, "KESILDI"));

        assertTrue(hata.getMessage().contains("şoför"), "Anlaşılır hata beklenir: " + hata.getMessage());
        verify(stokRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void durumGuncelle_kesildiSevkEdilmisseIdempotent() {
        Irsaliye i = createIrsaliye(1L);
        i.setDurum("SEVK_EDILDI");
        when(irsaliyeRepository.findById(1L)).thenReturn(Optional.of(i));

        var sonuc = irsaliyeService.durumGuncelle(1L, "KESILDI");

        assertEquals("SEVK_EDILDI", sonuc.getDurum());
        verify(irsaliyeRepository, never()).save(any());
        verify(stokRepository, never()).findByIdForUpdate(any());
    }
}