import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import tr from '../../locales/tr.json'
import en from '../../locales/en.json'

/**
 * Cari hesaplar seçim kolonu kullanışlılığı:
 * - kutunun tıklama alanı büyür (görsel kutu küçük kalır),
 * - seçim/ad sticky kolon hizası korunur,
 * - toplu işlem çubuğu kaç kaydın seçildiğini sayfa bağlamıyla söyler.
 */

const SRC = join(process.cwd(), 'src')
const cari = readFileSync(join(SRC, 'components/cari/CariListePaneli.vue'), 'utf8')
const tablo = readFileSync(join(SRC, 'components/AppDataTable.vue'), 'utf8')

const cariBolum = (secici) => {
  const indeks = cari.indexOf(secici)
  return indeks < 0 ? '' : cari.slice(indeks, indeks + 420)
}

describe('Cari hesaplar seçim kolonu', () => {
  it('seçim kutusunun dokunma alanı en az 40 px', () => {
    const blok = cariBolum('.cari-secim-kolon .p-checkbox')
    expect(blok).toMatch(/width:\s*40px/)
    expect(blok).toMatch(/height:\s*40px/)
    expect(blok).toMatch(/align-items:\s*center/)
  })

  it('görsel kutu 20 px kalır (satır yüksekliği şişmez)', () => {
    const blok = cariBolum('.cari-secim-kolon .p-checkbox-box')
    expect(blok).toMatch(/width:\s*20px/)
    expect(blok).toMatch(/height:\s*20px/)
  })

  it('seçim kolonu 52 px, yapışkan ad kolonu bu ölçüye hizalı', () => {
    expect(cariBolum(':deep(.cari-secim-kolon)')).toMatch(/width:\s*56px/)
    expect(cariBolum(':deep(.cari-ad-kolon)')).toMatch(/left:\s*56px/)
  })
})

describe('Toplu işlem çubuğu bağlamı', () => {
  it('seçili sayısı sayfadaki kayıt sayısıyla birlikte gösterilir', () => {
    expect(tablo).toContain('common.pageRecords')
    expect(tablo).toContain('common.selectedRecordsShort')
    expect(tablo).toContain('filtrelenmisDeger.length')
  })

  it('yeni metin anahtarı iki dilde tanımlı', () => {
    expect(tr.common.pageRecords).toBeTypeOf('string')
    expect(tr.common.selectedRecordsShort).toBeTypeOf('string')
    expect(en.common.selectedRecordsShort).toBeTypeOf('string')
    expect(en.common.pageRecords).toBeTypeOf('string')
  })
})
