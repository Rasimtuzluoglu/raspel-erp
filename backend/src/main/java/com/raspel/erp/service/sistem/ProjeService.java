package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.GorevDTO;
import com.raspel.erp.dto.sistem.ProjeDTO;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.entity.sistem.Proje;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.GorevRepository;
import com.raspel.erp.repository.sistem.ProjeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjeService {

    private final ProjeRepository projeRepository;
    private final GorevRepository gorevRepository;
    private final TenantChecker tenantChecker;

    public Page<ProjeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return projeRepository.findBySirketIdOrderByBaslangicDesc(sirketId, pageable).map(this::entityToDTO);
    }

    public ProjeDTO getir(Long id) {
        Proje p = projeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proje", id));
        tenantChecker.check(p.getSirketId(), "Proje");
        return entityToDTO(p);
    }

    public ProjeDTO olustur(ProjeDTO dto) {
        tenantChecker.checkSirketId(dto.getSirketId(), "Proje");
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

    public ProjeDTO guncelle(Long id, ProjeDTO dto) {
        Proje p = projeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proje", id));
        tenantChecker.check(p.getSirketId(), "Proje");
        p.setAd(dto.getAd());
        p.setAciklama(dto.getAciklama());
        p.setBaslangic(dto.getBaslangic());
        p.setBitis(dto.getBitis());
        if (dto.getDurum() != null) p.setDurum(dto.getDurum());
        p.setSorumlu(dto.getSorumlu());
        p = projeRepository.save(p);
        if (dto.getGorevler() != null) {
            gorevRepository.findByProjeIdOrderByBaslangicAsc(p.getId()).forEach(g -> gorevRepository.deleteById(g.getId()));
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
        Proje p = projeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proje", id));
        tenantChecker.check(p.getSirketId(), "Proje");
        p.setDurum(durum);
        return entityToDTO(projeRepository.save(p));
    }

    public ProjeDTO gorevEkle(Long projeId, GorevDTO dto) {
        Proje p = projeRepository.findById(projeId).orElseThrow(() -> new ResourceNotFoundException("Proje", projeId));
        tenantChecker.check(p.getSirketId(), "Proje");
        gorevRepository.save(Gorev.builder().projeId(projeId).ad(dto.getAd())
                .aciklama(dto.getAciklama()).durum("YAPILACAK")
                .atanan(dto.getAtanan()).baslangic(dto.getBaslangic())
                .bitis(dto.getBitis()).build());
        return entityToDTO(p);
    }

    public void sil(Long id) {
        Proje p = projeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proje", id));
        tenantChecker.check(p.getSirketId(), "Proje");
        gorevRepository.findByProjeIdOrderByBaslangicAsc(id).forEach(g -> gorevRepository.deleteById(g.getId()));
        projeRepository.deleteById(id);
    }

    public GorevDTO gorevDurumGuncelle(Long gorevId, String durum) {
        Gorev g = gorevRepository.findById(gorevId).orElseThrow(() -> new ResourceNotFoundException("Görev", gorevId));
        Proje p = projeRepository.findById(g.getProjeId()).orElseThrow(() -> new ResourceNotFoundException("Proje", g.getProjeId()));
        tenantChecker.check(p.getSirketId(), "Proje");
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
