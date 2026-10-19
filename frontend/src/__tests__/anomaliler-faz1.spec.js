import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Anomaliler revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (KPI + sekme hatırlama + seviye/tür çevirisi + EmptyState).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Anomaliler.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const engine = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sistem/AnomaliTespitEngine.java'),
  'utf8'
)

describe('Anomaliler — Faz 1', () => {
  it('KPI kartları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/anomaliler\.kpiToplam/)
    expect(view).toMatch(/anomaliler\.kpiKritik/)
    expect(view).toMatch(/const kritikYuksekSayisi = computed/)
    expect(view).toMatch(/const ortaSayisi = computed/)
    expect(view).toMatch(/const dusukSayisi = computed/)
  })

  it('sekme bağlama + hatırlama', () => {
    expect(view).toMatch(/v-model:active-index="aktifIndex"/)
    expect(view).toMatch(/raspel_anomaliler_tab/)
    expect(view).toMatch(/localStorage\.setItem\(AKTIF_SEKME_ANAHTAR/)
  })

  it('seviye/tür çevirisi + EmptyState', () => {
    expect(view).toMatch(/const seviyeLabel = /)
    expect(view).toMatch(/const turLabel = /)
    expect(view).toMatch(/seviyeLabel\(item\.seviye\)/)
    expect(view).toMatch(/turLabel\(item\.tur\)/)
    expect(view).toMatch(/<EmptyState/)
  })

  it('toast tekilleştirme + tür adı düzeltmesi', () => {
    expect(view).not.toMatch(/useToast\(/)
    expect(engine).toMatch(/YUKSEK_TUTAR/)
    expect(engine).not.toMatch(/ANORMAL_MASRAF/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'kpiToplam',
      'kpiKritik',
      'kpiOrta',
      'kpiDusuk',
      'kpiIp',
      'seviyeKritik',
      'seviyeYuksek',
      'seviyeOrta',
      'seviyeDusuk',
      'turMukerrerFatura',
      'turMukerrerOdeme',
      'turYuksekTutar'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
