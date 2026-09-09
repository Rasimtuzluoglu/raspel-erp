package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AISorguSonucDTO;
import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.envanter.Stok;
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
    private final AiConfigService aiConfigService;
    private final LlmClientService llmClientService;

    /** Şirket bazlı kısa dönem sohbet belleği (AI asistan çok turlu bağlam). */
    private final Map<Long, java.util.ArrayDeque<String>> sohbetHafizasi = new java.util.concurrent.ConcurrentHashMap<>();

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

        try {
            com.raspel.erp.dto.sistem.AiConfigDTO aiConfig = aiConfigService.getConfig(sirketId);
            if (aiConfig != null && Boolean.TRUE.equals(aiConfig.getAktif()) && !"YAPILANDIRILMADI".equals(aiConfig.getDurum())) {
                String apiKey = aiConfigService.getDecryptedKey(sirketId);
                String veriBaglami = veriBaglamiOlustur(sirketId);
                String systemPrompt = "Sen RasPel ERP sisteminin yapay zeka asistanısın. Aşağıda şirketin güncel özet verileri var. "
                        + "Soruları bu verilere dayanarak, Türkçe, profesyonel ve net cevapla. "
                        + "Sayısal cevaplarda para birimi TL, adet vb. belirt.\n\n"
                        + "ŞİRKET VERİLERİ:\n" + veriBaglami + "\n\n" + hafizaBaglami(sirketId);
                String response = llmClientService.sendQuery(aiConfig.getProvider(), aiConfig.getModel(), apiKey, systemPrompt, soru);
                hafizaEkle(sirketId, soru, response);
                
                return AISorguSonucDTO.builder()
                        .soru(soru)
                        .cevapMetni(response)
                        .grafikTipi("none")
                        .intent("LLM_YANIT")
                        .build();
            }
        } catch (Exception e) {
            log.error("LLM sorgusu basarisiz oldu, kural tabanli sisteme geciliyor: {}", e.getMessage());
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

        // 4. Stok Durumu & Kritik Stoklar
        if (temizSoru.contains("stok") || temizSoru.contains("kritik") || temizSoru.contains("depo") || temizSoru.contains("envanter")) {
            List<Stok> stoklar = stokRepository.findBySirketIdOrderByAd(sirketId);
            List<Map<String, Object>> tablo = new ArrayList<>();
            int kritikSayisi = 0;

            List<Stok> sirali = stoklar.stream()
                    .sorted((a, b) -> {
                        BigDecimal am = a.getMiktar() != null ? a.getMiktar() : BigDecimal.ZERO;
                        BigDecimal bm = b.getMiktar() != null ? b.getMiktar() : BigDecimal.ZERO;
                        return am.compareTo(bm);
                    })
                    .limit(10)
                    .collect(Collectors.toList());

            for (Stok s : sirali) {
                boolean kritik = s.getMinMiktar() != null && s.getMiktar() != null && s.getMiktar().compareTo(s.getMinMiktar()) <= 0;
                if (kritik) kritikSayisi++;
                tablo.add(Map.of(
                        "stok", s.getAd(),
                        "miktar", (s.getMiktar() != null ? s.getMiktar() : BigDecimal.ZERO) + " " + (s.getBirim() != null ? s.getBirim() : "adet"),
                        "durum", kritik ? "Kritik" : "Yeterli"
                ));
            }

            Map<String, Object> grafik = Map.of(
                    "labels", sirali.stream().map(Stok::getAd).collect(Collectors.toList()),
                    "datasets", List.of(Map.of(
                            "data", sirali.stream().map(s -> s.getMiktar() != null ? s.getMiktar() : BigDecimal.ZERO).collect(Collectors.toList()),
                            "backgroundColor", List.of("#f59e0b", "#10b981", "#3b82f6", "#8b5cf6", "#ef4444")
                    ))
            );

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("En düşük stok seviyesine sahip %d ürün listelendi (%d tanesi kritik seviyede).", sirali.size(), kritikSayisi))
                    .grafikTipi("bar")
                    .grafikVerisi(grafik)
                    .tabloVerisi(tablo)
                    .intent("STOK_DURUM")
                    .build();
        }

        // 5. Kârlılık / Kâr Marjı
        if (temizSoru.contains("kâr") || temizSoru.contains("kar") || temizSoru.contains("marj") || temizSoru.contains("kazanç") || temizSoru.contains("karlılık")) {
            List<Stok> stoklar = stokRepository.findBySirketIdOrderByAd(sirketId);

            List<Map<String, Object>> tablo = new ArrayList<>();
            for (Stok s : stoklar) {
                BigDecimal satis = s.getSatisFiyati() != null ? s.getSatisFiyati() : s.getFiyat();
                BigDecimal maliyet = s.getFiyat() != null ? s.getFiyat() : BigDecimal.ZERO;
                BigDecimal marj = satis != null ? satis.subtract(maliyet) : BigDecimal.ZERO;
                BigDecimal marjYuzde = BigDecimal.ZERO;
                if (satis != null && satis.signum() > 0) {
                    marjYuzde = marj.multiply(BigDecimal.valueOf(100)).divide(satis, 2, java.math.RoundingMode.HALF_UP);
                }
                tablo.add(Map.of(
                        "stok", s.getAd(),
                        "maliyet", maliyet + " ₺",
                        "satis", (satis != null ? satis : BigDecimal.ZERO) + " ₺",
                        "marj", marj + " ₺ (%" + marjYuzde + ")"
                ));
            }

            List<Map<String, Object>> enKarlilar = tablo.stream()
                    .sorted((a, b) -> {
                        String am = (String) a.get("marj");
                        String bm = (String) b.get("marj");
                        return bm.compareTo(am);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("%d ürün için kâr marjı hesaplandı. En yüksek marjlı ilk %d ürün aşağıda listelenmiştir.", tablo.size(), enKarlilar.size()))
                    .grafikTipi("none")
                    .tabloVerisi(enKarlilar)
                    .intent("KARLILIK")
                    .build();
        }

        // 6. Varsayılan / Genel Yanıt
        return AISorguSonucDTO.builder()
                .soru(soru)
                .cevapMetni("Sorunuzu tam olarak anlayamadım. Aşağıdaki gibi soruları deneyebilirsiniz:\n- 'Bu ay en çok ciro yaptığımız müşteriler kimler?'\n- 'Gelecek hafta vadesi gelen ödemelerim neler?'\n- 'Kasa ve banka toplam bakiyemiz nedir?'\n- 'Kritik seviyede stoklarım hangileri?'\n- 'En kârlı ürünlerim hangileri?'")
                .grafikTipi("none")
                .intent("GENEL")
                .build();
    }

    /**
     * AI sorgusunu akışlı (streaming) olarak çalıştırır; üretilen her token parçası
     * onToken geri çağrısına iletilir. AI yapılandırılmamışsa kural tabanlı yanıt tek parça halinde iletilir.
     */
    public void aiSorgulaStream(String soru, Long sirketId, java.util.function.Consumer<String> onToken) {
        if (soru == null || soru.isBlank()) {
            onToken.accept("Lütfen sormak istediğiniz soruyu yazın.");
            return;
        }
        try {
            com.raspel.erp.dto.sistem.AiConfigDTO aiConfig = aiConfigService.getConfig(sirketId);
            if (aiConfig != null && Boolean.TRUE.equals(aiConfig.getAktif()) && !"YAPILANDIRILMADI".equals(aiConfig.getDurum())) {
                String apiKey = aiConfigService.getDecryptedKey(sirketId);
                String veriBaglami = veriBaglamiOlustur(sirketId);
                String systemPrompt = "Sen RasPel ERP sisteminin yapay zeka asistanısın. Aşağıda şirketin güncel özet verileri var. "
                        + "Soruları bu verilere dayanarak, Türkçe, profesyonel ve net cevapla. "
                        + "Sayısal cevaplarda para birimi TL, adet vb. belirt.\n\n"
                        + "ŞİRKET VERİLERİ:\n" + veriBaglami + "\n\n" + hafizaBaglami(sirketId);
                StringBuilder toplanan = new StringBuilder();
                llmClientService.streamQuery(aiConfig.getProvider(), aiConfig.getModel(), apiKey, systemPrompt, soru, token -> {
                    toplanan.append(token);
                    onToken.accept(token);
                });
                hafizaEkle(sirketId, soru, toplanan.toString());
                return;
            }
        } catch (Exception e) {
            log.error("LLM akış sorgusu başarısız oldu, kural tabanlı sisteme geçiliyor: {}", e.getMessage());
        }
        // Kural tabanlı yanıtı tek parça olarak ilet.
        String cevap = aiSorgula(soru, sirketId).getCevapMetni();
        onToken.accept(cevap);
    }

    private String hafizaBaglami(Long sirketId) {
        java.util.ArrayDeque<String> q = sohbetHafizasi.get(sirketId);
        if (q == null || q.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("ÖNCEKİ KONUŞMA:\n");
        q.forEach(s -> sb.append(s).append("\n"));
        return sb.toString();
    }

    private void hafizaEkle(Long sirketId, String soru, String cevap) {
        if (sirketId == null) return;
        java.util.ArrayDeque<String> q = sohbetHafizasi.computeIfAbsent(sirketId, k -> new java.util.ArrayDeque<>());
        q.addLast("Kullanıcı: " + soru);
        q.addLast("Asistan: " + (cevap != null ? cevap : ""));
        while (q.size() > 10) q.removeFirst();
    }

    /** Şirketin güncel özet verisini LLM'e bağlam olarak üretir. */
    private String veriBaglamiOlustur(Long sirketId) {
        try {
            StringBuilder sb = new StringBuilder();

            List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                    .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                    .collect(Collectors.toList());
            int satisSayisi = (int) faturalar.stream().filter(f -> f.getTur() == Fatura.FaturaTur.SATIS).count();
            BigDecimal satisToplam = faturalar.stream().filter(f -> f.getTur() == Fatura.FaturaTur.SATIS)
                    .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Kasa> kasalar = kasaRepository.findBySirketIdOrderByAd(sirketId);
            List<Banka> bankalar = bankaRepository.findBySirketIdOrderByAd(sirketId);
            BigDecimal kasaToplam = kasalar.stream().map(k -> k.getBakiye() != null ? k.getBakiye() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bankaToplam = bankalar.stream().map(b -> b.getBakiye() != null ? b.getBakiye() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

            long stokSayisi = stokRepository.countBySirketId(sirketId);
            long kritikStok = stokRepository.countKritikStokBySirketId(sirketId);

            long cariSayisi = cariHesapRepository.countBySirketId(sirketId);

            sb.append("- Kesilmiş satış faturası sayısı: ").append(satisSayisi).append("\n");
            sb.append("- Toplam satış cirosu: ").append(satisToplam).append(" TL\n");
            sb.append("- Kasa toplamı: ").append(kasaToplam).append(" TL\n");
            sb.append("- Banka toplamı: ").append(bankaToplam).append(" TL\n");
            sb.append("- Toplam ürün sayısı: ").append(stokSayisi).append("\n");
            sb.append("- Kritik stok seviyesindeki ürün sayısı: ").append(kritikStok).append("\n");
            sb.append("- Toplam cari hesap sayısı: ").append(cariSayisi).append("\n");

            return sb.toString();
        } catch (Exception e) {
            log.warn("AI veri bağlamı oluşturulamadı: {}", e.getMessage());
            return "Veri bağlamı oluşturulamadı.";
        }
    }

    /**
     * Görüntüdeki fatura/fişi OCR ile okur. Görüntüyü LLM vision'a gönderip
     * yapılandırılmış metin (fatura kalemleri) döndürür.
     */
    public String aiOcrOku(String base64Image, String mimeType, Long sirketId) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new com.raspel.erp.exception.BusinessException("Görüntü verisi boş olamaz");
        }
        try {
            com.raspel.erp.dto.sistem.AiConfigDTO aiConfig = aiConfigService.getConfig(sirketId);
            if (aiConfig == null || !Boolean.TRUE.equals(aiConfig.getAktif()) || "YAPILANDIRILMADI".equals(aiConfig.getDurum())) {
                throw new com.raspel.erp.exception.BusinessException("OCR için önce Yapay Zeka (AI) yapılandırmasını tamamlayın");
            }
            String apiKey = aiConfigService.getDecryptedKey(sirketId);
            String systemPrompt = "Sen bir fatura/fiş okuma (OCR) uzmanısın. Görüntüdeki faturayı analiz et ve yalnızca JSON formatında dön. "
                    + "JSON şeması: {\"faturaNo\":\"...\", \"tarih\":\"...\", \"cari\":\"...\", \"kalemler\":[{\"ad\":\"ürün adı\", \"adet\":1, \"birimFiyat\":0.00, \"kdv\":0}] , \"genelToplam\":0.00}. "
                    + "Başka hiçbir açıklama ekleme, yalnızca JSON döndür.";
            return llmClientService.sendVisionQuery(aiConfig.getProvider(), aiConfig.getModel(), apiKey,
                    systemPrompt, "Aşağıdaki faturayı oku ve JSON çıktısı ver.", base64Image, mimeType);
        } catch (com.raspel.erp.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OCR okuma başarısız: {}", e.getMessage());
            throw new com.raspel.erp.exception.BusinessException("Fatura okunamadı: " + e.getMessage());
        }
    }

    private SohbetMesajDTO toDTO(SohbetMesaj m) {
        return SohbetMesajDTO.builder()
                .id(m.getId())
                .sirketId(m.getSirketId())
                .kullaniciId(m.getKullaniciId())
                .kullaniciAd(m.getKullaniciAd())
                .odaId(m.getOdaId())
                .mesaj(m.getMesaj())
                .olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}

