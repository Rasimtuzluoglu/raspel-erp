import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Faz3C — Hesap Ayarları revizyonu (kaynak-tabanlı doğrulama).
 */

const SRC = join(process.cwd(), 'src')
const kod = readFileSync(join(SRC, 'views/HesapAyarlari.vue'), 'utf8')

describe('Faz3C — Hesap Ayarları', () => {
  it('Kişisel / Sistem sekme gruplaması var', () => {
    expect(kod).toMatch(/const kisiselMi\s*=/)
    expect(kod).toMatch(/const bolumGrubu\s*=/)
    expect(kod).toMatch(/<SelectButton/)
    expect(kod).toMatch(/v-if="kisiselMi\(0\)"/)
    expect(kod).toMatch(/v-if="kisiselMi\(7\)"/)
  })

  it('şifre değiştirme tek kaynaktan (PasswordChangeModal)', () => {
    expect(kod).toMatch(/import PasswordChangeModal from '\.\.\/components\/PasswordChangeModal\.vue'/)
    expect(kod).toMatch(/<PasswordChangeModal v-model:visible="sifreModalAcik"/)
    // Eski satır içi şifre formu kaldırıldı (duplikasyon yok).
    expect(kod).not.toMatch(/sifreForm\.mevcutSifre/)
  })

  it('avatar yükleme akışı var (dosya -> URL)', () => {
    expect(kod).toMatch(/const avatarSec\s*=/)
    expect(kod).toMatch(/uploadAPI\.foto\(/)
  })

  it('bildirim tipleri BildirimZili ile hizalı (AJANDA dahil)', () => {
    expect(kod).toMatch(/value: 'AJANDA'/)
  })
})
