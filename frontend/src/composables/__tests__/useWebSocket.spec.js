import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const { wsMock } = vi.hoisted(() => {
  const abonelikler = []
  const istemci = {
    connected: false,
    ayarlar: null,
    activate: vi.fn(),
    deactivate: vi.fn(() => Promise.resolve()),
    subscribe: vi.fn((hedef, callback) => {
      const kayit = { hedef, callback, unsubscribe: vi.fn() }
      abonelikler.push(kayit)
      return kayit
    })
  }
  return {
    wsMock: {
      sockjsCtor: vi.fn(),
      istemci,
      abonelikler
    }
  }
})

vi.mock('sockjs-client', () => ({ default: wsMock.sockjsCtor }))

vi.mock('@stomp/stompjs', () => ({
  Client: class {
    constructor(ayarlar) {
      wsMock.istemci.ayarlar = ayarlar
      wsMock.istemci.connected = false
      return wsMock.istemci
    }
  }
}))

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => ({ isLoggedIn: true, sirketId: 7 })
}))

import {
  aboneOl,
  bagli,
  yenidenBaglandi,
  useWebSocket,
  webSocketSifirla
} from '../useWebSocket.js'

const baglan = () => {
  wsMock.istemci.connected = true
  wsMock.istemci.ayarlar.onConnect()
}

describe('useWebSocket — tek STOMP baglantisi', () => {
  beforeEach(() => {
    webSocketSifirla()
    wsMock.sockjsCtor.mockClear()
    wsMock.istemci.activate.mockClear()
    wsMock.istemci.deactivate.mockClear()
    wsMock.istemci.subscribe.mockClear()
    wsMock.istemci.connected = false
    wsMock.istemci.ayarlar = null
    wsMock.abonelikler.length = 0
    bagli.value = false
    yenidenBaglandi.value = 0
  })

  afterEach(() => {
    webSocketSifirla()
    vi.clearAllMocks()
  })

  it('iki abone icin tek SockJS/STOMP baglantisi acar', async () => {
    const iptal1 = aboneOl('/topic/a', vi.fn())
    const iptal2 = aboneOl('/topic/b', vi.fn())

    await vi.waitFor(() => expect(wsMock.sockjsCtor).toHaveBeenCalledTimes(1))
    expect(wsMock.istemci.activate).toHaveBeenCalledTimes(1)

    baglan()
    expect(wsMock.istemci.subscribe).toHaveBeenCalledTimes(2)
    expect(wsMock.istemci.subscribe.mock.calls.map(([hedef]) => hedef)).toEqual([
      '/topic/a',
      '/topic/b'
    ])
    expect(bagli.value).toBe(true)

    iptal1()
    iptal2()
  })

  it('son abone ayrilinca baglantiyi kapatir', async () => {
    const iptal1 = aboneOl('/topic/a', vi.fn())
    const iptal2 = aboneOl('/topic/b', vi.fn())
    await vi.waitFor(() => expect(wsMock.sockjsCtor).toHaveBeenCalledTimes(1))
    baglan()

    iptal1()
    expect(wsMock.istemci.deactivate).not.toHaveBeenCalled()

    iptal2()
    expect(wsMock.istemci.deactivate).toHaveBeenCalledTimes(1)
    expect(bagli.value).toBe(false)
  })

  it('yeniden baglaninca abonelikleri yeniden kurar ve sinyal verir', async () => {
    const geriCagri = vi.fn()
    const iptal = aboneOl('/topic/dashboard/7', geriCagri)
    await vi.waitFor(() => expect(wsMock.sockjsCtor).toHaveBeenCalledTimes(1))

    baglan()
    expect(wsMock.istemci.subscribe).toHaveBeenCalledTimes(1)
    expect(yenidenBaglandi.value).toBe(0)

    wsMock.istemci.connected = false
    wsMock.istemci.ayarlar.onWebSocketClose()
    expect(bagli.value).toBe(false)

    baglan()
    expect(wsMock.istemci.subscribe).toHaveBeenCalledTimes(2)
    expect(yenidenBaglandi.value).toBe(1)

    wsMock.istemci.subscribe.mock.calls.at(-1)[1]({ body: '{"tur":"STOK"}' })
    expect(geriCagri).toHaveBeenCalledWith({ body: '{"tur":"STOK"}' })

    iptal()
  })

  it('bildirim zili aboneligini /topic/bildirimler/{sirketId} uzerinden surdurur', async () => {
    const wrapper = mount(
      {
        setup() {
          const { sonBildirim } = useWebSocket()
          return { sonBildirim }
        },
        template: '<div>{{ sonBildirim?.tur }}</div>'
      },
      { global: { config: { warnHandler: () => {} } } }
    )

    await vi.waitFor(() => expect(wsMock.sockjsCtor).toHaveBeenCalledTimes(1))
    baglan()
    expect(wsMock.istemci.subscribe.mock.calls[0][0]).toBe('/topic/bildirimler/7')

    wsMock.istemci.subscribe.mock.calls[0][1]({
      body: '{"tur":"FATURA","baslik":"Yeni fatura"}'
    })
    await flushPromises()
    expect(wrapper.text()).toContain('FATURA')

    wrapper.unmount()
  })
})
