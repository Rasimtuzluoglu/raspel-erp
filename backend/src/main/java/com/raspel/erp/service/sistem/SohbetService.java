package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AISorguSonucDTO;
import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SohbetService {

    private final SohbetMesajRepository sohbetMesajRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FaturaRepository faturaRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final KasaRepository kasaRepository;
    private final BankaRepository bankaRepository;

    @Transactional(readOnly = true)
    public List<SohbetMesajDTO> sonMesajlar(Long sirketId) {
        List<SohbetMesaj> mesajlar = sohbetMesajRepository.findTop50BySirketIdOrderByOlusturmaTarihiDesc(sirketId);
        java.util.Collections.reverse(mesajlar);
        return mesajlar.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public SohbetMesajDTO mesajGonder(SohbetMesajDTO dto, Long sirketId, Long kullaniciId, String kullaniciAd) {
        if (dto.getMesaj() == null || dto.getMesaj().isBlank()) {
            throw new com.raspel.erp.exception.BusinessException("Mesaj boş olamaz");
        }
        SohbetMesaj mesaj = SohbetMesaj.builder()
                .sirketId(sirketId)
                .kullaniciId(kullaniciId)
                .kullaniciAd(kullaniciAd)
                .mesaj(dto.getMesaj().trim())
                .build();
        mesaj = sohbetMesajRepository.save(mesaj);
        SohbetMesajDTO dtoKayit = toDTO(mesaj);

        try {
            if (sirketId != null) {
                messagingTemplate.convertAndSend("/topic/sohbet/" + sirketId, dtoKayit);
            }
        } catch (Exception e) {
            log.warn("Sohbet mesajı yayınlanamadı: {}", e.getMessage());
        }
        return dtoKayit;
    }

    @Transactional(readOnly = true)
    public AISorguSonucDTO aiSorgula(String soru, Long sirketId) {
        if (soru == null || soru.isBlank()) {
            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni("Lütfen sormak istediğiniz soruyu yazın.")
                    .grafikTipi("none")
                    .build();
        }

        String temizSoru = soru.toLowerCase(Locale.forLanguageTag("tr"));

        // 1. Ciro & En Çok Satış Yapılan Müşteriler
        if (temizSoru.contains("ciro") || temizSoru.contains("müşteri") || temizSoru.contains("musteri") || temizSoru.contains("satış") || temizSoru.contains("satis")) {
            List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                    .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                    .collect(Collectors.toList());

            Map<String, BigDecimal> cariCiro = new HashMap<>();
            for (Fatura f : faturalar) {
                String cariAd = f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel Satış";
                BigDecimal tutar = f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO;
                cariCiro.merge(cariAd, tutar, BigDecimal::add);
            }

            List<Map.Entry<String, BigDecimal>> sirali = cariCiro.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .collect(Collectors.toList());

            List<String> labels = sirali.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            List<BigDecimal> data = sirali.stream().map(Map.Entry::getValue).collect(Collectors.toList());

            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", "Toplam Satış (TL)");
            dataset.put("data", data);
            dataset.put("backgroundColor", List.of("#3b82f6", "#10b981", "#8b5cf6", "#f59e0b", "#06b6d4"));

            Map<String, Object> grafik = new HashMap<>();
            grafik.put("labels", labels);
            grafik.put("datasets", List.of(dataset));

            List<Map<String, Object>> tablo = new ArrayList<>();
            for (int i = 0; i < sirali.size(); i++) {
                tablo.add(Map.of(
                        "sira", i + 1,
                        "musteri", sirali.get(i).getKey(),
                        "ciro", sirali.get(i).getValue() + " ₺"
                ));
            }

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("En yüksek ciro yapılan ilk %d müşteri listelendi. Toplam ciro dağılımı grafikte gösterilmektedir.", sirali.size()))
                    .grafikTipi("bar")
                    .grafikVerisi(grafik)
                    .tabloVerisi(tablo)
                    .intent("CIRO_MUSTERI")
                    .build();
        }

        // 2. Vadesi Gelen Ödemeler & Tahsilatlar
        if (temizSoru.contains("vade") || temizSoru.contains("ödeme") || temizSoru.contains("odeme") || temizSoru.contains("borç") || temizSoru.contains("alacak")) {
            List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                    .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                    .collect(Collectors.toList());

            LocalDate bugun = LocalDate.now();
            LocalDate gelecekHafta = bugun.plusDays(15);

            List<Map<String, Object>> tablo = new ArrayList<>();
            BigDecimal toplamAlacak = BigDecimal.ZERO;
            BigDecimal toplamBorc = BigDecimal.ZERO;

            for (Fatura f : faturalar) {
                LocalDate vade = f.getVadeTarihi() != null ? f.getVadeTarihi() : f.getTarih();
                if (vade != null && !vade.isBefore(bugun) && !vade.isAfter(gelecekHafta)) {
                    String tip = f.getTur() == Fatura.FaturaTur.SATIS ? "Tahsilat (Giriş)" : "Ödeme (Çıkış)";
                    BigDecimal tutar = f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO;
                    if (f.getTur() == Fatura.FaturaTur.SATIS) toplamAlacak = toplamAlacak.add(tutar);
                    else toplamBorc = toplamBorc.add(tutar);

                    tablo.add(Map.of(
                            "faturaNo", f.getFaturaNumarasi() != null ? f.getFaturaNumarasi() : ("#" + f.getId()),
                            "cari", f.getCariHesap() != null ? f.getCariHesap().getAd() : "-",
                            "vade", vade.toString(),
                            "tur", tip,
                            "tutar", tutar + " ₺"
                    ));
                }
            }

            Map<String, Object> grafik = Map.of(
                    "labels", List.of("Beklenen Tahsilatlar", "Yaklaşan Ödemeler"),
                    "datasets", List.of(Map.of(
                            "data", List.of(toplamAlacak, toplamBorc),
                            "backgroundColor", List.of("#10b981", "#ef4444")
                    ))
            );

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("Gelecek 15 gün içinde %s TL tahsilat ve %s TL ödeme vadesi bulunmaktadır.", toplamAlacak, toplamBorc))
                    .grafikTipi("doughnut")
                    .grafikVerisi(grafik)
                    .tabloVerisi(tablo)
                    .intent("VADESI_GELEN")
                    .build();
        }

        // 3. Kasa, Banka ve Likidite Durumu
        if (temizSoru.contains("kasa") || temizSoru.contains("banka") || temizSoru.contains("bakiye") || temizSoru.contains("likidite") || temizSoru.contains("para")) {
            List<Kasa> kasalar = kasaRepository.findBySirketIdOrderByAd(sirketId);
            List<Banka> bankalar = bankaRepository.findBySirketIdOrderByAd(sirketId);

            BigDecimal kasaToplam = kasalar.stream().map(k -> k.getBakiye() != null ? k.getBakiye() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bankaToplam = bankalar.stream().map(b -> b.getBakiye() != null ? b.getBakiye() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal genelBakiye = kasaToplam.add(bankaToplam);

            List<Map<String, Object>> tablo = new ArrayList<>();
            kasalar.forEach(k -> tablo.add(Map.of("hesap", k.getAd() + " (Kasa)", "tur", "Kasa", "bakiye", k.getBakiye() + " ₺")));
            bankalar.forEach(b -> tablo.add(Map.of("hesap", b.getAd() + " (Banka)", "tur", "Banka", "bakiye", b.getBakiye() + " ₺")));

            Map<String, Object> grafik = Map.of(
                    "labels", List.of("Kasa Varlıkları", "Banka Hesapları"),
                    "datasets", List.of(Map.of(
                            "data", List.of(kasaToplam, bankaToplam),
                            "backgroundColor", List.of("#f59e0b", "#3b82f6")
                    ))
            );

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("Şirketin toplam likiditesi %s TL'dir (Kasa: %s TL, Banka: %s TL).", genelBakiye, kasaToplam, bankaToplam))
                    .grafikTipi("doughnut")
                    .grafikVerisi(grafik)
                    .tabloVerisi(tablo)
                    .intent("LIKIDITE")
                    .build();
        }

        // 4. Varsayılan / Genel Yanıt
        return AISorguSonucDTO.builder()
                .soru(soru)
                .cevapMetni("Sorunuzu tam olarak anlayamadım. Aşağıdaki gibi soruları deneyebilirsiniz:\n- 'Bu ay en çok ciro yaptığımız müşteriler kimler?'\n- 'Gelecek hafta vadesi gelen ödemelerim neler?'\n- 'Kasa ve banka toplam bakiyemiz nedir?'")
                .grafikTipi("none")
                .intent("GENEL")
                .build();
    }

    private SohbetMesajDTO toDTO(SohbetMesaj m) {
        return SohbetMesajDTO.builder()
                .id(m.getId())
                .sirketId(m.getSirketId())
                .kullaniciId(m.getKullaniciId())
                .kullaniciAd(m.getKullaniciAd())
                .mesaj(m.getMesaj())
                .olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}

