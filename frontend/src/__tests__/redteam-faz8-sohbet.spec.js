import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz8 — Sohbet modulu sertlestirme.
 *
 * KUSURLAR (ozet):
 *  1) "Ayril" tek tikla kanali siliyordu, onay yoktu.
 *  2) AI konusma bellegi sirket bazliydi (kullanicilar birbirinin baglamini goruyordu).
 *  3) DRIVER rolu AI uzerinden sirket finansallarini sorgulayabiliyordu.
 *  4) Enter'a iki kez basmak iki mesaj/AI istegi gonderiyordu.
 *  5) Okunmamis rozeti genel sohbeti saymiyor ve canli degildi.
 *  6) Genel sohbette gecmis yoktu; eski mesaj yuklerken ekran zipliyordu.
 *  7) Oda olustururken uye secilemiyor, ozel kanal yoktu.
 *  8) Istenilen URL `dosyaUrl` olarak kaydedilebiliyordu (tracking/dis link).
 *  9) Mesaj boyutu ve hiz siniri yoktu.
 * 10) Oda silinince mesajlar oksuz kaliyordu.
 * 11) Tarih sabit tr-TR; AI tablosunda `marjYuzde` ham gorunuyordu.
 * 12) Mesaj listesinde aria-live yoktu.
 */

const KOK = join(process.cwd(), 'src')
const sohbet = readFileSync(join(KOK, 'views', 'Sohbet.vue'), 'utf8')
const sidebar = readFileSync(join(KOK, 'components', 'AppSidebar.vue'), 'utf8')
const api = readFileSync(join(KOK, 'api', 'modules', 'sistem.js'), 'utf8')
const tr = JSON.parse(readFileSync(join(KOK, 'locales', 'tr.json'), 'utf8'))
const en = JSON.parse(readFileSync(join(KOK, 'locales', 'en.json'), 'utf8'))

const BEK = join(process.cwd(), '..', 'backend', 'src', 'main', 'java', 'com', 'raspel', 'erp')
const odaServis = readFileSync(join(BEK, 'service', 'sistem', 'SohbetOdaService.java'), 'utf8')
const sohbetServis = readFileSync(join(BEK, 'service', 'sistem', 'SohbetService.java'), 'utf8')
const sohbetController = readFileSync(join(BEK, 'controller', 'sistem', 'SohbetController.java'), 'utf8')
const dosyaUrl = readFileSync(join(BEK, 'util', 'SohbetDosyaUrl.java'), 'utf8')
const hizSiniri = readFileSync(join(BEK, 'service', 'sistem', 'SohbetHizSiniri.java'), 'utf8')
const mesajRepo = readFileSync(join(BEK, 'repository', 'sistem', 'SohbetMesajRepository.java'), 'utf8')

const scriptKodu = (s) => {
  const bas = s.indexOf('<script setup>')
  const son = s.lastIndexOf('</script>')
  return bas === -1 || son <= bas ? '' : s.slice(bas, son)
}
const sohbetScript = scriptKodu(sohbet)

describe('Faz8.1 - ayrilma/silme guvenligi', () => {
  it('ayrilma onay ister ve kurucu icin uyari verir', () => {
    expect(sohbetScript).toContain('confirm.require({')
    expect(sohbetScript).toContain("t('sohbet.ayrilKurucuOnay')")
    expect(sohbetScript).toContain("t('sohbet.ayrilOnay')")
  })

  it('oda silme yalnizca yoneticiye gorunur ve onay ister', () => {
    expect(sohbet).toContain('v-if="odaYoneticisiMi(seciliOda)"')
    expect(sohbetScript).toContain('const odaYoneticisiMi =')
    expect(sohbetScript).toContain("t('sohbet.odaSilOnay'")
  })

  // Faz 9: oda adi/aciklamasi ve ozel kanal bayragi olusturulduktan sonra
  // degistirilemiyordu; backend destekliyordu ama arayuzde yol yoktu.
  it('oda duzenleme dialogu ozel kanal dahil kaydeder', () => {
    expect(sohbet).toContain('v-model:visible="duzenleDialogAc"')
    expect(sohbetScript).toContain('const odaDuzenleKaydet =')
    expect(sohbetScript).toContain('sohbetOdaAPI.guncelle(seciliOdaId.value, {')
    expect(sohbetScript).toContain('ozel: duzenleForm.value.ozel')
    expect(odaServis).toContain('if (dto.getOzel() != null)')
  })

  it('kurucu ayrilinca yonetim devredilir, son uyede mesajlar temizlenir', () => {
    expect(odaServis).toContain('SohbetOdaUye yeniSahip')
    expect(odaServis).toContain('setRol("OWNER")')
    expect(odaServis).toContain('mesajRepository.deleteByOdaId(odaId)')
    expect(mesajRepo).toContain('delete from SohbetMesaj m where m.odaId = :odaId')
  })
})

describe('Faz8.2 - AI bellegi ve yetki', () => {
  it('AI bellegi kullanici bazli anahtarlanir', () => {
    expect(sohbetServis).toContain('record HafizaAnahtari(Long sirketId, Long kullaniciId)')
    expect(sohbetServis).toContain('hafizaBaglami(sirketId, kullaniciId)')
  })

  it('AI uclari DRIVER rolune kapali (backend + frontend)', () => {
    expect(sohbetController).toContain('@PreAuthorize("hasAnyRole(\'ADMIN\', \'USER\', \'MUHASEBE\')")')
    expect(sohbetScript).toContain("['ADMIN', 'USER', 'MUHASEBE'].includes(kullaniciRolu.value)")
    expect(sohbet).toContain('v-if="aiErisimVar"')
  })
})

describe('Faz8.3 - cift gonderim ve hiz siniri', () => {
  it('ekip ve AI gonderiminde re-entrancy kilidi var', () => {
    expect(sohbetScript).toContain('if (gonderiliyor.value) return')
    expect(sohbetScript).toContain('if (aiYukleniyor.value) return')
    expect(sohbet).toContain(':disabled="gonderiliyor || !yeniMesaj.trim()"')
    expect(sohbet).toContain(':disabled="aiYukleniyor || !yeniMesaj.trim()"')
  })

  it('backend mesaj/AI/OCR/dosya icin hiz siniri uygular', () => {
    expect(hizSiniri).toContain('class SohbetHizSiniri')
    expect(sohbetServis).toContain('hizSiniri.kontrolEt("sohbet-mesaj:')
    expect(sohbetServis).toContain('hizSiniri.kontrolEt("sohbet-ai:')
    expect(sohbetServis).toContain('hizSiniri.kontrolEt("sohbet-ocr:')
    expect(odaServis).toContain('hizSiniri.kontrolEt("sohbet-dosya:')
  })

  // Faz 9: cok instance'li kurulumda ortak sayac; Redis yoksa bellek yedegi.
  it('hiz siniri Redis ile ortak, Redis yoksa bellek yedekli', () => {
    expect(hizSiniri).toContain('StringRedisTemplate')
    expect(hizSiniri).toContain('removeRangeByScore')
    expect(hizSiniri).toContain('yerelKontrol')
    expect(hizSiniri).toContain('Redis hiz siniri kullanilamadi')
  })

  it('mesaj boyutu DTO seviyesinde sinirli', () => {
    const dto = readFileSync(join(BEK, 'dto', 'sistem', 'SohbetMesajDTO.java'), 'utf8')
    expect(dto).toContain('@Size(max = 4000')
  })
})

describe('Faz8.4 - okunmamis rozeti ve sayfalama', () => {
  it('genel sohbet de rozete dahil ve canli tazelenir', () => {
    expect(sidebar).toContain('sohbetAPI.okunmamis()')
    expect(sidebar).toContain("window.addEventListener('raspel:sohbet-guncelle', rozetleriYukle)")
    expect(sidebar).toContain('setInterval(rozetleriYukle, 30000)')
    expect(sidebar).toContain('clearInterval(rozetZamanlayici)')
    expect(sohbetScript).toContain("window.dispatchEvent(new CustomEvent('raspel:sohbet-guncelle'))")
  })

  // Faz 9: okuma isareti localStorage yerine sunucuda (cihazdan bagimsiz).
  it('genel sohbet okuma isareti sunucuda tutulur', () => {
    expect(api).toContain('okunduGenel(sonOkunanId)')
    expect(sohbetScript).toContain('sohbetAPI.okunduGenel(son.id)')
    expect(sohbetScript).not.toContain('raspel_sohbet_genel_son_id')
    expect(sidebar).not.toContain('raspel_sohbet_genel_son_id')
    expect(sohbetController).toContain('@PostMapping("/okundu")')
    expect(sohbetServis).toContain('genelOkunduIsaretle(Long sirketId, Long kullaniciId, Long mesajId)')
  })

  it('genel sohbet cursor ile sayfalanir', () => {
    expect(api).toContain("okunmamis()")
    expect(sohbetScript).toContain('sohbetAPI.sonMesajlar({ cursor: mesajCursor.value, limit: MESAJ_SAYFA })')
    expect(sohbet).toContain('<div\n            v-if="dahaVar"')
    expect(sohbetServis).toContain('findBySirketIdAndOdaIdIsNullAndIdLessThanOrderByIdDesc')
  })

  it('eski mesaj yuklerken kaydirma konumu korunur', () => {
    expect(sohbetScript).toContain('oncekiYukseklik')
    expect(sohbetScript).toContain('el.scrollTop = oncekiUst + (el.scrollHeight - oncekiYukseklik)')
    expect(sohbetScript).toContain('const dipteMi = el.scrollHeight - el.scrollTop - el.clientHeight < 80')
  })
})

describe('Faz8.5 - oda olusturma', () => {
  it('uyeler ve ozel kanal secimi var', () => {
    expect(sohbet).toContain('<MultiSelect')
    expect(sohbet).toContain('v-model="odaForm.uyeIdleri"')
    expect(sohbet).toContain('v-model="odaForm.ozel"')
    expect(sohbetScript).toContain('uyeIdleri: odaForm.value.uyeIdleri')
    expect(sohbetScript).toContain('ozel: odaForm.value.ozel')
  })

  it('ozel kanala kendi kendine katilma backendde kapali', () => {
    expect(odaServis).toContain('if (oda.isOzel())')
    expect(odaServis).toContain('yönetici sizi eklemeli')
  })

  it('uye ekleme listesi aranabilir', () => {
    expect(sohbet).toContain('filter\n          :placeholder="t(\'sohbet.uyeEkle\')"')
  })
})

describe('Faz8.6 - dosya URL ve veri hijyeni', () => {
  it('dosyaUrl yalnizca kendi yukleme yolu olabilir', () => {
    expect(dosyaUrl).toContain('public static final String ONEK = "/api/uploads/sohbet/"')
    expect(sohbetServis).toContain('SohbetDosyaUrl.dogrula(dto.getDosyaUrl())')
    expect(odaServis).toContain('SohbetDosyaUrl.dogrula(dto.getDosyaUrl())')
    // Eski/harici URL'ler istemcide de render edilmez (tracking sizintisi).
    expect(sohbetScript).toContain("u.startsWith('/api/uploads/') || u.startsWith('blob:')")
  })

  it('oda listesi toplu uye/ad ceker (N+1 yok)', () => {
    expect(odaServis).toContain('uyeRepository.findByOdaIdIn(odaIdleri)')
    expect(odaServis).toContain('kullaniciRepository.findAllById(idler)')
  })
})

describe('Faz8.7 - erisilebilirlik, dil ve birim', () => {
  it('mesaj listesi aria-live log', () => {
    expect(sohbet).toContain('role="log"')
    expect(sohbet).toContain('aria-live="polite"')
    expect(sohbet).toContain('aria-relevant="additions"')
  })

  it('tarih arayuz diline uyar', () => {
    expect(sohbetScript).toContain("locale.value === 'en' ? 'en-GB' : 'tr-TR'")
  })

  it('marjYuzde basligi ve rozet birimi duzeltildi', () => {
    expect(sohbetScript).toContain('marjYuzde: t(\'sohbet.karMarji\')')
    expect(sohbetScript).toContain('const rozetBirimi =')
    expect(sohbet).toContain('{{ rozetBirimi(m) }}')
  })

  it('yaziyor metni anahtar adi degil', () => {
    expect(tr.sohbet.yaziyor).not.toBe('yaziyorKullanici')
    expect(tr.sohbet.yaziyor).toBeTruthy()
    expect(en.sohbet.yaziyor).toBeTruthy()
  })

  it('yeni sohbet anahtarlari iki dilde de var', () => {
    const anahtarlar = ['ayrilOnay', 'ayrilKurucuOnay', 'odaSil', 'odaSilOnay', 'odaDuzenle', 'odaDuzenlendi', 'ozelKanal', 'ozelKanalAciklama', 'uyelerSec', 'odaMesajlari']
    for (const k of anahtarlar) {
      expect(tr.sohbet[k], `tr.sohbet.${k}`).toBeTruthy()
      expect(en.sohbet[k], `en.sohbet.${k}`).toBeTruthy()
    }
  })
})
