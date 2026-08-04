package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MaasBordroService {

    private final MaasBordroRepository maasBordroRepository;
    private final PersonelRepository personelRepository;

    @Transactional(readOnly = true)
    public Page<MaasBordroDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return maasBordroRepository.findBySirketIdOrderByYilDescAyDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public MaasBordroDTO getir(Long id) {
        return entityToDTO(maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id)));
    }

    public MaasBordroDTO olustur(MaasBordroDTO dto, Long sirketId) {
        Personel personel = personelRepository.findById(dto.getPersonelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
        MaasBordro bordro = MaasBordro.builder()
                .personel(personel)
                .yil(dto.getYil())
                .ay(dto.getAy())
                .brutMaas(dto.getBrutMaas())
                .kesintiler(dto.getKesintiler())
                .netMaas(dto.getNetMaas())
                .odemeTarihi(dto.getOdemeTarihi())
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(maasBordroRepository.save(bordro));
    }

    public MaasBordroDTO guncelle(Long id, MaasBordroDTO dto) {
        MaasBordro bordro = maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id));
        if (dto.getYil() != null) bordro.setYil(dto.getYil());
        if (dto.getAy() != null) bordro.setAy(dto.getAy());
        if (dto.getBrutMaas() != null) bordro.setBrutMaas(dto.getBrutMaas());
        if (dto.getKesintiler() != null) bordro.setKesintiler(dto.getKesintiler());
        if (dto.getNetMaas() != null) bordro.setNetMaas(dto.getNetMaas());
        if (dto.getOdemeTarihi() != null) bordro.setOdemeTarihi(dto.getOdemeTarihi());
        if (dto.getAciklama() != null) bordro.setAciklama(dto.getAciklama());
        if (dto.getPersonelId() != null) {
            Personel personel = personelRepository.findById(dto.getPersonelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
            bordro.setPersonel(personel);
        }
        return entityToDTO(maasBordroRepository.save(bordro));
    }

    public void sil(Long id) {
        if (!maasBordroRepository.existsById(id))
            throw new ResourceNotFoundException("MaasBordro", id);
        maasBordroRepository.deleteById(id);
    }

    private MaasBordroDTO entityToDTO(MaasBordro m) {
        return MaasBordroDTO.builder()
                .id(m.getId())
                .personelId(m.getPersonel() != null ? m.getPersonel().getId() : null)
                .personelAdi(m.getPersonel() != null ? m.getPersonel().getAd() + " " + m.getPersonel().getSoyad() : null)
                .yil(m.getYil()).ay(m.getAy())
                .brutMaas(m.getBrutMaas()).kesintiler(m.getKesintiler()).netMaas(m.getNetMaas())
                .odemeTarihi(m.getOdemeTarihi()).sirketId(m.getSirketId())
                .aciklama(m.getAciklama()).olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
