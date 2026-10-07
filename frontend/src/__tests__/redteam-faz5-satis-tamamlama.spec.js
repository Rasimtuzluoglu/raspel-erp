import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz5 — Satis tamamlama / odeme guvenligi.
 *
 * UC KUSUR:
 *  1) F9/F10 (odeme durumu tuslari) satisi ANINDA tamamliyordu. Kasiyer
 *     odeme tipini secerken yanlislikla F9'a basmasi satisi kaydediyor,
 *     fis penceresi aciliyor ve geri donusu olmuyordu.
 *  2) GIZLI ZORUNLU ALAN: teslimat paneli varsayilan KAPALI; sofor secilince
 *     adres ZORUNLU hale geliyordu ama alan render edilmedigi icin
 *     gorunmuyordu. Kasa duruyor, toast cikiyor, kullanici paneli elle
 *     aramak zorunda kaliyordu.
 *  3) Dogrulama hatalari yalniz TOAST ile bildiriliyordu; hangi alanin eksik
 *     oldugu ekranda gorunmuyordu.
 */

const KOK = join(process.cwd(), 'src')
const posKod = readFileSync(join(KOK, 'views/HizliSatis.vue'), 'utf8')
const kisayolKod = readFileSync(join(KOK, 'composables/usePosKisayollar.js'), 'utf8')
const musteriPanelKod = readFileSync(join(KOK, 'components/PosMusteriPaneli.vue'), 'utf8')
const odemePanelKod = readFileSync(join(KOK, 'components/PosOdemePaneli.vue'), 'utf8')
const panelStilKod = readFileSync(join(KOK, 'assets', 'pos-panels.css'), 'utf8')

const yorumsuz = (s) =>
  s
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .split(/\r?\n/)
    .map((satir) => satir.replace(/(^|\s)\/\/.*$/, ''))
    .join('\n')

const scriptKodu = (s) => {
  const bas = s.indexOf('<script setup>')
  const son = s.lastIndexOf('</script>')
  return bas === -1 || son <= bas ? '' : yorumsuz(s.slice(bas, son))
}

const blokBul = (kod, anchor) => {
  const bas = kod.indexOf(anchor)
  if (bas === -1) return ''
  const suAc = kod.indexOf('{', bas)
  if (suAc === -1) return ''
  let derinlik = 0
  for (let i = suAc; i < kod.length; i++) {
    if (kod[i] === '{') derinlik++
    else if (kod[i] === '}') {
      derinlik--
      if (derinlik === 0) return kod.slice(bas, i + 1)
    }
  }
  return kod.slice(bas)
}

describe('Faz5.1 - F9/F10 satisi TAMAMLAMAZ (kazara kayit engeli)', () => {
  it('F9/F10 yalnizca odeme durumunu ayarlar', () => {
    const tuslar = blokBul(yorumsuz(kisayolKod), "case 'F9':")
    expect(tuslar).toContain("case 'F10':")
    expect(tuslar).toMatch(/odemeDurumu\.value\s*=/)
    // KRITIK: satisi tamamlayan cagri artık YOK
    expect(tuslar).not.toContain('satisiTamamla')
  })

  it('klavye baglaminda satisiTamamla artik gecirilmez', () => {
    const s = scriptKodu(posKod)
    const cagri = blokBul(s, 'usePosKisayollar(')
    expect(cagri).not.toMatch(/^\s*satisiTamamla,\s*$/m)
  })

  it('satisi bitirmek icin ACIK bir yol kalir (buton + Ctrl+S)', () => {
    // Buton
    expect(yorumsuz(posKod)).toContain('@click="satisiTamamla"')
    // Ctrl+S `useKisayollar` `kaydet` eylemine bagli
    expect(scriptKodu(posKod)).toMatch(/kaydet:\s*\(\)\s*=>\s*satisiTamamla\(\)/)
  })
})

describe('Faz5.2 - gizli zorunlu teslimat alani', () => {
  it('sofor secilince teslimat paneli OTOMATIK acilir', () => {
    const s = scriptKodu(posKod)
    const w = blokBul(s, 'watch(seciliSofor')
    expect(w).toContain('teslimatAcik.value = true')
  })

  it('hata gosterimi teslimat panelini acar ve alana odaklanir', () => {
    const s = scriptKodu(posKod)
    const hata = blokBul(s, 'const hataGoster =')
    expect(hata).toContain('teslimatAcik.value = true')
    expect(hata).toContain('hizli-teslim-adres')
    expect(hata).toContain('focus')
  })

  it('adres zorunlulugu hata isretini kullanir (toast tek basina degil)', () => {
    const tamamla = blokBul(scriptKodu(posKod), 'const satisiTamamlaOnaysiz =')
    expect(tamamla).toContain("hataGoster('teslimatAdresi')")
  })
})

describe('Faz5.3 - satir ici dogrulama gosterimi', () => {
  it('hata durumu ve temizleme mekanizmasi var', () => {
    const s = scriptKodu(posKod)
    expect(s).toContain('const hatalar = ref(')
    expect(s).toContain('const hataTemizle =')
    expect(s).toContain('const hataGoster =')
    // Alan doldurulunca hata temizlenir
    expect(blokBul(s, 'watch(teslimatAdresi')).toContain("hataTemizle('teslimatAdresi')")
    expect(blokBul(s, 'watch([taksitKurum, taksitTutar]')).toContain("hataTemizle('taksit')")
  })

  it('panel hatalari prop olarak alir', () => {
    expect(musteriPanelKod).toContain('hatalar: { type: Object, default: null }')
    expect(odemePanelKod).toContain('hatalar: { type: Object, default: null }')
    const sablon = yorumsuz(posKod)
    expect(sablon).toContain(':hatalar="hatalar"')
  })

  it('teslimat adresi alani hatayi ekranda gosterir', () => {
    expect(musteriPanelKod).toContain('hatalar.teslimatAdresi')
    expect(musteriPanelKod).toContain('alan-hata-metin')
    expect(musteriPanelKod).toContain('teslimatAdresiGerekli')
  })

  it('taksit alani hatayi ekranda gosterir', () => {
    expect(odemePanelKod).toContain('hatalar.taksit')
    expect(odemePanelKod).toContain('alan-hata-metin')
  })

  it('hata isaretinin gorunur stili tanimli', () => {
    expect(panelStilKod).toContain('.alan-hata')
    expect(panelStilKod).toContain('.alan-hata-metin')
  })

  it('musteri eksikligi de alan hatasi olarak isaretlenir', () => {
    const tamamla = blokBul(scriptKodu(posKod), 'const satisiTamamla = async')
    expect(tamamla).toContain("hataGoster('musteri')")
  })
})

describe('Faz5.4 - musteri/satis dogrulamalari hala korunuyor', () => {
  it('sepet bos kontrolu ve onay adimi yerinde', () => {
    const tamamla = blokBul(scriptKodu(posKod), 'const satisiTamamla = async')
    expect(tamamla).toContain('sepet.value.length === 0')
    expect(tamamla).toContain('onayIste.value')
    expect(tamamla).toContain('satisiTamamlaOnaysiz()')
  })

  it('taksit zorunlulugu ve kredi limiti tekrar denemesi korunuyor', () => {
    const onaysiz = blokBul(scriptKodu(posKod), 'const satisiTamamlaOnaysiz =')
    expect(onaysiz).toContain("TAKSIT")
    expect(onaysiz).toContain('krediLimitiGormezdenGel')
    expect(onaysiz).toContain('offlineKuyruk.ekle')
  })
})