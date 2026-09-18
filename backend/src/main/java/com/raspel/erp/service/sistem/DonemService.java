package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.DonemDTO;
import com.raspel.erp.entity.sistem.Donem;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.DonemRepository;
import com.raspel.erp.repository.sistem.DonemKapanisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DonemService {

    private final DonemRepository donemRepository;
    private final DonemKapanisRepository donemKapanisRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<DonemDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        return donemRepository.findBySirketIdOrderByBaslangicDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Cacheable(value = "lookup", key = "'donemSirket:' + #sirketId")
    @Transactional(readOnly = true)
    public List<DonemDTO> sirketeGoreGetir(Long sirketId) {
        return donemRepository.findBySirketIdOrderByBaslangicDesc(sirketId, Pageable.unpaged()).map(this::entityToDTO).getContent();
    }

    @Cacheable(value = "lookup", key = "'donemAktif:' + #sirketId")
    @Transactional(readOnly = true)
    public List<DonemDTO> aktifDonemler(Long sirketId) {
        return donemRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Cacheable(value = "lookup", key = "T(com.raspel.erp.config.TenantChecker).tenantKey('donemId:' + #id)")
    @Transactional(readOnly = true)
    public DonemDTO getir(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        return entityToDTO(d);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO olustur(DonemDTO dto) {
        if (dto.getSirketId() == null) {
            dto.setSirketId(tenantChecker.getCurrentSirketId());
        }
        if (dto.getSirketId() == null) {
            throw new com.raspel.erp.exception.BusinessException("Şirket bilgisi zorunludur");
        }
        tenantChecker.checkSirketId(dto.getSirketId(), "Dönem");
        boolean aktif = dto.getAktif() == null || dto.getAktif();
        if (aktif) {
            digerleriniPasifle(dto.getSirketId(), null);
        }
        Donem d = Donem.builder()
                .sirketId(dto.getSirketId())
                .ad(dto.getAd())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .aktif(aktif)
                .build();
        return entityToDTO(donemRepository.save(d));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO guncelle(Long id, DonemDTO dto) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        if (dto.getAd() != null) d.setAd(dto.getAd());
        if (dto.getBaslangic() != null) d.setBaslangic(dto.getBaslangic());
        if (dto.getBitis() != null) d.setBitis(dto.getBitis());
        if (Boolean.TRUE.equals(dto.getAktif())) {
            digerleriniPasifle(d.getSirketId(), id);
            d.setAktif(true);
        } else if (Boolean.FALSE.equals(dto.getAktif())) {
            d.setAktif(false);
        }
        return entityToDTO(donemRepository.save(d));
    }

    /** Belirtilen dönemi aktif yapar ve aynı şirketteki diğer dönemleri pasifleştirir. */
    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO aktifYap(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        digerleriniPasifle(d.getSirketId(), id);
        d.setAktif(true);
        return entityToDTO(donemRepository.save(d));
    }

    /** Aynı şirketteki, hariç tutulan id dışındaki aktif dönemleri pasifleştirir (tek aktif kuralı). */
    private void digerleriniPasifle(Long sirketId, Long haricId) {
        java.util.List<Donem> aktifler = donemRepository.findBySirketIdAndAktifTrue(sirketId);
        if (aktifler == null) return;
        for (Donem d : aktifler) {
            if (haricId != null && haricId.equals(d.getId())) continue;
            d.setAktif(false);
            donemRepository.save(d);
        }
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        if (Boolean.TRUE.equals(d.getKilitli())) {
            throw new BusinessException("Kilitli dönem silinemez. Önce kilidi açın.");
        }
        donemRepository.deleteById(id);
    }

    /**
     * Verilen tarihin kilitli bir döneme denk gelip gelmediğini kontrol eder.
     * Belge oluşturma/güncelleme işlemlerinde çağrılır.
     */
    @Transactional(readOnly = true)
    public boolean tarihKilitliMi(Long sirketId, LocalDate tarih) {
        if (sirketId == null || tarih == null) return false;
        List<Donem> kilitliler = donemRepository.findBySirketIdAndKilitliTrue(sirketId);
        return kilitliler.stream()
                .anyMatch(d -> !tarih.isBefore(d.getBaslangic()) && !tarih.isAfter(d.getBitis()));
    }

    /** Tarih kilitliyse açık hata fırlatır. */
    @Transactional(readOnly = true)
    public void kilitKontrol(Long sirketId, LocalDate tarih, String islemAdi) {
        if (tarihKilitliMi(sirketId, tarih)) {
            throw new BusinessException("Bu tarih kilitli bir döneme ait olduğu için " + islemAdi + " yapılamaz.");
        }
    }

    /** Dönemi kilitler; kilitli dönemdeki belgeler değiştirilemez. */
    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO kilitle(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        d.setKilitli(true);
        d.setKilitTarihi(LocalDateTime.now());
        d.setKilitKullaniciId(tenantChecker.getCurrentKullaniciId());
        return entityToDTO(donemRepository.save(d));
    }

    /** Dönemin kilidini açar (yalnızca ADMIN controller seviyesinde kısıtlanır). */
    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO kilidiAc(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        d.setKilitli(false);
        d.setKilitTarihi(null);
        d.setKilitKullaniciId(null);
        return entityToDTO(donemRepository.save(d));
    }

    /**
     * Yıl sonu kapanışı: ilgili mali yılı kapsayan dönemleri kilitler ve kapanış
     * özetini kalıcı olarak kaydeder. Aynı yıl için ikinci kez kapatılamaz.
     */
    @CacheEvict(value = "lookup", allEntries = true)
    public com.raspel.erp.dto.sistem.DonemKapanisDTO yilSonuKapat(Long sirketId, Integer yil,
                                                                  String ozet, Long kullaniciId) {
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi zorunludur");
        }
        tenantChecker.checkSirketId(sirketId, "Dönem");
        if (yil == null) {
            throw new BusinessException("Mali yıl zorunludur");
        }
        if (donemKapanisRepository.findBySirketIdAndYil(sirketId, yil).isPresent()) {
            throw new BusinessException(yil + " mali yılı zaten kapatılmış.");
        }

        LocalDate bas = LocalDate.of(yil, 1, 1);
        LocalDate bit = LocalDate.of(yil, 12, 31);

        List<Donem> kapsanan = donemRepository.findBySirketIdOrderByBaslangicDesc(sirketId, Pageable.unpaged())
                .getContent().stream()
                .filter(d -> !d.getBitis().isBefore(bas) && !d.getBaslangic().isAfter(bit))
                .collect(Collectors.toList());

        if (kapsanan.isEmpty()) {
            throw new BusinessException(yil + " mali yılına ait dönem bulunamadı. Önce dönem tanımlayın.");
        }

        Long kilitKullaniciId = kullaniciId != null ? kullaniciId : tenantChecker.getCurrentKullaniciId();
        for (Donem d : kapsanan) {
            d.setKilitli(true);
            d.setKilitTarihi(LocalDateTime.now());
            d.setKilitKullaniciId(kilitKullaniciId);
            donemRepository.save(d);
        }

        com.raspel.erp.entity.sistem.DonemKapanis kapanis =
                com.raspel.erp.entity.sistem.DonemKapanis.builder()
                        .sirketId(sirketId)
                        .donemId(kapsanan.size() == 1 ? kapsanan.get(0).getId() : null)
                        .yil(yil)
                        .kullaniciId(kilitKullaniciId)
                        .ozet(ozet)
                        .build();
        com.raspel.erp.entity.sistem.DonemKapanis kaydedilen = donemKapanisRepository.save(kapanis);

        return com.raspel.erp.dto.sistem.DonemKapanisDTO.builder()
                .id(kaydedilen.getId())
                .sirketId(kaydedilen.getSirketId())
                .donemId(kaydedilen.getDonemId())
                .yil(kaydedilen.getYil())
                .kapanisTarihi(kaydedilen.getKapanisTarihi())
                .kullaniciId(kaydedilen.getKullaniciId())
                .ozet(kaydedilen.getOzet())
                .kilitlenenDonemSayisi(kapsanan.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.sistem.DonemKapanisDTO> kapanislariGetir(Long sirketId) {
        if (sirketId == null) return List.of();
        return donemKapanisRepository.findBySirketIdOrderByYilDesc(sirketId).stream()
                .map(k -> com.raspel.erp.dto.sistem.DonemKapanisDTO.builder()
                        .id(k.getId()).sirketId(k.getSirketId()).donemId(k.getDonemId())
                        .yil(k.getYil()).kapanisTarihi(k.getKapanisTarihi())
                        .kullaniciId(k.getKullaniciId()).ozet(k.getOzet())
                        .build())
                .collect(Collectors.toList());
    }

    private DonemDTO entityToDTO(Donem d) {
        return DonemDTO.builder()
                .id(d.getId()).sirketId(d.getSirketId())
                .ad(d.getAd()).baslangic(d.getBaslangic())
                .bitis(d.getBitis()).aktif(d.getAktif())
                .kilitli(d.getKilitli()).kilitTarihi(d.getKilitTarihi())
                .kilitKullaniciId(d.getKilitKullaniciId())
                .olusturmaTarihi(d.getOlusturmaTarihi())
                .build();
    }
}
