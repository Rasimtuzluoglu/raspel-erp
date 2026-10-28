import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Onaylar revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (toplu seçim/karar, ret modalı, detay drawer, geçmiş filtreleri, mobil/a11y).
 */

const SRC = join(process.cwd(), 'src')
const oku = (yol) => readFileSync(join(SRC, yol), 'utf8')

const view = oku('views/Onaylar.vue')
const ayarView = oku('views/OnayAyarlari.vue')
const drawer = oku('components/OnayDetayDrawer.vue')
const router = oku('router/index.js')

describe('Onaylar — Faz 2', () => {
  it('çoklu seçim yalnızca aynı türde yapılır ve toplu karar gönderilir', () => {
    expect(view).toMatch(/const secimTuru = computed/)
    expect(view).toMatch(/const satirSecilebilir =/)
    expect(view).toMatch(/farkliTurSecimUyari/)
    expect(view).toMatch(/is-row-selectable/)
    expect(view).toMatch(/onaylarAPI\.toplu\(\{[\s\S]*?tur,[\s\S]*?kaynakIds:/)
    expect(view).toMatch(/topluKarar\('ONAY'/)
    expect(view).toMatch(/topluOnayla/)
    expect(view).toMatch(/topluReddet/)
  })

  it('ret gerekçesi prompt yerine erişilebilir modal ile alınır (min 3)', () => {
    expect(view).not.toMatch(/\bprompt\(/)
    expect(view).toMatch(/'onaylar\.retGerekceEtiket'/)
    expect(view).toMatch(/const retGecerli = computed\(\(\) => gerekce\.value\.trim\(\)\.length >= 3\)/)
    expect(view).toMatch(/<Textarea/)
    expect(view).toMatch(/aria-modal="true"/)
    expect(view).toMatch(/gerekceOdakla/)
    expect(view).toMatch(/gerekceTus/)
    expect(view).toMatch(/gerekceRef/)
    expect(view).toMatch(/retMinHata/)
  })

  it('toplu sonuç başarılı/atlanan özeti ile gösterilir', () => {
    expect(view).toMatch(/sonucuIsle/)
    expect(view).toMatch(/atlanan/)
    expect(view).toMatch(/sonucDialog/)
    expect(view).toMatch(/onaylar\.atlananKayit/)
  })

  it('detay drawer ortak alanlar, meta, tür rotası ve aksiyonlar içerir', () => {
    expect(view).toMatch(/<OnayDetayDrawer/)
    expect(drawer).toMatch(/kayit\.baslik/)
    expect(drawer).toMatch(/kayit\.altBaslik/)
    expect(drawer).toMatch(/kayit\.tutar/)
    expect(drawer).toMatch(/kayit\.tarih/)
    expect(drawer).toMatch(/kayit\.aciklama/)
    expect(drawer).toMatch(/metaSatirlari/)
    expect(drawer).toMatch(/IZIN: '\/izinler'/)
    expect(drawer).toMatch(/MASRAF: '\/masraflar'/)
    expect(drawer).toMatch(/SATINALMA: '\/satinalma'/)
    expect(drawer).toMatch(/SIPARIS: '\/siparisler'/)
    expect(drawer).toMatch(/ilgiliKaydaGit/)
    expect(drawer).toMatch(/\$emit\('onayla'/)
    expect(drawer).toMatch(/\$emit\('reddet'/)
    expect(drawer).toMatch(/:breakpoints="\{ '575px': '92vw' \}"/)
  })

  it('geçmiş sekmesi tür/karar/tarih/arama filtreleri ve sunucu sayfalaması kullanır', () => {
    expect(view).toMatch(/gecmisTur/)
    expect(view).toMatch(/gecmisKarar/)
    expect(view).toMatch(/gecmisAralik/)
    expect(view).toMatch(/gecmisQ/)
    expect(view).toMatch(/params\.baslangic/)
    expect(view).toMatch(/params\.bitis/)
    expect(view).toMatch(/onaylarAPI\.gecmis\(gecmisParams\(\)\)/)
    expect(view).toMatch(/gecmisSayfaDegisti/)
    expect(view).toMatch(/gecmisYok/)
  })

  it('mobil kart görünümü, Excel ve sonuç duyurusu (a11y) vardır', () => {
    expect(view).toMatch(/onay-mobil/)
    expect(view).toMatch(/kart-sec/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/aria-pressed/)
    expect(view).toMatch(/prefers-reduced-motion/)
    expect(view).toMatch(/excelAPI\.onaylar/)
    expect(view).toMatch(/formatCurrency\(/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
  })

  it('klavye ile sekme gezinmesi desteklenir', () => {
    expect(view).toMatch(/role="tablist"/)
    expect(view).toMatch(/role="tab"/)
    expect(view).toMatch(/:aria-selected="aktifSekme === index"/)
    expect(view).toMatch(/sekmeTus/)
    expect(view).toMatch(/ArrowRight/)
    expect(view).toMatch(/ArrowLeft/)
  })

  it('onay ayarları sekmesi Onaylar’dan çıkarılıp ayrı ekrana taşınmıştır', () => {
    expect(view).not.toMatch(/onayAyariAPI/)
    expect(view).not.toMatch(/ayarKaydet/)
    expect(view).not.toMatch(/onaylar\.onayAyarlari\b/)
    expect(view).toMatch(/onaylar\.onayAyarlariBaglanti/)
    expect(ayarView).toMatch(/onayAyariAPI/)
    expect(ayarView).toMatch(/silOnayla/)
    expect(ayarView).toMatch(/yeniKaydet/)
    expect(router).toMatch(/OnayAyarlari/)
  })
})
