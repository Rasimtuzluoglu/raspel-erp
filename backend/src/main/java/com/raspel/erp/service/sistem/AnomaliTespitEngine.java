package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Kasa;

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

        mukerrerFaturaKontrol(sirketId, anomaliler);
        mukerrerHareketKontrol(sirketId, anomaliler);
        anormalYuksekTutarKontrol(sirketId, anomaliler);

        log.info("Akıllı Anomali Taraması Tamamlandı - Toplam {} şüpheli durum tespit edildi. SirketId: {}", anomaliler.size(), sirketId);
        return anomaliler;
    }

    private void mukerrerFaturaKontrol(Long sirketId, List<AnomaliDTO> list) {
        List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, Pageable.unpaged()).getContent();
        
        Map<String, List<Fatura>> gruplar = faturalar.stream()
                .filter(f -> f.getCariHesap() != null && f.getGenelToplam() != null)
                .collect(Collectors.groupingBy(f -> f.getCariHesap().getId() + "_" + f.getGenelToplam().stripTrailingZeros().toPlainString()));

        for (Map.Entry<String, List<Fatura>> entry : gruplar.entrySet()) {
            if (entry.getValue().size() > 1) {
                Fatura ilk = entry.getValue().get(0);
                list.add(AnomaliDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .tur("MUKERRER_FATURA")
                        .seviye("YUKSEK")
                        .baslik("Mükerrer Fatura Şüphesi")
                        .aciklama(String.format("%s cari hesabı için aynı tutarda (%s TL) %d adet fatura kesilmiş.", ilk.getCariHesap().getAd(), ilk.getGenelToplam(), entry.getValue().size()))
                        .ilgiliKayitId(ilk.getId())
                        .oneri("Faturaların ayrıntılarını ve fatura numaralarını kontrol ederek mükerrer kaydı iptal ediniz.")
                        .tespitTarihi(LocalDateTime.now())
                        .build());
            }
        }
    }

    private void mukerrerHareketKontrol(Long sirketId, List<AnomaliDTO> list) {
        List<Hareket> hareketler = hareketRepository.findBySirketIdOrderByHareketTarihiDesc(sirketId, Pageable.unpaged()).getContent();
        Map<String, List<Hareket>> gruplar = hareketler.stream()
                .filter(h -> h.getCariHesap() != null && h.getTutar() != null)
                .collect(Collectors.groupingBy(h -> h.getCariHesap().getId() + "_" + h.getTutar().stripTrailingZeros().toPlainString() + "_" + h.getTur()));

        for (Map.Entry<String, List<Hareket>> entry : gruplar.entrySet()) {
            if (entry.getValue().size() > 1) {
                Hareket ilk = entry.getValue().get(0);
                list.add(AnomaliDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .tur("MUKERRER_ODEME")
                        .seviye("YUKSEK")
                        .baslik("Mükerrer Ödeme/Tahsilat Şüphesi")
                        .aciklama(String.format("Cari #%s için %s türünde aynı tutarda (%s TL) %d adet hareket kaydı var.", ilk.getCariHesap().getAd(), ilk.getTur(), ilk.getTutar(), entry.getValue().size()))
                        .ilgiliKayitId(ilk.getId())
                        .oneri("Kasa/Banka hesap ekstrelerini karşılaştırarak mükerrer finans hareketini siliniz.")
                        .tespitTarihi(LocalDateTime.now())
                        .build());
            }
        }
    }

    private void anormalYuksekTutarKontrol(Long sirketId, List<AnomaliDTO> list) {
        List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, Pageable.unpaged()).getContent();
        BigDecimal esik = BigDecimal.valueOf(50000);

        for (Fatura f : faturalar) {
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