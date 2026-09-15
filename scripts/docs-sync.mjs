#!/usr/bin/env node
/**
 * docs-sync.mjs — Dokümanlardaki yapısal sayıları (view/component/composable/store)
 * gerçek dosya sisteminden üretir ve AGENTS.md / README.md ile senkronize eder.
 *
 * Kullanım:
 *   node scripts/docs-sync.mjs           # dokümanları güncelle
 *   node scripts/docs-sync.mjs --check   # drift varsa hata ile çık (CI gate)
 *
 * Not: Test sayıları bu script kapsamında DEĞİLDİR; test suite'leri çalıştırıldığında
 * (backend 965 / frontend 614) elle güncellenir. Burada amaç, dosya sisteminden
 * türetilebilen yapısal sayıların bir daha "yanlış beyan" olmamasıdır.
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const CHECK = process.argv.includes('--check')

const say = (rel, ext) => fs.readdirSync(path.join(ROOT, rel)).filter((f) => f.endsWith(ext)).length

const stats = {
  views: say('frontend/src/views', '.vue'),
  components: say('frontend/src/components', '.vue'),
  composables: say('frontend/src/composables', '.js'),
  stores: say('frontend/src/stores', '.js'),
  apiModulleri: say('frontend/src/api/modules', '.js')
}

// Kural: {dosya, regex, yeni} — tüm eşleşmeler "yeni" ile değiştirilir.
const kurallar = [
  { dosya: 'AGENTS.md', re: /# \d+\+? views \(lazy-loaded\)/g, yeni: `# ${stats.views} views (lazy-loaded)`, ad: 'views' },
  { dosya: 'AGENTS.md', re: /# \d+\+? shared components/g, yeni: `# ${stats.components} shared components`, ad: 'components' },
  { dosya: 'AGENTS.md', re: /# \d+ composables/g, yeni: `# ${stats.composables} composables`, ad: 'composables' },
  { dosya: 'AGENTS.md', re: /# (?:\d+ )?Pinia stores/g, yeni: `# ${stats.stores} Pinia stores`, ad: 'stores' },
  { dosya: 'README.md', re: /# \d+\+? Görünüm/g, yeni: `# ${stats.views} Görünüm`, ad: 'views' },
  { dosya: 'README.md', re: /# (?:\d+ )?Paylaşılan Bileşenler/g, yeni: `# ${stats.components} Paylaşılan Bileşenler`, ad: 'components' },
  { dosya: 'README.md', re: /# (?:\d+ )?Pinia Durum Yönetimi/g, yeni: `# ${stats.stores} Pinia Durum Yönetimi`, ad: 'stores' },
  { dosya: 'README.md', re: /# \d+\+? Composable Hook/g, yeni: `# ${stats.composables} Composable Hook`, ad: 'composables' }
]

let drift = 0
for (const k of kurallar) {
  const yol = path.join(ROOT, k.dosya)
  const icerik = fs.readFileSync(yol, 'utf8')
  const guncel = icerik.replace(k.re, k.yeni)
  if (guncel === icerik) continue
  drift++
  if (CHECK) {
    console.error(`DRIFT: ${k.dosya} (${k.ad}) güncel değil -> "${k.yeni}" olmalı`)
  } else {
    fs.writeFileSync(yol, guncel, 'utf8')
    console.log(`guncellendi: ${k.dosya} (${k.ad}) -> ${k.yeni}`)
  }
}

console.log('---')
console.log(
  `Yapisal sayilar: views=${stats.views} components=${stats.components} composables=${stats.composables} stores=${stats.stores} api-modules=${stats.apiModulleri}`
)
if (CHECK) {
  if (drift > 0) {
    console.error(`Docs senkron degil (${drift} kural). "node scripts/docs-sync.mjs" calistirin.`)
    process.exit(1)
  }
  console.log('Docs senkron.')
}
