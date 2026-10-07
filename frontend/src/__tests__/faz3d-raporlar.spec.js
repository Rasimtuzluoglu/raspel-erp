import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Faz3D — Raporlar revizyonu (kaynak-tabanlı doğrulama).
 */

const SRC = join(process.cwd(), 'src')
const kod = readFileSync(join(SRC, 'views/Raporlar.vue'), 'utf8')

describe('Faz3D — Raporlar', () => {
  it('favoriler key tabanlı (index kaydedilmez, açılışta key->index çözülür)', () => {
    // raporFavoriDegistir push ederken index YAZMAMALI
    expect(kod).toMatch(/favoriRaporlar\.value\.push\(\{\s*key,\s*ad,/)
    expect(kod).toMatch(/favoriRaporlar\.value\.findIndex\(\(r\)\s*=>\s*r\.key === key\)/)
    // favoriAc key ile çözer
    expect(kod).toMatch(/RAPOR_SEKMELERI\.value\.find\(\(s\)\s*=>\s*s\.key === r\.key\)/)
  })

  it('üst tarih aralığı tüm tarih kullanan sekmeleri besler', () => {
    expect(kod).toMatch(/const tarihAraliginiUygula\s*=/)
    expect(kod).toMatch(/idx === 4\) \{ ckBas\.value = bas; ckBit\.value = bit \}/)
    expect(kod).toMatch(/tarihAraliginiUygula\(idx\)/)
    // Temsilci (9) artık kendi bileşeninde tarihAraligi prop'unu uygular.
    const temsilci = readFileSync(join(SRC, 'components/RaporTemsilci.vue'), 'utf8')
    expect(temsilci).toMatch(/tarihAraligi/)
    expect(temsilci).toMatch(/tarihUygula/)
  })

  it('tedarikçi ve ürün kârlılık tabloları ayrı bileşende sayfalanır', () => {
    const tu = readFileSync(join(SRC, 'components/RaporTedarikciUrunler.vue'), 'utf8')
    const uk = readFileSync(join(SRC, 'components/RaporUrunKarlilik.vue'), 'utf8')
    expect(tu).toMatch(/:value="tuFiltrelenmisData"[\s\S]{0,220}:paginator="true"/)
    expect(uk).toMatch(/:value="ukData"[\s\S]{0,160}:paginator="true"/)
    // Parent artık bu tabloları içermez (bileşene taşındı).
    expect(kod).toMatch(/<RaporTedarikciUrunler/)
    expect(kod).toMatch(/<RaporUrunKarlilik/)
  })

  it('export para birimi farkı kullanıcıya bildirilir', () => {
    expect(kod).toMatch(/raporlar\.exportParaBirimiNotu/)
  })
})
