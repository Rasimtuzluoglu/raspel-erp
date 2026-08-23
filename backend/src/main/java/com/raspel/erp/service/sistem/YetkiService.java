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
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.envanter.Stok;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class YetkiService {

    private final RolRepository rolRepository;
    private final YetkiRepository yetkiRepository;

    public List<Yetki> tumYetkileriGetir() {
        List<Yetki> yetkiler = yetkiRepository.findAll();
        if (yetkiler.isEmpty()) {
            yetkiler = varsayilanYetkileriOlustur();
        }
        return yetkiler;
    }

    public List<Rol> tumRolleriGetir() {
        List<Rol> roller = rolRepository.findAll();
        if (roller.isEmpty()) {
            roller = varsayilanRolleriOlustur();
        }
        return roller;
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

        return yetkiRepository.saveAll(list);
    }

    private List<Rol> varsayilanRolleriOlustur() {
        List<Yetki> tumYetkiler = tumYetkileriGetir();
        List<Rol> list = new ArrayList<>();

        list.add(Rol.builder().ad("ADMIN").aciklama("Tam Yetkili Sistem Yöneticisi").yetkiler(new HashSet<>(tumYetkiler)).build());
        list.add(Rol.builder().ad("MUHASEBE").aciklama("Ön Muhasebe ve Finans Yetkilisi").yetkiler(new HashSet<>(tumYetkiler)).build());
        list.add(Rol.builder().ad("SATIS").aciklama("Saha ve Mağaza Satış Temsilcisi").yetkiler(new HashSet<>(tumYetkiler)).build());
        list.add(Rol.builder().ad("DEPO").aciklama("Depo ve Sevkiyat Görevlisi").yetkiler(new HashSet<>(tumYetkiler)).build());
        list.add(Rol.builder().ad("PERSONEL").aciklama("Genel Şirket Personeli").yetkiler(new HashSet<>()).build());

        return rolRepository.saveAll(list);
    }
}