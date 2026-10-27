import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Ajanda revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (görev düzenleme, detay drawer'ı, filtreleme/sıralama, satır eylemleri, takvim çipleri).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Ajanda.vue')
const drawer = oku('components/AjandaGorevDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Ajanda — Faz 3', () => {
  it('görev düzenleme mevcut PUT uç noktasını kullanır', () => {
    expect(view).toMatch(/ajandaAPI\.gorevGuncelle\(duzenlenenGorevId\.value, govde\)/)
    expect(view).toMatch(/gorevDuzenleAc/)
    expect(view).toMatch(/gorevDuzenleBaslik/)
    expect(view).toMatch(/@hide="gorevDialogSifirla"/)
    expect(view).toMatch(/gorevDialogSifirla = \(\) => \{[\s\S]*?duzenlenenGorevId\.value = null/)
  })

  it('görev listesi arama + durum filtresi + gecikmiş önce sıralama yapar', () => {
    expect(view).toMatch(/const gorevArama = ref\(''\)/)
    expect(view).toMatch(/const gorevDurumFiltre = ref\('TUMU'\)/)
    expect(view).toMatch(/const filtrelenmisGorevler = computed\(/)
    expect(view).toMatch(/gorevGecikmis\(a\) \? 0 : 1/)
    expect(view).toMatch(/durumGecikmis/)
    expect(view).toMatch(/gorevSayisi/)
  })

  it('satır eylemleri ⋮ menüsü ile düzenle/sil/tamamla sunar', () => {
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/:items="gorevEylemleri\(g\)"/)
    expect(view).toMatch(/@duzenle="gorevDuzenleAc\(g\)"/)
    expect(view).toMatch(/etiket: g\.durum === 'TAMAMLANDI' \? t\('ajanda\.geriAl'\) : t\('ajanda\.tamamla'\)/)
  })

  it('detay drawer bileşeni görev/hatırlatıcı ayrımını ve aksiyonları içerir', () => {
    expect(drawer).toMatch(/position="right"/)
    expect(drawer).toMatch(/:breakpoints/)
    expect(drawer).toMatch(/emit\('duzenle'|defineEmits\(\['update:visible', 'duzenle', 'tamamla', 'sil'\]\)/)
    expect(drawer).toMatch(/hatirlaticiDetay/)
    expect(drawer).toMatch(/gorevDetay/)
    expect(drawer).toMatch(/formatTarihSaat\(kayit\.hatirlatmaZamani\)/)
    expect(view).toMatch(/<AjandaGorevDetayDrawer/)
    expect(view).toMatch(/@tamamla="gorevDurumDegistir"/)
  })

  it('takvim tip çipleri erişilebilir filtre sunar', () => {
    expect(view).toMatch(/class="takvim-lejant"/)
    expect(view).toMatch(/:aria-pressed="takvimTipFiltre === c\.tip"/)
    expect(view).toMatch(/const takvimTipFiltre = ref\('TUMU'\)/)
    expect(view).toMatch(/takvimTipFiltre\.value === tip \? 'TUMU' : tip/)
    expect(view).toMatch(/takvimTipFiltre\.value === 'TUMU' \|\| o\.tip === takvimTipFiltre\.value/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'gorevAra',
      'gorevSayisi',
      'filtreSonucYok',
      'detay',
      'gorevDetay',
      'hatirlaticiDetay',
      'tamamla',
      'geriAl',
      'duzenle',
      'gorevDuzenlendi',
      'gorevGuncellenemedi',
      'gorevDuzenleBaslik',
      'durumBekleyen',
      'durumTamamlanan',
      'durumGecikmis',
      'tumDurumlar',
      'tipFiltre'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
