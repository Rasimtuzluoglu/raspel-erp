import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useSayac } from '../useSayac.js'

describe('useSayac', () => {
  beforeEach(() => {
    window.matchMedia = vi.fn().mockReturnValue({ matches: true, media: '' })
  })

  it('reduced-motion açıkken hedefe anında atlar', async () => {
    const hedef = ref(100)
    const { gosterilen } = useSayac(hedef)
    await nextTick()
    expect(gosterilen.value).toBe(100)
  })

  it('hedef değişince yeni değere atlar (reduced-motion)', async () => {
    const hedef = ref(0)
    const { gosterilen } = useSayac(hedef)
    await nextTick()
    expect(gosterilen.value).toBe(0)
    hedef.value = 250
    await nextTick()
    expect(gosterilen.value).toBe(250)
  })
})
