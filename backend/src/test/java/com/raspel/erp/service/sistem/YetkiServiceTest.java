package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.repository.sistem.RolRepository;
import com.raspel.erp.repository.sistem.YetkiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.entity.envanter.Stok;

@ExtendWith(MockitoExtension.class)
class YetkiServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private YetkiRepository yetkiRepository;

    @InjectMocks
    private YetkiService yetkiService;

    private Rol mockRol;
    private Yetki mockYetki;

    @BeforeEach
    void setUp() {
        mockYetki = Yetki.builder().id(1L).kod("STOK_READ").modul("Stok").aciklama("Stok okuma").build();
        mockRol = Rol.builder().id(1L).ad("SATIS").aciklama("Satış Temsilcisi").yetkiler(new HashSet<>()).build();
    }

    @Test
    void testTumYetkileriGetir_MevcutListeyiDoner() {
        when(yetkiRepository.findAll()).thenReturn(List.of(mockYetki));

        List<Yetki> result = yetkiService.tumYetkileriGetir();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("STOK_READ", result.get(0).getKod());
    }

    @Test
    void testRolYetkileriniGuncelle_Basarili() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(mockRol));
        when(yetkiRepository.findAllById(Set.of(1L))).thenReturn(List.of(mockYetki));
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        Rol result = yetkiService.rolYetkileriniGuncelle(1L, Set.of(1L));

        assertNotNull(result);
        assertEquals(1, result.getYetkiler().size());
        verify(rolRepository, times(1)).save(mockRol);
    }
}