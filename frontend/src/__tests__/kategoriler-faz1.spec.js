import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Kategoriler revizyonu Faz 1 — API sozlesmesi ve veri akisi.
 * Backend uclari paralel gelistirildigi icin alan/uç adlari kaynak uzerinden
 * sabitlenir; kirilan bir sozlesme burada yakalanir.
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const api = oku('api/modules/stok.js')
const store = oku('stores/kategoriStore.js')
const view = oku('views/Kategoriler.vue')

describe('kategoriAPI sozlesmesi', () => {
  it('sayfali liste ve agac uclari', () => {
    expect(api).toMatch(/listele\(params\)\s*\{[\s\S]{0,80}\/kategoriler'/)
    expect(api).toMatch(/agac\(params\)\s*\{[\s\S]{0,80}\/kategoriler\/agac'/)
    expect(api).toMatch(/ozet\(params\)\s*\{[\s\S]{0,80}\/kategoriler\/ozet'/)
    expect(api).toMatch(/secenekler\(params\)\s*\{[\s\S]{0,80}\/kategoriler\/secenekler'/)
  })

  it('detay, kullanim, CRUD, arsiv, birlestirme ve sistem kategorileri uclari', () => {
    expect(api).toMatch(/getById\(id\)\s*\{[\s\S]{0,80}\/kategoriler\/\$\{id\}`/)
    expect(api).toMatch(/kullanim\(id\)\s*\{[\s\S]{0,80}\/kategoriler\/\$\{id\}\/kullanim`/)
    expect(api).toMatch(/update\(id, data\)\s*\{[\s\S]{0,80}apiClient\.put\(`\/kategoriler\/\$\{id\}`/)
    expect(api).toMatch(/arsivle\(id\)\s*\{[\s\S]{0,80}\/arsivle`/)
    expect(api).toMatch(/aktiflestir\(id\)\s*\{[\s\S]{0,80}\/aktiflestir`/)
    expect(api).toMatch(/birlestir\(data\)\s*\{[\s\S]{0,80}\/kategoriler\/birlestir'/)
    expect(api).toMatch(/sistemKategorileri\(\)\s*\{[\s\S]{0,80}\/kategoriler\/sistem-kategorileri'/)
  })

  it('geriye uyumlu getAll parametre kabul eder', () => {
    expect(api).toMatch(/getAll\(params\)\s*\{[\s\S]{0,80}apiClient\.get\('\/kategoriler', \{ params \}\)/)
  })
})

describe('kategoriStore akislari', () => {
  it('agac, ozet, secenekler, kullanim ve mutasyon action lari tanimli', () => {
    for (const ad of [
      'agacGetir',
      'ozetGetir',
      'seceneklerGetir',
      'kullanimGetir',
      'arsivle',
      'aktiflestir',
      'birlestir',
      'sistemKategorileriGetir',
      'onbellekTemizle'
    ]) {
      expect(store).toContain(ad)
    }
  })

  it('secenek onbellegi kapsam bazli tutulur', () => {
    expect(store).toMatch(/secenekOnbellek/)
    expect(store).toMatch(/\[kapsam\]: veri/)
  })
})

describe('Kategoriler.vue veri akisi', () => {
  it('bes kapsam ve kasa alt turleri backend enum degerleriyle hizali', () => {
    for (const kapsam of ['KASA_GELIR', 'KASA_GIDER', 'STOK', 'MASRAF', 'BUTCE']) {
      expect(view).toContain(kapsam)
    }
    expect(view).toContain('GECERLI_KAPSAM')
  })

  it('agac/ozet uclari ve hafiza anahtari kullanilir', () => {
    expect(view).toMatch(/kategoriStore\.agacGetir\(/)
    expect(view).toMatch(/kategoriStore\.ozetGetir\(/)
    expect(view).toContain('raspel_kategoriler_kapsam')
  })

  it('DTO alanlari (hiyerarsi, kullanim, alt kategori) tuketilir', () => {
    expect(view).toContain('ustKategoriId')
    expect(view).toContain('ustKategoriAd')
    expect(view).toContain('kullanimSayisi')
    expect(view).toContain('altKategoriSayisi')
    expect(view).toContain('altKategoriler')
  })

  it('kullanim drawer, form ve birlestirme akislari bagli', () => {
    expect(view).toMatch(/KategoriKullanimDrawer/)
    expect(view).toMatch(/KategoriFormDialog/)
    expect(view).toMatch(/kategoriStore\.birlestir\(/)
    expect(view).toMatch(/kategoriStore\.kullanimGetir\(/)
  })
})
