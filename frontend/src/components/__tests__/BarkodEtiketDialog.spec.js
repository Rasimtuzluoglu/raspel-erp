import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import i18n from '../../i18n.js'
import { stokAPI } from '../../api/index.js'

vi.mock('../../api/index.js', () => ({
  stokAPI: {
    etiketQr: vi.fn(),
    etiketPdf: vi.fn()
  }
}))

vi.mock('jsbarcode', () => ({
  default: vi.fn((el, icerik) => {
    el.setAttribute('data-icerik', icerik)
    el.innerHTML = '<rect data-test="cubuk" />'
  })
}))

const stubs = {
  Dialog: { template: '<div class="dialog"><slot /><footer><slot name="footer" /></footer></div>' },
  Button: {
    name: 'Button',
    props: ['label', 'icon', 'loading'],
    template: '<button class="btn-stub">{{ label }}</button>'
  }
}

const mountDialog = async (stok, visible = true) => {
  const BarkodEtiketDialog = (await import('../BarkodEtiketDialog.vue')).default
  const wrapper = mount(BarkodEtiketDialog, {
    props: { visible: false, stok },
    global: { stubs, plugins: [createPinia(), i18n] }
  })
  if (visible) {
    await wrapper.setProps({ visible: true })
    await flushPromises()
  }
  return wrapper
}

describe('BarkodEtiketDialog.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    stokAPI.etiketQr.mockResolvedValue({ data: new Blob(['png'], { type: 'image/png' }) })
    stokAPI.etiketPdf.mockResolvedValue({ data: new Blob(['pdf'], { type: 'application/pdf' }) })
    global.URL.createObjectURL = vi.fn(() => 'blob:test')
    global.URL.revokeObjectURL = vi.fn()
  })

  it('barkodsuz üründe de barkod çizer ve stok kodunu kodlar', async () => {
    const wrapper = await mountDialog({ id: 7, ad: 'MDF Lam', stokKodu: 'MDF-18', barkod: '', satisFiyati: 100 })

    const svg = wrapper.find('svg.etiket-barkod')
    expect(svg.exists()).toBe(true)
    expect(svg.attributes('data-icerik')).toBe('MDF-18')
    // Barkod yerine stok kodu kodlandığı kullanıcıya bildirilir.
    expect(wrapper.text()).toContain('stok kodu')
  })

  it('barkodlu üründe barkodu kodlar ve uyarı göstermez', async () => {
    const wrapper = await mountDialog({ id: 2, ad: 'Çay', stokKodu: 'CAY-1', barkod: '8690002', satisFiyati: 50 })

    expect(wrapper.find('svg.etiket-barkod').attributes('data-icerik')).toBe('8690002')
    expect(wrapper.text()).not.toContain('etiketinde stok kodu')
  })

  it('QR sekmesinde sunucudan QR yükler', async () => {
    const wrapper = await mountDialog({ id: 7, ad: 'MDF Lam', stokKodu: 'MDF-18', barkod: '' })

    await wrapper.findAll('.etiket-tip-secim button')[1].trigger('click')
    await flushPromises()

    expect(stokAPI.etiketQr).toHaveBeenCalledWith(7)
    expect(wrapper.find('img.etiket-qr').exists()).toBe(true)
  })

  it('QR önizlemesi CSP uyumlu data URL kullanır', async () => {
    const wrapper = await mountDialog({ id: 7, ad: 'MDF Lam', stokKodu: 'MDF-18', barkod: '' })

    await wrapper.findAll('.etiket-tip-secim button')[1].trigger('click')
    await vi.waitFor(() => {
      const src = wrapper.find('img.etiket-qr').attributes('src') || ''
      expect(src.startsWith('data:')).toBe(true)
    })
  })

  it('fiyat satisFiyati yoksa fiyat alanına düşer', async () => {
    const wrapper = await mountDialog({ id: 7, ad: 'MDF Lam', stokKodu: 'MDF-18', barkod: '', fiyat: 75 })

    expect(wrapper.find('.etiket-fiyat').text()).toContain('75')
  })

  it('kapanınca QR nesne URLini serbest bırakır', async () => {
    const wrapper = await mountDialog({ id: 7, ad: 'MDF', stokKodu: 'MDF-18', barkod: '' })

    await wrapper.findAll('.etiket-tip-secim button')[1].trigger('click')
    await flushPromises()
    await wrapper.setProps({ visible: false })
    await flushPromises()

    expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:test')
  })
})
