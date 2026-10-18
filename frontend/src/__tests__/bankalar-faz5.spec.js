import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Bankalar revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + KPI + arama/sıralama + bakiye gizle + ⋮ menü + mobil kart +
 *  IBAN doğrulama + Drawer özet/filtre + Excel bildirimi).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const bankalar = oku('views/Bankalar.vue')
const api = oku('api/modules/finans.js')

describe('Bankalar revizyonu', () => {
  it('PageHeader + KPI özeti (/bankalar/ozet)', () => {
    expect(bankalar).toMatch(/<PageHeader/)
    expect(bankalar).toMatch(/<KpiKart/)
    expect(api).toMatch(/ozet\(\)\s*\{[\s\S]*?\/bankalar\/ozet/)
    expect(bankalar).toMatch(/bankaAPI\.ozet\(/)
  })

  it('satır aksiyonları ⋮ menüye taşındı (Sil yalnız admin)', () => {
    expect(bankalar).toMatch(/<SatirEylemleri/)
    expect(bankalar).toMatch(/const bankaMenuItems/)
    expect(bankalar).toMatch(/sil: authStore\.isAdmin/)
    // Eski ham aksiyon butonları kaldırıldı.
    expect(bankalar).not.toMatch(/p-button-help/)
  })

  it('arama + sıralama + bakiye gizle/göster', () => {
    expect(bankalar).toMatch(/arama-aktif/)
    expect(bankalar).toMatch(/\bsortable\b/)
    expect(bankalar).toMatch(/const bakiyeGorunur = ref/)
    expect(bankalar).toMatch(/BAKIYE_ANAHTAR/)
    expect(bankalar).toMatch(/bakiyeGorunurlukDegistir/)
  })

  it('mobil kart görünümü ve tercih', () => {
    expect(bankalar).toMatch(/class="banka-kartlar"/)
    expect(bankalar).toMatch(/const gosterim = ref/)
    expect(bankalar).toMatch(/GORUNUM_ANAHTAR/)
    expect(bankalar).toMatch(/matchMedia\('\(max-width: 768px\)'\)/)
  })

  it('IBAN (TR, 26 hane) doğrulaması', () => {
    expect(bankalar).toMatch(/IBAN_TR_RE/)
    expect(bankalar).toMatch(/const ibanGecerli = computed/)
    expect(bankalar).toMatch(/ibanNormalize/)
    expect(bankalar).toMatch(/bankalar\.ibanGecersiz/)
  })

  it('Drawer detay: borç/alacak özeti + durum/tarih filtresi', () => {
    expect(bankalar).toMatch(/hareketBorcToplam = computed/)
    expect(bankalar).toMatch(/hareketAlacakToplam = computed/)
    expect(bankalar).toMatch(/filtreliHareketler = computed/)
    expect(bankalar).toMatch(/const hareketDurum = ref/)
    expect(bankalar).toMatch(/const hareketTarih = ref/)
  })

  it('Excel export bildirimi (yükleniyor + toast)', () => {
    expect(bankalar).toMatch(/excelYukleniyor/)
    expect(bankalar).toMatch(/bankalar\.excelIndirildi/)
    expect(bankalar).toMatch(/bankalar\.excelHata/)
    // Eski sessiz catch kaldırıldı.
    expect(bankalar).not.toMatch(/\/\* silent \*\//)
  })
})
