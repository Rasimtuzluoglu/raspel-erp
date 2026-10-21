package com.raspel.erp.service.sube;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.entity.sube.Sube;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sube.SubeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SubeService {

    /** Telefon: rakam, boşluk ve + - ( ) karakterlerinden oluşan 7-20 karakter. */
    private static final Pattern TELEFON_DESENI = Pattern.compile("^[0-9+()\\s-]{7,20}$");

    private final SubeRepository subeRepository;
    private final com.raspel.erp.repository.sube.DepoRepository depoRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<SubeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        return subeRepository.findBySirketIdOrderByAdAsc(sirketId, pageable).map(this::entityToDTO);
    }

    /** Şube KPI özeti: toplam/aktif/pasif + şirket depo sayısı. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.sube.SubeOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.sube.SubeOzetDTO.builder().build();
        }
        return com.raspel.erp.dto.sube.SubeOzetDTO.builder()
                .toplam(subeRepository.countBySirketId(sirketId))
                .aktif(subeRepository.countBySirketIdAndAktifTrue(sirketId))
                .pasif(subeRepository.countBySirketIdAndAktifFalse(sirketId))
                .depo(depoRepository.countBySirketId(sirketId))
                .build();
    }

    @Cacheable(value = "lookup", sync = true, key = "'sube:aktif:sirket:' + #sirketId")
    @Transactional(readOnly = true)
    public List<SubeDTO> aktifSubeler(Long sirketId) {
        return subeRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubeDTO getir(Long id) {
        Sube s = getirVeyaHata(id);
        return entityToDTO(s);
    }

    /** Bir şubeye bağlı depolar (şube detay çekmecesi). */
    @Transactional(readOnly = true)
    public List<DepoDTO> depolar(Long subeId) {
        Sube s = getirVeyaHata(subeId);
        return depoRepository.findBySubeIdAndSirketIdOrderByAdAsc(subeId, s.getSirketId()).stream()
                .map(d -> DepoDTO.builder()
                        .id(d.getId()).ad(d.getAd()).adres(d.getAdres())
                        .yetkili(d.getYetkili()).subeId(d.getSubeId())
                        .sirketId(d.getSirketId()).aktif(d.getAktif())
                        .olusturmaTarihi(d.getOlusturmaTarihi()).build())
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SubeDTO olustur(SubeDTO dto) {
        tenantChecker.checkSirketId(dto.getSirketId(), "Şube");
        String ad = adDogrula(dto.getAd());
        if (subeRepository.existsBySirketIdAndAdIgnoreCase(dto.getSirketId(), ad)) {
            throw new BusinessException("Bu isimde bir şube zaten mevcut");
        }
        String telefon = telefonDogrula(dto.getTelefon());
        Sube s = Sube.builder()
                .ad(ad).adres(dto.getAdres())
                .telefon(telefon).yetkili(dto.getYetkili())
                .aktif(dto.getAktif() == null ? Boolean.TRUE : dto.getAktif())
                .sirketId(dto.getSirketId()).build();
        return entityToDTO(subeRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SubeDTO guncelle(Long id, SubeDTO dto) {
        Sube s = getirVeyaHata(id);
        if (dto.getAd() != null) {
            String ad = adDogrula(dto.getAd());
            if (!ad.equalsIgnoreCase(s.getAd())
                    && subeRepository.existsBySirketIdAndAdIgnoreCaseAndIdNot(s.getSirketId(), ad, id)) {
                throw new BusinessException("Bu isimde bir şube zaten mevcut");
            }
            s.setAd(ad);
        }
        if (dto.getAdres() != null) s.setAdres(dto.getAdres());
        if (dto.getTelefon() != null) s.setTelefon(telefonDogrula(dto.getTelefon()));
        if (dto.getYetkili() != null) s.setYetkili(dto.getYetkili());
        if (dto.getAktif() != null) s.setAktif(dto.getAktif());
        return entityToDTO(subeRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Sube s = getirVeyaHata(id);
        if (depoRepository.existsBySubeId(id)) {
            throw new BusinessException("Bu şubeye bağlı depolar var; önce depoları taşıyın veya silin");
        }
        subeRepository.deleteById(s.getId());
    }

    private Sube getirVeyaHata(Long id) {
        Sube s = subeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sube", id));
        tenantChecker.check(s.getSirketId(), "Sube");
        return s;
    }

    /** Şube adı: zorunlu, baş/son boşluk temizlenir. */
    private String adDogrula(String ad) {
        String temiz = ad == null ? null : ad.trim();
        if (temiz == null || temiz.isEmpty()) {
            throw new BusinessException("Şube adı zorunludur");
        }
        return temiz;
    }

    /** Telefon: boş olabilir; doluysa geçerli biçimde olmalı. */
    private String telefonDogrula(String telefon) {
        String temiz = telefon == null ? null : telefon.trim();
        if (temiz == null || temiz.isEmpty()) {
            return null;
        }
        if (!TELEFON_DESENI.matcher(temiz).matches()) {
            throw new BusinessException("Telefon numarası geçersiz (7-20 karakter; rakam, boşluk, + - ( ) )");
        }
        return temiz;
    }

    private SubeDTO entityToDTO(Sube s) {
        return SubeDTO.builder()
                .id(s.getId()).ad(s.getAd()).adres(s.getAdres())
                .telefon(s.getTelefon()).yetkili(s.getYetkili())
                .sirketId(s.getSirketId()).aktif(s.getAktif())
                .olusturmaTarihi(s.getOlusturmaTarihi()).build();
    }
}
