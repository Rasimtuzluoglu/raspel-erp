import { readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const SRC = join(process.cwd(), 'src')
const IMPORT = "import { GRAFIK, SERI } from '../utils/grafikRenkleri.js'"

const DOSYALAR = {
  'views/Tahsilat.vue': [
    ['renk="#3b82f6"', ':renk="GRAFIK.mavi"'],
    ['renk="#ef4444"', ':renk="GRAFIK.kirmizi"'],
    ['renk="#f59e0b"', ':renk="GRAFIK.amber"'],
    ['renk="#10b981"', ':renk="GRAFIK.yesil"'],
    ["hedefGerceklesme >= 100 ? '#10b981' : '#8b5cf6'", 'hedefGerceklesme >= 100 ? GRAFIK.yesil : GRAFIK.mor']
  ],
  'views/Raporlar.vue': [
    ['style="color: #fbbf24"', ':style="{ color: GRAFIK.amberAcik }"'],
    ["< 0 ? '#ef4444' : '#10b981'", '< 0 ? GRAFIK.kirmizi : GRAFIK.yesil'],
    ["['#3b82f6', '#f59e0b']", '[GRAFIK.mavi, GRAFIK.amber]']
  ],
  'views/CekSenet.vue': [
    ['renk="#3b82f6"', ':renk="GRAFIK.mavi"'],
    ['renk="#f59e0b"', ':renk="GRAFIK.amber"'],
    ['renk="#ef4444"', ':renk="GRAFIK.kirmizi"'],
    ['renk="#14b8a6"', ':renk="GRAFIK.teal"'],
    ['renk="#64748b"', ':renk="GRAFIK.gri"']
  ],
  'views/Muhasebe.vue': [
    ["['#3b82f6', '#f59e0b']", '[GRAFIK.mavi, GRAFIK.amber]'],
    ["['#10b981', '#ef4444', '#3b82f6']", '[GRAFIK.yesil, GRAFIK.kirmizi, GRAFIK.mavi]']
  ],
  'views/Ajanda.vue': [
    ['renk="#3b82f6"', ':renk="GRAFIK.mavi"'],
    ['renk="#f59e0b"', ':renk="GRAFIK.amber"'],
    ['renk="#8b5cf6"', ':renk="GRAFIK.mor"'],
    ['renk="#ef4444"', ':renk="GRAFIK.kirmizi"']
  ],
  'views/Anomaliler.vue': [
    ['renk="#3b82f6"', ':renk="GRAFIK.mavi"'],
    ['renk="#ef4444"', ':renk="GRAFIK.kirmizi"'],
    ['renk="#f59e0b"', ':renk="GRAFIK.amber"'],
    ['renk="#64748b"', ':renk="GRAFIK.gri"'],
    ['renk="#8b5cf6"', ':renk="GRAFIK.mor"']
  ],
  'views/KarlilikAnalizi.vue': [
    ['renk="#3b82f6"', ':renk="GRAFIK.mavi"'],
    ['renk="#f59e0b"', ':renk="GRAFIK.amber"'],
    ['renk="#10b981"', ':renk="GRAFIK.yesil"'],
    ['renk="#8b5cf6"', ':renk="GRAFIK.mor"'],
    ["const renkler = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4', '#a3e635']", 'const renkler = SERI'],
    ["borderColor: '#3b82f6'", 'borderColor: GRAFIK.mavi'],
    ["borderColor: '#f59e0b'", 'borderColor: GRAFIK.amber'],
    ["borderColor: '#10b981'", 'borderColor: GRAFIK.yesil'],
    ["(x.brutKar < 0 ? '#ef4444' : '#10b981')", '(x.brutKar < 0 ? GRAFIK.kirmizi : GRAFIK.yesil)']
  ],
  'components/StokDetayDialog.vue': [
    ["borderColor: '#10b981'", 'borderColor: GRAFIK.yesil'],
    ["pointBackgroundColor: '#10b981'", 'pointBackgroundColor: GRAFIK.yesil'],
    ["borderColor: '#3b82f6'", 'borderColor: GRAFIK.mavi'],
    ["pointBackgroundColor: '#3b82f6'", 'pointBackgroundColor: GRAFIK.mavi']
  ]
}

let toplam = 0
for (const [dosya, ciftler] of Object.entries(DOSYALAR)) {
  const yol = join(SRC, dosya)
  let kaynak = readFileSync(yol, 'utf8')
  let degisen = 0

  for (const [eski, yeni] of ciftler) {
    if (!kaynak.includes(eski)) {
      console.log(`  UYARI (${dosya}): bulunamadi -> ${eski.slice(0, 50)}`)
      continue
    }
    while (kaynak.includes(eski)) {
      kaynak = kaynak.replace(eski, yeni)
      degisen += 1
    }
  }

  if (degisen > 0) {
    const gerekli = []
    if (kaynak.includes('GRAFIK.')) gerekli.push('GRAFIK')
    if (kaynak.includes('SERI')) gerekli.push('SERI')
    const importSatiri = `import { ${gerekli.join(', ')} } from '../utils/grafikRenkleri.js'`
    if (!kaynak.includes('grafikRenkleri.js')) {
      kaynak = kaynak.replace('<script setup>\n', `<script setup>\n${importSatiri}\n`)
    }
    writeFileSync(yol, kaynak, 'utf8')
  }
  toplam += degisen
  console.log(`${dosya}: ${degisen}`)
}
console.log(`Toplam: ${toplam}`)
