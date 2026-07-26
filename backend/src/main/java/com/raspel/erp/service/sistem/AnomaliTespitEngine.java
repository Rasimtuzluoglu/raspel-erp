package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.repository.FaturaRepository;
import com.raspel.erp.repository.HareketRepository;
import com.raspel.erp.repository.StokHareketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AnomaliTespitEngine {

    private final FaturaRepository faturaRepository;
    private final HareketRepository hareketRepository;
    private final StokHareketRepository stokHareketRepository;

    public List<AnomaliDTO> anomalileriTara(Long sirketId) {
        List<AnomaliDTO> anomaliler = new ArrayList<>();

        // 1. Mükerrer Fatura Tespiti (Aynı cari, aynı tutar ve aynı tarih)
        mukerrerFaturaKONTROL(sirketId, anomaliler);

        // 2. Mükerrer Ödeme/Hareket Tespiti
        mukerrerHareketKontrol(sirketId, anomaliler);

        // 3. Anormal Yüksek Masraf Tespiti (>50.000 TL)
        anormalYuksekTutarKontrol(sirketId, anomaliler);

        log.info("Akıllı Anomali Taraması Tamamlandı - Toplam {} şüpheli durum tespit edildi. SirketId: {}", anomaliler.size(), sirketId);
        return anomaliler;
    }

    private void mukerrerFaturaKONTROL(Long sirketId, List<AnomaliDTO> list) {
        var faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        // Group by (cariHesapId + genelToplam)
        Map<String, List<com.raspel.erp.entity.Fatura>> gruplar = faturalar.stream()
                .filter(f -> f.getCariHesapId() != null && f.getGenelToplam() != null)
                .collect(Collectors.groupingBy(f -> f.getCariHesapId() + "_" + f.getGenelToplam().stripTrailingZeros().toPlainString()));

        for (Map.Entry<String, List<com.raspel.erp.entity.Fatura>> entry : gruplar.entrySet()) {
            if (entry.getValue().size() > 1) {
                var ilk = entry.getValue().get(0);
                list.add(AnomaliDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .tur("MUKERRER_FATURA")
                        .seviye("YUKSEK")
                        .baslik("Mükerrer Fatura Şüphesi")
                        .aciklama(String.format("%s cari hesabı için aynı tutarda (%s TL) %d adet fatura kesilmiş.", ilk.getCariHesapAd(), ilk.getGenelToplam(), entry.getValue().size()))
                        .ilgiliKayitId(ilk.getId())
                        .oneri("Faturaların ayrıntılarını ve fatura numaralarını kontrol ederek mükerrer kaydı iptal ediniz.")
                        .tespitTarihi(LocalDateTime.now())
                        .build());
            }
        }
    }

    private void mukerrerHareketKontrol(Long sirketId, List<AnomaliDTO> list) {
        var hareketler = hareketRepository.findBySirketIdOrderByTarihDesc(sirketId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        Map<String, List<com.raspel.erp.entity.Hareket>> gruplar = hareketler.stream()
                .filter(h -> h.getCariHesapId() != null && h.getTutar() != null)
                .collect(Collectors.groupingBy(h -> h.getCariHesapId() + "_" + h.getTutar().stripTrailingZeros().toPlainString() + "_" + h.getTur()));

        for (Map.Entry<String, List<com.raspel.erp.entity.Hareket>> entry : gruplar.entrySet()) {
            if (entry.getValue().size() > 1) {
                var ilk = entry.getValue().get(0);
                list.add(AnomaliDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .tur("MUKERRER_ODEME")
                        .seviye("YUKSEK")
                        .baslik("Mükerrer Ödeme/Tahsilat Şüphesi")
                        .aciklama(String.format("CariId #%d için %s türünde aynı tutarda (%s TL) %d adet hareket kaydı var.", ilk.getCariHesapId(), ilk.getTur(), ilk.getTutar(), entry.getValue().size()))
                        .ilgiliKayitId(ilk.getId())
                        .oneri("Kasa/Banka hesap ekstrelerini karşılaştırarak mükerrer finans hareketini siliniz.")
                        .tespitTarihi(LocalDateTime.now())
                        .build());
            }
        }
    }

    private void anormalYuksekTutarKontrol(Long sirketId, List<AnomaliDTO> list) {
        var faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        BigDecimal esik = BigDecimal.valueOf(50000);

        for (var f : faturalar) {
            if (f.getGenelToplam() != null && f.getGenelToplam().compareTo(esik) > 0) {
                list.add(AnomaliDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .tur("ANORMAL_MASRAF")
                        .seviye("ORTA")
                        .baslik("Yüksek Tutarlı İşlem Uyarısı")
                        .aciklama(String.format("%s numaralı fatura %s TL tutarı ile yüksek tutarlı işlem eşiğini (%s TL) aşmaktadır.", f.getFaturaNumarasi(), f.getGenelToplam(), esik))
                        .ilgiliKayitId(f.getId())
                        .oneri("Yüksek tutarlı fatura için yönetici onay durumunu kontrol edin.")
                        .tespitTarihi(LocalDateTime.now())
                        .build());
            }
        }
    }
}
