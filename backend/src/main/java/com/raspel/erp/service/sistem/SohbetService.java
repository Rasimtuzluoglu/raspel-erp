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
    private final DosyaDepolamaService dosyaDepolama;

    private static final String DOSYA_KLASOR = "sohbet";
    private static final java.util.Set<String> IZINLI_UZANTILAR = java.util.Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".webp", ".gif", ".txt", ".csv",
            ".doc", ".docx", ".xls", ".xlsx", ".zip");

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
        boolean mesajVar = dto.getMesaj() != null && !dto.getMesaj().isBlank();
        boolean dosyaVar = dto.getDosyaUrl() != null && !dto.getDosyaUrl().isBlank();
        if (!mesajVar && !dosyaVar) {
            throw new com.raspel.erp.exception.BusinessException("Mesaj boş olamaz");
        }
        SohbetMesaj mesaj = SohbetMesaj.builder()
                .sirketId(sirketId)
                .kullaniciId(kullaniciId)
                .kullaniciAd(kullaniciAd)
                .mesaj(mesajVar ? dto.getMesaj().trim() : "")
                .dosyaUrl(dosyaVar ? dto.getDosyaUrl() : null)
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

    /**
     * Genel sohbet mesajını siler. Yalnızca ADMIN rolü erişebilir (controller'da
     * @PreAuthorize ile kısıtlanır). Şirket uyuşmazlığı reddedilir.
     */
    @Transactional
    public void mesajSil(Long mesajId, Long sirketId) {
        SohbetMesaj mesaj = sohbetMesajRepository.findById(mesajId)
                .orElseThrow(() -> new com.raspel.erp.exception.ResourceNotFoundException("Mesaj", mesajId));
        if (sirketId != null && !sirketId.equals(mesaj.getSirketId())) {
            throw new com.raspel.erp.exception.BusinessException("Bu mesaja erişim yetkiniz yok");
        }
        sohbetMesajRepository.delete(mesaj);
        try {
            if (mesaj.getSirketId() != null) {
                messagingTemplate.convertAndSend("/topic/sohbet/" + mesaj.getSirketId() + "/sil", mesajId);
            }
        } catch (Exception e) {
            log.warn("Sohbet silme olayı yayınlanamadı: {}", e.getMessage());
        }
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
        String intent = niyetBelirle(temizSoru);
        LocalDate[] aralik = tarihAraligi(temizSoru);

        // 1. Ciro & En Çok Satış Yapılan Müşteriler
        if ("CIRO_MUSTERI".equals(intent)) {
            List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                    .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                    .filter(f -> aralik == null || (f.getTarih() != null && !f.getTarih().isBefore(aralik[0]) && !f.getTarih().isAfter(aralik[1])))
                    .collect(Collectors.toList());

            Map<String, BigDecimal> cariCiro = new LinkedHashMap<>();
            for (Fatura f : faturalar) {
                String cariAd = f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel Satış";
                BigDecimal tutar = f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO;
                cariCiro.merge(cariAd, tutar, BigDecimal::add);
            }

            List<Map.Entry<String, BigDecimal>> sirali = cariCiro.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(5)
                    .collect(Collectors.toList());

            List<String> labels = sirali.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            List<BigDecimal> data = sirali.stream().map(Map.Entry::getValue).collect(Collectors.toList());

            Map<String, Object> dataset = new LinkedHashMap<>();
            dataset.put("label", "Toplam Satış (TL)");
            dataset.put("data", data);
            dataset.put("backgroundColor", List.of("#3b82f6", "#10b981", "#8b5cf6", "#f59e0b", "#06b6d4"));

            Map<String, Object> grafik = new LinkedHashMap<>();
            grafik.put("labels", labels);
            grafik.put("datasets", List.of(dataset));

            List<Map<String, Object>> tablo = new ArrayList<>();
            for (int i = 0; i < sirali.size(); i++) {
                tablo.add(satir("sira", i + 1, "musteri", sirali.get(i).getKey(), "ciro", paraMetni(sirali.get(i).getValue())));
            }
            BigDecimal toplam = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            String donem = aralik == null ? "" : " (" + aralik[0] + " - " + aralik[1] + ")";

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("En yüksek ciro yapılan ilk %d müşteri listelendi%s. Toplam: %s.", sirali.size(), donem, paraMetni(toplam)))
                    .grafikTipi(sirali.isEmpty() ? "none" : "bar")
                    .grafikVerisi(sirali.isEmpty() ? null : grafik)
                    .tabloVerisi(tablo)
                    .intent("CIRO_MUSTERI")
                    .build();
        }

        // 2. Vadesi Gelen Ödemeler & Tahsilatlar
        if ("VADESI_GELEN".equals(intent)) {
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

                    tablo.add(satir(
                            "faturaNo", f.getFaturaNumarasi() != null ? f.getFaturaNumarasi() : ("#" + f.getId()),
                            "cari", f.getCariHesap() != null ? f.getCariHesap().getAd() : "-",
                            "vade", vade.toString(),
                            "tur", tip,
                            "tutar", paraMetni(tutar)
                    ));
                }
            }
            tablo.sort(Comparator.comparing(m -> String.valueOf(m.get("vade"))));

            Map<String, Object> grafik = new LinkedHashMap<>();
            grafik.put("labels", List.of("Beklenen Tahsilatlar", "Yaklaşan Ödemeler"));
            Map<String, Object> vadeDataset = new LinkedHashMap<>();
            vadeDataset.put("data", List.of(toplamAlacak, toplamBorc));
            vadeDataset.put("backgroundColor", List.of("#10b981", "#ef4444"));
            grafik.put("datasets", List.of(vadeDataset));

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("Gelecek 15 gün içinde %s tahsilat ve %s ödeme vadesi bulunmaktadır.", paraMetni(toplamAlacak), paraMetni(toplamBorc)))
                    .grafikTipi("doughnut")
                    .grafikVerisi(grafik)
                    .tabloVerisi(tablo)
                    .intent("VADESI_GELEN")
                    .build();
        }

        // 3. Kasa, Banka ve Likidite Durumu
        if ("LIKIDITE".equals(intent)) {
            List<Kasa> kasalar = kasaRepository.findBySirketIdOrderByAd(sirketId);
            List<Banka> bankalar = bankaRepository.findBySirketIdOrderByAd(sirketId);

            BigDecimal kasaToplam = kasalar.stream().map(k -> k.getBakiye() != null ? k.getBakiye() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bankaToplam = bankalar.stream().map(b -> b.getBakiye() != null ? b.getBakiye() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal genelBakiye = kasaToplam.add(bankaToplam);

            List<Map<String, Object>> tablo = new ArrayList<>();
            kasalar.forEach(k -> tablo.add(satir("hesap", k.getAd() + " (Kasa)", "tur", "Kasa", "bakiye", paraMetni(k.getBakiye()))));
            bankalar.forEach(b -> tablo.add(satir("hesap", b.getAd() + " (Banka)", "tur", "Banka", "bakiye", paraMetni(b.getBakiye()))));

            Map<String, Object> grafik = new LinkedHashMap<>();
            grafik.put("labels", List.of("Kasa Varlıkları", "Banka Hesapları"));
            Map<String, Object> likDataset = new LinkedHashMap<>();
            likDataset.put("data", List.of(kasaToplam, bankaToplam));
            likDataset.put("backgroundColor", List.of("#f59e0b", "#3b82f6"));
            grafik.put("datasets", List.of(likDataset));

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("Şirketin toplam likiditesi %s'dir (Kasa: %s, Banka: %s).", paraMetni(genelBakiye), paraMetni(kasaToplam), paraMetni(bankaToplam)))
                    .grafikTipi("doughnut")
                    .grafikVerisi(grafik)
                    .tabloVerisi(tablo)
                    .intent("LIKIDITE")
                    .build();
        }

        // 4. Stok Durumu & Kritik Stoklar
        if ("STOK_DURUM".equals(intent)) {
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
                tablo.add(satir(
                        "stok", s.getAd(),
                        "miktar", (s.getMiktar() != null ? s.getMiktar() : BigDecimal.ZERO) + " " + (s.getBirim() != null ? s.getBirim() : "adet"),
                        "durum", kritik ? "Kritik" : "Yeterli"
                ));
            }

            Map<String, Object> grafik = new LinkedHashMap<>();
            grafik.put("labels", sirali.stream().map(Stok::getAd).collect(Collectors.toList()));
            Map<String, Object> stokDataset = new LinkedHashMap<>();
            stokDataset.put("data", sirali.stream().map(s -> s.getMiktar() != null ? s.getMiktar() : BigDecimal.ZERO).collect(Collectors.toList()));
            stokDataset.put("backgroundColor", "#3b82f6");
            grafik.put("datasets", List.of(stokDataset));

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
        if ("KARLILIK".equals(intent)) {
            List<Stok> stoklar = stokRepository.findBySirketIdOrderByAd(sirketId);

            List<Object[]> hesaplanan = new ArrayList<>();
            for (Stok s : stoklar) {
                BigDecimal satis = s.getSatisFiyati() != null ? s.getSatisFiyati() : s.getFiyat();
                BigDecimal maliyet = s.getFiyat() != null ? s.getFiyat() : BigDecimal.ZERO;
                BigDecimal marj = satis != null ? satis.subtract(maliyet) : BigDecimal.ZERO;
                BigDecimal marjYuzde = BigDecimal.ZERO;
                if (satis != null && satis.signum() > 0) {
                    marjYuzde = marj.multiply(BigDecimal.valueOf(100)).divide(satis, 2, java.math.RoundingMode.HALF_UP);
                }
                hesaplanan.add(new Object[]{s.getAd(), maliyet, (satis != null ? satis : BigDecimal.ZERO), marjYuzde});
            }
            // Deterministik sıralama: önce marj (sayısal) azalan, eşitlikte ürün adı artan.
            hesaplanan.sort((a, b) -> {
                int c = ((BigDecimal) b[3]).compareTo((BigDecimal) a[3]);
                return c != 0 ? c : String.valueOf(a[0]).compareTo(String.valueOf(b[0]));
            });

            int toplamUrun = hesaplanan.size();
            List<Map<String, Object>> enKarlilar = new ArrayList<>();
            List<String> chartLabels = new ArrayList<>();
            List<BigDecimal> chartData = new ArrayList<>();
            for (int i = 0; i < hesaplanan.size(); i++) {
                Object[] h = hesaplanan.get(i);
                if (i < 5) {
                    enKarlilar.add(satir("stok", h[0], "maliyet", paraMetni((BigDecimal) h[1]),
                            "satis", paraMetni((BigDecimal) h[2]), "marjYuzde", "%" + h[3]));
                }
                if (i < 10) {
                    chartLabels.add(String.valueOf(h[0]));
                    chartData.add((BigDecimal) h[3]);
                }
            }

            Map<String, Object> grafik = new LinkedHashMap<>();
            grafik.put("labels", chartLabels);
            Map<String, Object> karDataset = new LinkedHashMap<>();
            karDataset.put("data", chartData);
            karDataset.put("backgroundColor", "#10b981");
            grafik.put("datasets", List.of(karDataset));

            return AISorguSonucDTO.builder()
                    .soru(soru)
                    .cevapMetni(String.format("%d ürün için kâr marjı hesaplandı. En yüksek marjlı ilk %d ürün aşağıda listelenmiştir.", toplamUrun, enKarlilar.size()))
                    .grafikTipi(enKarlilar.isEmpty() ? "none" : "bar")
                    .grafikVerisi(enKarlilar.isEmpty() ? null : grafik)
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
            // Sağlayıcı/hata detayı istemciye sızdırılmaz.
            throw new com.raspel.erp.exception.BusinessException("Fatura okunamadı. Lütfen AI yapılandırmasını kontrol edin.");
        }
    }

    /** Genel sohbete dosya/görsel yükler ve erişim URL'ini döndürür. */
    @Transactional(readOnly = true)
    public String dosyaYukle(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.raspel.erp.exception.BusinessException("Dosya boş");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!IZINLI_UZANTILAR.contains(ext)) {
            throw new com.raspel.erp.exception.BusinessException("Bu dosya tipi desteklenmiyor: "
                    + (ext.isBlank() ? "(uzantısız)" : ext));
        }
        try {
            return "/api/uploads/sohbet/" + dosyaDepolama.kaydet(DOSYA_KLASOR, file);
        } catch (java.io.IOException e) {
            throw new com.raspel.erp.exception.BusinessException("Dosya yüklenemedi: " + e.getMessage());
        }
    }

    private static final List<String> KAR_HARIC = List.of("kargo", "karar", "kart", "karşı", "karsi", "karne", "karadeniz");
    private static final List<String> PARA_HARIC = List.of("parametre", "parça", "parca", "param", "paragraf");

    /** Kelime kökü eşleşmesine göre niyet puanı üretir (deterministik). */
    private int puan(String soru, String... kokler) {
        int p = 0;
        for (String token : soru.split("[^\\p{L}\\p{N}]+")) {
            if (token.isEmpty()) continue;
            for (String kok : kokler) {
                if (!token.startsWith(kok)) continue;
                if (("kar".equals(kok) || "kâr".equals(kok)) && KAR_HARIC.contains(token)) continue;
                if ("para".equals(kok) && PARA_HARIC.contains(token)) continue;
                p++;
                break;
            }
        }
        return p;
    }

    /** Soru için niyeti puanlama + sabit öncelik sırasıyla belirler. */
    private String niyetBelirle(String soru) {
        int ciro = puan(soru, "ciro", "müşteri", "musteri", "satı", "satis");
        int vade = puan(soru, "vade", "ödeme", "odeme", "borç", "borc", "alacak");
        int likidite = puan(soru, "kasa", "banka", "bakiye", "likidite", "para");
        int karlilik = puan(soru, "kâr", "kar", "marj", "kazanç", "kazanc", "karlılık", "karlilik");
        int stok = puan(soru, "stok", "kritik", "depo", "envanter");
        int max = Math.max(Math.max(Math.max(ciro, vade), Math.max(likidite, karlilik)), stok);
        if (max == 0) return "GENEL";
        if (ciro == max) return "CIRO_MUSTERI";
        if (vade == max) return "VADESI_GELEN";
        if (likidite == max) return "LIKIDITE";
        if (karlilik == max) return "KARLILIK";
        return "STOK_DURUM";
    }

    /** Sorudaki tarih ifadesinden [baslangic, bitis] üretir; yoksa null. */
    private LocalDate[] tarihAraligi(String soru) {
        LocalDate bugun = LocalDate.now();
        if (soru.contains("bu ay")) {
            return new LocalDate[]{bugun.withDayOfMonth(1), bugun.withDayOfMonth(bugun.lengthOfMonth())};
        }
        if (soru.contains("geçen ay") || soru.contains("gecen ay")) {
            LocalDate gecen = bugun.minusMonths(1);
            return new LocalDate[]{gecen.withDayOfMonth(1), gecen.withDayOfMonth(gecen.lengthOfMonth())};
        }
        if (soru.contains("bu yıl") || soru.contains("bu yil")) {
            return new LocalDate[]{bugun.withDayOfYear(1), bugun.withDayOfYear(bugun.lengthOfYear())};
        }
        if (soru.contains("son 30") || soru.contains("son bir ay") || soru.contains("son 1 ay")) {
            return new LocalDate[]{bugun.minusDays(30), bugun};
        }
        return null;
    }

    /** Sıra korunan (deterministik) tablo satırı üretir. */
    private static Map<String, Object> satir(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    private static String paraMetni(BigDecimal v) {
        BigDecimal d = v != null ? v : BigDecimal.ZERO;
        return d.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " ₺";
    }

    private SohbetMesajDTO toDTO(SohbetMesaj m) {
        return SohbetMesajDTO.builder()
                .id(m.getId())
                .sirketId(m.getSirketId())
                .kullaniciId(m.getKullaniciId())
                .kullaniciAd(m.getKullaniciAd())
                .odaId(m.getOdaId())
                .mesaj(m.getMesaj())
                .dosyaUrl(m.getDosyaUrl())
                .olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}

