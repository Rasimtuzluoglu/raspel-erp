import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'

// Gerçek zincir test edilir: dialog -> tahsilatAPI -> apiClient.post.
// Idempotency başlığı ve çift tıklama koruması bu katmanda gözlenir.
const post = vi.fn(() => Promise.resolve({ data: {} }))
const get = vi.fn(() => Promise.resolve({ data: [] }))

vi.mock('axios', () => ({
  default: {
    get,
    post,
    create: vi.fn(() => ({
      get,
      post,
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  Button: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
  FormField: { template: '<div><slot /></div>' },
  Select: { template: '<div><slot /></div>' },
  InputNumber: { template: '<div><slot /></div>' },
  DatePicker: { template: '<div><slot /></div>' },
  Textarea: { template: '<div><slot /></div>' }
}

const cari = { id: 1, ad: 'Test Cari', bakiye: 500 }

const acVeBul = async () => {
  const TahsilatGirDialog = (await import('../../components/TahsilatGirDialog.vue')).default
  // Diyalog gerçekte visible=false ile mount edilip sonra açılır; formu
  // dolduran watch 'immediate' değil. Test de aynı yaşam döngüsünü izlemeli.
  const wrapper = mount(TahsilatGirDialog, {
    props: { visible: false, cariler: [], cari },
    global: { stubs, plugins: [createPinia(), ToastService, i18n] }
  })
  await wrapper.setProps({ visible: true })
  await flushPromises()
  const btn = wrapper.findAll('button').find((b) => b.attributes('loading') !== undefined)
  return { wrapper, btn }
}

describe('TahsilatGirDialog.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    post.mockClear()
    get.mockClear()
  })

  it('renders payment method buttons (Nakit/Kart/Taksit)', async () => {
    const TahsilatGirDialog = (await import('../../components/TahsilatGirDialog.vue')).default
    const wrapper = mount(TahsilatGirDialog, {
      props: { visible: true, cariler: [] },
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('Nakit')
    expect(text).toContain('Kart')
    expect(text).toContain('Taksit')
    expect(wrapper.find('.tahsilat-form').exists()).toBe(true)
  })

  /**
   * Cift tiklama korumasi: buton :loading ile kapansa da JS seviyesinde de
   * ikinci cagri reddedilir. Yoksa cari bakiyesi ve kasa iki kez guncellenir.
   */
  it('cift tiklamada tahsilat bir kez gonderilir', async () => {
    // Cevap bilerek bitmez: istek "uçuşta" kalir, ikinci tiklama denenir.
    post.mockImplementation(() => new Promise(() => {}))
    const { btn } = await acVeBul()

    await btn.trigger('click')
    await btn.trigger('click')
    await flushPromises()

    expect(post).toHaveBeenCalledTimes(1)
    post.mockImplementation(() => Promise.resolve({ data: {} }))
  })

  /** Her gonderimde X-Idempotency-Key gonderilir; sunucu mükerrer kaydi reddeder. */
  it('idempotency anahtari gonderilir', async () => {
    const { btn } = await acVeBul()

    await btn.trigger('click')
    await flushPromises()

    expect(post).toHaveBeenCalledTimes(1)
    const [url, , config] = post.mock.calls[0]
    expect(url).toBe('/tahsilat')
    expect(config?.headers?.['X-Idempotency-Key']).toBeTruthy()
  })

  /**
   * Korumanin ise yaramasi icin anahtar form eylemi basina sabit olmali.
   * Hata sonrasi kullanici "tekrar dene" derse ayni anahtar gonderilir;
   * boylece sunucu ikinci tahsilati reddeder.
   */
  it('hata sonrasi tekrar denemede ayni idempotency anahtarini kullanir', async () => {
    post.mockImplementation(() => Promise.reject(new Error('baglanti koptu')))
    const { btn } = await acVeBul()

    await btn.trigger('click')
    await flushPromises()
    await btn.trigger('click')
    await flushPromises()

    expect(post).toHaveBeenCalledTimes(2)
    expect(post.mock.calls[0][2].headers['X-Idempotency-Key'])
      .toBe(post.mock.calls[1][2].headers['X-Idempotency-Key'])
    post.mockImplementation(() => Promise.resolve({ data: {} }))
  })
})