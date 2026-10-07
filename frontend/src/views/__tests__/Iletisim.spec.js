import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { iletisimAPI } from '../../api/index.js'
import Iletisim from '../Iletisim.vue'

vi.mock('../../api/index.js', () => ({
  iletisimAPI: {
    whatsapp: vi.fn(() =>
      Promise.resolve({ data: { telefon: '05551112233', link: 'https://wa.me/905551112233' } })
    ),
    mailGonder: vi.fn(() => Promise.resolve({ data: { durum: 'GONDERILDI' } }))
  }
}))

const stubs = {
  PageHeader: true,
  Card: { template: '<div class="card-stub"><slot name="content" /></div>' },
  AutoComplete: {
    props: ['modelValue', 'suggestions'],
    emits: ['update:modelValue', 'complete'],
    template: '<div class="ac-stub" />'
  },
  InputText: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  Textarea: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  Button: {
    props: ['label', 'icon', 'disabled', 'loading'],
    emits: ['click'],
    template: '<button class="btn-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>'
  }
}

const cari = { id: 5, ad: 'Test Cari', telefon: '05551112233', email: 'test@ornek.com' }

const kur = () => {
  const wrapper = mount(Iletisim, { global: { stubs, plugins: [ToastService, i18n] } })
  wrapper.vm.$nextTick()
  return wrapper
}

const cariSec = async (wrapper, deger = cari) => {
  wrapper.vm.seciliCari = deger
  await wrapper.vm.$nextTick()
}

const whatsappMetni = async (wrapper, k, m) => {
  wrapper.vm.konu = k
  wrapper.vm.mesaj = m
  await wrapper.vm.$nextTick()
}

/** WhatsApp butonu: eylem satirindaki ikinci buton. */
const waButon = (wrapper) => wrapper.findAll('.btn-stub')[1]

describe('Iletisim.vue — WhatsApp mesajı', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('konu + mesajı wa.me bağlantısına ?text= olarak ekler', async () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => ({}))
    const wrapper = kur()
    await cariSec(wrapper)
    await whatsappMetni(wrapper, 'Bilgilendirme', 'Merhaba, fatura hazır.')
    await waButon(wrapper).trigger('click')
    await flushPromises()

    expect(iletisimAPI.whatsapp).toHaveBeenCalledWith(5)
    const cagri = openSpy.mock.calls[0]
    expect(cagri[0]).toContain('https://wa.me/905551112233?text=')
    const cozulen = decodeURIComponent(cagri[0])
    expect(cozulen).toContain('Merhaba, fatura hazır.')
    expect(cozulen).toContain('Bilgilendirme')
    openSpy.mockRestore()
  })

  it('mesaj yoksa WhatsApp butonu pasiftir (boş mesaj kutusu açılmaz)', async () => {
    const wrapper = kur()
    await cariSec(wrapper)
    expect(waButon(wrapper).attributes('disabled')).toBeDefined()
  })

  it('telefon yoksa WhatsApp butonu pasiftir', async () => {
    const wrapper = kur()
    await cariSec(wrapper, { id: 6, ad: 'Telefonsuz', email: 'x@y.com' })
    await whatsappMetni(wrapper, 'K', 'M')
    expect(waButon(wrapper).attributes('disabled')).toBeDefined()
  })

  it('hazır şablon tıklanınca konu ve mesajı doldurur', async () => {
    const wrapper = kur()
    await cariSec(wrapper)
    await wrapper.find('.sablon-cip').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('input').element.value.length).toBeGreaterThan(0)
    expect(wrapper.find('textarea').element.value.length).toBeGreaterThan(0)
  })
})
