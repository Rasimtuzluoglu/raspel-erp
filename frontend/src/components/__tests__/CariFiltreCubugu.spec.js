import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CariFiltreCubugu from '../cari/CariFiltreCubugu.vue'

const InputStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input class="arama-input" v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
}

const DropdownStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue', 'change'],
  template: '<button class="dropdown-sec" @click="$emit(\'update:modelValue\', \'Tedarikci\'); $emit(\'change\')">sec</button>'
}

const ButtonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button class="test-button" @click="$emit(\'click\')">{{ label }}</button>'
}

const stubs = {
  IconField: { template: '<span><slot /></span>' },
  InputIcon: true,
  InputText: InputStub,
  Dropdown: DropdownStub,
  AutoComplete: InputStub,
  Button: ButtonStub,
  TabloAyarlari: {
    emits: ['update:kolonlar', 'update:yogunluk'],
    template: '<div class="tablo-ayarlari-stub"><button class="kolon-guncelle" @click="$emit(\'update:kolonlar\', [{ field: \'ad\', visible: true }])">kolon</button><button class="yogunluk-guncelle" @click="$emit(\'update:yogunluk\', \'compact\')">yogunluk</button></div>'
  }
}

const mountCubuk = (props = {}) => mount(CariFiltreCubugu, {
  global: { plugins: [i18n], stubs },
  props: {
    arama: '',
    tur: null,
    bakiye: null,
    etiket: '',
    etiketOnerileri: [],
    cariTurSecenekleri: [
      { label: 'Müşteri', value: 'Musteri' },
      { label: 'Tedarikçi', value: 'Tedarikci' }
    ],
    bakiyeFiltreleri: [
      { label: 'Tahsil Edilecek', value: 'alacak' },
      { label: 'Ödenecek', value: 'borc' }
    ],
    aktifFiltreler: [],
    riskGorebilir: false,
    kolonlar: [],
    ...props
  }
})

describe('CariFiltreCubugu', () => {
  it('arama cubugu ve filtre alanlarini render eder', () => {
    const wrapper = mountCubuk({ arama: 'abc' })

    expect(wrapper.find('.cari-filtreler').exists()).toBe(true)
    expect(wrapper.find('.cari-arama').exists()).toBe(true)
    expect(wrapper.find('.arama-input').element.value).toBe('abc')
  })

  it('arama degisince update ve ara olaylarini yayar', async () => {
    const wrapper = mountCubuk()

    await wrapper.find('.arama-input').setValue('yeni')

    expect(wrapper.emitted('update:arama')[0]).toEqual(['yeni'])
    expect(wrapper.emitted('ara')).toHaveLength(1)
  })

  it('tur secilince update ve filtre-degisti olaylarini yayar', async () => {
    const wrapper = mountCubuk()

    await wrapper.find('.dropdown-sec').trigger('click')

    expect(wrapper.emitted('update:tur')[0]).toEqual(['Tedarikci'])
    expect(wrapper.emitted('filtre-degisti')).toHaveLength(1)
  })

  it('aktif filtre varken temizle butonunu gosterir ve olay yayar', async () => {
    const wrapper = mountCubuk({ tur: 'Musteri' })

    const temizle = wrapper.findAll('.test-button').find((b) => b.text().includes('Temizle'))
    expect(temizle).toBeTruthy()
    await temizle.trigger('click')

    expect(wrapper.emitted('filtreleri-temizle')).toHaveLength(1)
  })

  it('aktif filtre cipini kaldirma olayi yayar', async () => {
    const wrapper = mountCubuk({
      aktifFiltreler: [{ tip: 'tur', etiket: 'Müşteri' }]
    })

    const cip = wrapper.find('.cari-filtre-cip')
    expect(cip.exists()).toBe(true)
    expect(cip.text()).toContain('Müşteri')
    await cip.trigger('click')

    expect(wrapper.emitted('cip-kaldir')[0]).toEqual(['tur'])
  })

  it('riskli filtre butonu riskli olayini yayar', async () => {
    const wrapper = mountCubuk({ riskGorebilir: true })

    await wrapper.find('[data-test="cari-riskli"]').trigger('click')

    expect(wrapper.emitted('riskli')).toHaveLength(1)
  })

  it('TabloAyarlari guncellemelerini yukari iletir', async () => {
    const wrapper = mountCubuk()

    await wrapper.find('.kolon-guncelle').trigger('click')
    await wrapper.find('.yogunluk-guncelle').trigger('click')

    expect(wrapper.emitted('update:kolonlar')[0][0]).toEqual([{ field: 'ad', visible: true }])
    expect(wrapper.emitted('update:yogunluk')[0]).toEqual(['compact'])
  })
})
