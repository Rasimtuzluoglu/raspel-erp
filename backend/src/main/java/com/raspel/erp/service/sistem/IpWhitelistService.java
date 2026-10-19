package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.IpWhitelist;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.IpWhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * IP beyaz listesi: sirket (tenant) bazli, veritabaninda kalici.
 * Yalnizca IPv4 ve IPv4/CIDR kabul edilir; ayni IP ikinci kez eklenemez.
 */
@Service
@RequiredArgsConstructor
public class IpWhitelistService {

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4_CIDR = Pattern.compile(
            "^" + OCTET + "(\\." + OCTET + "){3}(/(3[0-2]|[12]?\\d))?$");
    private static final Set<String> GECERLI_DURUMLAR = Set.of("AKTIF", "PASIF");

    private final IpWhitelistRepository ipWhitelistRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listele(Long sirketId) {
        if (sirketId == null) return List.of();
        return ipWhitelistRepository.findBySirketIdOrderByIdAsc(sirketId).stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    @Transactional
    public List<Map<String, Object>> ekle(Long sirketId, Map<String, Object> entry) {
        if (sirketId == null) throw new BusinessException("Şirket bağlamı bulunamadı");
        String ip = ipDegeri(entry);
        dogrula(ip);
        if (ipWhitelistRepository.existsBySirketIdAndIpAdresi(sirketId, ip)) {
            throw new BusinessException("Bu IP/CIDR zaten tanımlı: " + ip);
        }
        IpWhitelist kayit = IpWhitelist.builder()
                .sirketId(sirketId)
                .ipAdresi(ip)
                .aciklama(entry.get("aciklama") != null ? String.valueOf(entry.get("aciklama")) : null)
                .durum(durumDegeri(entry, "AKTIF"))
                .eklemeTarihi(LocalDate.now())
                .build();
        ipWhitelistRepository.save(kayit);
        return listele(sirketId);
    }

    @Transactional
    public List<Map<String, Object>> guncelle(Long sirketId, Long id, Map<String, Object> entry) {
        if (sirketId == null) throw new BusinessException("Şirket bağlamı bulunamadı");
        IpWhitelist kayit = ipWhitelistRepository.findByIdAndSirketId(id, sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("IP", id));
        String ip = entry != null && entry.get("ipAdresi") != null
                ? String.valueOf(entry.get("ipAdresi")).trim()
                : kayit.getIpAdresi();
        dogrula(ip);
        if (!ip.equals(kayit.getIpAdresi())
                && ipWhitelistRepository.existsBySirketIdAndIpAdresiAndIdNot(sirketId, ip, id)) {
            throw new BusinessException("Bu IP/CIDR zaten tanımlı: " + ip);
        }
        kayit.setIpAdresi(ip);
        if (entry != null && entry.get("aciklama") != null) {
            kayit.setAciklama(String.valueOf(entry.get("aciklama")));
        }
        if (entry != null && entry.get("durum") != null) {
            kayit.setDurum(durumDegeri(entry, kayit.getDurum()));
        }
        ipWhitelistRepository.save(kayit);
        return listele(sirketId);
    }

    @Transactional
    public List<Map<String, Object>> sil(Long sirketId, Long id) {
        IpWhitelist kayit = ipWhitelistRepository.findByIdAndSirketId(id, sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("IP", id));
        ipWhitelistRepository.delete(kayit);
        return listele(sirketId);
    }

    private String ipDegeri(Map<String, Object> entry) {
        String ip = entry != null && entry.get("ipAdresi") != null
                ? String.valueOf(entry.get("ipAdresi")).trim() : null;
        if (ip == null || ip.isEmpty()) {
            throw new BusinessException("IP adresi zorunludur");
        }
        return ip;
    }

    private void dogrula(String ip) {
        if (ip == null || !IPV4_CIDR.matcher(ip).matches()) {
            throw new BusinessException("Geçersiz IP/CIDR: " + ip);
        }
    }

    private String durumDegeri(Map<String, Object> entry, String varsayilan) {
        if (entry == null || entry.get("durum") == null) return varsayilan;
        String d = String.valueOf(entry.get("durum")).trim().toUpperCase(java.util.Locale.ROOT);
        if (!GECERLI_DURUMLAR.contains(d)) {
            throw new BusinessException("Geçersiz durum: " + entry.get("durum"));
        }
        return d;
    }

    private Map<String, Object> toMap(IpWhitelist w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("ipAdresi", w.getIpAdresi());
        m.put("aciklama", w.getAciklama());
        m.put("durum", w.getDurum());
        m.put("eklemeTarihi", w.getEklemeTarihi() != null ? w.getEklemeTarihi().toString() : null);
        return m;
    }
}
