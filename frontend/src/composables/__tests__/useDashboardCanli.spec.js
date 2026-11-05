import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

const { wsMock } = vi.hoisted(() => ({
  wsMock: { aboneOl: vi.fn(), iptal: vi.fn(), hedef: null, geriCagri: null }
}))

vi.mock('../useWebSocket.js', async () => {
  const { ref } = await import('vue')
  return {
    aboneOl: wsMock.aboneOl,
    bagli: ref(false),
    yenidenBaglandi: ref(0)
  }
})

import { bagli, yenidenBaglandi } from '../useWebSocket.js'
import { CANLI_ANAHTAR, useDashboardCanli } from '../useDashboardCanli.js'

const mountEt = (props = {}) =>
  mount(
    {
      props: ['sirketId', 'yedekYenile'],
      setup(yerel) {
        const canli = useDashboardCanli({
          sirketId: () => yerel.sirketId,
          yedekYenile: yerel.yedekYenile
        })
        return { ...canli }
      },
      template: '<div />'
    },
    { props: { sirketId: 7, ...props } }
  )

describe('useDashboardCanli', () => {
  beforeEach(() => {
    localStorage.clear()
    wsMock.aboneOl.mockReset()
    wsMock.iptal.mockReset()
    wsMock.hedef = null
    wsMock.geriCagri = null
    wsMock.aboneOl.mockImplementation((hedef, geriCagri) => {
      wsMock.hedef = hedef
      wsMock.geriCagri = geriCagri
      return wsMock.iptal
    })
    bagli.value = false
    yenidenBaglandi.value = 0
  })

  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('/topic/dashboard/{sirketId} aboneligini kurar ve sonOlay doldurur', async () => {
    const wrapper = mountEt({ sirketId: 42 })
    await nextTick()

    expect(wsMock.aboneOl).toHaveBeenCalledWith(
      '/topic/dashboard/42',
      expect.any(Function)
    )

    wsMock.geriCagri({
      body: '{"tur":"FATURA","mesaj":"Yeni fatura","zaman":"2026-11-05T10:00:00Z"}'
    })
    await nextTick()

    expect(wrapper.vm.sonOlay).toEqual({
      tur: 'FATURA',
      mesaj: 'Yeni fatura',
      zaman: '2026-11-05T10:00:00Z'
    })

    wrapper.unmount()
  })

  it('bagli durumunu paylasilan STOMP baglantisindan yansitir', async () => {
    const wrapper = mountEt({ sirketId: 42 })
    await nextTick()
    expect(wrapper.vm.bagli).toBe(false)

    bagli.value = true
    await nextTick()
    expect(wrapper.vm.bagli).toBe(true)

    wrapper.unmount()
    expect(wsMock.iptal).toHaveBeenCalled()
  })

  it('yeniden baglanma sinyalini kendi sayacina cevirir', async () => {
    const wrapper = mountEt({ sirketId: 42 })
    await nextTick()
    expect(wrapper.vm.yenidenBaglandi).toBe(0)

    yenidenBaglandi.value = 1
    await nextTick()
    expect(wrapper.vm.yenidenBaglandi).toBe(1)

    wrapper.unmount()
  })

  it('canli mod kapaliysa abonelik kurmaz', async () => {
    localStorage.setItem(CANLI_ANAHTAR, '0')
    const wrapper = mountEt({ sirketId: 42 })
    await nextTick()

    expect(wrapper.vm.aktif).toBe(false)
    expect(wsMock.aboneOl).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('canli mod kapatilinca abonelik iptal edilir', async () => {
    const wrapper = mountEt({ sirketId: 42 })
    await nextTick()
    expect(wsMock.aboneOl).toHaveBeenCalledTimes(1)

    wrapper.vm.ayarla(false)
    await nextTick()

    expect(wsMock.iptal).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem(CANLI_ANAHTAR)).toBe('0')

    wrapper.unmount()
  })

  it('sirket bilgisi yoksa abonelik kurmaz', async () => {
    const wrapper = mountEt({ sirketId: null })
    await nextTick()
    expect(wsMock.aboneOl).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
