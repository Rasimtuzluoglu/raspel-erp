import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * İzinler revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (sunucu doğrulaması/gün hesabı + onay yetkisi + detay çekmecesi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Izinler.vue')
const personelView = oku('views/Personel.vue')
const drawer = oku('components/IzinDetayDrawer.vue')
const ikApi = oku('api/modules/ik.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelIzinController.java')
const service = okuB('service/ik/PersonelIzinService.java')

describe('İzinler — Faz 3', () => {
  it('backend: izin doğrulaması + sunucu gün hesabı', () => {
    expect(service).toMatch(/izinDogrula\(/)
    expect(service).toMatch(/Personel seçilmelidir/)
    expect(service).toMatch(/İzin türü zorunludur/)
    expect(service).toMatch(/Bitiş tarihi başlangıç tarihinden önce olamaz/)
    expect(service).toMatch(/gunHesapla\(/)
    expect(service).toMatch(/ChronoUnit\.DAYS\.between/)
    expect(service).toMatch(/Geçersiz izin durumu/)
  })

  it('backend: durum istemciden yazılamaz, yetki uçları korumalı', () => {
    expect(service).not.toMatch(/izin\.setDurum\(dto\.getDurum\(\)\)/)
    expect(controller).toMatch(/@PutMapping\("\/\{id\}\/durum"\)[\s\S]*?@PreAuthorize\("hasAnyRole\('ADMIN', 'MUHASEBE'\)"\)/)
    expect(controller).toMatch(/@DeleteMapping\("\/\{id\}"\)[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
  })

  it('frontend: onay yetkisi ADMIN/MUHASEBE + ⋮ aksiyonları + detay çekmecesi', () => {
    expect(view).toMatch(/const canApprove = computed/)
    expect(view).toMatch(/'ADMIN', 'MUHASEBE'/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const izinAksiyonlari = \(i\)/)
    expect(view).toMatch(/<IzinDetayDrawer/)
    expect(view).toMatch(/personelIzinAPI\.getById\(i\.id\)/)
    expect(drawer).toMatch(/onaylanabilir/)
    expect(drawer).toMatch(/onaylayan/)
    expect(ikApi).toMatch(/getById\(id\)\s*\{[\s\S]*?\/personel-izin\/\$\{id\}/)
  })

  it('frontend: gün sayısı istemcide hesaplanmaz', () => {
    expect(personelView).not.toMatch(/gunSayisi = Math\.ceil/)
    expect(personelView).not.toMatch(/\bgunSayisi,/)
    expect(personelView).toMatch(/personel\.izinBitisOnce/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'detayYuklenemedi', 'onaylayan', 'olusturma']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
    expect(tr).toMatch(/"izinBitisOnce"/)
    expect(en).toMatch(/"izinBitisOnce"/)
  })
})
