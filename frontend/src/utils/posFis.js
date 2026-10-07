/**
 * POS fis HTML uretimi.
 *
 * HizliSatis.vue icinde `fisiYazdir` 98 satirdi ve icinde gömülü bir
 * `<style>` blogu + 25 satirlik HTML sablonu barindiriyordu. Bu yüzden
 * dosyanin sonunda ucuncu bir `<style>` blogu görünüyordu ve fişin
 * görünümünü değiştirmek için 100 satirlik bir fonksiyonu okumak gerekiyordu.
 *
 * Burada fiş gorunumu TEK bir fonksiyon: `posFisHtml`. Duz bir veri girer,
 * tam HTML dokumani dondurur. Format (genislik, yazici tipi) parametre,
 * i18n ve para bicimi disaridan verilir — boylece test edilebilir ve
 * HizliSatis'ten bagimsiz.
 *
 * `fisYazdir.js` bu katmanin degil, yazdirma PENCERESINI acan katmandir.
 */

import { escapeHtml } from './escapeHtml.js'
import { formatCurrency } from './format.js'

/** Agirlik metni: 1 ton ve uzeri ton, altinda kg. */
export const agirlikMetni = (kg) => {
  const deger = Number(kg) || 0
  return deger >= 1000 ? (deger / 1000).toFixed(3) + ' ton' : deger.toFixed(2) + ' kg'
}

/** Teslim durumu etiketi. `t` verilmezse kod degeri doner. */
export const teslimDurumEtiketi = (durum, t) => {
  const etiketler = {
    BEKLIYOR: 'faturalar.durumBekliyor',
    YOLDA: 'faturalar.durumYolda',
    TESLIM_EDILDI: 'faturalar.durumTeslimEdildi'
  }
  const anahtar = etiketler[durum]
  if (!anahtar) return durum
  return t ? t(anahtar) : durum
}

/**
 * Yazdirma penceresinin kendi CSS'i.
 *
 * Bu stil SFC icinde degil, yazdirilan AYRI bir dokumanda gecer; dolayisiyla
 * uygulamanin tema jetonlarini veya kapsam kurallarini tasimaz, kendi
 * Courier/New tabanli fis gorunumunu tanimlar.
 */
export const fisStili = (genislik = '80') => `
  * { margin: 0; padding: 0; box-sizing: border-box; }
  /* KRITIK: @page olmadan 80mm govde tarayicinin VARSAYILAN kagit boyutuna
     (A4/Letter) basilir; sagdan/soldan kirpilir veya fazladan sayfa uretir.
     Termal fis kendi genisliginde, kenar bosluksuz basilmali. */
  @page { size: ${genislik === '58' ? '58mm' : '80mm'} auto; margin: 0; }
  body { font-family: 'Courier New', monospace; width: ${genislik === '58' ? '58mm' : '80mm'}; margin: 0 auto; color: #000; font-size: 12px; }
  .aracubuk {
    position: fixed; top: 0; left: 0; right: 0; z-index: 10;
    width: 100%; padding: 10px; text-align: center;
    background: #1e293b; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  }
  .aracubuk button {
    font-family: Arial, sans-serif; font-size: 14px; font-weight: 600;
    padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer;
    background: var(--accent); color: #fff; margin: 0 4px;
  }
  .aracubuk button.iptal { background: #475569; }
  .fis { padding: 6px 4px; margin-top: 52px; }
  .baslik { text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 4px; }
  .logo { display: block; max-height: 48px; max-width: 140px; margin: 0 auto 4px; object-fit: contain; }
  .tarih, .fisno { text-align: center; font-size: 10px; margin-top: 2px; }
  .musteri { margin-top: 6px; font-size: 11px; }
  .ayrac { text-align: center; color: #555; margin: 4px 0; letter-spacing: 1px; }
  .satir { display: flex; justify-content: space-between; padding: 2px 0; }
  .satir .ad { flex: 1; white-space: pre-wrap; word-break: break-word; padding-right: 6px; }
  .satir .tutar { white-space: nowrap; }
  .satir.genel { border-top: 2px solid #000; font-weight: bold; padding-top: 4px; margin-top: 4px; }
  .tesekkur { text-align: center; margin-top: 8px; font-size: 10px; }
  @media print {
    .aracubuk { display: none !important; }
    .fis { margin-top: 0; }
  }
`

const AYRAC = '- - - - - - - - - - - - - -'

/**
 * Fis kalem satirlari (fiyatli modda tutarli, fiyatsiz modda yalniz ad/adet).
 *
 * @param {object} p
 * @param {Array} p.sepet Sepet satirlari
 * @param {boolean} p.fiyatli Fiyatli fis mi
 */
const kalemSatirlari = ({ sepet, fiyatli }) =>
  sepet
    .map(
      (i) =>
        `<div class="satir"><span class="ad">${escapeHtml(i.ad || '')} x${i.miktar}</span>` +
        (fiyatli ? `<span class="tutar">${formatCurrency(i.miktar * i.fiyat)}</span>` : '') +
        '</div>'
    )
    .join('')

/**
 * Fis ozet blogu: ara toplam, indirim, genel toplam, odenen, kalan.
 * `fiyatli` degilse TAMAMEN bos doner.
 */
const ozetBlogu = ({ t, fiyatli, toplam, indirimDegeri, indirimTipi, indirimTutari, genelToplam, odenenTutar, kalanTutar }) => {
  if (!fiyatli) return ''
  const indirimSatiri =
    indirimDegeri > 0
      ? `<div class="satir"><span class="ad">${t('hizliSatis.indirim')}${indirimTipi === 'yuzde' ? ' (' + indirimDegeri + '%)' : ''}</span><span class="tutar">-${formatCurrency(indirimTutari)}</span></div>`
      : ''
  return `
    <div class="ayrac">${AYRAC}</div>
    <div class="satir"><span class="ad">${t('hizliSatis.araToplam')}</span><span class="tutar">${formatCurrency(toplam)}</span></div>
    ${indirimSatiri}
    <div class="satir genel"><span class="ad">${t('hizliSatis.fisGenelToplam')}</span><span class="tutar">${formatCurrency(genelToplam)}</span></div>
    <div class="ayrac">${AYRAC}</div>
    <div class="satir"><span class="ad">${t('hizliSatis.fisOdenen')}</span><span class="tutar">${formatCurrency(odenenTutar)}</span></div>
    ${kalanTutar > 0 ? `<div class="satir"><span class="ad">${t('hizliSatis.fisKalan')}</span><span class="tutar">${formatCurrency(kalanTutar)}</span></div>` : ''}
  `
}

/**
 * Tam fis dokumani uretir.
 *
 * @param {object} p
 * @param {Function} p.t vue-i18n `t`
 * @param {Array}  p.sepet Sepet satirlari
 * @param {boolean} p.fiyatli Fiyatli fis mi (fiyatsizda tutar satirlari gizlenir)
 * @param {number} p.toplam Sepet ara toplami
 * @param {number} p.indirimDegeri Indirim degeri (0 = indirim yok)
 * @param {'tutar'|'yuzde'} p.indirimTipi
 * @param {number} p.indirimTutari
 * @param {number} p.genelToplam Indirim sonrasi toplam
 * @param {number} p.odenenTutar
 * @param {number} p.kalanTutar
 * @param {number} [p.toplamAgirlik]
 * @param {string} [p.sirketLogosu]
 * @param {string} [p.sirketAdi]
 * @param {string} [p.simdikiTarih]
 * @param {string} [p.fisNo]
 * @param {string} [p.musteriAdi]
 * @param {string} [p.teslimEden]
 * @param {string} [p.teslimDurumu]
 * @param {string} [p.teslimNotu]
 * @param {string} [p.odemeDurumText]
 * @param {string} [p.saticiAdi]
 * @param {'58'|'80'} [p.fisGenislik]
 * @returns {string} Tam HTML dokumani
 */
export function posFisHtml(p) {
  const {
    t,
    sepet = [],
    fiyatli = true,
    toplam = 0,
    indirimDegeri = 0,
    indirimTipi = 'tutar',
    indirimTutari = 0,
    genelToplam = 0,
    odenenTutar = 0,
    kalanTutar = 0,
    toplamAgirlik = 0,
    sirketLogosu = '',
    sirketAdi = '',
    simdikiTarih = '',
    fisNo = '',
    musteriAdi = '',
    teslimEden = '',
    teslimDurumu = '',
    teslimNotu = '',
    odemeDurumText = '',
    saticiAdi = '',
    fisGenislik = '80'
  } = p

  const agirlikSatiri =
    Number(toplamAgirlik) > 0
      ? `<div class="satir"><span class="ad">${t('hizliSatis.toplamAgirlik')}</span><span class="tutar">${agirlikMetni(toplamAgirlik)}</span></div>`
      : ''

  const musteriSatiri = musteriAdi
    ? `<div class="musteri">${t('hizliSatis.fisMusteri')} ${escapeHtml(musteriAdi)}</div>`
    : ''

  return `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${t('hizliSatis.fisOnizleme')}</title>
<style>${fisStili(fisGenislik)}</style>
</head>
<body>
  <div class="aracubuk">
    <button onclick="window.print()">${t('hizliSatis.yazdir')}</button>
    <button class="iptal" onclick="window.close()">${t('hizliSatis.kapat')}</button>
  </div>
  <div class="fis">
    ${sirketLogosu ? `<img class="logo" src="${escapeHtml(sirketLogosu)}" alt="logo" />` : ''}
    <div class="baslik">${escapeHtml(sirketAdi || 'RASPEL ERP')}</div>
    <div class="tarih">${simdikiTarih}</div>
    <div class="fisno">${t('hizliSatis.fisNo')} ${escapeHtml(fisNo || '')}</div>
    ${musteriSatiri}
    ${teslimEden ? `<div class="musteri">${t('hizliSatis.teslimEden')}: ${escapeHtml(teslimEden)}</div>` : ''}
    ${teslimDurumu && teslimDurumu !== 'BEKLIYOR' ? `<div class="musteri">${t('hizliSatis.teslimEtiketi')}: ${teslimDurumEtiketi(teslimDurumu, t)}</div>` : ''}
    ${teslimNotu ? `<div class="musteri">${t('hizliSatis.not')}: ${escapeHtml(teslimNotu)}</div>` : ''}
    <div class="ayrac">${AYRAC}</div>
    ${kalemSatirlari({ sepet, fiyatli })}
    ${agirlikSatiri}
    ${ozetBlogu({ t, fiyatli, toplam, indirimDegeri, indirimTipi, indirimTutari, genelToplam, odenenTutar, kalanTutar })}
    <div class="satir"><span class="ad">${t('hizliSatis.toplamUrun')}</span><span class="tutar">${sepet.length}</span></div>
    <div class="satir"><span class="ad">${t('common.status')}</span><span class="tutar">${odemeDurumText}</span></div>
    <div class="ayrac">${AYRAC}</div>
    <div class="tesekkur">${t('hizliSatis.islemYapan')} ${escapeHtml(saticiAdi || '-')}</div>
    <div class="tesekkur">${t('hizliSatis.iyiGunlerDileriz')}</div>
  </div>
</body>
</html>`
}