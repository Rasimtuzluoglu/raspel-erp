import { vi } from 'vitest'
import { config } from '@vue/test-utils'

// AppDataTable global olarak kayitli (main.js). Testlerde de passthrough
// olarak tanimliyoruz; boylece altindaki DataTable stub'i ile satirlar/
// slotlar render edilebilir.
config.global.components.AppDataTable = {
  name: 'AppDataTable',
  template: '<div><slot /><slot name="empty" /></div>'
}

const localStorageMock = (() => {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] ?? null),
    setItem: vi.fn((key, value) => {
      store[key] = String(value)
    }),
    removeItem: vi.fn((key) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
    get length() {
      return Object.keys(store).length
    },
    key: vi.fn((i) => Object.keys(store)[i] ?? null)
  }
})()

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, writable: true })

// jsdom'da olmayan tarayıcı API'leri
if (!window.matchMedia) {
  window.matchMedia = vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
}

if (!window.ResizeObserver) {
  window.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

if (!window.IntersectionObserver) {
  window.IntersectionObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

if (!navigator.clipboard) {
  Object.defineProperty(navigator, 'clipboard', {
    value: { writeText: vi.fn().mockResolvedValue(undefined) },
    writable: true
  })
}
