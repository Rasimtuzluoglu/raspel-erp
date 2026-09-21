package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.AdresDefteriDTO;
import com.raspel.erp.entity.sistem.AdresDefteri;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.AdresDefteriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdresDefteriService {

    private final AdresDefteriRepository adresDefteriRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<AdresDefteriDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        return adresDefteriRepository.findBySirketIdOrderByAdAsc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public Page<AdresDefteriDTO> filtrele(Long sirketId, String q, String tur, String etiket, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        return adresDefteriRepository.filtreli(sirketId, bosuNullYap(q), bosuNullYap(tur), bosuNullYap(etiket), pageable)
                .map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public AdresDefteriDTO getir(Long id) {
        AdresDefteri a = adresDefteriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdresDefteri", id));
        tenantChecker.check(a.getSirketId(), "AdresDefteri");
        return entityToDTO(a);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public AdresDefteriDTO olustur(AdresDefteriDTO dto, Long sirketId) {
        AdresDefteri a = AdresDefteri.builder()
                .sirketId(sirketId)
                .ad(trim(dto.getAd()))
                .tur(trim(dto.getTur()))
                .telefon(trim(dto.getTelefon()))
                .email(trim(dto.getEmail()))
                .adres(trim(dto.getAdres()))
                .etiketler(normalizeEtiketler(dto.getEtiketler()))
                .notlar(dto.getNotlar())
                .build();
        return entityToDTO(adresDefteriRepository.save(a));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public AdresDefteriDTO guncelle(Long id, AdresDefteriDTO dto) {
        AdresDefteri a = adresDefteriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdresDefteri", id));
        tenantChecker.check(a.getSirketId(), "AdresDefteri");
        a.setAd(trim(dto.getAd()));
        a.setTur(trim(dto.getTur()));
        a.setTelefon(trim(dto.getTelefon()));
        a.setEmail(trim(dto.getEmail()));
        a.setAdres(trim(dto.getAdres()));
        a.setEtiketler(normalizeEtiketler(dto.getEtiketler()));
        a.setNotlar(dto.getNotlar());
        return entityToDTO(adresDefteriRepository.save(a));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        AdresDefteri a = adresDefteriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdresDefteri", id));
        tenantChecker.check(a.getSirketId(), "AdresDefteri");
        adresDefteriRepository.delete(a);
    }

    @Cacheable(value = "lookup", sync = true, key = "'adresTur:' + #sirketId")
    @Transactional(readOnly = true)
    public List<String> turListesi(Long sirketId) {
        if (sirketId == null) return List.of();
        return adresDefteriRepository.turListesi(sirketId);
    }

    @Cacheable(value = "lookup", sync = true, key = "'adresEtiket:' + #sirketId")
    @Transactional(readOnly = true)
    public List<String> etiketListesi(Long sirketId) {
        if (sirketId == null) return List.of();
        return adresDefteriRepository.etiketSatirlari(sirketId).stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private AdresDefteriDTO entityToDTO(AdresDefteri a) {
        return AdresDefteriDTO.builder()
                .id(a.getId()).ad(a.getAd()).tur(a.getTur()).telefon(a.getTelefon())
                .email(a.getEmail()).adres(a.getAdres()).etiketler(a.getEtiketler()).notlar(a.getNotlar())
                .olusturmaTarihi(a.getOlusturmaTarihi()).guncellemeTarihi(a.getGuncellemeTarihi())
                .build();
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String bosuNullYap(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Etiketleri virgülle ayırır, kırpar, tekrarları atar ve ", " ile birleştirir. */
    private static String normalizeEtiketler(String etiketler) {
        if (etiketler == null || etiketler.isBlank()) return null;
        List<String> temiz = Arrays.stream(etiketler.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return temiz.isEmpty() ? null : String.join(", ", temiz);
    }
}
