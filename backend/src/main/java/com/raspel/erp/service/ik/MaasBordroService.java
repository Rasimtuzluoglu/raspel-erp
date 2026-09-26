package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.service.muhasebe.OtomatikMuhasebeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class MaasBordroService {

    private final MaasBordroRepository maasBordroRepository;
    private final PersonelRepository personelRepository;
    private final OtomatikMuhasebeService otomatikMuhasebeService;
    private final com.raspel.erp.config.TenantChecker tenantChecker;
    private final com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    private final com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    private final com.raspel.erp.service.sistem.DonemService donemService;

    @Transactional(readOnly = true)
    public Page<MaasBordroDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return maasBordroRepository.findBySirketIdOrderByYilDescAyDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public MaasBordroDTO getir(Long id) {
        MaasBordro bordro = maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id));
        tenantChecker.check(bordro.getSirketId(), "MaasBordro");
        return entityToDTO(bordro);
    }

    public MaasBordroDTO olustur(MaasBordroDTO dto, Long sirketId) {
        Personel personel = personelRepository.findById(dto.getPersonelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
        tenantChecker.check(personel.getSirketId(), "Personel");
        MaasBordro bordro = MaasBordro.builder()
                .personel(personel)
                .yil(dto.getYil())
                .ay(dto.getAy())
                .brutMaas(dto.getBrutMaas())
                .kesintiler(dto.getKesintiler() != null ? dto.getKesintiler() : BigDecimal.ZERO)
                // Net maaş sunucuda hesaplanır (brüt - kesinti); istemci değeri güvenilmez.
                .netMaas(netHesapla(dto.getBrutMaas(), dto.getKesintiler()))
                .odemeTarihi(dto.getOdemeTarihi())
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .durum("TASLAK")
                .build();
        MaasBordro kaydedilen = maasBordroRepository.save(bordro);
        // Otomatik yevmiye fişi (bloklamayan).
        otomatikMuhasebeService.bordroIsle(kaydedilen);
        return entityToDTO(kaydedilen);
    }

    public MaasBordroDTO guncelle(Long id, MaasBordroDTO dto) {
        MaasBordro bordro = maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id));
        tenantChecker.check(bordro.getSirketId(), "MaasBordro");
        if ("ONAYLANDI".equals(bordro.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Onaylanmış bordro düzenlenemez. Önce onayı kaldırın.");
        }
        if (dto.getYil() != null) bordro.setYil(dto.getYil());
        if (dto.getAy() != null) bordro.setAy(dto.getAy());
        if (dto.getBrutMaas() != null) bordro.setBrutMaas(dto.getBrutMaas());
        if (dto.getKesintiler() != null) bordro.setKesintiler(dto.getKesintiler());
        if (dto.getOdemeTarihi() != null) bordro.setOdemeTarihi(dto.getOdemeTarihi());
        if (dto.getAciklama() != null) bordro.setAciklama(dto.getAciklama());
        if (dto.getPersonelId() != null) {
            Personel personel = personelRepository.findById(dto.getPersonelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
            tenantChecker.check(personel.getSirketId(), "Personel");
            bordro.setPersonel(personel);
        }
        // Net maaş her zaman brüt - kesinti olarak yeniden hesaplanır.
        bordro.setNetMaas(netHesapla(bordro.getBrutMaas(), bordro.getKesintiler()));
        MaasBordro kaydedilen = maasBordroRepository.save(bordro);
        // Tutarlar değişmiş olabilir: eski fişi iptal edip yenisini üret.
        otomatikMuhasebeService.bordroIptal(kaydedilen.getId(), kaydedilen.getSirketId());
        otomatikMuhasebeService.bordroIsle(kaydedilen);
        return entityToDTO(kaydedilen);
    }

    /**
     * Bordroyu onaylar ve kilitler. İstenirse net tutar kasadan ödenir (kasaId verilirse).
     * Onay sonrası bordro düzenlenemez/silinemez.
     */
    public MaasBordroDTO onayla(Long id, String onaylayan, Long kasaId) {
        MaasBordro bordro = maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id));
        tenantChecker.check(bordro.getSirketId(), "MaasBordro");
        if ("ONAYLANDI".equals(bordro.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException("Bu bordro zaten onaylanmış.");
        }
        bordro.setDurum("ONAYLANDI");
        bordro.setOnayTarihi(java.time.LocalDateTime.now());
        bordro.setOnaylayan(onaylayan);
        MaasBordro kaydedilen = maasBordroRepository.save(bordro);
        if (kasaId != null) {
            kasaOdemeYap(kaydedilen, kasaId, onaylayan);
        }
        return entityToDTO(kaydedilen);
    }

    /** Onaylanmış bordroyu yeniden düzenlenebilir hale getirir (fiş iptal edilir). */
    public MaasBordroDTO onayKaldir(Long id) {
        MaasBordro bordro = maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id));
        tenantChecker.check(bordro.getSirketId(), "MaasBordro");
        if (!"ONAYLANDI".equals(bordro.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException("Bordro onaylı değil.");
        }
        bordro.setDurum("TASLAK");
        bordro.setOnayTarihi(null);
        bordro.setOnaylayan(null);
        return entityToDTO(maasBordroRepository.save(bordro));
    }

    private BigDecimal netHesapla(BigDecimal brut, BigDecimal kesintiler) {
        BigDecimal b = brut != null ? brut : BigDecimal.ZERO;
        BigDecimal k = kesintiler != null ? kesintiler : BigDecimal.ZERO;
        BigDecimal net = b.subtract(k);
        return net.signum() < 0 ? BigDecimal.ZERO : net;
    }

    private void kasaOdemeYap(MaasBordro bordro, Long kasaId, String onaylayan) {
        com.raspel.erp.entity.finans.Kasa kasa = kasaRepository.findById(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        donemService.kilitKontrol(bordro.getSirketId(),
                bordro.getOdemeTarihi() != null ? bordro.getOdemeTarihi() : java.time.LocalDate.now(),
                "bordro ödemesi");
        BigDecimal tutar = bordro.getNetMaas() != null ? bordro.getNetMaas() : BigDecimal.ZERO;
        kasa.setBakiye((kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO).subtract(tutar));
        kasaRepository.save(kasa);
        String personel = bordro.getPersonel() != null
                ? bordro.getPersonel().getAd() + " " + bordro.getPersonel().getSoyad() : "";
        kasaHareketRepository.save(com.raspel.erp.entity.finans.KasaHareket.builder()
                .kasa(kasa).tur("GIDER").tutar(tutar)
                .hareketTarihi(bordro.getOdemeTarihi() != null ? bordro.getOdemeTarihi() : java.time.LocalDate.now())
                .aciklama("Bordro ödemesi " + bordro.getYil() + "-" + bordro.getAy() + " - " + personel)
                .kaynakTip("BORDRO").build());
    }

    public void sil(Long id) {
        MaasBordro bordro = maasBordroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaasBordro", id));
        tenantChecker.check(bordro.getSirketId(), "MaasBordro");
        if ("ONAYLANDI".equals(bordro.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Onaylanmış bordro silinemez. Önce onayı kaldırın.");
        }
        otomatikMuhasebeService.bordroIptal(bordro.getId(), bordro.getSirketId());
        maasBordroRepository.delete(bordro);
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
                .durum(m.getDurum()).onayTarihi(m.getOnayTarihi()).onaylayan(m.getOnaylayan())
                .build();
    }
}
