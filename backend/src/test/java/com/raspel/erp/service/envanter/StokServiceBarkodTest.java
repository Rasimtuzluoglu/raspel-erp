package com.raspel.erp.service.envanter;

import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.repository.envanter.StokFiyatRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokSeriRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sube.DepoStokService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StokServiceBarkodTest {

    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private BildirimService bildirimService;
    @Mock private TenantChecker tenantChecker;
    @Mock private CacheYardimci cacheYardimci;
    @Mock private StokFiyatRepository stokFiyatRepository;
    @Mock private DepoStokService depoStokService;
    @Mock private DepoRepository depoRepository;
    @Mock private StokSeriRepository stokSeriRepository;
    @Mock private MaliyetService maliyetService;
    @Mock private BarkodUretService barkodUretService;

    @InjectMocks private StokService stokService;

    private Stok stok(Long id, String barkod, String stokKodu) {
        return Stok.builder().id(id).ad("Ürün").sirketId(4L).barkod(barkod).stokKodu(stokKodu).build();
    }

    @Test
    void olustur_barkodBosIseOtomatikUretir() {
        when(barkodUretService.ean13Uret(4L)).thenReturn("8690000000005");
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));

        StokDTO sonuc = stokService.olustur(StokDTO.builder().ad("Yeni Ürün").birim("Adet").build(), 4L);

        assertEquals("8690000000005", sonuc.getBarkod());
    }

    @Test
    void olustur_barkodVerildiyseDokunmaz() {
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));

        StokDTO sonuc = stokService.olustur(
                StokDTO.builder().ad("Yeni Ürün").birim("Adet").barkod("8691234567890").build(), 4L);

        assertEquals("8691234567890", sonuc.getBarkod());
        verify(barkodUretService, never()).ean13Uret(anyLong());
    }

    @Test
    void barkodUret_bosBarkodlariDoldurur() {
        Stok bos = stok(5L, null, "STK-5");
        when(stokRepository.findById(5L)).thenReturn(Optional.of(bos));
        when(barkodUretService.ean13Uret(eq(4L), anySet())).thenReturn("8690000000012");
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));

        int uretildi = stokService.barkodUret(List.of(5L), 4L);

        assertEquals(1, uretildi);
        assertEquals("8690000000012", bos.getBarkod());
    }

    @Test
    void barkodUret_doluBarkodlaraDokunmaz() {
        Stok dolu = stok(6L, "8690000000005", "STK-6");
        when(stokRepository.findById(6L)).thenReturn(Optional.of(dolu));

        int uretildi = stokService.barkodUret(List.of(6L), 4L);

        assertEquals(0, uretildi);
        verify(stokRepository, never()).save(any(Stok.class));
    }

    @Test
    void barkodIleBul_bulunamazsaStokKoduIleDener() {
        when(stokRepository.findBySirketIdAndBarkod(4L, "MDF-18")).thenReturn(List.of());
        when(stokRepository.findBySirketIdAndStokKodu(4L, "MDF-18"))
                .thenReturn(Optional.of(stok(3L, null, "MDF-18")));

        StokDTO dto = stokService.barkodIleBul("MDF-18", 4L);

        assertNotNull(dto);
        assertEquals(3L, dto.getId());
    }

    @Test
    void barkodIleBul_barkodOncelikli() {
        when(stokRepository.findBySirketIdAndBarkod(4L, "8690002"))
                .thenReturn(List.of(stok(2L, "8690002", "PVC-B01")));

        StokDTO dto = stokService.barkodIleBul("8690002", 4L);

        assertNotNull(dto);
        assertEquals(2L, dto.getId());
    }
}
