package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.KurulumDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.service.sistem.KullaniciService;
import com.raspel.erp.service.sistem.KurulumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KurulumServiceTest {

    @Mock private SirketRepository sirketRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private KullaniciService kullaniciService;
    @Mock private DataSource dataSource;
    @InjectMocks private KurulumService kurulumService;

    private KurulumDTO ornekDTO() {
        return KurulumDTO.builder()
                .ad("Yeni Firma").vergiNo("1234567890").vergiDairesi("Kadıköy VD")
                .adminUsername("yonetici").adminPassword("Sifre1!")
                .adminDisplayName("Yönetici")
                .build();
    }

    @Test
    void durum_kurulumGerekliWhenEmpty() {
        when(sirketRepository.count()).thenReturn(0L);
        var result = kurulumService.durum();
        assertEquals(true, result.get("kurulumGerekli"));
    }

    @Test
    void durum_kurulumTamamWhenCompanyExists() {
        when(sirketRepository.count()).thenReturn(1L);
        var result = kurulumService.durum();
        assertEquals(false, result.get("kurulumGerekli"));
    }

    @Test
    void kurulumYap_createsCompanyAndAdmin() throws Exception {
        // PostgreSQL advisory lock başarıyla alınıyor
        Connection conn = mock(Connection.class);
        Statement st = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(st);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(true);

        when(sirketRepository.count()).thenReturn(0L);
        when(sirketRepository.save(any(Sirket.class))).thenAnswer(inv -> {
            Sirket s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(kullaniciService.olustur(any(KullaniciDTO.class)))
                .thenReturn(KullaniciDTO.builder().id(1L).username("yonetici").build());
        Kullanici admin = Kullanici.builder().id(1L).username("yonetici").role("ADMIN").active(true).build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(kullaniciService.giris(any(LoginRequest.class)))
                .thenReturn(LoginResponse.builder().id(1L).username("yonetici").girisToken("tok").build());

        var result = kurulumService.kurulumYap(ornekDTO());

        assertNotNull(result);
        assertEquals("yonetici", result.getUsername());
        verify(kullaniciService).olustur(any(KullaniciDTO.class));
        verify(kullaniciService).giris(any(LoginRequest.class));
    }

    @Test
    void kurulumYap_lockAlinamazsaKilitsizCalistirmaz() throws Exception {
        Connection conn = mock(Connection.class);
        Statement st = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(st);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> kurulumService.kurulumYap(ornekDTO()));
        assertTrue(ex.getMessage().toLowerCase().contains("başka bir istek"));
        // Kilitsiz kurulum yürütülmemeli
        verify(sirketRepository, never()).save(any(Sirket.class));
    }

    @Test
    void kurulumYap_beklenmeyenHataKilitsizDevamEtmez() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB bağlantı hatası"));

        BusinessException ex = assertThrows(BusinessException.class, () -> kurulumService.kurulumYap(ornekDTO()));
        assertTrue(ex.getMessage().toLowerCase().contains("tekrar deneyin"));
        verify(sirketRepository, never()).save(any(Sirket.class));
        verify(kullaniciService, never()).olustur(any(KullaniciDTO.class));
    }

    @Test
    void kurulumYap_whenAlreadySetup_throws() throws Exception {
        // Advisory lock başarıyla alınır, ardından firma zaten var olduğu için hata döner
        Connection conn = mock(Connection.class);
        Statement st = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(st);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(true);

        when(sirketRepository.count()).thenReturn(1L);
        assertThrows(BusinessException.class, () -> kurulumService.kurulumYap(ornekDTO()));
    }
}
