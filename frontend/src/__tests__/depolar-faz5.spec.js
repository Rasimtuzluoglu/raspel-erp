import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Depolar revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + KPI + ⋮ menü + Depo 360 + sekme geçişi + mobil kart +
 *  transfer/talep akışları + miktar biçimi).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const depolar = oku('views/Depolar.vue')
const dialog360 = oku('components/DepoDetayDialog.vue')
const api = oku('api/modules/stok.js')

describe('Depolar revizyonu', () => {
  it('PageHeader + KPI özeti (/depolar/ozet)', () => {
    expect(depolar).toMatch(/<PageHeader/)
    expect(depolar).toMatch(/<KpiKart/)
    expect(api).toMatch(/ozet\(\)\s*\{[\s\S]*?\/depolar\/ozet/)
    expect(depolar).toMatch(/depoAPI\.ozet\(/)
  })

  it('satır aksiyonları ⋮ menüye taşındı (SatirEylemleri)', () => {
    expect(depolar).toMatch(/<SatirEylemleri/)
    expect(depolar).toMatch(/const depoMenuItems/)
    // Eski ham aksiyon butonları kaldırıldı.
    expect(depolar).not.toMatch(/p-button-rounded p-button-text/)
  })

  it('Depo 360 detay modalı + sekme otomatik geçişi', () => {
    expect(depolar).toMatch(/<DepoDetayDialog/)
    expect(dialog360).toMatch(/depo-360/)
    expect(depolar).toMatch(/v-model:active-index="aktifSekme"/)
    expect(depolar).toMatch(/aktifSekme\.value = 1/)
  })

  it('mobil kart görünümü ve tercih', () => {
    expect(depolar).toMatch(/class="depo-kartlar"/)
    expect(depolar).toMatch(/const gosterim = ref/)
    expect(depolar).toMatch(/matchMedia\('\(max-width: 768px\)'\)/)
  })

  it('miktar adet olarak biçimlenir (formatCurrency kaldırıldı)', () => {
    expect(depolar).toMatch(/const formatMiktar/)
    expect(depolar).not.toMatch(/formatCurrency\(/)
  })

  it('transfer doğrulaması + talep oluşturma akışı', () => {
    expect(depolar).toMatch(/const hedefAyni = computed/)
    expect(depolar).toMatch(/const transferGecerli = computed/)
    expect(depolar).toMatch(/transferSonrasi/)
    expect(depolar).toMatch(/depoTransferAPI\.create\(/)
    expect(depolar).toMatch(/const talepGecerli = computed/)
    expect(api).toMatch(/create\(data\)\s*\{[\s\S]*?\/depo-transferler/)
  })

  it('depo stokları sekmesi: arama + özet + sıfır vurgusu', () => {
    expect(depolar).toMatch(/const depoStokArama/)
    expect(depolar).toMatch(/const depoStokToplam = computed/)
    expect(depolar).toMatch(/const filtreliDepoStoklari = computed/)
    expect(depolar).toMatch(/mevcut-stok-bilgi/)
  })
})
