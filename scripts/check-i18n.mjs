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

console.log(`\ni18n ozeti: kullanilan ${usedKeys.size} | tr ${tr.length} | en ${en.length} | parametreli cagri ${paramCalls}`)
if (failures > 0) {
  console.log(`\ni18n KONTROL BASARISIZ (${failures} hata)`)
  process.exit(1)
}
console.log('\ni18n kontrolu temiz.')