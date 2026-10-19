import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Stok Sayımı revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (TASLAK düzenleme + detay çekmecesi + sunucu taraflı fark/doğrulama).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSayim.vue')
const drawer = oku('components/StokSayimDetayDrawer.vue')
const stokApi = oku('api/modules/stok.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSayimService.java'),
  'utf8'
)

describe('StokSayim — Faz 3', () => {
  it('API update + ⋮ eylem menüsü', () => {
    expect(stokApi).toMatch(/update\(id, data\)[\s\S]*?stok-sayim\/\$\{id\}/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const sayimAksiyonlari = /)
    expect(view).toMatch(/stokSayimAPI\.update\(form\.value\.id/)
  })

  it('detay çekmecesi + açıklama alanı', () => {
    expect(view).toMatch(/<StokSayimDetayDrawer/)
    expect(view).toMatch(/stokSayim\.aciklamaAlani/)
    expect(drawer).toMatch(/formatNumber\(fark\)/)
    expect(drawer).toMatch(/durumLabel\(sayim\.durum, t\)/)
  })

  it('form doğrulaması', () => {
    expect(view).toMatch(/stokSayim\.tarihZorunlu/)
    expect(view).toMatch(/stokSayim\.urunZorunlu/)
    expect(view).toMatch(/stokSayim\.sayilanNegatifOlamaz/)
  })

  it('backend: yalnız TASLAK düzenlenebilir + sunucu taraflı fark', () => {
    expect(service).toMatch(/Yalnızca taslak sayımlar düzenlenebilir/)
    expect(service).toMatch(/fark\(sayilan\.subtract\(beklenen\)\)/)
    expect(service).toMatch(/sayim\.setFark\(nz\(sayim\.getSayilanMiktar\(\)\)\.subtract/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'aciklamaAlani', 'olusturma', 'sayilanNegatifOlamaz']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
