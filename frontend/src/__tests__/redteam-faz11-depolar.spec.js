import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const KOK = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(KOK, p), 'utf8')
const depolar = oku('views/Depolar.vue')
const liste = oku('components/depo/DepoListePaneli.vue')
const stok = oku('components/depo/DepoStokPaneli.vue')
const transfer = oku('components/depo/DepoTransferPaneli.vue')
const hareket = oku('components/depo/DepoHareketPaneli.vue')
const form = oku('components/depo/DepoFormDialog.vue')
const kpi = oku('components/depo/DepoOzetKartlari.vue')
const tr = JSON.parse(readFileSync(join(KOK, 'locales', 'tr.json'), 'utf8'))
const en = JSON.parse(readFileSync(join(KOK, 'locales', 'en.json'), 'utf8'))

describe('Faz11 - Depolar operasyon merkezi (UI)', () => {
  it('Depolar.vue yeni panelleri ve formu kullanir', () => {
    for (const c of ['DepoOzetKartlari', 'DepoListePaneli', 'DepoStokPaneli', 'DepoTransferPaneli', 'DepoHareketPaneli', 'DepoFormDialog']) {
      expect(depolar).toContain(`<${c}`)
    }
    expect(depolar).toContain('@kpi-sec="kpiSec"')
    expect(depolar).toContain('islem-yapabilir="islemYapabilir(seciliDepo)"')
  })

  it('KPI seridi tiklanabilir ve geciken talebi gosterir', () => {
    expect(kpi).toContain("$emit('kpi-sec'")
    expect(kpi).toContain('kpiGecikenTalep')
  })

  it('liste paneli arama/filtre/sayfalama ve CSV icerir', () => {
    expect(liste).toContain('useDepoStore')
    expect(liste).toContain('filtreDegistir')
    expect(liste).toContain('Paginator')
    expect(liste).toContain("tur: 'liste'")
    expect(liste).toContain('sadeceKritik')
  })

  it('stok paneli negatif/sifir miktari engeller ve hareket emit eder', () => {
    expect(stok).toContain('const gecerli = computed')
    expect(stok).toContain('Number(form.value.miktar) > 0')
    // InputNumber modeli blur'da yaziyor; tus vurusunda tazelenmezse
    // miktar yazildigi halde "Ekle" kapali kaliyordu.
    expect(stok).toContain('@keyup="miktarTazele"')
    expect(stok).toContain("'hareket-goster'")
    expect(stok).toContain("tur: 'stok'")
  })

  it('transfer paneli ADMIN aksiyonlari ve gerekceli red icerir', () => {
    expect(transfer).toContain('v-if="adminMi"')
    expect(transfer).toContain('redSebep.trim().length < 3')
    expect(transfer).toContain("durum === 'BEKLIYOR'")
    expect(transfer).toContain('slaDurumu')
    expect(transfer).toContain("tur: 'transfer'")
  })

  it('hareket paneli kullanici yoksa tire gosterir', () => {
    expect(hareket).toContain("data.kullaniciAd || '—'")
    expect(hareket).toContain('depoAPI.hareketler')
  })

  it('depo formu kapasite ve sorumlu kullanici alanlarini icerir', () => {
    expect(form).toContain('form.kapasiteKalem')
    expect(form).toContain('form.sorumluKullaniciId')
    expect(form).toContain(':min="0"')
  })

  it('yeni i18n anahtarlari iki dilde de var', () => {
    const anahtarlar = ['kpiGecikenTalep', 'depoAra', 'sadeceKritik', 'csvIndir', 'stokDegeri', 'doluluk', 'sorumluKullanici', 'kapasite', 'minStok', 'hareketler', 'transferler', 'guzergah', 'talepEden', 'sla', 'slaNORMAL', 'slaYAKLASIYOR', 'slaGECIKTI', 'onayla', 'reddet', 'redSebebi', 'bekleyen', 'onaylanan', 'reddedilen', 'tumu', 'onceDepoSecin', 'hareketTipi', 'hareketYok']
    for (const k of anahtarlar) {
      expect(tr.depolar[k], `tr.depolar.${k}`).toBeTruthy()
      expect(en.depolar[k], `en.depolar.${k}`).toBeTruthy()
    }
  })
})
