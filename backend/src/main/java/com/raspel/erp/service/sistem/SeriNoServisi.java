package com.raspel.erp.service.sistem;

import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.TeklifRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Belge numarasi uretimi. Numara, `sistem.seri_sayac` tablosunda tutulan bir sayac
 * uzerinden ATOMIK olarak uretilir (INSERT ... ON CONFLICT DO UPDATE ... RETURNING).
 * Boylece es zamanli istekler (coklu kasa/instance) ayni numarayi uretemez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeriNoServisi {

    private final FaturaRepository faturaRepository;
    private final SiparisRepository siparisRepository;
    private final TeklifRepository teklifRepository;
    private final JdbcTemplate jdbcTemplate;

    public String faturaNoUret(Long sirketId) {
        String prefix = "FTR-" + (sirketId != null ? sirketId + "-" : "") + LocalDate.now().getYear() + "-";
        return sonraki(sirketId, "FATURA", prefix, () -> faturaRepository.findFaturaNumarasiByPrefix(prefix, sirketId));
    }

    public String siparisNoUret(Long sirketId) {
        String prefix = "SIP-" + (sirketId != null ? sirketId + "-" : "") + LocalDate.now().getYear() + "-";
        return sonraki(sirketId, "SIPARIS", prefix, () -> siparisRepository.findSiparisNoByPrefix(prefix, sirketId));
    }

    public String teklifNoUret(Long sirketId) {
        String prefix = "TKL-" + (sirketId != null ? sirketId + "-" : "") + LocalDate.now().getYear() + "-";
        return sonraki(sirketId, "TEKLIF", prefix, () -> teklifRepository.findTeklifNoByPrefix(prefix, sirketId));
    }

    private String sonraki(Long sirketId, String tur, String prefix, Supplier<List<String>> mevcutGetter) {
        // sirketId null ise (teorik) sayac tutulamaz; eski davranisa dus.
        if (sirketId == null) {
            return prefix + String.format("%06d", maxSeri(mevcutGetter.get(), prefix) + 1);
        }
        // Sayac ilk kez olusturulurken mevcut belgelerden tohum degeri hesaplanir;
        // satir zaten varsa agir tarama yapilmaz.
        Integer mevcutDeger = jdbcTemplate.query(
                "SELECT deger FROM sistem.seri_sayac WHERE sirket_id = ? AND tur = ?",
                rs -> rs.next() ? rs.getInt(1) : null, sirketId, tur);
        int tohum = mevcutDeger != null ? 0 : maxSeri(mevcutGetter.get(), prefix) + 1;

        Integer deger = jdbcTemplate.queryForObject(
                "INSERT INTO sistem.seri_sayac (sirket_id, tur, deger) VALUES (?, ?, ?) "
                        + "ON CONFLICT (sirket_id, tur) DO UPDATE SET deger = sistem.seri_sayac.deger + 1 "
                        + "RETURNING deger",
                Integer.class, sirketId, tur, tohum);
        return prefix + String.format("%06d", deger != null ? deger : tohum);
    }

    private int maxSeri(List<String> mevcutlar, String prefix) {
        if (mevcutlar == null) return 0;
        return mevcutlar.stream()
                .mapToInt(no -> {
                    try {
                        return Integer.parseInt(no.substring(prefix.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
    }
}
