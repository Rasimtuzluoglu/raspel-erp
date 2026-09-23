#!/usr/bin/env node
// RasPel ERP — i18n bütünlük kontrolü
// Kullanım: node scripts/check-i18n.mjs   (repo kökünden)
// Kontroller:
//   1) Kaynakta kullanilan tum ceviri anahtarlari tr/en dosyalarinda var mi
//   2) {parametre} yer tutuculari verilen argumanlarla eslesiyor mu (runtime hata oncesi)
//   3) Cop degerler (anahtar kopyasi, TODO, lorem, isaretci dizileri)
//   4) tr/en anahtar sayisi simetrisi ve namespace icinde yinelenen anahtar
// Bir sorun bulunursa exit 1 (CI gate).
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const SRC = path.join(ROOT, 'frontend', 'src')
const LOCALES = path.join(SRC, 'locales')
const TR_PATH = path.join(LOCALES, 'tr.json')
const EN_PATH = path.join(LOCALES, 'en.json')

let failures = 0
const report = (severity, msg) => {
  if (severity === 'ERROR') failures++
  console.log(`[${severity}] ${msg}`)
}

function flatten(obj, prefix = '', out = {}) {
  for (const k of Object.keys(obj)) {
    const key = prefix ? `${prefix}.${k}` : k
    const v = obj[k]
    if (v && typeof v === 'object' && !Array.isArray(v)) flatten(v, key, out)
    else out[key] = v
  }
  return out
}

function flattenWithSource(obj, prefix = '', out = []) {
  for (const k of Object.keys(obj)) {
    const key = prefix ? `${prefix}.${k}` : k
    const v = obj[k]
    if (v && typeof v === 'object' && !Array.isArray(v)) flattenWithSource(v, key, out)
    else out.push([key, v])
  }
  return out
}

// Namespace bazli yinelenen anahtar taramasi (JSON.parse son kaydi tutar; kaynak metinde gor).
function findDuplicateKeys(jsonText, fileLabel, out) {
  const nsRe = /^\s*"([A-Za-z]\w*)"\s*:\s*{/gm
  let ns
  while ((ns = nsRe.exec(jsonText))) {
    const nsName = ns[1]
    const start = jsonText.indexOf('{', ns.index)
    let depth = 0
    let end = -1
    for (let i = start; i < jsonText.length; i++) {
      const ch = jsonText[i]
      if (ch === '{') depth++
      else if (ch === '}') {
        depth--
        if (depth === 0) { end = i; break }
      }
    }
    if (end === -1) continue
    const body = jsonText.slice(start + 1, end)
    const counts = {}
    for (const km of body.matchAll(/"([A-Za-z][\w]*(?:\.[A-Za-z][\w]*)*)"\s*:/g)) {
      counts[km[1]] = (counts[km[1]] || 0) + 1
    }
    for (const [k, c] of Object.entries(counts)) {
      if (c > 1 && k.indexOf('.') === -1) out.push(`${fileLabel}  ${nsName}.${k} x${c}`)
    }
  }
}

// Kaynak tarama dosyalari
function walk(dir, acc = []) {
  let entries
  try { entries = fs.readdirSync(dir, { withFileTypes: true }) } catch { return acc }
  for (const e of entries) {
    if (e.isDirectory()) { if (e.name !== 'locales' && e.name !== 'node_modules') walk(path.join(dir, e.name), acc) }
    else if (/\.(vue|js)$/.test(e.name)) acc.push(path.join(dir, e.name))
  }
  return acc
}

// triggerToz 1: kullanilan anahtarlar
const KEY_RE = /(?<!\w)(?:\$t|t)\(\s*['"`]([A-Za-z0-9_.-]+)['"`]\s*(?:[,)])/g
const usedKeys = new Set()
const usedFiles = walk(SRC).filter((f) => !f.includes('locales'))
for (const f of usedFiles) {
  const code = fs.readFileSync(f, 'utf8')
  let m
  while ((m = KEY_RE.exec(code))) usedKeys.add(m[1])
}

const trRaw = fs.readFileSync(TR_PATH, 'utf8')
const enRaw = fs.readFileSync(EN_PATH, 'utf8')
const tr = flattenWithSource(JSON.parse(trRaw))
const en = flattenWithSource(JSON.parse(enRaw))
const trMap = new Map(tr)
const enMap = new Map(en)
const trFlat = flatten(JSON.parse(trRaw))

for (const k of [...usedKeys].sort()) {
  if (!trMap.has(k)) report('ERROR', `EKSIK ANAHTAR (tr)  kullanilan:${k}`)
  if (!enMap.has(k)) report('ERROR', `EKSIK ANAHTAR (en)  kullanilan:${k}`)
}

// check 2: parametre eslesmesi
function placeholderKeys(s) { return [...s.matchAll(/\{([a-zA-Z0-9_]+)\}/g)].map((x) => x[1]) }
const PARAM_CALL_RE = /\bt\(\s*['"`]([A-Za-z0-9_.-]+)['"`]\s*,\s*\{([^}]*)\}\s*\)/g
let paramCalls = 0
for (const f of usedFiles) {
  const code = fs.readFileSync(f, 'utf8')
  let m
  while ((m = PARAM_CALL_RE.exec(code))) {
    paramCalls++
    const key = m[1]
    const value = trMap.get(key)
    if (typeof value !== 'string') continue
    const passed = new Set()
    for (const idm of m[2].matchAll(/\b[a-zA-Z_][a-zA-Z0-9_]*\b/g)) {
      const prev = m[2].slice(0, idm.index).trimEnd()
      if (/[:.\[\]"'`]$/.test(prev)) continue
      passed.add(idm[0])
    }
    const needed = placeholderKeys(value)
    for (const p of needed) {
      if (!passed.has(p)) report('ERROR', `TANIMSIZ PARAM {${p}}  ${key} (${f.split(path.sep).pop()})`)
    }
  }
}

// check 3: cop degerler
const GARBAGE_RE = /\x60|TODO|lorem ipsum|\{\{|\}\}|###|çeviri yap|TO_TRANSLATE/i
for (const [key, value] of tr) {
  if (typeof value !== 'string') continue
  if (value === key) report('WARN', `DEGER = ANAHTAR  ${key}`)
  else if (GARBAGE_RE.test(value)) report('ERROR', `COPVALUE  ${key}: ${value.slice(0, 60)}`)
}

// check 4: simetri + yinelenen anahtar
const trDups = []
const enDups = []
findDuplicateKeys(trRaw, 'tr.json', trDups)
findDuplicateKeys(enRaw, 'en.json', enDups)
for (const d of trDups.concat(enDups)) report('ERROR', `YINELENEN ${d}`)

// check 5: .vue icinde i18n'e baglanmamis Turkce metin taramasi
// (uyari; --strict ile hata). console/import/style/comment ve t() satirlari haric.
const STRICT = process.argv.includes('--strict')
const TR_CHAR_RE = /[çşğıöüİĞŞÇÖÜ]/
const LITERAL_RE = /(['"`])((?:\\.|(?!\1)[\s\S])*?)\1/g
const SKIP_LINE_RE = /\$?t\(|console\.|import\s|from\s+['"]|require\(/
const turkceBulunan = []
for (const f of usedFiles) {
  if (!f.endsWith('.vue')) continue
  let code = fs.readFileSync(f, 'utf8').replace(/<style[\s\S]*?<\/style>/gi, '')
  code = code.replace(/\/\/[^\n]*/g, '').replace(/\/\*[\s\S]*?\*\//g, '')
  let bulundu = 0
  for (const line of code.split('\n')) {
    if (SKIP_LINE_RE.test(line)) continue
    for (const m of line.matchAll(LITERAL_RE)) {
      if (TR_CHAR_RE.test(m[2])) bulundu++
    }
  }
  if (bulundu > 0) turkceBulunan.push([bulundu, path.relative(ROOT, f)])
}
if (turkceBulunan.length) {
  turkceBulunan.sort((a, b) => b[0] - a[0])
  const toplam = turkceBulunan.reduce((s, [c]) => s + c, 0)
  for (const [c, f] of turkceBulunan.slice(0, 10)) {
    report(STRICT ? 'ERROR' : 'WARN', `I18N'E BAGLANMAMIS TURKCE METIN  ${f} (x${c})`)
  }
  if (turkceBulunan.length > 10) console.log(`[WARN] ... ve ${turkceBulunan.length - 10} dosya daha`)
  console.log(`[WARN] Toplam ${toplam} satirda i18n disi Turkce literal (${turkceBulunan.length} dosya)`)
}

// check 6: ASCII'ye indirgenmis Turkce (noktali harfi eksik) literalleri yakala.
// check 5 yalnizca ozel karakter iceren metinleri gorur; "Satis", "Islem",
// "Musteri", "Gecersiz" gibi ASCII'ye indirgenmis kelimeler hic yakalanmiyordu.
// Yanlis pozitifi onlemek icin dar ve yuksek guvenli kelime listesi kullanilir;
// yalnizca kullaniciya gorunen metin baglamlari (label/header/placeholder/title/
// description/aciklama/mesaj) hedeflenir.
const ASCII_TR = [
  'Satis', 'Siparis', 'Islem', 'Musteri', 'Gecersiz', 'Gecerli', 'Kayit',
  'Odeme', 'Sifre', 'Guncelle', 'Yapilandirma', 'Dogrulama', 'Basarili',
  'Irsaliye', 'Maas', 'Izin', 'Cek', 'Doviz', 'Sirket', 'Kullanici',
  'Tum', 'Adet', 'Urun', 'Bakiye', 'Tutar', 'Isme', 'Kritik', 'Stok',
  'Sifirdan', 'Buyuk', 'Kucuk', 'Olmalidir', 'Yapilamaz', 'Bulunamadi',
  'Iptal', 'Onaylandi', 'Tamamlandi', 'Basarisiz', 'Degistir', 'Icerik'
]
const ASCII_TR_RE = new RegExp('\\b(' + ASCII_TR.join('|') + ')\\b')
const GORUNUR_ATTR_RE = /(?:header|label|placeholder|title|description|caption|summary|aciklama|mesaj|baslik|labelText)\s*[:=]\s*(['"`])((?:\\.|(?!\1)[\s\S])*?)\1/g
const asciiBulunan = new Map()
for (const f of usedFiles) {
  if (!f.endsWith('.vue')) continue
  const code = fs.readFileSync(f, 'utf8').replace(/<style[\s\S]*?<\/style>/gi, '')
  for (const m of code.matchAll(GORUNUR_ATTR_RE)) {
    const metin = m[2]
    if (TR_CHAR_RE.test(metin)) continue // zaten Turkce karakterli; check 5 ilgilenir
    const hit = metin.match(ASCII_TR_RE)
    if (hit) {
      const rel = path.relative(ROOT, f)
      const onceki = asciiBulunan.get(rel) || 0
      asciiBulunan.set(rel, onceki + 1)
    }
  }
}
if (asciiBulunan.size) {
  let toplam = 0
  const sirali = [...asciiBulunan.entries()].sort((a, b) => b[1] - a[1])
  for (const [, c] of sirali) toplam += c
  for (const [f, c] of sirali.slice(0, 10)) {
    report(STRICT ? 'ERROR' : 'WARN', `ASCII TURKCE METIN (noktali harf eksik)  ${f} (x${c})`)
  }
  console.log(`[WARN] Toplam ${toplam} yerde ASCII Turkce metin (${asciiBulunan.size} dosya)`)
}


// check 7: vue-i18n sozdizimi riski — kacissiz '@' ve literal '|'.
// '@' vue-i18n'de linked mesaj baslangicidir; literal icin {'@'} yazilmalidir.
// Aksi halde derleyici "SyntaxError: Invalid linked format" firlatir.
// '|' ise plural ayiricidir; literal ayirici olarak kullanmak beklenmeyen sonuc verir.
for (const [key, value] of tr) {
  if (typeof value !== 'string') continue
  if (value.replace(/\{'@'\}/g, '').includes('@')) {
    report('ERROR', `KACISSIZ @ (linked format riski)  ${key}: ${value.slice(0, 80)}`)
  }
  if (value.includes('|')) report('ERROR', `LITERAL | (plural ayirici: metnin yarisi kaybolur)  ${key}: ${value.slice(0, 80)}`)
}
for (const [key, value] of en) {
  if (typeof value !== 'string') continue
  if (value.replace(/\{'@'\}/g, '').includes('@')) {
    report('ERROR', `KACISSIZ @ (en)  ${key}: ${value.slice(0, 80)}`)
  }
  if (value.includes('|')) report('ERROR', `LITERAL | (en)  ${key}: ${value.slice(0, 80)}`)
}

// --unused: tanimli ama kaynakta (statik) kullanilmayan anahtarlari listeler.
// Dinamik kullanim yanlis pozitif uretmesin diye anahtar metni kaynakta
// herhangi bir yerde geciyorsa "kullaniliyor" sayilir. Build'i kirmaz.
if (process.argv.includes('--unused')) {
  const tumKaynak = usedFiles.map((f) => fs.readFileSync(f, 'utf8')).join('\n')
  const kullanilmayan = [...trMap.keys()].filter((k) => !usedKeys.has(k) && !tumKaynak.includes(k))
  console.log(`\n[unused] Kullanilmayan i18n anahtari (dinamik kullanim haric): ${kullanilmayan.length}`)
  for (const k of kullanilmayan.slice(0, 40)) console.log(`  - ${k}`)
  if (kullanilmayan.length > 40) console.log(`  ... ve ${kullanilmayan.length - 40} adet daha`)
}

console.log(`\ni18n ozeti: kullanilan ${usedKeys.size} | tr ${tr.length} | en ${en.length} | parametreli cagri ${paramCalls}`)
if (failures > 0) {
  console.log(`\ni18n KONTROL BASARISIZ (${failures} hata)`)
  process.exit(1)
}
console.log('\ni18n kontrolu temiz.')