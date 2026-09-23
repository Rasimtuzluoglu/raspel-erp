import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { fisPenceresiAcVeYazdir } from '../fisYazdir.js'

describe('fisYazdir.js', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const sahtePencere = () => {
    const pencere = {
      document: { open: vi.fn(), write: vi.fn(), close: vi.fn() },
      focus: vi.fn(),
      print: vi.fn()
    }
    vi.spyOn(window, 'open').mockReturnValue(pencere)
    return pencere
  }

  it('açar, HTML yazar ve gecikmeyle yazdırır', () => {
    const pencere = sahtePencere()

    const sonuc = fisPenceresiAcVeYazdir('<html></html>')

    expect(sonuc).toBe(pencere)
    expect(pencere.document.write).toHaveBeenCalledWith('<html></html>')
    expect(pencere.document.close).toHaveBeenCalled()
    expect(pencere.print).not.toHaveBeenCalled()

    vi.advanceTimersByTime(300)
    expect(pencere.focus).toHaveBeenCalled()
    expect(pencere.print).toHaveBeenCalled()
  })

  it('popup engellenmişse null döner', () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    expect(fisPenceresiAcVeYazdir('<html></html>')).toBeNull()
  })

  it('özel genişlik/yükseklik ve gecikmeyi kullanır', () => {
    const pencere = sahtePencere()
    fisPenceresiAcVeYazdir('<html></html>', { genislik: 320, yukseklik: 480, gecikme: 100 })

    expect(window.open).toHaveBeenCalledWith('', '_blank', 'width=320,height=480')
    vi.advanceTimersByTime(100)
    expect(pencere.print).toHaveBeenCalled()
  })
})
