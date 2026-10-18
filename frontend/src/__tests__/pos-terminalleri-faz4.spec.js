import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * POS Terminalleri revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + KPI + yetki gating + EmptyState + ⋮ menü + özet etkileşimi +
 *  banka zorunluluğu + Gün Sonu raporu + müşteri detayı).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const pos = oku('views/PosTerminalleri.vue')
const rapor = oku('components/PosGunSonuRaporDialog.vue')
const api = oku('api/modules/finans.js')

describe('POS Terminalleri revizyonu', () => {
  it('PageHeader + KPI özeti (client-side)', () => {
    expect(pos).toMatch(/<PageHeader/)
    expect(pos).toMatch(/<KpiKart/)
    expect(pos).toMatch(/const kpi = computed/)
    expect(pos).toMatch(/bugunCiro/)
    expect(pos).toMatch(/bugunKomisyon/)
  })

  it('yetki kapıları backend ile hizalı', () => {
    expect(pos).toMatch(/const yonetebilir = computed/)
    expect(pos).toMatch(/const silebilir = computed/)
    expect(pos).toMatch(/role === 'MUHASEBE'/)
    expect(pos).toMatch(/v-if="yonetebilir"/)
    expect(pos).toMatch(/duzenle: yonetebilir, sil: silebilir/)
  })

  it('EmptyState kullanımı (eski boş blok kaldırıldı)', () => {
    expect(pos).toMatch(/<EmptyState/)
    expect(pos).not.toMatch(/class="bos"/)
    expect(pos).not.toMatch(/<Message/)
  })

  it('kart ⋮ menüsü + özet etkileşimi', () => {
    expect(pos).toMatch(/<SatirEylemleri/)
    expect(pos).toMatch(/const posMenuItems/)
    expect(pos).toMatch(/const ozeteTikla/)
    expect(pos).toMatch(/vurguluPosId/)
    expect(pos).toMatch(/scrollIntoView/)
  })

  it('banka zorunluluğu', () => {
    expect(pos).toMatch(/posTerminalleri\.bankaZorunluUyari/)
    expect(pos).toMatch(/posTerminalleri\.onceBankaEkle/)
    expect(pos).toMatch(/:disabled="!form\.ad\?\.trim\(\) \|\| !form\.bankaId"/)
  })

  it('Gün Sonu raporu (yeni bileşen + uç)', () => {
    expect(api).toMatch(/gunSonuRapor\(\)\s*\{[\s\S]*?\/pos-terminalleri\/gun-sonu\/rapor/)
    expect(pos).toMatch(/<PosGunSonuRaporDialog/)
    expect(pos).toMatch(/posTerminalleri\.gunSonuRaporu/)
    expect(rapor).toMatch(/posAPI\.gunSonuRapor\(/)
    expect(rapor).toMatch(/<EmptyState/)
  })

  it('müşteri detayı: arama + sıralama + toplam', () => {
    expect(pos).toMatch(/const filtreliMusteriler = computed/)
    expect(pos).toMatch(/const musteriToplam = computed/)
    expect(pos).toMatch(/const musteriArama = ref/)
    expect(pos).toMatch(/class="musteri-arac"/)
  })

  it('dialog genişlikleri responsive', () => {
    const bp = (pos.match(/:breakpoints="\{ '575px': '92vw' \}"/g) || []).length
    expect(bp).toBe(2)
    expect(rapor).toMatch(/:breakpoints="\{ '575px': '92vw' \}"/)
  })
})
