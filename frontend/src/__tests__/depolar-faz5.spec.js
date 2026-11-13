import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Depolar revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + KPI + ⋮ menü + Depo 360 + sekme geçişi + mobil kart +
 *  transfer/talep akışları + miktar biçimi).
 *
 * Faz 11'de liste/stok/transfer/hareket panelleri ayrı bileşenlere taşındı;
 * davranış aynı kaldığı için doğrulamalar ilgili dosyada sürüyor.
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const depolar = oku('views/Depolar.vue')
const dialog360 = oku('components/DepoDetayDialog.vue')
const api = oku('api/modules/stok.js')
const listePanel = oku('components/depo/DepoListePaneli.vue')
const stokPanel = oku('components/depo/DepoStokPaneli.vue')
const kpiSerit = oku('components/depo/DepoOzetKartlari.vue')

describe('Depolar revizyonu', () => {
  it('PageHeader + KPI özeti (/depolar/ozet)', () => {
    expect(depolar).toMatch(/<PageHeader/)
    // KPI şeridi Faz 11'de bileşene taşındı; kartlar hâlâ KpiKart ile çizilir.
    expect(depolar).toMatch(/<DepoOzetKartlari/)
    expect(kpiSerit).toMatch(/<KpiKart/)
    expect(api).toMatch(/ozet\(\)\s*\{[\s\S]*?\/depolar\/ozet/)
    expect(depolar).toMatch(/depoAPI\.ozet\(/)
  })

  it('satır aksiyonları ⋮ menüye taşındı (SatirEylemleri)', () => {
    expect(listePanel).toMatch(/<SatirEylemleri/)
    expect(listePanel).toMatch(/const menuItems/)
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
    expect(listePanel).toMatch(/class="depo-kartlar"/)
    expect(depolar).toMatch(/const gosterim = ref/)
    expect(depolar).toMatch(/matchMedia\('\(max-width: 768px\)'\)/)
  })

  it('miktar adet olarak biçimlenir (formatCurrency kaldırıldı)', () => {
    expect(depolar).toMatch(/const formatMiktar/)
    // Liste/stok panelleri para biçimini yalnız değer kolonlarında kullanır.
    expect(stokPanel).toMatch(/const formatMiktar/)
    expect(stokPanel).not.toMatch(/formatMiktar\([^)]*\)\s*:\s*formatCurrency/)
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
    // Sekme içeriği Faz 11'de DepoStokPaneli'ne taşındı: sunucu taraflı arama,
    // toplam kalem sayacı ve mevcut stok bilgisi aynı sözleşmeyi korur.
    expect(stokPanel).toMatch(/const q = ref/)
    expect(stokPanel).toMatch(/const toplam = ref/)
    expect(stokPanel).toMatch(/toplam\.value = Number\(r\.data\?\.totalElements/)
    expect(stokPanel).toMatch(/mevcut-stok-bilgi/)
    expect(depolar).toMatch(/<DepoStokPaneli/)
  })
})
