import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PosSepetPaneli from '../PosSepetPaneli.vue'
import i18n from '../../i18n.js'

/**
 * POS SEPET PANELI — sozlesme testleri (karakterizasyon).
 *
 * Bu panel bir fazdaki "sepet ekrani ayristirildi" isinin gorunur yuzu. Satir
 * verisi, toplam hesabi, indirim ve ozet mantigi view'da (HizliSatis.vue)
 * duruyor; panel yalnizca OLAY YAYAR. Dolayisiyla burada sabitlenmesi gereken
 * sey props/emits SOZLESMESIDIR: refactor sirasinda bu olaylarin adi veya
 * baglanma bicimi degisirse test kirilir.
 *
 * Neden mount testi (kaynak kod taramasi degil)? Cunku Faz1'de bu view'dan
 * mantik composable'lara tasinacak. Kaynak metnine bakan testler tasima ile
 * kirilir; mount ederek davranisan testler tasimadan etkilenmez.
 */

const stubs = {
  // PrimeVue `Button` metni `label` prop'undan alir; stub slot'a birakirsa
  // dugmelerin etiketi kaybolur ve "Temizle" gibi butonlar bulunamaz.
  Button: { name: 'Button', props: ['label', 'icon'], template: '<button><i v-if="icon" :class="icon" /><slot />{{ label }}</button>' },
  InputNumber: { template: '<input class="inputnumber" />' },
  SelectButton: { template: '<div class="selectbutton"><slot /></div>' },
  AutoComplete: { template: '<div class="autocomplete"><slot /></div>' },
  // REDTEAM/Faz2: Adet kontrolu `PosAdetGirisi`ye tasindi. Bu test panelin
  // OLAY YAYMA sozlesmesini dogrular; girdinin kendi davranisi
  // PosAdetGirisi.spec.js'de test edilir.
  PosAdetGirisi: {
    name: 'PosAdetGirisi',
    props: ['miktar', 'stokMiktari', 'birim'],
    emits: ['azalt', 'artir', 'commit'],
    template:
      '<div class="adet-grup"><button class="adet-azalt" @click="$emit(\'azalt\')">-</button>' +
      '<input class="adet-girdi" :value="miktar" @change="$emit(\'commit\', $event.target.value)" />' +
      '<button class="adet-artir" @click="$emit(\'artir\')">+</button></div>'
  }
}

const kur = (props = {}) =>
  mount(PosSepetPaneli, {
    props: {
      sepet: [],
      indirimTipleri: [
        { label: 'Tutar', value: 'tutar' },
        { label: 'Yüzde', value: 'yuzde' }
      ],
      ...props
    },
    // `teleport: true` SART: satir eylem menusu `body`'ye teleport edilir
    // (tablo scroll konteynerinde kirpilmayi onlemek icin). Teleport
    // stub'lanmazsa menu wrapper agacinin disinda kalir ve `.eylem-menu`
    // sorgusu bos doner.
    global: { plugins: [i18n], stubs: { ...stubs, teleport: true } }
  })

const urun = (ek = {}) => ({
  id: 1,
  ad: 'Vida M8',
  stokKodu: 'V-001',
  barkod: '8690000000001',
  miktar: 2,
  fiyat: 10,
  fiyatlar: [
    { ad: 'Perakende', fiyat: 10 },
    { ad: 'Toptan', fiyat: 9 }
  ],
  fiyatTipi: 'Perakende',
  birim: 'adet',
  kdvOrani: 20,
  birimHacim: 0.01,
  agirlik: 0.05,
  ...ek
})

describe('PosSepetPaneli — baslik ve bos durum', () => {
  it('bos sepette ekleme yonlendirmesi gosterir', () => {
    const w = kur({ sepet: [] })
    expect(w.find('.sepet-bos').exists()).toBe(true)
  })

  it('basliktaki satir sayisini gosterir', () => {
    const w = kur({ sepet: [urun(), urun({ id: 2, ad: 'Somun' })] })
    expect(w.text()).toContain('2')
  })

  it('baslik dugmesi toggle yayar', async () => {
    const w = kur({ sepet: [urun()] })
    await w.find('.sepet-baslik-toggle').trigger('click')
    expect(w.emitted('toggle')).toBeTruthy()
  })

  it('kapaliyken icerik gizlenir (v-show ile, DOMda kalir)', async () => {
    const w = kur({ sepet: [urun()], acik: false })
    const icerik = w.find('.sepet-icerik')
    expect(icerik.exists()).toBe(true)
    expect(icerik.attributes('style')).toContain('display: none')
  })
})

describe('PosSepetPaneli — satir olaylari', () => {
  it('satira tiklamak satir-sec yayar', async () => {
    const w = kur({ sepet: [urun()] })
    await w.find('.sepet-item').trigger('click')
    expect(w.emitted('satir-sec')).toEqual([[0]])
  })

  it('sil dugmesi sil olayini satır indeksiyle yayar', async () => {
    const w = kur({ sepet: [urun()] })
    await w.find('.sepet-sil').trigger('click')
    expect(w.emitted('sil')).toEqual([[0]])
  })

  it('urun adina tiklamak urun-degistir-ac yayar (silmeden degistirme)', async () => {
    const w = kur({ sepet: [urun()] })
    await w.find('.sepet-ad').trigger('click')
    expect(w.emitted('urun-degistir-ac')).toEqual([[0]])
  })
  // Adet azalt/artir ve girdi olaylari icin bkz. asagidaki
  // "adet ve fiyat girdileri" bolumu (PosAdetGirisi ile birlikte test edilir).
})

describe('PosSepetPaneli — geri al barlari', () => {
  it('sepet geri al barinda geri-al yayar', async () => {
    const w = kur({ sepet: [urun()], geriAlSepet: true })
    const bar = w.findAll('.geri-al-bar')[0]
    expect(bar.exists()).toBe(true)
    await bar.find('button').trigger('click')
    expect(w.emitted('geri-al')).toBeTruthy()
  })

  it('satir geri al barinda satir-geri-al yayar', async () => {
    const w = kur({ sepet: [urun()], geriAlSatir: true })
    const bar = w.find('.geri-al-bar')
    expect(bar.exists()).toBe(true)
    await bar.find('button').trigger('click')
    expect(w.emitted('satir-geri-al')).toBeTruthy()
  })

  it('iki geri al bari birlikte de ayirt edilebilir', () => {
    const w = kur({ sepet: [urun()], geriAlSepet: true, geriAlSatir: true })
    expect(w.findAll('.geri-al-bar')).toHaveLength(2)
  })

  it('geri al barlari kapaliyken render edilmez', () => {
    const w = kur({ sepet: [urun()], geriAlSepet: false, geriAlSatir: false })
    expect(w.findAll('.geri-al-bar')).toHaveLength(0)
  })
})

describe('PosSepetPaneli — satir vurgulama', () => {
  it('aktifSatir satirina aktif-satir sinifi uygular', () => {
    const w = kur({ sepet: [urun(), urun({ id: 2 })], aktifSatir: 1 })
    const satirlar = w.findAll('.sepet-item')
    expect(satirlar[0].classes()).not.toContain('aktif-satir')
    expect(satirlar[1].classes()).toContain('aktif-satir')
  })

  it('vurguluId satirina yeni-satir sinifi uygular (yeni ekleme animasyonu)', () => {
    const w = kur({ sepet: [urun({ id: 7 })], vurguluId: 7 })
    expect(w.find('.sepet-item').classes()).toContain('yeni-satir')
  })

  it('vurguluId baska bir satiri gosteriyorsa o satir isaretlenmez', () => {
    const w = kur({ sepet: [urun({ id: 7 })], vurguluId: 99 })
    expect(w.find('.sepet-item').classes()).not.toContain('yeni-satir')
  })
})

describe('PosSepetPaneli — adet ve fiyat girdileri', () => {
  // REDTEAM/Faz2: Adet artik `PosAdetGirisi` bileseninde; panel yalnizca
  // OLAY YAYAR ve stok miktarisini/`birim`i one devretir.
  it('adet kontrolu icin stok ve birimi one devreder', () => {
    const w = kur({ sepet: [urun({ stokMiktari: 12, birim: 'Kg' })] })
    const adet = w.findComponent({ name: 'PosAdetGirisi' })
    expect(adet.props('miktar')).toBe(2)
    expect(adet.props('stokMiktari')).toBe(12)
    expect(adet.props('birim')).toBe('Kg')
  })

  it('azalt/artir olaylari satir indeksiyle yayar', async () => {
    const w = kur({ sepet: [urun()] })
    await w.find('.adet-azalt').trigger('click')
    expect(w.emitted('miktar-azalt')).toEqual([[0]])
    await w.find('.adet-artir').trigger('click')
    expect(w.emitted('miktar-artir')).toEqual([[0]])
  })

  it('girdiden deger gelince miktar-degistir yayar (indeks + ham deger)', async () => {
    const w = kur({ sepet: [urun()] })
    await w.find('.adet-girdi').setValue('7')
    expect(w.emitted('miktar-degistir')).toEqual([[{ idx: 0, miktar: '7' }]])
  })

  it('urun degistirme acikken adet kontrolu gizlenir (yerine arama gelir)', () => {
    const w = kur({ sepet: [urun()], urunDegistirSatir: 0 })
    expect(w.findComponent({ name: 'PosAdetGirisi' }).exists()).toBe(false)
    expect(w.find('.sepet-urun-degistir').exists()).toBe(true)
  })

  it('fiyat tipi secilince fiyat-tipi-degisti yayar', async () => {
    const w = kur({ sepet: [urun()] })
    const select = w.find('.fiyat-tip-select')
    const optionlar = select.findAll('option')
    expect(optionlar.map((o) => o.element.value)).toEqual(['Perakende', 'Toptan'])
    await select.setValue('Toptan')
    expect(w.emitted('fiyat-tipi-degisti')).toBeTruthy()
  })

  it('son alis fiyati varsa gosterilir', () => {
    const w = kur({
      sepet: [urun({ sonAldigiFiyat: 8, sonAldigiTarih: '2026-01-15' })]
    })
    expect(w.find('.sepet-son-alis').exists()).toBe(true)
  })
})

describe('PosSepetPaneli — surukle-birak', () => {
  it('tutamak suruklemeyi baslatir ve hedef satir uzerine birakilabilir', async () => {
    const w = kur({ sepet: [urun(), urun({ id: 2 })] })
    await w.findAll('.sepet-tutamac')[0].trigger('dragstart')
    expect(w.emitted('surukleme-basla')).toEqual([[0]])

    await w.findAll('.sepet-item')[1].trigger('drop')
    expect(w.emitted('surukleme-birak')).toEqual([[1]])

    await w.findAll('.sepet-tutamac')[0].trigger('dragend')
    expect(w.emitted('surukleme-bitir')).toBeTruthy()
  })
})

describe('PosSepetPaneli — ozet', () => {
  it('genel toplami gosterir', () => {
    const w = kur({ sepet: [urun({ miktar: 3, fiyat: 25 })], genelToplam: 75 })
    expect(w.find('.genel-toplam-deger').exists()).toBe(true)
    expect(w.text()).toContain('75')
  })

  it('detay acma dugmesi detay-toggle yayar', async () => {
    const w = kur({ sepet: [urun()], detayAcik: false })
    await w.find('.ozet-detay-btn').trigger('click')
    expect(w.emitted('detay-toggle')).toBeTruthy()
  })

  it('temizle dugmesi temizle yayar', async () => {
    const w = kur({ sepet: [urun()] })
    const btn = w.findAll('button').find((b) => b.text().includes('Temizle'))
    expect(btn).toBeTruthy()
    await btn.trigger('click')
    expect(w.emitted('temizle')).toBeTruthy()
  })
})

describe('PosSepetPaneli — satir eylem menusu', () => {
  it('menudeki "adedi sifirla" adedi-sifirla yayar', async () => {
    const w = kur({ sepet: [urun()] })
    const menuDugmesi = w.findComponent({ name: 'SatirEylemleri' })
    // Menuyu ac
    await menuDugmesi.find('button').trigger('click')
    const sifirla = w.findAll('.eylem-menu button').find((b) =>
      b.text().includes('Adedi 1 yap')
    )
    expect(sifirla).toBeTruthy()
    await sifirla.trigger('click')
    expect(w.emitted('adedi-sifirla')).toEqual([[0]])
  })
})