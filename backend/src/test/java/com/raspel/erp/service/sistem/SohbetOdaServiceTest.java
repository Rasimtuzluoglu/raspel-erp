package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SohbetOdaDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.entity.sistem.SohbetOda;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import com.raspel.erp.repository.sistem.SohbetOdaRepository;
import com.raspel.erp.repository.sistem.SohbetOdaUyeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SohbetOdaServiceTest {

    @Mock private SohbetOdaRepository odaRepository;
    @Mock private SohbetOdaUyeRepository uyeRepository;
    @Mock private SohbetMesajRepository mesajRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private SohbetOdaService odaService;

    @Test
    void olustur_bosAdHataVerir() {
        assertThrows(BusinessException.class,
                () -> odaService.olustur(SohbetOdaDTO.builder().ad("  ").build(), 1L, 10L));
    }

    @Test
    void olustur_odayiOlustururVeKurucuyuUyeYapar() {
        when(odaRepository.save(any(SohbetOda.class))).thenAnswer(inv -> {
            SohbetOda o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        SohbetOdaDTO sonuc = odaService.olustur(SohbetOdaDTO.builder().ad("Satış Ekibi").build(), 1L, 10L);

        assertEquals(1L, sonuc.getId());
        assertTrue(sonuc.isUyeMi());
        verify(uyeRepository).save(argThat(u -> u.getOdaId().equals(1L) && u.getKullaniciId().equals(10L)));
    }

    @Test
    void mesajGonder_uyeDegilseHataVerir() {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.of(Kullanici.builder().id(99L).role("USER").build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> odaService.mesajGonder(1L, com.raspel.erp.dto.sistem.SohbetMesajDTO.builder().mesaj("merhaba").build(),
                        1L, 99L, "Ali"));
    }

    @Test
    void mesajGonder_uyeIseKaydeder() {
        when(odaRepository.findById(1L)).thenReturn(Optional.of(SohbetOda.builder().id(1L).sirketId(1L).build()));
        when(uyeRepository.existsByOdaIdAndKullaniciId(1L, 99L)).thenReturn(true);
        when(mesajRepository.save(any(SohbetMesaj.class))).thenAnswer(inv -> {
            SohbetMesaj m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var sonuc = odaService.mesajGonder(1L,
                com.raspel.erp.dto.sistem.SohbetMesajDTO.builder().mesaj("merhaba").build(),
                1L, 99L, "Ali");

        assertEquals("merhaba", sonuc.getMesaj());
        assertEquals(1L, sonuc.getOdaId());
    }
}
