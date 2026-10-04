package com.raspel.erp.service;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.service.ik.MaasBordroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaasBordroServiceTest {

    @Mock private MaasBordroRepository maasBordroRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;
    @Mock private com.raspel.erp.config.TenantChecker tenantChecker;
    @Mock private com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    @Mock private com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    @Mock private com.raspel.erp.service.sistem.DonemService donemService;
    @InjectMocks private MaasBordroService maasBordroService;

    private Personel createPersonel() {
        Personel p = new Personel();
        p.setId(1L);
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        return p;
    }

    private MaasBordro createBordro(Long id) {
        return MaasBordro.builder()
                .id(id)
                .personel(createPersonel())
                .yil(2026)
                .ay(7)
                .brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00"))
                .netMaas(new BigDecimal("21000.00"))
                .odemeTarihi(LocalDate.of(2026, 7, 30))
                .sirketId(1L)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void getir_returnsById() {
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(createBordro(1L)));
        var result = maasBordroService.getir(1L);
        assertEquals(1L, result.getId());
        assertEquals("Ahmet Yilmaz", result.getPersonelAdi());
        assertEquals(0, new BigDecimal("30000.00").compareTo(result.getBrutMaas()));
    }

    @Test
    void getir_notFound_throws() {
        when(maasBordroRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> maasBordroService.getir(99L));
    }

    @Test
    void olustur_creates() {
        MaasBordroDTO dto = MaasBordroDTO.builder()
                .personelId(1L).yil(2026).ay(7)
                .brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00"))
                .netMaas(new BigDecimal("21000.00"))
                .odemeTarihi(LocalDate.of(2026, 7, 30))
                .build();
        when(personelRepository.findById(1L)).thenReturn(Optional.of(createPersonel()));
        when(maasBordroRepository.save(any(MaasBordro.class))).thenReturn(createBordro(1L));
        var result = maasBordroService.olustur(dto, 1L);
        assertEquals(1L, result.getPersonelId());
        assertEquals(2026, result.getYil());
    }

    @Test
    void sil_deletes() {
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(createBordro(1L)));
        maasBordroService.sil(1L);
        verify(maasBordroRepository).delete(any(MaasBordro.class));
    }

    @Test
    void onayla_kilitlerveOnayBilgisiYazar() {
        MaasBordro bordro = createBordro(1L);
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(bordro));
        when(maasBordroRepository.save(any(MaasBordro.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = maasBordroService.onayla(1L, "Yonetici", null);

        assertEquals("ONAYLANDI", result.getDurum());
        assertEquals("Yonetici", result.getOnaylayan());
        assertNotNull(result.getOnayTarihi());
    }

    @Test
    void onayla_zatenOnayliysaHata() {
        MaasBordro bordro = createBordro(1L);
        bordro.setDurum("ONAYLANDI");
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(bordro));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> maasBordroService.onayla(1L, "Yonetici", null));
    }

    @Test
    void guncelle_onayliBordroDuzenlenemez() {
        MaasBordro bordro = createBordro(1L);
        bordro.setDurum("ONAYLANDI");
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(bordro));

        MaasBordroDTO dto = MaasBordroDTO.builder().brutMaas(new BigDecimal("100")).build();
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> maasBordroService.guncelle(1L, dto));
    }

    private Kasa kasa(String bakiye) {
        Kasa k = new Kasa();
        k.setId(5L);
        k.setSirketId(1L);
        k.setBakiye(new BigDecimal(bakiye));
        return k;
    }

    /**
     * C3 (çift ödeme yarışı): ödeme kontrolü KASA kilidi alınmadan ÖNCE
     * çalışıyordu. İki eşzamanlı istek ikisi de "ödenmemiş" görüp ikisi de
     * kasadan düşüyordu. Artık bordro satırı önce kilitleniyor.
     */
    @Test
    void ode_bordroyuKilitliOkurVeOder() {
        MaasBordro onayli = createBordro(1L);
        onayli.setDurum("ONAYLANDI");
        Kasa k = kasa("100000");
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(onayli));
        when(maasBordroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onayli));
        when(kasaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(k));
        when(maasBordroRepository.save(any(MaasBordro.class))).thenAnswer(inv -> inv.getArgument(0));

        maasBordroService.ode(1L, 5L);

        verify(maasBordroRepository).findByIdForUpdate(1L);
        verify(kasaRepository).findByIdForUpdate(5L);
        assertEquals(0, new BigDecimal("79000").compareTo(k.getBakiye()));
        assertEquals("ODENDI", onayli.getOdemeDurumu());
        assertEquals(5L, onayli.getOdemeKasaId());
        verify(kasaHareketRepository).save(any());
    }

    /** C3: zaten ödenmiş bordro ikinci kez ödenemez (kasa hareketi yazılmaz). */
    @Test
    void ode_zatenOdenmisBordroReddedilir() {
        MaasBordro onayli = createBordro(1L);
        onayli.setDurum("ONAYLANDI");
        onayli.setOdemeDurumu("ODENDI");
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(onayli));
        when(maasBordroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onayli));

        var hata = assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> maasBordroService.ode(1L, 5L));

        assertTrue(hata.getMessage().toLowerCase().contains("zaten"));
        verify(kasaRepository, never()).findByIdForUpdate(any());
        verify(kasaHareketRepository, never()).save(any());
    }

    /**
     * C3 yarış senaryosunun deterministik simülasyonu: kilitli okuma "ödenmiş"
     * satırı döndürürse ikinci istek kasa hiç kilitlemeden reddedilir.
     */
    @Test
    void ode_ikinciEşzamanliIstekKasaDokunmadanReddedilir() {
        MaasBordro çağıranınOkudugu = createBordro(1L);
        çağıranınOkudugu.setDurum("ONAYLANDI");
        MaasBordro bayatKopya = createBordro(1L);
        bayatKopya.setDurum("ONAYLANDI");
        bayatKopya.setOdemeDurumu("ODENDI"); // ilk istek tamamlanmış

        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(çağıranınOkudugu));
        when(maasBordroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bayatKopya));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> maasBordroService.ode(1L, 5L));
        verify(kasaRepository, never()).findByIdForUpdate(any());
    }

    /** C3: kasa seçilmeden ödeme yapılamaz. */
    @Test
    void ode_kasaSecilmedenHataVerir() {
        MaasBordro onayli = createBordro(1L);
        onayli.setDurum("ONAYLANDI");
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(onayli));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> maasBordroService.ode(1L, null));
    }

    /** C3: onaylanmamış bordro ödenemez. */
    @Test
    void ode_onaylanmamisBordroReddedilir() {
        when(maasBordroRepository.findById(1L)).thenReturn(Optional.of(createBordro(1L)));

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> maasBordroService.ode(1L, 5L));
        verify(maasBordroRepository, never()).findByIdForUpdate(any());
    }
}
