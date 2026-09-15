import { describe, it, expect, beforeEach } from 'vitest'
import { nextTick } from 'vue'
import { useSunumModu } from '../useSunumModu.js'

describe('useSunumModu', () => {
  beforeEach(async () => {
    useSunumModu().kapat()
    await nextTick()
  })

  it('baslangicta kapalidir', () => {
    const { aktif } = useSunumModu()
    expect(aktif.value).toBe(false)
  })

  it('degistir durumu cevirir ve documentElement niteligini ayarlar', async () => {
    const { aktif, degistir } = useSunumModu()

    degistir()
    await nextTick()
    expect(aktif.value).toBe(true)
    expect(document.documentElement.getAttribute('data-sunum')).toBe('on')

    degistir()
    await nextTick()
    expect(aktif.value).toBe(false)
    expect(document.documentElement.getAttribute('data-sunum')).toBe('off')
  })

  it('ac/kapat dogrudan ayarlar', async () => {
    const { aktif, ac, kapat } = useSunumModu()
    ac()
    await nextTick()
    expect(aktif.value).toBe(true)
    kapat()
    await nextTick()
    expect(aktif.value).toBe(false)
  })
})
