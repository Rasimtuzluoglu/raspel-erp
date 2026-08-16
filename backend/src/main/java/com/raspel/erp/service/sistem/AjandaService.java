package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.repository.sistem.GorevRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AjandaService {

    private final GorevRepository gorevRepository;
    private final FaturaRepository faturaRepository;

    @Transactional(readOnly = true)
    public List<AjandaOlayDTO> olaylar(Long sirketId, LocalDate baslangic, LocalDate bitis) {
        List<AjandaOlayDTO> olaylar = new ArrayList<>();

        List<Gorev> gorevler = gorevRepository.sirketGorevleri(sirketId, baslangic, bitis);
        for (Gorev g : gorevler) {
            LocalDate tarih = g.getBaslangic() != null ? g.getBaslangic() : baslangic;
            olaylar.add(AjandaOlayDTO.builder()
                    .tarih(tarih).tip("GOREV")
                    .baslik(g.getAd())
                    .aciklama("Durum: " + g.getDurum() + (g.getAtanan() != null ? " · Atanan: " + g.getAtanan() : ""))
                    .build());
        }

        List<Fatura> vadeler = faturaRepository.findVadesiYaklasan(
                sirketId, Fatura.FaturaDurum.KESILDI, List.of("ODENDI", "IPTAL"), baslangic, bitis);
        for (Fatura f : vadeler) {
            if (f.getVadeTarihi() == null) continue;
            olaylar.add(AjandaOlayDTO.builder()
                    .tarih(f.getVadeTarihi()).tip("VADE")
                    .baslik("Fatura #" + f.getFaturaNumarasi())
                    .aciklama((f.getCariHesap() != null ? f.getCariHesap().getAd() + " · " : "")
                            + "Kalan: " + (f.getKalanTutar() != null ? f.getKalanTutar() : "0"))
                    .build());
        }

        olaylar.sort(Comparator.comparing(AjandaOlayDTO::getTarih));
        return olaylar;
    }
}
