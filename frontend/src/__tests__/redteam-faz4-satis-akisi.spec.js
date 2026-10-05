import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { normalizeKalem } from '../utils/satisPayload.js'

/**
 * "Yeni Satış" (views/Satis.vue) akisinin regresyon denetimleri.
 *
 * Buradaki bulgular KOD TARAMASI ile kanitlanmistir; hicbiri derleyici ya da
 * ESLint uyarisi uretmez:
 *
 *  1) Indirim motoru olusu: kalemlere `iskontoOrani: 0` yaziliyordu. Backend
 *     (`FaturaService`) iskonto `null` oldugunda `IskontoMotoruService`'i
 *     calistirir, `0` gordugunde ATLAR. Yani sirketin kademeli indirim
 *     kurallari satistan hicbir zaman uygulanmazdi.
 *  2) Olu stok kontrolu: kayittan once adet > stok denetimi yoktu; fazla satis
 *     ancak kayit sonrasi kritik stok uyarisiyla bildiriliyordu ve stok
 *     hareketi geri alinmiyordu. `satis.yetersizStok` anahtari OLU bir
 *     anahtardi (hicbir yerde kullanilmazdi).
 *  3) KDV varsayilani: ekran `%0`, payload uretici ve backend `%20` kullaniyordu.
 *     Stokta KDV tanimli olmayan kalemde ekrandaki toplam ile kaydedilen tutar
 *     ayrisiyordu.
 *  4) Olu istek: `stokStore.getAll()` cagriliyor, sonucu hicbir yerde
 *     kullanilmiyordu (ayrica parametresiz oldugu icin 50 kayitlik tavana takiliyordu).
 *  5) Hardcoded Turkce: teslim durumu etiketleri ve termal fis metinleri dil
 *     degistiriciye bagli degildi.
 */

const KOK = join(process.cwd(), 'src')
const satisKaynak = readFileSync(join(KOK, 'views/Satis.vue'), 'utf8')
const kalemlerKaynak = readFileSync(join(KOK, 'components/FaturaKalemleri.vue'), 'utf8')
const hesaplaKaynak = readFileSync(join(KOK, 'utils/faturaHesapla.js'), 'utf8')

/**
 * Yorum satirlarini temizler; denetim KOD okumalidir, metni degil.
 * Satir yorumlari da (`//`) silinir: duzeltmelerin kendi aciklamalarinda
 * ihlali ANLATAN ifadeler geciyor ve tarayiciyi yaniltirdi.
 */
const yorumsuz = (s) =>
  s
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    // NOT: dosyalar CRLF kaydedilmis olabilir; JS regexte `.` \r'yi TUTMAZ, bu
    // yuzden satirlari once \r'den arindiriyoruz, yoksa `//` temizligi sessizce
    // basarisiz oluyor ve test kendi aciklamasini ihlal sayiyor.
    .split(/\r?\n/)
    .map((satir) => satir.replace(/(^|\s)\/\/.*$/, ''))
    .join('\n')

/**
 * Sadece `<script>` bloklarini dondurur. `<style>` icindeki `.stokStore`
 * gecen yorumlarimiz da metni kirletmesin diye ayrica sart.
 */
const scriptKodu = (s) => {
  const bas = s.indexOf('<script setup>')
  const son = s.lastIndexOf('</script>')
  return bas === -1 || son <= bas ? '' : yorumsuz(s.slice(bas, son))
}

describe('Satis.vue - indirim motoru canli olmali', () => {
  it('kalem eklerken iskonto alani null BIRAKILIR (motor calissin)', () => {
    // Sabit `0` gondermek motoru atlatiyordu.
    expect(yorumsuz(kalemlerKaynak)).not.toMatch(/iskontoOrani:\s*0\b/)
  })

  it('normalizeKalem iskonto verilmediginde alani hic yazmaz', () => {
    const k = normalizeKalem({ ad: 'Urun', adet: 2, birimFiyat: 50 })
    expect('iskontoOrani' in k).toBe(false)
    // Kullanici bilerek sifir indirim istiyorsa 0 GONDERILIR
    expect(normalizeKalem({ iskontoOrani: 0 }).iskontoOrani).toBe(0)
    expect(normalizeKalem({ iskontoOrani: 20 }).iskontoOrani).toBe(20)
  })

  it('iskonto placeholder metni tanimli (bos = kural uygulanir)', () => {
    expect(kalemlerKaynak).toContain("faturaKalemleri.iskontoKural")
  })
})

describe('Satis.vue - stok kontrolu kayittan once yapilmali', () => {
  it('stok Miktarini kalemle birlikte tasir', () => {
    expect(kalemlerKaynak).toMatch(/stokMiktar:\s*s\?\.miktar/)
    expect(yorumsuz(satisKaynak)).toMatch(/k\.stokMiktar\s*=\s*stok\.miktar/)
  })

  it('adet > stok satirlarini hesaplar ve onay ister', () => {
    expect(yorumsuz(satisKaynak)).toContain('stokYetersizKalemler')
    expect(yorumsuz(satisKaynak)).toContain('stokYetersizOnayiSor')
    // Kaydetme akisinda GERCEKTEN cagriliyor olmali
    expect(yorumsuz(satisKaynak)).toMatch(
      /if \(satisModu\.value === 'SATIS' && !\(await stokYetersizOnayiSor\(\)\)\) return/
    )
  })

  it('kullaniciyi bilgilendiren i18n metinleri tanimli', () => {
    expect(yorumsuz(satisKaynak)).toContain('satis.yetersizStokBaslik')
    expect(yorumsuz(satisKaynak)).toContain('satis.yetersizStokSatirlar')
  })

  it('adet alani stokta kac adet oldugunu gosterir', () => {
    expect(kalemlerKaynak).toContain('stokBilgisiGosterilebilir')
    expect(kalemlerKaynak).toContain('stokYetersizMu')
    expect(kalemlerKaynak).toContain('faturaKalemleri.stokMiktari')
  })
})

describe('Satis.vue - KDV varsayilani tek kaynaktan gelmeli', () => {
  it('faturaHesapla VARSAYILAN_KDV_ORANI sabitini export eder', () => {
    expect(hesaplaKaynak).toMatch(/export const VARSAYILAN_KDV_ORANI\s*=\s*20/)
  })

  it('Satis.vue sabiti kullanir, sabit 0 GECIRMEZ', () => {
    const kod = yorumsuz(satisKaynak)
    expect(kod).toContain('VARSAYILAN_KDV_ORANI')
    expect(kod).toContain('const kdvVarsayilan = VARSAYILAN_KDV_ORANI')
    // :kdv-varsayilan="0" -> :kdv-varsayilan="kdvVarsayilan"
    expect(kod).not.toMatch(/:kdv-varsayilan="0"/)
  })
})

describe('Satis.vue - olu istek ve olu import kaldirilmis olmali', () => {
  it('stokStore kullanilmaz', () => {
    const kod = scriptKodu(satisKaynak)
    expect(kod).not.toContain('useStokStore')
    expect(kod).not.toContain('stokStore.getAll(')
    expect(kod).not.toMatch(/const stokStore\s*=/)
  })
})

describe('Satis.vue - hardcoded Turkce kalmamali', () => {
  it('teslim durumu etiketleri i18n kaynakli', () => {
    const kod = yorumsuz(satisKaynak)
    expect(kod).not.toMatch(/label:\s*'Bekliyor'/)
    expect(kod).not.toMatch(/label:\s*'Yolda'/)
    expect(kod).not.toMatch(/label:\s*'Teslim Edildi'/)
    expect(kod).toContain('teslimatlar.durumBeklemede')
  })

  it('termal fis metinleri i18n kaynakli', () => {
    const kod = yorumsuz(satisKaynak)
    // Bunlar template literal icinde olduklari icin HTML olarak geciyordu.
    for (const metin of [
      'Yazdır (Termal 80mm)',
      'SATIŞ FİŞİ',
      'ARA TOPLAM:',
      'GENEL TOPLAM:',
      'Bizi tercih ettiğiniz için teşekkür ederiz!'
    ]) {
      expect(kod, `termal fis metni hala sabit: ${metin}`).not.toContain(metin)
    }
    expect(kod).toContain('satis.termalGenelToplam')
    expect(kod).toContain('satis.termalTesekkur')
  })
})