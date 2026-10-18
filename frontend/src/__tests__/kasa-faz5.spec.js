import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Kasa revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + KPI + yetki gating + bakiye gizle + ⋮ menü + hareket
 *  arama/filtre + aktarım doğrulama/önizleme + Excel bildirimi).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const kasa = oku('views/Kasa.vue')
const api = oku('api/modules/finans.js')

describe('Kasa revizyonu', () => {
  it('PageHeader + KPI özeti (/kasalar/ozet)', () => {
    expect(kasa).toMatch(/<PageHeader/)
    expect(kasa).toMatch(/<KpiKart/)
    expect(api).toMatch(/ozet\(\)\s*\{[\s\S]*?\/kasalar\/ozet/)
    expect(kasa).toMatch(/kasaAPI\.ozet\(/)
  })

  it('yetki kapıları (403 önleme) backend ile hizalı', () => {
    expect(kasa).toMatch(/const yazabilir = computed/)
    expect(kasa).toMatch(/const silebilir = computed/)
    expect(kasa).toMatch(/const aktarabilir = computed/)
    expect(kasa).toMatch(/FINANS_WRITE/)
    expect(kasa).toMatch(/FINANS_DELETE/)
    expect(kasa).toMatch(/<Menu[\s\S]*transferMenuModel/)
  })

  it('bakiye göster/gizle + Excel bildirimi', () => {
    expect(kasa).toMatch(/const bakiyeGorunur = ref/)
    expect(kasa).toMatch(/BAKIYE_ANAHTAR/)
    expect(kasa).toMatch(/excelYukleniyor/)
    expect(kasa).toMatch(/kasa\.excelIndirildi/)
    expect(kasa).toMatch(/kasa\.excelHata/)
    expect(kasa).not.toMatch(/\/\* silent \*\//)
  })

  it('kasa kartı ⋮ menüsü + seçili vurgusu', () => {
    expect(kasa).toMatch(/<SatirEylemleri/)
    expect(kasa).toMatch(/const kasaMenuItems/)
    expect(kasa).toMatch(/duzenle: yazabilir, sil: silebilir/)
    expect(kasa).toMatch(/class="pi pi-check-circle secili-ikon"/)
  })

  it('hareket arama/filtre + EmptyState', () => {
    expect(kasa).toMatch(/const hareketTurFiltre = ref/)
    expect(kasa).toMatch(/const hareketTarihFiltre = ref/)
    expect(kasa).toMatch(/const filtreliHareketler = computed/)
    expect(kasa).toMatch(/:value="filtreliHareketler"/)
    expect(kasa).not.toMatch(/<Message/)
  })

  it('aktarım doğrulama + bakiye önizlemesi', () => {
    expect(kasa).toMatch(/const kasaAktarAyni = computed/)
    expect(kasa).toMatch(/const kasaAktarGecerli = computed/)
    expect(kasa).toMatch(/const kasaAktarSonrasi = computed/)
    expect(kasa).toMatch(/bankaAktarGecerli|bankadanAktarGecerli/)
    expect(kasa).toMatch(/kasa\.yetersizBakiye/)
    expect(kasa).toMatch(/transfer-etki/)
  })

  it('dialog genişlikleri responsive + formatDate importu tek yerde', () => {
    const breakpointSayisi = (kasa.match(/:breakpoints="\{ '575px': '92vw' \}"/g) || []).length
    expect(breakpointSayisi).toBe(5)
    // Dosya sonundaki geç import kaldırıldı; üstte tek yerde.
    const importSayisi = (kasa.match(/formatTarih as formatDate/g) || []).length
    expect(importSayisi).toBe(1)
  })
})
