import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Erken kurtarma scripti (public/kurtarma.js) iki tetikleyici sunar:
 *   1) script/chunk yukleme hatasi (window 'error' / unhandledrejection)
 *   2) BOOT WATCHDOG: sayfa yuklendikten sonra Vue HALA mount olmadiysa
 *      (#app-loading hala duruyorsa) bir kez toparlar.
 *
 * Bu test watchdog davranisini dogrular: cache temizligi + SW kaldirma +
 * oturum basina tek deneme (dongu korumasi).
 */

const KAYNAK = readFileSync(join(process.cwd(), 'public/kurtarma.js'), 'utf8')

const kur = () => {
  const cacheSilinen = []
  const unregister = vi.fn(() => Promise.resolve(true))
  vi.stubGlobal('caches', {
    keys: () => Promise.resolve(['pages-dev', 'static-assets-dev']),
    delete: (k) => { cacheSilinen.push(k); return Promise.resolve(true) }
  })
  vi.stubGlobal('navigator', {
    serviceWorker: { getRegistrations: () => Promise.resolve([{ unregister }]) }
  })
  // jsdom location.reload "not implemented" atar; sahte ata.
  try { window.location.reload = vi.fn() } catch { /* yoksay */ }
  return { cacheSilinen, unregister }
}

const bodyYaz = (loadingVar) => {
  document.body.innerHTML = loadingVar
    ? '<div id="app"><div id="app-loading">yukleniyor</div></div>'
    : '<div id="app"><div class="app-root">hazir</div></div>'
}

describe('kurtarma.js boot watchdog', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    sessionStorage.clear()
    bodyYaz(true)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    document.body.innerHTML = ''
  })

  it('mount olmadiysa (#app-loading duruyorsa) onbellegi temizleyip SW kaldirir', async () => {
    const { cacheSilinen, unregister } = kur()
    // eslint-disable-next-line no-new-func
    new Function(KAYNAK)()
    window.dispatchEvent(new Event('load'))
    await vi.advanceTimersByTimeAsync(9000)
    await Promise.resolve()
    await Promise.resolve()

    expect(cacheSilinen.length).toBeGreaterThan(0)
    expect(unregister).toHaveBeenCalled()
    expect(sessionStorage.getItem('raspel_kurtarma_denendi')).toBe('1')
  })

  it('mount olduysa (#app-loading yoksa) mudahale ETMEZ', async () => {
    bodyYaz(false)
    const { cacheSilinen, unregister } = kur()
    // eslint-disable-next-line no-new-func
    new Function(KAYNAK)()
    window.dispatchEvent(new Event('load'))
    await vi.advanceTimersByTimeAsync(9000)
    await Promise.resolve()

    expect(cacheSilinen).toEqual([])
    expect(unregister).not.toHaveBeenCalled()
    expect(sessionStorage.getItem('raspel_kurtarma_denendi')).toBeNull()
  })

  it('oturum basina yalnizca BIR kez toparlar (dongu korumasi)', async () => {
    const { cacheSilinen } = kur()
    // eslint-disable-next-line no-new-func
    new Function(KAYNAK)()
    window.dispatchEvent(new Event('load'))
    await vi.advanceTimersByTimeAsync(9000)
    await Promise.resolve()
    await Promise.resolve()
    const ilkSayi = cacheSilinen.length
    expect(ilkSayi).toBeGreaterThan(0)

    // Ikinci bir script yukleme hatasi tetiklense bile kilit nedeniyle
    // yeniden temizlik yapilmaz.
    window.dispatchEvent(new Event('error'))
    await vi.advanceTimersByTimeAsync(2000)
    await Promise.resolve()
    expect(cacheSilinen.length).toBe(ilkSayi)
  })
})
