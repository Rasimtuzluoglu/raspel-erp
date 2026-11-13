import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../../stores/authStore.js'
import { useDepoYetki } from '../useDepoYetki.js'

describe('useDepoYetki', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('ADMIN her depoda islem yapabilir', () => {
    const auth = useAuthStore()
    auth.kullanici = { id: 1, role: 'ADMIN' }
    const { islemYapabilir } = useDepoYetki()
    expect(islemYapabilir({ sorumluKullaniciId: 99 })).toBe(true)
    expect(islemYapabilir({ sorumluKullaniciId: null })).toBe(true)
  })

  it('USER yalniz sorumlusu oldugu depoda islem yapabilir', () => {
    const auth = useAuthStore()
    auth.kullanici = { id: 7, role: 'USER' }
    const { islemYapabilir } = useDepoYetki()
    expect(islemYapabilir({ sorumluKullaniciId: 7 })).toBe(true)
    expect(islemYapabilir({ sorumluKullaniciId: 8 })).toBe(false)
    expect(islemYapabilir({ sorumluKullaniciId: null })).toBe(false)
  })
})
