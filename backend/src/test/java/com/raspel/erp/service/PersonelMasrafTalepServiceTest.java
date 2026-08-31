package com.raspel.erp.service;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.dto.ik.PersonelMasrafTalepDTO;
import com.raspel.erp.entity.ik.PersonelMasrafTalep;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.service.finans.MasrafService;
import com.raspel.erp.service.ik.PersonelMasrafTalepService;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.OnayAyariService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonelMasrafTalepServiceTest {

    @Mock private PersonelMasrafTalepRepository talepRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private MasrafService masrafService;
    @Mock private BildirimService bildirimService;
    @Mock private TenantChecker tenantChecker;
    @Mock private OnayAyariService onayAyariService;
    @InjectMocks private PersonelMasrafTalepService talepService;

    @Test
    void shouldCreateTalep() {
        when(talepRepository.save(any(PersonelMasrafTalep.class))).thenAnswer(inv -> {
            PersonelMasrafTalep t = inv.getArgument(0);
            t.setId(10L);
            t.setOlusturmaTarihi(LocalDateTime.now());
            return t;
        });

        PersonelMasrafTalepDTO dto = PersonelMasrafTalepDTO.builder()
                .tutar(BigDecimal.valueOf(450))
                .kategori("YAKIT")
                .aciklama("Saha benzin fişi")
                .build();

        PersonelMasrafTalepDTO result = talepService.talepOlustur(dto, 1L, 2L);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(450), result.getTutar());
        assertEquals("BEKLEMEDE", result.getDurum());
        verify(talepRepository).save(any(PersonelMasrafTalep.class));
    }

    @Test
    void shouldApproveMasrafAndCreateFinansExpense() {
        PersonelMasrafTalep talep = PersonelMasrafTalep.builder()
                .id(1L)
                .sirketId(1L)
                .tur("MASRAF")
                .kategori("YEMEK")
                .tutar(BigDecimal.valueOf(300))
                .tarih(LocalDate.now())
                .aciklama("Müşteri yemeği")
                .durum("BEKLEMEDE")
                .olusturmaTarihi(LocalDateTime.now())
                .build();

        when(talepRepository.findById(1L)).thenReturn(Optional.of(talep));
        when(talepRepository.save(any(PersonelMasrafTalep.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonelMasrafTalepDTO result = talepService.onayla(1L, "admin", "Onaylandı");

        assertNotNull(result);
        assertEquals("ONAYLANDI", result.getDurum());
        assertEquals("admin", result.getOnaylayan());
        verify(masrafService).olustur(any(MasrafDTO.class), eq(1L));
    }

    @Test
    void shouldRejectTalep() {
        PersonelMasrafTalep talep = PersonelMasrafTalep.builder()
                .id(1L)
                .sirketId(1L)
                .durum("BEKLEMEDE")
                .olusturmaTarihi(LocalDateTime.now())
                .build();

        when(talepRepository.findById(1L)).thenReturn(Optional.of(talep));
        when(talepRepository.save(any(PersonelMasrafTalep.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonelMasrafTalepDTO result = talepService.reddet(1L, "admin", "Fiş görseli okunamıyor");

        assertNotNull(result);
        assertEquals("REDDEDILDI", result.getDurum());
    }

    @Test
    void talepOlustur_otomatikOnayGecerliIseOtomatikOnaylar() {
        when(talepRepository.save(any(PersonelMasrafTalep.class))).thenAnswer(inv -> {
            PersonelMasrafTalep t = inv.getArgument(0);
            t.setId(10L);
            t.setOlusturmaTarihi(LocalDateTime.now());
            return t;
        });
        when(onayAyariService.otomatikOnayGecerli(eq(1L), eq("MASRAF"), any())).thenReturn(true);

        PersonelMasrafTalepDTO dto = PersonelMasrafTalepDTO.builder()
                .tur("MASRAF").kategori("YAKIT").tutar(BigDecimal.valueOf(100)).tarih(LocalDate.now()).build();

        PersonelMasrafTalepDTO result = talepService.talepOlustur(dto, 1L, 2L);

        assertEquals("ONAYLANDI", result.getDurum());
        verify(masrafService).olustur(any(MasrafDTO.class), eq(1L));
    }
}
