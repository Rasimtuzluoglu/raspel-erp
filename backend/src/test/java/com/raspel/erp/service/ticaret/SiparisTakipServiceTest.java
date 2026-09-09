package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.SiparisTakipDTO;
import com.raspel.erp.entity.envanter.UretimEmri;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiparisTakipServiceTest {

    @Mock private SiparisRepository siparisRepository;
    @Mock private UretimEmriRepository uretimEmriRepository;
    @Mock private IrsaliyeRepository irsaliyeRepository;
    @Mock private TeslimatRepository teslimatRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @InjectMocks private SiparisTakipService takipService;

    @Test
    void zincir_siparisinUretimSevkTeslimatDurumunuDoldurur() {
        Siparis s = Siparis.builder().id(1L).siparisNo("SP-1").durum("ONAYLANDI").cariHesapId(7L).tarih(LocalDate.now()).build();
        when(siparisRepository.findBySirketIdOrderByTarihDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(s)));

        when(uretimEmriRepository.findBySirketIdAndSiparisId(1L, 1L))
                .thenReturn(List.of(UretimEmri.builder().id(1L).sirketId(1L).siparisId(1L).urunId(10L).miktar(java.math.BigDecimal.ONE).durum("URETIMDE").build()));
        when(irsaliyeRepository.findBySirketIdAndSiparisId(1L, 1L))
                .thenReturn(List.of(Irsaliye.builder().id(1L).sirketId(1L).siparisId(1L).irsaliyeNo("IRS-1").durum("KESILDI").tur("SATIS").build()));
        when(teslimatRepository.findBySirketIdAndSiparisId(1L, 1L))
                .thenReturn(List.of(Teslimat.builder().id(1L).sirketId(1L).siparisId(1L).durum("TESLIM_EDILDI").build()));
        when(cariHesapRepository.findById(7L)).thenReturn(java.util.Optional.of(
                com.raspel.erp.entity.finans.CariHesap.builder().id(7L).ad("A Ltd").build()));

        List<SiparisTakipDTO> sonuc = takipService.zincir(1L);

        assertEquals(1, sonuc.size());
        assertEquals("SP-1", sonuc.get(0).getSiparisNo());
        assertEquals("URETIMDE", sonuc.get(0).getUretimDurum());
        assertEquals("KESILDI", sonuc.get(0).getSevkDurum());
        assertEquals("TESLIM_EDILDI", sonuc.get(0).getTeslimatDurum());
        assertEquals("A Ltd", sonuc.get(0).getCariAd());
    }

    @Test
    void zincir_bosSiparisListesindeBosDoner() {
        when(siparisRepository.findBySirketIdOrderByTarihDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertTrue(takipService.zincir(1L).isEmpty());
    }
}
