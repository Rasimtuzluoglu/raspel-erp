package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.repository.sistem.RolRepository;
import com.raspel.erp.repository.sistem.YetkiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    /** Yetki kodu üretir (kayıtta id atanmadığı için findById yerine findAll kullanılır). */
    private Yetki yetki(String kod) {
        return Yetki.builder().kod(kod).modul("Test").aciklama(kod).build();
    }

    @Test
    void tumRolleriGetir_RolTablosuBos_VarsayilanRolleriKurar() {
        when(rolRepository.findAll()).thenReturn(List.of());
        // REDTEAM/Faz1.4: MUHASEBE rolunun yetki eşleşmesi doğrulanabilsin diye
        // finans kapsamı da mock'a ekleniyor.
        when(yetkiRepository.findAll()).thenReturn(List.of(
                yetki("STOK_READ"), yetki("STOK_WRITE"), yetki("STOK_DELETE"),
                yetki("FINANS_READ"), yetki("FINANS_WRITE"), yetki("IK_READ"),
                yetki("IK_WRITE")));
        when(rolRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Rol> roller = yetkiService.tumRolleriGetir();

// ADMIN + USER + SAHA + DRIVER + MUHASEBE
        // REDTEAM/Faz1.4: MUHASEBE rolu eklendi. UI'da secilebilen bu rol
        // sistem.rol'de HICBIR YERDE olusmadigi icin YetkiKontrol (fail-closed)
        // her kontrolunde false donuyor ve Onaylar ekraninin 3/5 sekmesi
        // kalici 403 veriyordu.
        assertEquals(5, roller.size());
        Rol admin = roller.stream().filter(r -> "ADMIN".equals(r.getAd())).findFirst().orElseThrow();
        Rol user = roller.stream().filter(r -> "USER".equals(r.getAd())).findFirst().orElseThrow();
        // ADMIN: mock'taki TÜM yetkiler (7).
        assertEquals(7, admin.getYetkiler().size());
        // USER yalnızca READ/WRITE alır; silme yetkisi verilmez (6).
        assertEquals(6, user.getYetkiler().size());
        assertTrue(user.getYetkiler().stream().allMatch(y -> y.getKod().endsWith("_READ") || y.getKod().endsWith("_WRITE")));

        // MUHASEBE: finans/IK yazma kapsamı; DELETE ve EXPORT verilmez.
        Rol muhasebe = roller.stream().filter(r -> "MUHASEBE".equals(r.getAd())).findFirst().orElseThrow();
        assertFalse(muhasebe.getYetkiler().isEmpty(),
                "MUHASEBE rolune yetki atanmaliydi; bos kalirsa YetkiKontrol 403 doner");
        assertTrue(muhasebe.getYetkiler().stream()
                        .anyMatch(y -> "FINANS_WRITE".equals(y.getKod())),
                "MUHASEBE en az FINANS_WRITE almalı");
        assertFalse(muhasebe.getYetkiler().stream()
                        .anyMatch(y -> y.getKod().endsWith("_DELETE") || y.getKod().endsWith("_EXPORT")),
                "MUHASEBE'ye DELETE/EXPORT verilmemeli (V149 kuralı)");
    }

    @Test
    void tumRolleriGetir_EksikSahaVeSoforRolleriniOlusturur() {
        Rol admin = Rol.builder().id(1L).ad("ADMIN").yetkiler(new HashSet<>(Set.of(yetki("STOK_READ")))).build();
        Rol user = Rol.builder().id(2L).ad("USER").yetkiler(new HashSet<>()).build();
        when(rolRepository.findAll())
                .thenReturn(List.of(admin, user))                       // ilk çağrı: mevcut roller
                .thenReturn(List.of(admin, user));                      // sonraki çağrı: findAll tekrarı
        when(yetkiRepository.findAll()).thenReturn(List.of(
                yetki("STOK_READ"), yetki("SIPARIS_READ"), yetki("CARI_READ"), yetki("FATURA_READ"), yetki("IK_READ")));
        when(rolRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        yetkiService.tumRolleriGetir();

        ArgumentCaptor<List<Rol>> captor = ArgumentCaptor.forClass(List.class);
        verify(rolRepository).saveAll(captor.capture());
        List<Rol> eklenen = captor.getValue();
        assertEquals(2, eklenen.size());
        assertTrue(eklenen.stream().anyMatch(r -> "SAHA".equals(r.getAd())));
        assertTrue(eklenen.stream().anyMatch(r -> "DRIVER".equals(r.getAd())));
    }

    @Test
    void tumRolleriGetir_MevcutRolVarsaEklememeYapar() {
        Rol admin = Rol.builder().id(1L).ad("ADMIN").yetkiler(new HashSet<>()).build();
        Rol saha = Rol.builder().id(3L).ad("SAHA").yetkiler(new HashSet<>()).build();
        Rol driver = Rol.builder().id(4L).ad("DRIVER").yetkiler(new HashSet<>()).build();
        when(rolRepository.findAll()).thenReturn(List.of(admin, saha, driver));
        when(yetkiRepository.findAll()).thenReturn(List.of(yetki("STOK_READ")));

        yetkiService.tumRolleriGetir();

        verify(rolRepository, never()).saveAll(anyList());
    }

    // ---------- C8: seed kontrolu (sistem.yetki bos tabloda kalirsa) ----------

    /**
     * C8: `sistem.yetki` tablosu bos oldugunda USER rolunun tum yazma uclari
     * 403 donuyordu (YetkiKontrol fail-closed + CROSS JOIN bos tabloya yaziyordu).
     * Acilis seed'i bos tabloyu doldurur.
     */
    @Test
    void seedKontrolu_bosYetkiTablosunuDoldurur() {
        when(yetkiRepository.findAll()).thenReturn(List.of());
        when(rolRepository.findAll()).thenReturn(List.of());
        when(yetkiRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(rolRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        yetkiService.seedKontrolu();

        verify(yetkiRepository).saveAll(anyList());
        verify(rolRepository).saveAll(anyList());
    }

    /** C8: rol tablosu bos olmasa da eksik roller tamamlanir. */
    @Test
    void seedKontrolu_eksikRolleriTamamlar() {
        when(yetkiRepository.findAll()).thenReturn(List.of(yetki("STOK_READ")));
        when(rolRepository.findAll()).thenReturn(List.of(Rol.builder().id(1L).ad("ADMIN").yetkiler(new HashSet<>()).build()));

        yetkiService.seedKontrolu();

        verify(rolRepository).saveAll(anyList());
    }

/** C8: veriler zaten doluysa hicbir sey yazilmaz (idempotent). */
    @Test
    void seedKontrolu_doluVerideYazmaYapar() {
        List<Yetki> tumYetkiler = tumVarsayilanYetkiler();
        List<Rol> roller = new ArrayList<>();
        // Varsayilan roller + kullanımda olan operasyonel roller (SAHA, DRIVER).
        for (String ad : List.of("ADMIN", "USER", "MUHASEBE", "SATIS", "DEPO",
                "PERSONEL", "SAHA", "DRIVER")) {
            roller.add(Rol.builder().id((long) roller.size() + 1L).ad(ad).yetkiler(new HashSet<>()).build());
        }
        when(yetkiRepository.findAll()).thenReturn(tumYetkiler);
        when(rolRepository.findAll()).thenReturn(roller);

        yetkiService.seedKontrolu();

        verify(yetkiRepository, never()).saveAll(anyList());
        verify(rolRepository, never()).saveAll(anyList());
    }

    /**
     * C8: eksik kod tamamlama yalnızca eksikleri yazar. Karşılaştırma için
     * tüm liste yeniden kaydedilmemelidir (var olan kodlar UNIQUE ihlali/veri
     * kaybı riski taşır).
     */
    @Test
    void seedKontrolu_yalnizcaEksikKoduYazar() {
        List<Yetki> mevcut = new ArrayList<>(tumVarsayilanYetkiler());
        Yetki silinecek = mevcut.remove(0);
        when(yetkiRepository.findAll()).thenReturn(mevcut);
        when(rolRepository.findAll()).thenReturn(List.of(Rol.builder().id(1L).ad("ADMIN").yetkiler(new HashSet<>()).build()));

        yetkiService.seedKontrolu();

        ArgumentCaptor<List<Yetki>> captor = ArgumentCaptor.forClass(List.class);
        verify(yetkiRepository).saveAll(captor.capture());
        List<Yetki> yazilan = captor.getValue();
        assertEquals(1, yazilan.size());
        assertEquals(silinecek.getKod(), yazilan.get(0).getKod());
    }

    /** C8: seed hatasinda uygulama ayaga kalkar, hata loglanir (fail-open). */
    @Test
    void seedKontrolu_hataDurumundaYutulur() {
        when(yetkiRepository.findAll()).thenThrow(new RuntimeException("tablo yok"));

        assertDoesNotThrow(() -> yetkiService.seedKontrolu());

        verify(rolRepository, never()).saveAll(anyList());
    }

/** Yardimci: servisin urettigi tum varsayilan yetki kodlari. */
    private List<Yetki> tumVarsayilanYetkiler() {
        when(yetkiRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(rolRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        yetkiService.seedKontrolu();
        ArgumentCaptor<List<Yetki>> captor = ArgumentCaptor.forClass(List.class);
        verify(yetkiRepository).saveAll(captor.capture());
        List<Yetki> kodlar = new ArrayList<>(captor.getValue());
        // Yardimci metodun kendi cagrilari biriktirmesin: sonraki testler kendi
        // seedKontrolu cagrilarini dogrulasin.
        clearInvocations(yetkiRepository, rolRepository);
        return kodlar;
    }
}