package com.raspel.erp.service.sube;

import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepoExportServiceTest {

    @Mock private DepoService depoService;
    @Mock private DepoTransferService depoTransferService;
    @InjectMocks private DepoExportService exportService;

    @Test
    void liste_bomVeBaslikIcerir() {
        when(depoService.tumunuGetir(eq(1L), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(DepoDTO.builder()
                        .ad("Ana Depo").subeAdi("Merkez").aktif(true)
                        .kalemSayisi(4L).kritikStokSayisi(1L).sifirStokSayisi(0L)
                        .stokDegeri(new BigDecimal("1200.50")).dolulukYuzde(40).build())));

        byte[] veri = exportService.csv("liste", 1L, null, null, null, null, null, null, null);
        String metin = new String(veri, StandardCharsets.UTF_8);

        assertTrue(veri.length >= 3);
        assertEquals((byte) 0xEF, veri[0]);
        assertEquals((byte) 0xBB, veri[1]);
        assertEquals((byte) 0xBF, veri[2]);
        assertTrue(metin.contains("\"Depo\";\"Şube\""));
        assertTrue(metin.contains("\"Ana Depo\";\"Merkez\""));
        assertTrue(metin.contains("\"1200,5\""));
    }

    @Test
    void formulEnjeksiyonuKacirilir() {
        when(depoService.tumunuGetir(eq(1L), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(DepoDTO.builder()
                        .ad("=HYPERLINK(\"http://evil\")").aktif(true).build())));

        String metin = new String(exportService.csv("liste", 1L, null, null, null, null, null, null, null),
                StandardCharsets.UTF_8);

        assertTrue(metin.contains("\"'=HYPERLINK(\"\"http://evil\"\")\""));
    }

    @Test
    void stok_depoSecmedenHataVerir() {
        assertThrows(BusinessException.class,
                () -> exportService.csv("stok", 1L, null, null, null, null, null, null, null));
    }

    @Test
    void gecersizTurHataVerir() {
        assertThrows(BusinessException.class,
                () -> exportService.csv("baska", 1L, null, null, null, null, null, null, null));
    }
}
