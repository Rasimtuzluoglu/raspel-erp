/**
 * `dist/stats.html` (rollup-plugin-visualizer, version 2) verisinden en büyük
 * modülleri listeler. Kullanım: `npm run build:analyze` sonra
 * `node scripts/analiz-ozet.mjs`.
 */
import { readFileSync } from 'node:fs'

const dosya = process.argv[2] || 'dist/stats.html'
const html = readFileSync(dosya, 'utf8')
const baslangic = html.indexOf('const data = ')
if (baslangic < 0) {
  console.error('stats.html içinde veri bulunamadı')
  process.exit(1)
}

/** JSON nesnesini string/escape farkındalığıyla süslü parantez sayarak keser. */
function jsonuKes(metin, bas) {
  let derinlik = 0
  let tirnak = null
  let kacis = false
  for (let i = bas; i < metin.length; i += 1) {
    const karakter = metin[i]
    if (tirnak) {
      if (kacis) kacis = false
      else if (karakter === '\\') kacis = true
      else if (karakter === tirnak) tirnak = null
      continue
    }
    if (karakter === '"' || karakter === "'") {
      tirnak = karakter
      continue
    }
    if (karakter === '{') derinlik += 1
    if (karakter === '}') {
      derinlik -= 1
      if (derinlik === 0) return metin.slice(bas, i + 1)
    }
  }
  throw new Error('JSON nesnesi kapatılamadı')
}

const jsonBas = html.indexOf('{', baslangic)
const veri = JSON.parse(jsonuKes(html, jsonBas))

const adlar = new Map()
const yuru = (dugum, yol) => {
  const tamYol = dugum.name ? `${yol}/${dugum.name}` : yol
  if (dugum.uid) adlar.set(dugum.uid, tamYol)
  for (const cocuk of dugum.children || []) yuru(cocuk, tamYol)
}
if (veri.tree) yuru(veri.tree, '')

const parcalar = veri.nodeParts || {}
const liste = Object.entries(parcalar)
  .map(([uid, parca]) => ({
    ad: adlar.get(uid) || uid,
    boyut: parca.gzipLength ?? parca.renderedLength ?? 0
  }))
  .sort((a, b) => b.boyut - a.boyut)
  .slice(0, 20)

for (const oge of liste) {
  const normal = oge.ad.replace(/\\/g, '/').replace(/^\/+/, '')
  const ad = normal.length > 95 ? `…${normal.slice(-93)}` : normal
  console.log(`${(oge.boyut / 1024).toFixed(1).padStart(8)} kB  ${ad}`)
}
