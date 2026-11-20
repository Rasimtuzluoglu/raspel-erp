/**
 * Tek seferlik makyaj göçü: hedef dosyaların <style> bloklarındaki sabit
 * renkleri app.css token'larına çevirir. Şablon/JS içindeki grafik renklerine
 * (Chart.js, KpiKart renk propları) dokunmaz.
 *
 * Kullanım: node scripts/makyaj-hex-gocu.mjs [--dry]
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const SRC = join(process.cwd(), 'src')
const dry = process.argv.includes('--dry')

const DOSYALAR = [
  'views/Tahsilat.vue',
  'views/Raporlar.vue',
  'views/CekSenet.vue',
  'views/Teklifler.vue',
  'views/Muhasebe.vue',
  'views/Ajanda.vue',
  'views/Anomaliler.vue',
  'views/KarlilikAnalizi.vue',
  'components/QuickSearch.vue',
  'components/StokDetayDialog.vue'
]

// Yerel paletler bilinçli: giriş, durum sayfası, tema seçici ve kağıt blokları.
const DOKUNMA_BLOKLARI = [/\.teklif-mektup-kagit\s*\{[^}]*\}/g]

const ESLEME = [
  // anlam renkleri
  [/#ef4444\b/gi, 'var(--danger)'],
  [/#f87171\b/gi, 'var(--danger)'],
  [/#dc2626\b/gi, 'var(--danger)'],
  [/#991b1b\b/gi, 'var(--danger)'],
  [/#10b981\b/gi, 'var(--success)'],
  [/#22c55e\b/gi, 'var(--success)'],
  [/#4ade80\b/gi, 'var(--success)'],
  [/#34d399\b/gi, 'var(--success)'],
  [/#5eead4\b/gi, 'var(--accent)'],
  [/#f59e0b\b/gi, 'var(--warning)'],
  [/#fbbf24\b/gi, 'var(--warning)'],
  [/#f97316\b/gi, 'var(--warning)'],
  [/#e65100\b/gi, 'var(--warning)'],
  [/#3b82f6\b/gi, 'var(--info)'],
  [/#38bdf8\b/gi, 'var(--info)'],
  [/#06b6d4\b/gi, 'var(--info)'],
  [/#8b5cf6\b/gi, 'var(--purple)'],
  [/#a3e635\b/gi, 'var(--success)'],
  // yumuşak zeminler
  [/#fff3e0\b/gi, 'var(--warning-soft)'],
  [/#fce4ec\b/gi, 'var(--danger-soft)'],
  [/#880e4f\b/gi, 'var(--danger)'],
  [/#b45309\b/gi, 'var(--warning)'],
  [/#b91c1c\b/gi, 'var(--danger)'],
  // gri ölçeği
  [/#64748b\b/gi, 'var(--text-muted)'],
  [/#94a3b8\b/gi, 'var(--text-muted)'],
  [/#6f8294\b/gi, 'var(--text-muted)'],
  [/#9fb0c0\b/gi, 'var(--text-secondary)'],
  [/#475569\b/gi, 'var(--text-secondary)'],
  [/#cbd5e1\b/gi, 'var(--text-secondary)'],
  [/#e8eef5\b/gi, 'var(--text-primary)'],
  [/#e2e8f0\b/gi, 'var(--border)'],
  [/#f1f5f9\b/gi, 'var(--bg-secondary)'],
  [/#f8fafc\b/gi, 'var(--bg-secondary)'],
  [/#1e293b\b/gi, 'var(--text-primary)'],
  [/#0f172a\b/gi, 'var(--text-primary)'],
  [/#1f2937\b/gi, 'var(--text-primary)']
]

let toplam = 0
for (const dosya of DOSYALAR) {
  const yol = join(SRC, dosya)
  const kaynak = readFileSync(yol, 'utf8')
  let degisen = 0

  const yeni = kaynak.replace(/<style[\s\S]*?<\/style>/g, (blok) => {
    let sonuc = blok
    const korunan = []
    // `var(--token, #hex)` fallback'leri zaten token kullanır; çift sarmayı önle.
    sonuc = sonuc.replace(/var\(--[^)]*#[0-9a-fA-F]{3,8}[^)]*\)/g, (eslesme) => {
      korunan.push(eslesme)
      return `/*__KORUNAN_${korunan.length - 1}__*/`
    })
    for (const desen of DOKUNMA_BLOKLARI) {
      sonuc = sonuc.replace(desen, (eslesme) => {
        korunan.push(eslesme)
        return `/*__KORUNAN_${korunan.length - 1}__*/`
      })
    }
    for (const [desen, token] of ESLEME) {
      sonuc = sonuc.replace(desen, () => {
        degisen += 1
        return token
      })
    }
    korunan.forEach((blokMetni, indeks) => {
      sonuc = sonuc.replace(`/*__KORUNAN_${indeks}__*/`, blokMetni)
    })
    return sonuc
  })

  if (degisen > 0 && !dry) writeFileSync(yol, yeni, 'utf8')
  toplam += degisen
  console.log(`${dosya}: ${degisen} renk -> token`)
}
console.log(`Toplam: ${toplam}`)
