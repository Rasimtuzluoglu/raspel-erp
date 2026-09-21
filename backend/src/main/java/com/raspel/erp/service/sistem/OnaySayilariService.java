package com.raspel.erp.service.sistem;

import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sidebar onay sayaçları için hafif toplama servisi.
 * Önceden üç tam liste çekilip istemcide filtreleniyordu (ve sayfalama
 * varsayılanı nedeniyle eksik sayabiliyordu); artık doğrudan COUNT sorguları
 * çalıştırılır.
 */
@Service
@RequiredArgsConstructor
public class OnaySayilariService {

    private final PersonelIzinRepository personelIzinRepository;
    private final SatinalmaTalepRepository satinalmaTalepRepository;
    private final SiparisRepository siparisRepository;

    @Transactional(readOnly = true)
    public Map<String, Long> sayilariGetir(Long sirketId) {
        Map<String, Long> sonuc = new LinkedHashMap<>();
        sonuc.put("izin", personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId));
        sonuc.put("satinalma", satinalmaTalepRepository.countBySirketIdAndDurum(sirketId, "TASLAK"));
        sonuc.put("siparis", siparisRepository.countBySirketIdAndDurum(sirketId, "BEKLIYOR"));
        return sonuc;
    }
}
