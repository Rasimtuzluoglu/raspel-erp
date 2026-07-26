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

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class YetkiService {

    private final RolRepository rolRepository;
    private final YetkiRepository yetkiRepository;

    @Transactional(readOnly = true)
    public List<Yetki> tumYetkileriGetir() {
        List<Yetki> yetkiler = yetkiRepository.findAll();
        if (yetkiler.isEmpty()) {
            yetkiler = varsayilanYetkileriOlustur();
        }
        return yetkiler;
    }

    @Transactional(readOnly = true)
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
        list.add(Yetki.builder().kod("STOK_READ").modul("Stok").aciklama("Stok listesini görüntüleme").build());
        list.add(Yetki.builder().kod("STOK_WRITE").modul("Stok").aciklama("Yeni stok ekleme ve güncelleme").build());
        list.add(Yetki.builder().kod("STOK_DELETE").modul("Stok").aciklama("Stok kaydı silme").build());

        list.add(Yetki.builder().kod("FATURA_READ").modul("Fatura").aciklama("Faturaları görüntüleme").build());
        list.add(Yetki.builder().kod("FATURA_WRITE").modul("Fatura").aciklama("Fatura oluşturma ve düzenleme").build());
        list.add(Yetki.builder().kod("FATURA_DELETE").modul("Fatura").aciklama("Fatura silme ve iptal etme").build());

        list.add(Yetki.builder().kod("CARI_READ").modul("Cari").aciklama("Cari hesapları görüntüleme").build());
        list.add(Yetki.builder().kod("CARI_WRITE").modul("Cari").aciklama("Cari kart ekleme ve düzenleme").build());
        list.add(Yetki.builder().kod("CARI_DELETE").modul("Cari").aciklama("Cari kart silme").build());

        list.add(Yetki.builder().kod("EXPORT_DATA").modul("Sistem").aciklama("Excel/CSV dışa aktarım izni").build());

        return yetkiRepository.saveAll(list);
    }

    private List<Rol> varsayilanRolleriOlustur() {
        List<Yetki> tumYetkiler = tumYetkileriGetir();
        List<Rol> list = new ArrayList<>();

        list.add(Rol.builder().ad("ADMIN").aciklama("Tam Yetkili Sistem Yöneticisi").yetkiler(new HashSet<>(tumYetkiler)).build());
        list.add(Rol.builder().ad("MUHASEBE").aciklama("Ön Muhasebe ve Finans Yetkilisi").yetkiler(new HashSet<>(tumYetkiler)).build());
        list.add(Rol.builder().ad("SATIS").aciklama("Satış Temsilcisi").yetkiler(new HashSet<>()).build());
        list.add(Rol.builder().ad("DEPO").aciklama("Depo Görevlisi").yetkiler(new HashSet<>()).build());

        return rolRepository.saveAll(list);
    }
}
