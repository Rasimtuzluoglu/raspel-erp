import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Üretim revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (hat / iş merkezi ataması, doluluk, migration).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')
const okuR = (p) => readFileSync(join(ROOT, 'backend/src/main/resources', p), 'utf8')

const view = oku('views/Uretim.vue')
const stokApi = oku('api/modules/stok.js')
const drawer = oku('components/UretimEmriDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const hatEntity = okuB('entity/envanter/UretimHat.java')
const hatService = okuB('service/envanter/UretimHatService.java')
const hatController = okuB('controller/envanter/UretimHatController.java')
const uretimService = okuB('service/envanter/UretimService.java')
const uretimController = okuB('controller/envanter/UretimController.java')
const migration = okuR('db/migration/V167__uretim_hat.sql')

describe('Üretim — Faz 3', () => {
  it('backend: hat entity, CRUD ve doluluk', () => {
    expect(hatEntity).toMatch(/@Table\(name = "uretim_hat", schema = "stok"\)/)
    expect(hatService).toMatch(/public List<UretimHatDTO> listele\(Long sirketId\)/)
    expect(hatService).toMatch(/countAktifByHat/)
    expect(hatService).toMatch(/countGecikenByHat/)
    expect(hatService).toMatch(/Math\.min\(100, Math\.round\(aktif \* 100\.0 \/ kapasite\)\)/)
    expect(hatController).toMatch(/@RequestMapping\("\/api\/uretim\/hatlar"\)/)
  })

  it('backend: emir hat ataması ve filtre', () => {
    expect(uretimController).toMatch(/@PutMapping\("\/emirler\/\{id\}\/hat"\)/)
    expect(uretimController).toMatch(/@RequestParam\(required = false\) Long hatId/)
    expect(uretimService).toMatch(/public UretimEmriDTO emirHatAta\(Long id, Long hatId, Long sirketId, Long kullaniciId\)/)
    expect(uretimService).toMatch(/hatDogrula\(dto\.getHatId\(\), sirketId\)/)
    expect(uretimService).toMatch(/\.hatId\(e\.getHatId\(\)\)\.hatAd\(hatAd\(e\.getHatId\(\)\)\)/)
  })

  it('migration: tablo, kolon, FK ve indeks', () => {
    expect(migration).toMatch(/CREATE TABLE IF NOT EXISTS stok\.uretim_hat/)
    expect(migration).toMatch(/ADD COLUMN IF NOT EXISTS hat_id BIGINT/)
    expect(migration).toMatch(/FOREIGN KEY \(hat_id\) REFERENCES stok\.uretim_hat/)
    expect(migration).toMatch(/CREATE INDEX IF NOT EXISTS ix_uretim_emri_hat/)
  })

  it('frontend: hat sekmesi, doluluk kartları, atama ve filtre', () => {
    expect(view).toMatch(/uretim\.hatlarTab/)
    expect(view).toMatch(/class="hat-kartlari"/)
    expect(view).toMatch(/role="progressbar"/)
    expect(view).toMatch(/const hatAtaKaydet = async/)
    expect(view).toMatch(/uretimAPI\.emirHatAta\(seciliEmir\.value\.id, hatAtaSecim\.value \|\| null\)/)
    expect(view).toMatch(/v-model="emirHat"/)
    expect(view).toMatch(/if \(emirHat\.value\) p\.hatId = emirHat\.value/)
    expect(drawer).toMatch(/emir\.hatAd \|\| '-'/)
    expect(stokApi).toMatch(/hatlar\(\)\s*\{[\s\S]*?\/uretim\/hatlar/)
    expect(stokApi).toMatch(/emirHatAta\(id, hatId\)\s*\{[\s\S]*?\/uretim\/emirler\/\$\{id\}\/hat/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'hat',
      'hatlar',
      'hatlarTab',
      'yeniHat',
      'hatDuzenle',
      'hatAta',
      'hatAdZorunlu',
      'hatKapasite',
      'hatDoluluk',
      'hatSilOnay',
      'hatAtandi'
    ]) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
