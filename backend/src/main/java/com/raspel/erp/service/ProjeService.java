package com.raspel.erp.service;

import com.raspel.erp.dto.GorevDTO;
import com.raspel.erp.dto.ProjeDTO;
import com.raspel.erp.entity.Gorev;
import com.raspel.erp.entity.Proje;
import com.raspel.erp.repository.GorevRepository;
import com.raspel.erp.repository.ProjeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjeService {

    private final ProjeRepository projeRepository;
    private final GorevRepository gorevRepository;

    public List<ProjeDTO> tumunuGetir(Long sirketId) {
        return projeRepository.findBySirketIdOrderByBaslangicDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    public ProjeDTO getir(Long id) {
        return entityToDTO(projeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı: " + id)));
    }

    public ProjeDTO olustur(ProjeDTO dto) {
        Proje p = Proje.builder().ad(dto.getAd()).aciklama(dto.getAciklama())
                .baslangic(dto.getBaslangic()).bitis(dto.getBitis())
                .durum("DEVAM_EDIYOR").sorumlu(dto.getSorumlu())
                .sirketId(dto.getSirketId()).build();
        p = projeRepository.save(p);
        if (dto.getGorevler() != null) {
            for (GorevDTO g : dto.getGorevler()) {
                gorevRepository.save(Gorev.builder().projeId(p.getId()).ad(g.getAd())
                        .aciklama(g.getAciklama()).durum("YAPILACAK")
                        .atanan(g.getAtanan()).baslangic(g.getBaslangic())
                        .bitis(g.getBitis()).build());
            }
        }
        return entityToDTO(p);
    }

    public ProjeDTO durumGuncelle(Long id, String durum) {
        Proje p = projeRepository.findById(id).orElseThrow(() -> new RuntimeException("Proje bulunamadı: " + id));
        p.setDurum(durum);
        return entityToDTO(projeRepository.save(p));
    }

    public ProjeDTO gorevEkle(Long projeId, GorevDTO dto) {
        Proje p = projeRepository.findById(projeId).orElseThrow(() -> new RuntimeException("Proje bulunamadı: " + projeId));
        gorevRepository.save(Gorev.builder().projeId(projeId).ad(dto.getAd())
                .aciklama(dto.getAciklama()).durum("YAPILACAK")
                .atanan(dto.getAtanan()).baslangic(dto.getBaslangic())
                .bitis(dto.getBitis()).build());
        return entityToDTO(p);
    }

    public void sil(Long id) {
        gorevRepository.findByProjeIdOrderByBaslangicAsc(id).forEach(g -> gorevRepository.deleteById(g.getId()));
        projeRepository.deleteById(id);
    }

    public GorevDTO gorevDurumGuncelle(Long gorevId, String durum) {
        Gorev g = gorevRepository.findById(gorevId).orElseThrow(() -> new RuntimeException("Görev bulunamadı: " + gorevId));
        g.setDurum(durum);
        return gorevToDTO(gorevRepository.save(g));
    }

    private ProjeDTO entityToDTO(Proje p) {
        List<GorevDTO> gorevler = gorevRepository.findByProjeIdOrderByBaslangicAsc(p.getId()).stream()
                .map(this::gorevToDTO).collect(Collectors.toList());
        return ProjeDTO.builder().id(p.getId()).ad(p.getAd()).aciklama(p.getAciklama())
                .baslangic(p.getBaslangic()).bitis(p.getBitis()).durum(p.getDurum())
                .sorumlu(p.getSorumlu()).sirketId(p.getSirketId())
                .olusturmaTarihi(p.getOlusturmaTarihi()).gorevler(gorevler).build();
    }

    private GorevDTO gorevToDTO(Gorev g) {
        return GorevDTO.builder().id(g.getId()).projeId(g.getProjeId()).ad(g.getAd())
                .aciklama(g.getAciklama()).durum(g.getDurum()).atanan(g.getAtanan())
                .baslangic(g.getBaslangic()).bitis(g.getBitis())
                .olusturmaTarihi(g.getOlusturmaTarihi()).build();
    }
}
