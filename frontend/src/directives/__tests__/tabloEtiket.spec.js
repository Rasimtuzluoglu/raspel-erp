import { describe, it, expect, afterEach } from 'vitest'
import { tabloEtiketle } from '../tabloEtiket.js'

const tablo = (thead, tbody) => {
  document.body.innerHTML = `
    <div class="p-datatable">
      <table>
        <thead class="p-datatable-thead"><tr>${thead}</tr></thead>
        <tbody class="p-datatable-tbody">${tbody}</tbody>
      </table>
    </div>`
}

describe('tabloEtiket', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('thead basliklarini tbody hucrelerine data-label olarak yazar', () => {
    tablo('<th>ID</th><th>Ad</th>', '<tr><td>1</td><td>Test</td></tr>')
    tabloEtiketle(document)
    const tdler = document.querySelectorAll('.p-datatable-tbody td')
    expect(tdler[0].getAttribute('data-label')).toBe('ID')
    expect(tdler[1].getAttribute('data-label')).toBe('Ad')
    expect(tdler[0].getAttribute('data-label-ilk')).toBe('1')
    expect(tdler[1].hasAttribute('data-label-ilk')).toBe(false)
  })

  it('basligi bos kolonlari (secim/expander) atlar ve ilk dolu kolonu isaretler', () => {
    tablo('<th></th><th>Ad</th>', '<tr><td><input type="checkbox"></td><td>Test</td></tr>')
    tabloEtiketle(document)
    const tdler = document.querySelectorAll('.p-datatable-tbody td')
    expect(tdler[0].hasAttribute('data-label')).toBe(false)
    expect(tdler[1].getAttribute('data-label')).toBe('Ad')
    expect(tdler[1].getAttribute('data-label-ilk')).toBe('1')
  })

  it('birden fazla satiri etiketler', () => {
    tablo('<th>A</th>', '<tr><td>x</td></tr><tr><td>y</td></tr>')
    tabloEtiketle(document)
    const tdler = document.querySelectorAll('.p-datatable-tbody td')
    expect(tdler[0].getAttribute('data-label')).toBe('A')
    expect(tdler[1].getAttribute('data-label')).toBe('A')
  })
})
