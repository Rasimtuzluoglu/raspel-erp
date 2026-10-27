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
        when(irsaliyeRepository.save(any(Irsaliye.class))).thenReturn(irsaliye);
        var result = irsaliyeService.durumGuncelle(1L, "SEVK_EDILDI");
        assertEquals("SEVK_EDILDI", result.getDurum());
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
}