import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Anomaliler revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (kalıcı incelendi/yok say + ilgili kayda git + IP tam yönetim/ADMIN).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Anomaliler.vue')
const sistemApi = oku('api/modules/sistem.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sistem/AnomaliController.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sistem/AnomaliDurumService.java'),
  'utf8'
)
const ipService = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sistem/IpWhitelistService.java'),
  'utf8'
)
const engine = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sistem/AnomaliTespitEngine.java'),
  'utf8'
)
const migration = readFileSync(
  join(ROOT, 'backend/src/main/resources/db/migration/V166__anomali_durum.sql'),
  'utf8'
)

describe('Anomaliler — Faz 3', () => {
  it('kalıcı incelendi/yok say', () => {
    expect(view).toMatch(/const durumGuncelle = async/)
    expect(view).toMatch(/const anomaliAksiyonlari = /)
    expect(view).toMatch(/anomaliler\.incelendi/)
    expect(view).toMatch(/anomaliler\.yokSay/)
    expect(view).toMatch(/anomaliler\.geriAl/)
    expect(view).toMatch(/yokSayilanlariGoster/)
    expect(sistemApi).toMatch(/durumGuncelle\(anahtar, durum\)[\s\S]*?\/anomaliler\/\$\{anahtar\}\/durum/)
    expect(migration).toMatch(/CREATE TABLE IF NOT EXISTS sistem\.anomali_durum/)
    expect(service).toMatch(/isaretle\(Long sirketId, String anahtar, String durum/)
    expect(engine).toMatch(/UUID\.nameUUIDFromBytes/)
    expect(controller).toMatch(/@PutMapping\("\/\{anahtar\}\/durum"\)/)
  })

  it('ilgili kayda git', () => {
    expect(view).toMatch(/const ilgiliKaydaGit = /)
    expect(view).toMatch(/query: \{ no: item\.ilgiliKayitNo \}/)
  })

  it('IP tam yönetim + ADMIN', () => {
    expect(view).toMatch(/const ipDialogAc = /)
    expect(view).toMatch(/const ipAksiyonlari = /)
    expect(view).toMatch(/IPV4_CIDR/)
    expect(view).toMatch(/ipAktifMi/)
    expect(view).toMatch(/v-if="isAdmin"/)
    expect(sistemApi).toMatch(/updateIpWhitelist\(id, data\)/)
    expect(controller).toMatch(/@PutMapping\("\/ip-whitelist\/\{id\}"\)/)
    expect(controller).toMatch(/hasRole\('ADMIN'\)/)
    expect(ipService).toMatch(/IPV4_CIDR/)
    expect(ipService).toMatch(/existsBySirketIdAndIpAdresi/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'incelendi',
      'yokSayildi',
      'yokSay',
      'geriAl',
      'yokSayilanlariGoster',
      'ilgiliKaydaGit',
      'durumGuncellendi',
      'ipDuzenle',
      'ipAktif',
      'ipPasif',
      'ipAktifYap',
      'ipPasifYap',
      'ipSilOnay',
      'ipGecersiz',
      'ipGuncellendi'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
