import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { routerKey } from 'vue-router'
import i18n from '../../i18n.js'
import KategoriKullanimDrawer from '../KategoriKullanimDrawer.vue'

const Button = {
  props: ['label', 'icon', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot />{{ label }}</button>'
}

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    emits: ['update:visible'],
    template:
      '<div v-if="visible" class="drawer"><h2 class="drawer-baslik">{{ header }}</h2><slot /><slot name="footer" /></div>'
  },
  Button,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const router = { push: vi.fn() }

const kur = (props = {}) =>
  mount(KategoriKullanimDrawer, {
    props: {
      visible: true,
      yukleniyor: false,
      kullanim: {
        kategoriId: 5,
        ad: 'Yakıt',
        toplam: 12,
        renk: '#f59e0b',
        ikon: 'pi pi-car',
        detaylar: [
          { modul: 'STOK', adet: 7 },
          { modul: 'KASA_GIDER', adet: 3 },
          { modul: 'MASRAF', adet: 2 }
        ]
      },
      ...props
    },
    global: {
      plugins: [i18n],
      stubs,
      provide: { [routerKey]: router }
    }
  })

describe('KategoriKullanimDrawer.vue', () => {
  it('yukleniyor durumunda durum metni gosterir', () => {
    const w = kur({ kullanim: null, yukleniyor: true })
    expect(w.text()).toContain('Kullanım bilgisi yükleniyor')
    expect(w.find('.kullanim').exists()).toBe(false)
  })

  it('toplam ve modul bazli adetleri gosterir', () => {
    const w = kur()
    expect(w.find('.drawer-baslik').text()).toBe('Kategori Kullanımı')
    expect(w.text()).toContain('Yakıt')
    expect(w.find('.kullanim-toplam').text()).toContain('12')
    const satirlar = w.findAll('.kullanim-satir')
    expect(satirlar).toHaveLength(3)
    expect(satirlar[0].text()).toContain('Stok')
    expect(satirlar[0].text()).toContain('7')
    expect(satirlar[1].text()).toContain('Kasa')
    expect(satirlar[2].text()).toContain('Masraf')
  })

  it('ilgili ekrana baglanti verir ve yonlendirir', async () => {
    const w = kur()
    const baglantilar = w.findAll('.kullanim-satir button')
    expect(baglantilar).toHaveLength(3)

    await baglantilar[0].trigger('click')
    expect(router.push).toHaveBeenCalledWith('/stoklar')
    expect(w.emitted('git')[0]).toEqual([{ modul: 'STOK', yol: '/stoklar' }])

    await baglantilar[1].trigger('click')
    expect(router.push).toHaveBeenCalledWith('/kasa')
  })

  it('bilinmeyen modul icin baglanti gostermez', () => {
    const w = kur({
      kullanim: {
        kategoriId: 9,
        ad: 'Diğer',
        toplam: 1,
        detaylar: [{ modul: 'BILINMEYEN', adet: 1 }]
      }
    })
    expect(w.find('.kullanim-satir').text()).toContain('BILINMEYEN')
    expect(w.find('.kullanim-satir button').exists()).toBe(false)
  })

  it('kullanim yoksa bos mesaji gosterir', () => {
    const w = kur({
      kullanim: { kategoriId: 1, ad: 'Boş', toplam: 0, detaylar: [] }
    })
    expect(w.find('.kullanim-bos').text()).toContain('Bu kategori henüz kullanılmıyor')
  })
})
