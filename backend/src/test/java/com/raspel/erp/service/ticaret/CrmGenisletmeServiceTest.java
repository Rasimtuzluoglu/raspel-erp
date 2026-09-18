package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.CrmAktiviteDTO;
import com.raspel.erp.dto.ticaret.CrmKampanyaDTO;
import com.raspel.erp.dto.ticaret.CrmLeadDTO;
import com.raspel.erp.entity.ticaret.CrmAktivite;
import com.raspel.erp.entity.ticaret.CrmKampanya;
import com.raspel.erp.entity.ticaret.CrmLead;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.CrmAktiviteRepository;
import com.raspel.erp.repository.ticaret.CrmKampanyaRepository;
import com.raspel.erp.repository.ticaret.CrmLeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmGenisletmeServiceTest {

    @Mock private CrmLeadRepository crmLeadRepository;
    @Mock private CrmAktiviteRepository crmAktiviteRepository;
    @Mock private CrmKampanyaRepository crmKampanyaRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private TenantChecker tenantChecker;
    @InjectMocks private CrmGenisletmeService service;

    private CrmLead lead(Long id) {
        return CrmLead.builder().id(id).sirketId(1L).ad("Lead " + id).durum("YENI").skor(10).build();
    }

    @Test
    void leadleriGetir_returnsPage() {
        when(crmLeadRepository.findBySirketIdOrderByOlusturmaTarihiDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lead(1L))));
        var r = service.leadleriGetir(1L, null, Pageable.unpaged());
        assertEquals(1, r.getContent().size());
    }

    @Test
    void leadleriGetir_durumFiltresi() {
        when(crmLeadRepository.findBySirketIdAndDurumOrderBySkorDesc(1L, "YENI")).thenReturn(List.of(lead(1L)));
        var r = service.leadleriGetir(1L, "YENI", Pageable.unpaged());
        assertEquals(1, r.getContent().size());
    }

    @Test
    void leadOlustur_creates() {
        CrmLeadDTO dto = CrmLeadDTO.builder().ad("Yeni Lead").tahminiDeger(BigDecimal.TEN).build();
        when(crmLeadRepository.save(any(CrmLead.class))).thenAnswer(i -> i.getArgument(0));
        var r = service.leadOlustur(dto, 1L);
        assertEquals("Yeni Lead", r.getAd());
        assertEquals("YENI", r.getDurum());
    }

    @Test
    void leadOlustur_sirketYoksaHata() {
        assertThrows(BusinessException.class, () -> service.leadOlustur(CrmLeadDTO.builder().ad("X").build(), null));
    }

    @Test
    void leadDonustur_cariyeBaglar() {
        when(crmLeadRepository.findById(1L)).thenReturn(Optional.of(lead(1L)));
        when(cariHesapRepository.findById(7L)).thenReturn(Optional.of(
                com.raspel.erp.entity.finans.CariHesap.builder().id(7L).sirketId(1L).ad("A Ltd").build()));
        when(crmLeadRepository.save(any(CrmLead.class))).thenAnswer(i -> i.getArgument(0));

        var r = service.leadDonustur(1L, 7L);

        assertEquals("DONUSTURULDU", r.getDurum());
        assertEquals(7L, r.getCariHesapId());
    }

    @Test
    void leadGetir_throwsWhenNotFound() {
        when(crmLeadRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.leadGetir(99L));
    }

    @Test
    void aktiviteOlustur_creates() {
        CrmAktiviteDTO dto = CrmAktiviteDTO.builder().tur("ARAMA").baslik("Müşteri aradı").build();
        when(crmAktiviteRepository.save(any(CrmAktivite.class))).thenAnswer(i -> i.getArgument(0));
        var r = service.aktiviteOlustur(dto, 1L);
        assertEquals("ARAMA", r.getTur());
        assertFalse(r.getTamamlandi());
    }

    @Test
    void aktiviteTamamla_isaretler() {
        CrmAktivite a = CrmAktivite.builder().id(1L).sirketId(1L).tur("GOREV").baslik("X").tamamlandi(false).build();
        when(crmAktiviteRepository.findById(1L)).thenReturn(Optional.of(a));
        when(crmAktiviteRepository.save(any(CrmAktivite.class))).thenAnswer(i -> i.getArgument(0));

        var r = service.aktiviteTamamla(1L, true);

        assertTrue(r.getTamamlandi());
        assertNotNull(r.getTamamlanmaTarihi());
    }

    @Test
    void kampanyaOlustur_creates() {
        CrmKampanyaDTO dto = CrmKampanyaDTO.builder().ad("Yaz Kampanyası").butce(BigDecimal.valueOf(5000)).build();
        when(crmKampanyaRepository.save(any(CrmKampanya.class))).thenAnswer(i -> {
            CrmKampanya k = i.getArgument(0);
            k.setId(3L);
            return k;
        });
        when(crmLeadRepository.countBySirketIdAndKampanyaId(1L, 3L)).thenReturn(0L);

        var r = service.kampanyaOlustur(dto, 1L);

        assertEquals("Yaz Kampanyası", r.getAd());
        assertEquals("PLANLANDI", r.getDurum());
        assertEquals(0L, r.getLeadSayisi());
    }

    @Test
    void kampanyaSil_deletes() {
        CrmKampanya k = CrmKampanya.builder().id(1L).sirketId(1L).ad("X").build();
        when(crmKampanyaRepository.findById(1L)).thenReturn(Optional.of(k));
        service.kampanyaSil(1L);
        verify(crmKampanyaRepository).delete(k);
    }
}
