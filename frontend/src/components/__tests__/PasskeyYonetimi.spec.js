import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import PasskeyYonetimi from '../PasskeyYonetimi.vue'

const { apiMock, olusturMock, toastMock } = vi.hoisted(() => ({
  apiMock: {
    listele: vi.fn(),
    kayitSecenekleri: vi.fn(),
    kaydet: vi.fn(),
    sil: vi.fn()
  },
  olusturMock: vi.fn(),
  toastMock: { basarili: vi.fn(), hata: vi.fn(), uyari: vi.fn(), bilgi: vi.fn() }
}))

vi.mock('../../api/index.js', () => ({ passkeyAPI: apiMock }))
vi.mock('../../utils/webauthn.js', () => ({
  passkeyDestekleniyor: () => true,
  passkeyOlustur: olusturMock
}))
vi.mock('../../composables/useToastBildirim.js', () => ({
  useToastBildirim: () => toastMock
}))

const CardStub = {
  template: '<section><slot name="title"/><slot name="content"/></section>'
}
const DataTableStub = {
  props: ['value'],
  template: '<div><slot name="empty" v-if="!value || !value.length"/><slot /></div>'
}
const ColumnStub = { template: '<div><slot name="body" :data="{}"/></div>' }
const DialogStub = {
  props: ['visible'],
  template: '<div v-if="visible"><slot/><slot name="footer"/></div>'
}
const InputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" v-bind="$attrs" @input="$emit(\'update:modelValue\', $event.target.value)">'
}
const ButtonStub = {
  props: ['label', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button v-bind="$attrs" :disabled="disabled" @click="$emit(\'click\')">{{ label }}<slot /></button>'
}

const mountEt = () =>
  mount(PasskeyYonetimi, {
    global: {
      plugins: [i18n],
      stubs: {
        Card: CardStub,
        DataTable: DataTableStub,
        Column: ColumnStub,
        Dialog: DialogStub,
        InputText: InputTextStub,
        Button: ButtonStub
      }
    }
  })

describe('PasskeyYonetimi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMock.listele.mockResolvedValue({ data: [] })
    apiMock.kayitSecenekleri.mockResolvedValue({
      data: { challengeId: 'c1', publicKey: { challenge: 'x' } }
    })
    apiMock.kaydet.mockResolvedValue({ data: { id: 1 } })
    olusturMock.mockResolvedValue({ id: 'cred-1' })
  })

  it('açılışta passkey listesini yükler ve boş durumu gösterir', async () => {
    const w = mountEt()
    await flushPromises()

    expect(apiMock.listele).toHaveBeenCalled()
    expect(w.text()).toContain(i18n.global.t('passkey.yok'))
    w.unmount()
  })

  it('parola ve etiketle passkey kaydeder', async () => {
    const w = mountEt()
    await flushPromises()

    await w.find('[data-test="passkey-ekle"]').trigger('click')
    await w.find('[data-test="passkey-parola"]').setValue('gizli')
    await w.find('[data-test="passkey-etiket"]').setValue('Telefon')
    await w.find('[data-test="passkey-kaydet"]').trigger('click')
    await flushPromises()

    expect(apiMock.kayitSecenekleri).toHaveBeenCalledWith('gizli')
    expect(olusturMock).toHaveBeenCalled()
    expect(apiMock.kaydet).toHaveBeenCalledWith({
      challengeId: 'c1',
      etiket: 'Telefon',
      credential: { id: 'cred-1' }
    })
    expect(toastMock.basarili).toHaveBeenCalled()
    w.unmount()
  })

  it('boş parolayı reddeder', async () => {
    const w = mountEt()
    await flushPromises()

    await w.find('[data-test="passkey-ekle"]').trigger('click')
    await w.find('[data-test="passkey-kaydet"]').trigger('click')
    await flushPromises()

    expect(apiMock.kayitSecenekleri).not.toHaveBeenCalled()
    expect(w.text()).toContain(i18n.global.t('passkey.parolaZorunlu'))
    w.unmount()
  })
})
