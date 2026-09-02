import { describe, it, expect, beforeEach } from 'vitest'
import { useCanliAyar } from '../useCanliAyar.js'

describe('useCanliAyar', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('varsayılan değeri döner', () => {
    const { deger } = useCanliAyar('test_anahtar', 'varsayilan')
    expect(deger.value).toBe('varsayilan')
  })

  it('localStorage değerini okur', () => {
    localStorage.setItem('test_anahtar', 'kayitli')
    const { deger } = useCanliAyar('test_anahtar', 'varsayilan')
    expect(deger.value).toBe('kayitli')
  })

  it('ayarla değeri günceller ve kaydeder', () => {
    const { deger, ayarla } = useCanliAyar('test_anahtar', 'varsayilan')
    ayarla('yeni')
    expect(deger.value).toBe('yeni')
    expect(localStorage.getItem('test_anahtar')).toBe('yeni')
  })
})
