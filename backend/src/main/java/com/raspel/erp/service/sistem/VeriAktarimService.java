package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.VeriAktarimDTO;
import com.raspel.erp.dto.sistem.VeriAktarimSonucDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeriAktarimService {

    private final StokRepository stokRepository;
    private final CariHesapRepository cariHesapRepository;
    private final SirketRepository sirketRepository;

    @Transactional
    public VeriAktarimSonucDTO aktarimYap(VeriAktarimDTO dto) {
        Sirket kaynak = sirketRepository.findById(dto.getKaynakSirketId())
                .orElseThrow(() -> new ResourceNotFoundException("Kaynak Şirket", dto.getKaynakSirketId()));
        Sirket hedef = sirketRepository.findById(dto.getHedefSirketId())
                .orElseThrow(() -> new ResourceNotFoundException("Hedef Şirket", dto.getHedefSirketId()));

        if (kaynak.getId().equals(hedef.getId())) {
            throw new BusinessException("Kaynak ve hedef şirket aynı olamaz");
        }

        int aktarilanStok = 0, atlananStok = 0;
        int aktarilanCari = 0, atlananCari = 0;

        // Stok aktarımı
        if (dto.isStoklariAktar()) {
            List<Stok> kaynakStoklar = stokRepository.findBySirketIdOrderByAd(dto.getKaynakSirketId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
            for (Stok kayStok : kaynakStoklar) {
                // Mükerrer kontrol: aynı stok kodu hedefte var mı?
                if (kayStok.getStokKodu() != null &&
                    stokRepository.findBySirketIdAndStokKodu(dto.getHedefSirketId(), kayStok.getStokKodu()).isPresent()) {
                    atlananStok++;
                    continue;
                }
                Stok yeniStok = Stok.builder()
                        .ad(kayStok.getAd())
                        .stokKodu(kayStok.getStokKodu())
                        .barkod(kayStok.getBarkod())
                        .birim(kayStok.getBirim())
                        .fiyat(dto.isFiyatlariKoru() ? kayStok.getFiyat() : BigDecimal.ZERO)
                        .satisFiyati(dto.isFiyatlariKoru() ? kayStok.getSatisFiyati() : null)
                        .miktar(BigDecimal.ZERO)
                        .minMiktar(kayStok.getMinMiktar())
                        .kdvOrani(kayStok.getKdvOrani())
                        .stokGrubu(kayStok.getStokGrubu())
                        .rafNo(kayStok.getRafNo())
                        .marka(kayStok.getMarka())
                        .agirlik(kayStok.getAgirlik())
                        .kategori(kayStok.getKategori())
                        .aciklama(kayStok.getAciklama())
                        .fotoUrl(kayStok.getFotoUrl())
                        .birim2(kayStok.getBirim2())
                        .cevrimKatsayisi(kayStok.getCevrimKatsayisi())
                        .tedarikciId(kayStok.getTedarikciId())
                        .tedarikciStokKodu(kayStok.getTedarikciStokKodu())
                        .tedarikciFiyat(dto.isFiyatlariKoru() ? kayStok.getTedarikciFiyat() : null)
                        .maliyetYontemi(kayStok.getMaliyetYontemi())
                        .sirketId(dto.getHedefSirketId())
                        .build();
                stokRepository.save(yeniStok);
                aktarilanStok++;
            }
            log.info("Stok aktarımı tamamlandı: {} aktarıldı, {} atlandı", aktarilanStok, atlananStok);
        }

        // Cari hesap aktarımı
        if (dto.isCarileriAktar()) {
            List<CariHesap> kaynakCariler = cariHesapRepository.findBySirketId(dto.getKaynakSirketId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
            for (CariHesap kayCari : kaynakCariler) {
                // Mükerrer kontrol: aynı vergi no hedefte var mı?
                if (kayCari.getVergiNumarasi() != null && !kayCari.getVergiNumarasi().isEmpty()) {
                    boolean mevcutMu = cariHesapRepository.findBySirketId(dto.getHedefSirketId(), org.springframework.data.domain.Pageable.unpaged())
                            .getContent().stream()
                            .anyMatch(c -> kayCari.getVergiNumarasi().equals(c.getVergiNumarasi()));
                    if (mevcutMu) {
                        atlananCari++;
                        continue;
                    }
                }
                CariHesap yeniCari = CariHesap.builder()
                        .ad(kayCari.getAd())
                        .vergiNumarasi(kayCari.getVergiNumarasi())
                        .telefon(kayCari.getTelefon())
                        .email(kayCari.getEmail())
                        .adres(kayCari.getAdres())
                        .tur(kayCari.getTur())
                        .il(kayCari.getIl())
                        .ilce(kayCari.getIlce())
                        .vergiDairesi(kayCari.getVergiDairesi())
                        .yetkiliKisi(kayCari.getYetkiliKisi())
                        .yetkiliTelefon(kayCari.getYetkiliTelefon())
                        .iban(kayCari.getIban())
                        .notlar(kayCari.getNotlar())
                        .fotoUrl(kayCari.getFotoUrl())
                        .aktif(kayCari.getAktif())
                        .krediLimiti(kayCari.getKrediLimiti())
                        .odemeVadesi(kayCari.getOdemeVadesi())
                        .bakiye(dto.isBakiyeleriSifirla() ? BigDecimal.ZERO : kayCari.getBakiye())
                        .sirketId(dto.getHedefSirketId())
                        .build();
                cariHesapRepository.save(yeniCari);
                aktarilanCari++;
            }
            log.info("Cari aktarımı tamamlandı: {} aktarıldı, {} atlandı", aktarilanCari, atlananCari);
        }

        return VeriAktarimSonucDTO.builder()
                .aktarilanStokSayisi(aktarilanStok)
                .atlananStokSayisi(atlananStok)
                .aktarilanCariSayisi(aktarilanCari)
                .atlananCariSayisi(atlananCari)
                .kaynakSirketAdi(kaynak.getAd())
                .hedefSirketAdi(hedef.getAd())
                .aktarimTarihi(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public VeriAktarimSonucDTO onizleme(Long kaynakSirketId, Long hedefSirketId) {
        Sirket kaynak = sirketRepository.findById(kaynakSirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Kaynak Şirket", kaynakSirketId));
        Sirket hedef = sirketRepository.findById(hedefSirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Hedef Şirket", hedefSirketId));

        long stokSayisi = stokRepository.countBySirketId(kaynakSirketId);
        long cariSayisi = cariHesapRepository.findBySirketId(kaynakSirketId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        return VeriAktarimSonucDTO.builder()
                .aktarilanStokSayisi((int) stokSayisi)
                .atlananStokSayisi(0)
                .aktarilanCariSayisi((int) cariSayisi)
                .atlananCariSayisi(0)
                .kaynakSirketAdi(kaynak.getAd())
                .hedefSirketAdi(hedef.getAd())
                .aktarimTarihi(LocalDateTime.now())
                .build();
    }
}
