package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.IpWhitelist;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.IpWhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IP beyaz listesi: sirket (tenant) bazli, veritabaninda kalici.
 * Onceden AnomaliTespitEngine icinde bellekte tutuluyordu (ortak ve uçucu).
 */
@Service
@RequiredArgsConstructor
public class IpWhitelistService {

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
        String ip = entry != null && entry.get("ipAdresi") != null ? String.valueOf(entry.get("ipAdresi")).trim() : null;
        if (ip == null || ip.isEmpty()) throw new BusinessException("IP adresi zorunludur");
        IpWhitelist kayit = IpWhitelist.builder()
                .sirketId(sirketId)
                .ipAdresi(ip)
                .aciklama(entry.get("aciklama") != null ? String.valueOf(entry.get("aciklama")) : null)
                .durum(entry.get("durum") != null ? String.valueOf(entry.get("durum")) : "AKTIF")
                .eklemeTarihi(LocalDate.now())
                .build();
        ipWhitelistRepository.save(kayit);
        return listele(sirketId);
    }

    @Transactional
    public List<Map<String, Object>> sil(Long sirketId, Long id) {
        ipWhitelistRepository.findById(id)
                .filter(w -> sirketId != null && sirketId.equals(w.getSirketId()))
                .ifPresent(ipWhitelistRepository::delete);
        return listele(sirketId);
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
