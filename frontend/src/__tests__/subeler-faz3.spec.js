import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Şubeler revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (doğrulama + silme guard'ı + ADMIN yazma + detay çekmecesi/bağlı depolar).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Subeler.vue')
const drawer = oku('components/SubeDetayDrawer.vue')
const sistemApi = oku('api/modules/sistem.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/sube/SubeController.java')
const service = okuB('service/sube/SubeService.java')
const subeRepo = okuB('repository/sube/SubeRepository.java')
const depoRepo = okuB('repository/sube/DepoRepository.java')

describe('Subeler — Faz 3', () => {
  it('backend: ADMIN yazma + silme guard + bağlı depolar', () => {
    expect(controller).toMatch(/@PostMapping[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
    expect(controller).toMatch(/@PutMapping[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
    expect(controller).toMatch(/@GetMapping\("\/\{id\}\/depolar"\)/)
    expect(service).toMatch(/depoRepository\.existsBySubeId/)
    expect(service).toMatch(/BusinessException\("Bu şubeye bağlı depolar var/)
    expect(depoRepo).toMatch(/existsBySubeId/)
    expect(depoRepo).toMatch(/findBySubeIdAndSirketIdOrderByAdAsc/)
  })

  it('backend: ad zorunlu/benzersiz + telefon doğrulama', () => {
    expect(service).toMatch(/adDogrula/)
    expect(service).toMatch(/Şube adı zorunludur/)
    expect(service).toMatch(/existsBySirketIdAndAdIgnoreCase/)
    expect(service).toMatch(/existsBySirketIdAndAdIgnoreCaseAndIdNot/)
    expect(service).toMatch(/telefonDogrula/)
    expect(service).toMatch(/TELEFON_DESENI/)
    expect(subeRepo).toMatch(/existsBySirketIdAndAdIgnoreCaseAndIdNot/)
  })

  it('frontend: ADMIN gating + ⋮ aksiyonları + detay çekmecesi', () => {
    expect(view).toMatch(/useAuthStore/)
    expect(view).toMatch(/const isAdmin = computed/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const subeAksiyonlari = \(d\)/)
    expect(view).toMatch(/<SubeDetayDrawer/)
    expect(view).toMatch(/v-if="isAdmin"/)
    expect(drawer).toMatch(/bagliDepolar/)
    expect(drawer).toMatch(/depolar/)
    expect(sistemApi).toMatch(/depolar\(id\)\s*\{[\s\S]*?\/subeler\/\$\{id\}\/depolar/)
  })

  it('frontend: form doğrulama + aktif create', () => {
    expect(view).toMatch(/subeler\.adZorunlu/)
    expect(view).toMatch(/subeler\.telefonGecersiz/)
    expect(view).not.toMatch(/v-if="duzenleme"\s*\n\s*class="field"/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'bagliDepolar', 'bagliDepoYok', 'olusturma', 'adZorunlu', 'telefonGecersiz']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
