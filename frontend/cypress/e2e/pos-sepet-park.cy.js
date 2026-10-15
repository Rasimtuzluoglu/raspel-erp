// Faz E: coklu sepet (park / askiya al) — posYeniAkis acikken.
describe('POS coklu sepet (park)', () => {
  const urunler = [
    { id: 1, ad: 'Test Ürün', stokKodu: 'STK-1', barkod: '1234567890', fiyat: 100, satisFiyati: 100, miktar: 50, birim: 'Adet', kategori: 'Gıda' }
  ]

  beforeEach(() => {
    cy.viewport(1600, 900)
    cy.girisYap()
    cy.posKatalogMockla(urunler)
    cy.intercept('POST', '/api/faturalar', { statusCode: 200, body: { id: 99, faturaNumarasi: 'FTR-1' } })
    cy.visit('/hizli-satis', {
      onBeforeLoad(win) {
        win.localStorage.setItem('raspel_pos_yeni_akis', 'true')
      }
    })
    cy.get('.pos-container').should('exist')
  })

  it('sepeti park eder, yeni sepet acar, parktan geri alir', () => {
    // 2 adetlik sepet park et
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('2*1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    cy.get('.sepet-item .adet-girdi').should('have.value', '2')

    cy.get('.park-btn').click()
    // Park sonrasi sepet bosalir, bir chip gorunur.
    cy.get('.sepet-item').should('have.length', 0)
    cy.get('.park-chip').should('have.length', 1)

    // Yeni sepet: 1 adet
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item .adet-girdi').should('have.value', '1')

    // Parktaki sepeti geri al -> 2 adetlik sepet geri gelir
    cy.get('.park-chip').first().click()
    cy.get('.sepet-item').should('have.length', 1)
    cy.get('.sepet-item .adet-girdi').should('have.value', '2')
  })

  it('bos sepet park edilemez (buton devre disi)', () => {
    cy.get('.park-btn').should('be.disabled')
  })
})
