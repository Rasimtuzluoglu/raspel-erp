package com.raspel.erp.service.envanter;

import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarkodUretServiceTest {

    @Mock private StokRepository stokRepository;
    @InjectMocks private BarkodUretService barkodUretService;

    @Test
    void ean13_kontrolHanesiDogruHesaplanir() {
        // 869000000001 -> 8 + 6*3 + 9 + 1*3 = 38 -> kontrol hanesi 2
        assertEquals(2, BarkodUretService.kontrolHanesi("869000000001"));
        assertEquals("8690000000012", BarkodUretService.ean13(1));
        assertEquals(13, BarkodUretService.ean13(1).length());
    }

    @Test
    void ean13_sifirSiraIcinGecerliKod() {
        String kod = BarkodUretService.ean13(0);
        assertEquals("8690000000005", kod);
    }

    @Test
    void ean13Uret_mevcutEnBuyuktenSonrakiSirayiVerir() {
        when(stokRepository.maxEan13Barkod(4L)).thenReturn("8690000000012");
        when(stokRepository.existsBySirketIdAndBarkod(eq(4L), anyString())).thenReturn(false);

        assertEquals("8690000000029", barkodUretService.ean13Uret(4L));
    }

    @Test
    void ean13Uret_cakismadaSonrakiSirayaGecer() {
        when(stokRepository.maxEan13Barkod(4L)).thenReturn("8690000000012");
        when(stokRepository.existsBySirketIdAndBarkod(4L, "8690000000029")).thenReturn(true);
        when(stokRepository.existsBySirketIdAndBarkod(4L, "8690000000036")).thenReturn(false);

        assertEquals("8690000000036", barkodUretService.ean13Uret(4L));
    }

    @Test
    void ean13Uret_topluIslemdeUretilenleriAtlar() {
        when(stokRepository.maxEan13Barkod(4L)).thenReturn("8690000000012");
        when(stokRepository.existsBySirketIdAndBarkod(eq(4L), anyString())).thenReturn(false);

        String kod = barkodUretService.ean13Uret(4L, Set.of("8690000000029"));
        assertEquals("8690000000036", kod);
    }

    @Test
    void ean13Uret_tumAdaylarDoluysaHataFirlatir() {
        when(stokRepository.maxEan13Barkod(4L)).thenReturn(null);
        when(stokRepository.existsBySirketIdAndBarkod(eq(4L), anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> barkodUretService.ean13Uret(4L));
    }
}
