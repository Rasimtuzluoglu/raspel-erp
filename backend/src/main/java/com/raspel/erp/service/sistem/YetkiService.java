package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.RolRepository;
import com.raspel.erp.repository.sistem.YetkiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class YetkiService {

    private final RolRepository rolRepository;
    private final YetkiRepository yetkiRepository;

    /**
     * Kullanıcı tablosunda rol olarak kullanılabilen ancak `sistem.rol`
     * tablosunda satırı bulunmayan roller ve verilecek en az yetkileri.
     * Her dizinin son elemanı rol açıklamasıdır.
     * Bu roller olmadan YetkiKontrol sorgusu rolü bulamaz ve her isteği
     * reddeder; saha/şoför akışları bu yüzden tanımlı olmalıdır.
     */
    private static final Map<String, String[]> EKSIK_ROL_TANIMLARI = new LinkedHashMap<>();

    static {
        // Saha personeli: stok görüntüleme, sipariş/fatura okuma, saha notu.
        EKSIK_ROL_TANIMLARI.put("SAHA", new String[]{
                "STOK_READ", "SIPARIS_READ", "FATURA_READ", "CARI_READ", "IK_READ",
                "Saha Portalı Kullanıcısı"
        });
        // Şoför: teslimat akışı için sipariş ve cari okuma, stok görüntüleme.
        EKSIK_ROL_TANIMLARI.put("DRIVER", new String[]{
                "SIPARIS_READ", "CARI_READ", "STOK_READ",
                "Şoför / Teslimat Kullanıcısı"
        });
    }

public List<Yetki> tumYetkileriGetir() {
        List<Yetki> yetkiler = yetkiRepository.findAll();
        if (yetkiler.isEmpty()) {
            yetkiler = varsayilanYetkileriOlustur();
        }
        return yetkiler;
    }

    /**
     * Roller listesini döner. Tablo boşsa varsayılan roller kurulur; ayrıca
     * sistemde kullanımda olan roller (DRIVER, SAHA) eksikse tamamlanır.
     * Saha/şoför kullanıcılarının rol satırı olmadığında yetki kontrolü her
     * zaman false döndüğü için akışları bozmamak adına bu roller de bulunmalıdır.
     */
    public List<Rol> tumRolleriGetir() {
        List<Rol> roller = rolRepository.findAll();
        if (roller.isEmpty()) {
            return varsayilanRolleriOlustur();
        }
        eksikRolleriTamamla(roller);
        return rolRepository.findAll();
    }

    /**
     * Kullanımda olan ama tanımsız rolleri yetkileriyle birlikte oluşturur.
     * Ayrıca eksik yetki kodlarını da tamamlar (migration seed'ine ek savunma).
     *
     * <p>Uygulama açılışında bir kez çağrılır: {@link #seedKontrolu()}.
     */
    private void eksikRolleriTamamla(List<Rol> mevcut) {
        eksikRolleriTamamla(mevcut, tumYetkileriGetir());
    }

    /**
     * Aynı iş, yetki listesi önceden biliniyorsa. {@code seedKontrolu} az önce
     * yetkileri doldurduğu halde burada tekrar okumak (ve tablo hâlâ boş
     * görünürse) aynı kodların ikinci kez yazılmasına yol açıyordu.
     */
    private void eksikRolleriTamamla(List<Rol> mevcut, List<Yetki> tumYetkiler) {
        Set<String> varOlan = mevcut.stream().map(Rol::getAd).collect(Collectors.toSet());
        List<Rol> eklenecek = new ArrayList<>();
        for (Map.Entry<String, String[]> tanim : EKSIK_ROL_TANIMLARI.entrySet()) {
            if (varOlan.contains(tanim.getKey())) continue;
            String[] degerler = tanim.getValue();
            Set<Yetki> secili = tumYetkiler.stream()
                    .filter(y -> Arrays.asList(degerler).contains(y.getKod()))
                    .collect(Collectors.toCollection(HashSet::new));
            eklenecek.add(Rol.builder()
                    .ad(tanim.getKey())
                    .aciklama(degerler[degerler.length - 1])
                    .yetkiler(secili)
                    .build());
            log.info("Eksik rol oluşturuldu: {} ({} yetki)", tanim.getKey(), secili.size());
        }
        if (!eklenecek.isEmpty()) rolRepository.saveAll(eklenecek);
    }

    /**
     * Uygulama açılışında bir kez çalışır: {@code sistem.yetki} ve {@code sistem.rol}
     * tablolarının seed'ini garanti eder.
     *
     * <p><b>Neden gerekli:</b> Bu tablolar yalnızca "Yetki Yönetimi" ekranı
     * açıldığında dolduruluyordu. {@code YetkiKontrol.kontrol} rolü bulamazsa
     * {@code false} döndürür (fail-closed), dolayısıyla yeni kurulumda bir
     * yönetici izin ekranını açana kadar {@code USER} rolünün <b>tüm</b>
     * {@code @yetkiKontrol} korumalı yazma uçları 403 döndürüyordu ve neden
     * yalnızca log.warn ile belli oluyordu. Migration seed'i (V147) birincil
     * çözüm; bu metot migration uygulanmamış/yarım kalmış veritabanlarında ve
     * eski kurulumlarda güvenlik ağıdır.
     */
    @Transactional
    public void seedKontrolu() {
        try {
            List<Yetki> yetkiler = yetkiRepository.findAll();
            List<Yetki> etkinYetkiler = yetkiler;
            if (yetkiler.isEmpty()) {
                etkinYetkiler = yetkiRepository.saveAll(varsayilanYetkiListesi());
                log.info("sistem.yetki tablosu boştu; {} yetki kodu seed edildi", etkinYetkiler.size());
            } else {
                // Var olan kodlara ek olarak eksik kodu ekle (yeni modül eklenmişse).
                // DİKKAT: `varsayilanYetkiListesi()` saf bir üreticidir; kaydetme
                // yalnızca gerçekten eksik olanlar için yapılır. Daha önce
                // karşılaştırma amacıyla tüm liste kaydediliyordu (stream filtresi
                // içinde yan etki) ve var olan kodlar yeniden yazılıyordu.
                Set<String> varOlan = yetkiler.stream().map(Yetki::getKod).collect(Collectors.toSet());
                Set<String> beklenen = varsayilanYetkiKodlari();
                Set<String> eksik = new LinkedHashSet<>(beklenen);
                eksik.removeAll(varOlan);
                if (!eksik.isEmpty()) {
                    List<Yetki> eklenecek = varsayilanYetkiListesi().stream()
                            .filter(y -> eksik.contains(y.getKod()))
                            .collect(Collectors.toList());
                    etkinYetkiler = new ArrayList<>(yetkiler);
                    etkinYetkiler.addAll(yetkiRepository.saveAll(eklenecek));
                    log.info("Eksik yetki kodları eklendi: {}", eksik);
                }
            }
            List<Rol> roller = rolRepository.findAll();
            if (roller.isEmpty()) {
                int adet = varsayilanRolleriOlustur(etkinYetkiler).size();
                log.info("sistem.rol tablosu boştu; {} rol seed edildi", adet);
            } else {
                eksikRolleriTamamla(roller, etkinYetkiler);
            }
        } catch (Exception e) {
            // Seed başarısız olursa uygulama yine de ayağa kalkar; ADMIN rolü
            // sistemde her zaman var olduğu için yönetim yine de müdahale edebilir.
            log.error("Yetki/rol seed kontrolü başarısız oldu. ADMIN dışındaki roller "
                    + "için @yetkiKontrol kontrolleri reddedilecektir.", e);
        }
    }

    /** Beklenen tüm yetki kodları (yan etkisiz; tanım kaynağı: {@link #varsayilanYetkiListesi}). */
    private Set<String> varsayilanYetkiKodlari() {
        return varsayilanYetkiListesi().stream()
                .map(Yetki::getKod)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Rol rolYetkileriniGuncelle(Long rolId, Set<Long> yetkiIdleri) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));

        List<Yetki> secilenYetkiler = yetkiRepository.findAllById(yetkiIdleri);
        rol.setYetkiler(new HashSet<>(secilenYetkiler));

        Rol guncel = rolRepository.save(rol);
        log.info("Rol yetkileri güncellendi: {} -> {} yetki atandı", rol.getAd(), secilenYetkiler.size());
        return guncel;
    }

    private List<Yetki> varsayilanYetkileriOlustur() {
        return yetkiRepository.saveAll(varsayilanYetkiListesi());
    }

    /**
     * Varsayılan yetki kodlarını <b>kaydetmeden</b> üretir.
     *
     * <p>Karşılaştırma/filtreleme amaçlı çağrılarda yan etki olmaması için
     * ayrılmıştır; aksi halde "eksik kod hangileri" sorusundan sonra tüm
     * liste yeniden yazılıyordu.
     */
    private List<Yetki> varsayilanYetkiListesi() {
        List<Yetki> list = new ArrayList<>();
        // Cari Modülü
        list.add(Yetki.builder().kod("CARI_READ").modul("Cari").aciklama("Cari hesapları görüntüleme").build());
        list.add(Yetki.builder().kod("CARI_WRITE").modul("Cari").aciklama("Cari kart ekleme ve düzenleme").build());
        list.add(Yetki.builder().kod("CARI_DELETE").modul("Cari").aciklama("Cari kart silme").build());
        list.add(Yetki.builder().kod("CARI_EXPORT").modul("Cari").aciklama("Cari listesi Excel/PDF aktarımı").build());

        // Fatura Modülü
        list.add(Yetki.builder().kod("FATURA_READ").modul("Fatura").aciklama("Faturaları görüntüleme").build());
        list.add(Yetki.builder().kod("FATURA_WRITE").modul("Fatura").aciklama("Fatura oluşturma ve düzenleme").build());
        list.add(Yetki.builder().kod("FATURA_DELETE").modul("Fatura").aciklama("Fatura silme ve iptal etme").build());
        list.add(Yetki.builder().kod("FATURA_EXPORT").modul("Fatura").aciklama("Fatura listesi Excel/PDF aktarımı").build());

        // Stok & Envanter
        list.add(Yetki.builder().kod("STOK_READ").modul("Stok").aciklama("Stok listesini görüntüleme").build());
        list.add(Yetki.builder().kod("STOK_WRITE").modul("Stok").aciklama("Yeni stok ekleme ve güncelleme").build());
        list.add(Yetki.builder().kod("STOK_DELETE").modul("Stok").aciklama("Stok kaydı silme").build());
        list.add(Yetki.builder().kod("STOK_EXPORT").modul("Stok").aciklama("Stok listesi Excel/PDF aktarımı").build());

        // Finans (Banka, Kasa, Çek/Senet)
        list.add(Yetki.builder().kod("FINANS_READ").modul("Finans").aciklama("Banka, kasa ve çek/senet hareketlerini görüntüleme").build());
        list.add(Yetki.builder().kod("FINANS_WRITE").modul("Finans").aciklama("Kasa/Banka işlemi ve tahsilat/ödeme kaydı").build());
        list.add(Yetki.builder().kod("FINANS_DELETE").modul("Finans").aciklama("Finansal hareket silme").build());
        list.add(Yetki.builder().kod("FINANS_EXPORT").modul("Finans").aciklama("Ekstre ve finansal rapor aktarımı").build());

        // Sipariş & Teklif
        list.add(Yetki.builder().kod("SIPARIS_READ").modul("Siparis").aciklama("Sipariş ve satış tekliflerini görüntüleme").build());
        list.add(Yetki.builder().kod("SIPARIS_WRITE").modul("Siparis").aciklama("Yeni sipariş ve teklif oluşturma").build());
        list.add(Yetki.builder().kod("SIPARIS_DELETE").modul("Siparis").aciklama("Sipariş/teklif silme ve iptal").build());
        list.add(Yetki.builder().kod("SIPARIS_EXPORT").modul("Siparis").aciklama("Sipariş ve teklif mektubu aktarımı").build());

        // Satın Alma
        list.add(Yetki.builder().kod("SATINALMA_READ").modul("Satinalma").aciklama("Satınalma talep ve siparişlerini görüntüleme").build());
        list.add(Yetki.builder().kod("SATINALMA_WRITE").modul("Satinalma").aciklama("Satınalma talebi açma ve sipariş verme").build());
        list.add(Yetki.builder().kod("SATINALMA_DELETE").modul("Satinalma").aciklama("Satınalma kaydı silme").build());
        list.add(Yetki.builder().kod("SATINALMA_EXPORT").modul("Satinalma").aciklama("Satınalma raporları aktarımı").build());

        // İrsaliye
        list.add(Yetki.builder().kod("IRSALIYE_READ").modul("Irsaliye").aciklama("Sevk ve gelen irsaliyeleri görüntüleme").build());
        list.add(Yetki.builder().kod("IRSALIYE_WRITE").modul("Irsaliye").aciklama("İrsaliye düzenleme ve kabul").build());
        list.add(Yetki.builder().kod("IRSALIYE_DELETE").modul("Irsaliye").aciklama("İrsaliye kaydı silme").build());
        list.add(Yetki.builder().kod("IRSALIYE_EXPORT").modul("Irsaliye").aciklama("İrsaliye listesi aktarımı").build());

        // İnsan Kaynakları & Personel
        list.add(Yetki.builder().kod("IK_READ").modul("IK").aciklama("Personel, izin ve puantaj görüntüleme").build());
        list.add(Yetki.builder().kod("IK_WRITE").modul("IK").aciklama("Personel kaydı, izin ve masraf onayı").build());
        list.add(Yetki.builder().kod("IK_DELETE").modul("IK").aciklama("Personel/izin kaydı silme").build());
        list.add(Yetki.builder().kod("IK_EXPORT").modul("IK").aciklama("Bordro ve personel listesi aktarımı").build());

        // Raporlar & Analiz
        list.add(Yetki.builder().kod("RAPOR_READ").modul("Rapor").aciklama("Finansal ve ticari raporları görüntüleme").build());
        list.add(Yetki.builder().kod("RAPOR_EXPORT").modul("Rapor").aciklama("Rapor çıktılarını dışa aktarma").build());

        // Sistem & Yönetim
        list.add(Yetki.builder().kod("SISTEM_READ").modul("Sistem").aciklama("Sistem ayarları ve kullanıcıları görüntüleme").build());
        list.add(Yetki.builder().kod("SISTEM_WRITE").modul("Sistem").aciklama("Kullanıcı, rol ve sistem ayarları yönetimi").build());
        list.add(Yetki.builder().kod("SISTEM_DELETE").modul("Sistem").aciklama("Kullanıcı ve sistem kaydı silme").build());
        list.add(Yetki.builder().kod("SISTEM_EXPORT").modul("Sistem").aciklama("Sistem denetim ve yedekleme aktarımı").build());

        return list;
    }

    private List<Rol> varsayilanRolleriOlustur() {
        return varsayilanRolleriOlustur(tumYetkileriGetir());
    }

    /** Aynı iş, yetki listesi önceden biliniyorsa (gereksiz yeniden sorgu yok). */
    private List<Rol> varsayilanRolleriOlustur(List<Yetki> tumYetkiler) {
        List<Rol> list = new ArrayList<>();

        // ADMIN: tüm yetkiler. USER: operasyonel kapsam (tüm modüllerin
        // READ/WRITE'i). Silme ve dışa aktarma yetkileri kasıtlı olarak
        // verilmez; yetkiler yalnızca YetkiYonetimi ekranından atanır.
        // Bu kurgu canlı veri ile (USER 19/38) örtüşür, böylece yeni kurulumlar
        // da aynı en az yetki ilkesiyle başlar.
        Set<Yetki> adminYetkileri = new HashSet<>(tumYetkiler);
        Set<Yetki> userYetkileri = tumYetkiler.stream()
                .filter(y -> y.getKod().endsWith("_READ") || y.getKod().endsWith("_WRITE"))
                .collect(Collectors.toCollection(HashSet::new));

        list.add(Rol.builder().ad("ADMIN").aciklama("Tam Yetkili Sistem Yöneticisi").yetkiler(adminYetkileri).build());
        list.add(Rol.builder().ad("USER").aciklama("Standart Kullanıcı").yetkiler(userYetkileri).build());

        // Saha ve şoför rolleri operasyonel akışla uyumlu en az yetkiyle kurulur.
        for (Map.Entry<String, String[]> tanim : EKSIK_ROL_TANIMLARI.entrySet()) {
            String[] degerler = tanim.getValue();
            Set<Yetki> secili = tumYetkiler.stream()
                    .filter(y -> Arrays.asList(degerler).contains(y.getKod()))
                    .collect(Collectors.toCollection(HashSet::new));
            list.add(Rol.builder().ad(tanim.getKey()).aciklama(degerler[degerler.length - 1]).yetkiler(secili).build());
        }

        return rolRepository.saveAll(list);
    }
}