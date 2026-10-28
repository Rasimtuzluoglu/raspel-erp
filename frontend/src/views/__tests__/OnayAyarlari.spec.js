import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { onayAyariAPI } from '../../api/index.js'
import OnayAyarlari from '../OnayAyarlari.vue'

const { AYARLAR, confirmRequire, routerPush } = vi.hoisted(() => ({
  AYARLAR: [
    { id: 1, modul: 'MASRAF', esikTutar: 2500, otomatikOnay: false },
    { id: 2, modul: 'SATINALMA', esikTutar: 10000, otomatikOnay: true }
  ],
  confirmRequire: vi.fn(),
  routerPush: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: confirmRequire })
}))

vi.mock('../../api/index.js', () => ({
  onayAyariAPI: {
    listele: vi.fn(() => Promise.resolve({ data: AYARLAR })),
    kaydet: vi.fn(() => Promise.resolve({ data: {} }))
  }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'description'],
    template: '<div class="ph"><span class="ph-title">{{ title }}</span><slot name="actions" /></div>'
  },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  AppDataTable: {
    props: ['value', 'loading'],
    template: '<div class="adt"><slot name="empty" /><slot /></div>'
  },
  Column: true,
  Button: {
    props: ['label', 'disabled', 'loading'],
    template: '<button class="btn" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>'
  },
  InputNumber: true,
  ToggleSwitch: true,
  Select: true,
  Dialog: {
    props: ['visible', 'header'],
    template:
      '<div v-if="visible" class="dialog"><span class="dialog-header">{{ header }}</span><slot /><slot name="footer" /></div>'
  },
  EmptyState: { props: ['message'], template: '<div class="empty">{{ message }}</div>' }
}

const kur = () =>
  mount(OnayAyarlari, {
    global: { plugins: [ToastService, i18n], stubs }
  })

describe('OnayAyarlari.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    onayAyariAPI.listele.mockResolvedValue({ data: AYARLAR })
  })

  it('ayar listesini yükler ve başlığı gösterir', async () => {
    const w = kur()
    await flushPromises()

    expect(onayAyariAPI.listele).toHaveBeenCalled()
    expect(w.find('.ph-title').text()).toBe('Onay Ayarları')
    expect(w.vm.ayarlar).toHaveLength(2)
    expect(w.vm.ayarlar[0].modul).toBe('MASRAF')
  })

  it('eşik ve otomatik onay değişikliğini kaydeder', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.kaydet({ modul: 'MASRAF', esikTutar: 7500, otomatikOnay: true })
    await flushPromises()

    expect(onayAyariAPI.kaydet).toHaveBeenCalledWith({
      modul: 'MASRAF',
      esikTutar: 7500,
      otomatikOnay: true
    })
    expect(onayAyariAPI.listele).toHaveBeenCalledTimes(2)
  })

  it('yeni ayar ekler ve varsayılan eşiği gönderir', async () => {
    const w = kur()
    await flushPromises()

    w.vm.yeniAc()
    expect(w.vm.yeniDialog).toBe(true)
    expect(w.vm.yeniForm.modul).toBe('MASRAF')

    w.vm.yeniForm.modul = 'IZIN'
    w.vm.yeniForm.esikTutar = 4000
    await w.vm.yeniKaydet()
    await flushPromises()

    expect(onayAyariAPI.kaydet).toHaveBeenCalledWith({
      modul: 'IZIN',
      esikTutar: 4000,
      otomatikOnay: false
    })
    expect(w.vm.yeniDialog).toBe(false)
    expect(onayAyariAPI.listele).toHaveBeenCalledTimes(2)
  })

  it('modül seçilmeden ekleme yapmaz', async () => {
    const w = kur()
    await flushPromises()

    w.vm.yeniAc()
    w.vm.yeniForm.modul = null
    await w.vm.yeniKaydet()

    expect(onayAyariAPI.kaydet).not.toHaveBeenCalled()
    expect(w.vm.yeniHata).toBe('Modül seçin')
  })

  it('silme onayı ister ve ayarı varsayılana sıfırlar', async () => {
    const w = kur()
    await flushPromises()

    w.vm.silAc(w.vm.ayarlar[0])
    expect(confirmRequire).toHaveBeenCalledTimes(1)

    const secenek = confirmRequire.mock.calls[0][0]
    expect(secenek.message).toContain('Masraf ayarı sıfırlanacak')

    await secenek.accept()
    await flushPromises()

    expect(onayAyariAPI.kaydet).toHaveBeenCalledWith({
      modul: 'MASRAF',
      esikTutar: 0,
      otomatikOnay: false
    })
    expect(onayAyariAPI.listele).toHaveBeenCalledTimes(2)
  })

  it('yeni ayar formu desteklenen modülleri listeler', async () => {
    const w = kur()
    await flushPromises()

    w.vm.yeniAc()
    expect(w.vm.modulSecenekleri.map((secenek) => secenek.value)).toEqual([
      'MASRAF',
      'SATINALMA',
      'IZIN'
    ])
    expect(w.vm.yeniDialog).toBe(true)
  })

  it('liste boşsa boş durum gösterir', async () => {
    onayAyariAPI.listele.mockResolvedValue({ data: [] })
    const w = kur()
    await flushPromises()

    expect(w.vm.ayarlar).toEqual([])
    expect(w.find('.empty').text()).toBe('Tanımlı onay ayarı bulunmuyor')
  })

  it('onaylar ekranına geri döner', async () => {
    const w = kur()
    await flushPromises()

    const geri = w.findAll('.btn').find((buton) => buton.text() === 'Onaylar')
    await geri.trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/onaylar')
  })
})
