import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ B — Tek ekran fatura formu (3 panel).
 * Kaynak duzeyinde sozlesme testleri; agir view mount edilmez.
 */

const oku = (p) => readFileSync(join(process.cwd(), 'src', p), 'utf8')
const ekran = oku('views/FaturaFormEkrani.vue')
const form = oku('composables/useFaturaForm.js')
const router = oku('router/index.js')
const liste = oku('views/Faturalar.vue')
const detay = oku('views/FaturaDetay.vue')

describe('FaturaFormEkrani — Faz B', () => {
  it('3 panelli yerlesim ve panel basliklari vardir', () => {
    expect(ekran).toContain('fatura-form-grid')
    expect(ekran).toContain('form-panel-sol')
    expect(ekran).toContain('form-panel-orta')
    expect(ekran).toContain('form-panel-sag')
    expect(ekran).toContain('bolumFaturaBilgileri')
    expect(ekran).toContain('faturaKalemleri')
    expect(ekran).toContain('toplamlar')
  })

  it('cari, tur, tarih ve para birimi alanlari vardir', () => {
    expect(ekran).toContain('data-test="fatura-cari"')
    expect(ekran).toContain('data-test="fatura-tur"')
    expect(ekran).toContain('data-test="fatura-tarih"')
    expect(ekran).toContain('data-test="fatura-para-birimi"')
    expect(ekran).toContain('kurDegeri')
  })

  it('kalem bileseni mevcut sozlesmeyle kullanilir', () => {
    expect(ekran).toContain('<FaturaKalemleri')
    expect(ekran).toContain(':kalemler="form.kalemler"')
    expect(ekran).toContain(':ara-toplam="araToplam"')
    expect(ekran).toContain(':kdv-toplam="kdvToplam"')
    expect(ekran).toContain(':genel-toplam="genelToplam"')
    expect(ekran).toContain('@add="kalemEkle"')
    expect(ekran).toContain('@remove="removeKalem"')
    expect(ekran).toContain('@geri-al="kalemGeriAl"')
  })

  it('sag panelde toplamlar, odeme ve kaydet/kes aksiyonlari vardir', () => {
    expect(ekran).toContain('faturaDetay.araToplam')
    expect(ekran).toContain('faturaDetay.genelToplam')
    expect(ekran).toContain('kalanTutar')
    expect(ekran).toContain('data-test="fatura-kaydet"')
    expect(ekran).toContain('data-test="fatura-kaydet-kes"')
    expect(ekran).toContain('taslakKaydet')
    expect(ekran).toContain('kaydetVeKes')
    expect(ekran).toContain('kesilebilir')
  })

  it('mobil sticky bar ve kirli form korumasi vardir', () => {
    expect(ekran).toContain('fatura-form-mobil-bar')
    expect(ekran).toContain('onBeforeRouteLeave')
    expect(ekran).toContain('kirliMesaj')
    expect(ekran).toContain('@media (max-width: 767px)')
    expect(ekran).toContain('@media (max-width: 1199px)')
  })

  it('duzenlenemez fatura icin kilit mesaji gosterir', () => {
    expect(ekran).toContain('duzenlenebilir')
    expect(ekran).toContain('kilitNedeni')
    expect(ekran).toContain('data-test="fatura-kilitli"')
  })

  it('depo, manuel kur ve teslimat alanlari vardir', () => {
    expect(ekran).toContain('data-test="fatura-depo"')
    expect(ekran).toContain('data-test="fatura-kur"')
    expect(ekran).toContain('data-test="fatura-sofor"')
    expect(ekran).toContain('data-test="fatura-teslimat-adresi"')
    expect(ekran).toContain('depoKilitli')
    expect(ekran).toContain('kurKilitli')
    expect(ekran).toContain('teslimatVar')
    expect(form).toContain('depoAPI.getAll')
    expect(form).toContain('personelAPI.getAll')
    expect(form).toContain('teslimatAPI.suruculer')
    expect(form).toContain('teslimatKaydet')
  })

  it('manuel kur yalnizca yeni kayitta ve TRY disinda gonderilir', () => {
    expect(form).toContain("form.value.paraBirimi !== 'TRY'")
    expect(form).toContain('cekirdek.kur = kur')
    expect(form).toContain('if (!duzenlemeMi.value) {')
  })

  it('kaydedilmis faturada etiket ve fis yazdirma aksiyonlari vardir', () => {
    expect(ekran).toContain('data-test="fatura-fis-yazdir"')
    expect(ekran).toContain('data-test="fatura-urun-etiketi"')
    expect(ekran).toContain('FaturaTasarimModal')
    expect(ekran).toContain('topluEtiket')
    expect(ekran).toContain("tip: 'BARKOD'")
    expect(ekran).toContain('etiketlenebilirKalemler')
  })

  it('kaynaktan doldurma siparis/irsaliye/teklif destekler ve baglari payloada ekler', () => {
    expect(ekran).toContain('data-test="fatura-kaynaktan-doldur"')
    expect(ekran).toContain('fatura-kaynak-secim')
    expect(ekran).toContain('kaynakYukle')
    expect(ekran).toContain('kaynaktanDoldur')
    expect(form).toContain('siparisAPI.getAll')
    expect(form).toContain('irsaliyeAPI.filtreli')
    expect(form).toContain('teklifAPI.getAll')
    expect(form).toContain('cekirdek.siparisId')
    expect(form).toContain('cekirdek.irsaliyeId')
    expect(form).toContain('kaynakFaturaVar')
  })

  it('kaydettikten sonra ekranda kalir ve PDF/e-fatura aksiyonlari sunar', () => {
    expect(form).toContain("router.replace({ name: 'FaturaDuzenle', params: { id: kayitId } })")
    expect(form).toContain('pdfAPI.fatura')
    expect(form).toContain('eFaturaAPI.olustur')
    expect(form).toContain('eFaturaAPI.gibGonder')
    expect(form).toContain('eFaturaAPI.durumSorgula')
    expect(ekran).toContain('data-test="fatura-pdf"')
    expect(ekran).toContain('data-test="fatura-efatura-olustur"')
    expect(ekran).toContain('data-test="fatura-gib-gonder"')
    expect(ekran).toContain("mevcutFatura?.durum === 'KESILDI'")
    expect(ekran).toContain('kesildiktenSonra')
  })

  it('rotalar FATURA_WRITE ile tanimli ve detay rotasindan once', () => {
    const yeniIdx = router.indexOf("name: 'FaturaYeni'")
    const duzenleIdx = router.indexOf("name: 'FaturaDuzenle'")
    const detayIdx = router.indexOf("name: 'FaturaDetay'")
    expect(yeniIdx).toBeGreaterThan(-1)
    expect(duzenleIdx).toBeGreaterThan(-1)
    expect(router).toContain("path: '/faturalar/yeni'")
    expect(router).toContain("path: '/faturalar/:id/duzenle'")
    expect(yeniIdx).toBeLessThan(detayIdx)
    const metaBlok = router.slice(yeniIdx, yeniIdx + 300)
    expect(metaBlok).toContain('FATURA_WRITE')
  })
})

describe('useFaturaForm — Faz B', () => {
  it('kalem toplamlari ve payload tek kaynaktan uretilir', () => {
    expect(form).toContain('kalemNetTutar')
    expect(form).toContain('kalemKdv')
    expect(form).toContain('satisPayloadUret')
    expect(form).toContain('getLocalDateString')
  })

  it('iskonto null semantigini korur, 0 elle sifirdir', () => {
    expect(form).toContain('iskontoOrani: k.iskontoOrani ?? null')
    expect(form).toContain("iskontoOrani: k.iskontoOrani ?? null")
  })

  it('TASLAK ve KESILDI kaydetme yollarini destekler', () => {
    expect(form).toContain("durum = 'TASLAK'")
    expect(form).toContain("durum: 'KESILDI'")
    expect(form).toContain('updateDurum')
    expect(form).toContain('krediLimitiGormezdenGel')
  })

  it('IPTAL ve odemeli KESILDI faturalari kilitler', () => {
    expect(form).toContain("durum === 'IPTAL'")
    expect(form).toContain("durum === 'KESILDI'")
    expect(form).toContain('odenenTutar')
  })

  it('duzenleme ve kopya modunda taslak geri yuklemez', () => {
    expect(form).toContain('restore: !faturaId.value && !kopyaId.value')
    expect(form).toContain('kopyalananForm')
  })

  it('guncellemede odeme alanlarini ezmez', () => {
    expect(form).toContain('if (!duzenlemeMi.value) {')
    expect(form).toContain("cekirdek.odemeDurumu = 'ODENMEDI'")
  })
})

describe('Liste/detay entegrasyonu — Faz B', () => {
  it('liste yeni ve duzenle akislarini rotaya baglar', () => {
    expect(liste).toContain("router.push({ name: 'FaturaYeni' })")
    expect(liste).toContain("router.push({ name: 'FaturaDuzenle', params: { id: fatura.id } })")
    expect(liste).toContain("query: { kopya: fatura.id }")
  })

  it('derin baglanti query\'leri forma yonlendirilir', () => {
    expect(liste).toContain("router.replace({ name: 'FaturaYeni', query: { cariId: route.query.cariId } })")
    expect(liste).toContain("router.replace({ name: 'FaturaYeni' })")
  })

  it('detay ekrani duzenlemeyi yeni forma yonlendirir', () => {
    expect(detay).toContain("name: 'FaturaDuzenle'")
    expect(detay).not.toContain("name: 'Satislar', query: { duzenle: fatura.id }")
  })
})
