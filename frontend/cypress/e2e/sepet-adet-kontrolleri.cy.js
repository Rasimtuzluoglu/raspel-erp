// Sepet adet kontrolleri (REGRESYON): +/− butonlari ve adet penceresi.
//
// GECMIS BUG: `@miktar-artir="satirAktifYap(...)"` Vue tarafindan
// `$event => satirAktifYap(...)` olarak derleniyordu; sarmalayici olusturulup
// olay argumaniyla (idx) CAGIRILMIYORDU -> "+/− calismiyor". Bu spec zinciri
// gercek tarayicida dogrular.
describe('Sepet adet kontrolleri', () => {
  beforeEach(() => {
    cy.girisYap()
    cy.posKatalogMockla([
      {
        id: 9000,
        ad: 'Test Urun',
        stokKodu: 'TST-001',
        barkod: '1234567890',
        satisFiyati: 100,
        miktar: 50,
        birim: 'Adet'
      }
    ])
    cy.intercept('GET', '/api/kasalar*', { statusCode: 200, body: { content: [{ id: 1, ad: 'Kasa A', bakiye: 1000 }], totalElements: 1 } }).as('kasalar')
    cy.intercept('GET', '/api/bankalar*', { statusCode: 200, body: { content: [], totalElements: 0 } }).as('bankalar')
    cy.intercept('GET', '/api/cari-hesaplar*', { statusCode: 200, body: { content: [], totalElements: 0 } }).as('cariler')
    cy.intercept('GET', '/api/personel*', { statusCode: 200, body: { content: [], totalElements: 0 } }).as('personel')
    cy.intercept('GET', '/api/poz/terminal*', { statusCode: 200, body: [] }).as('poslar')
    cy.intercept('GET', '/api/satislar/bugun*', { statusCode: 200, body: [] }).as('bugunSatis')
    cy.visit('/hizli-satis')
    cy.wait(800)
  })

  it('sepet + ve − butonlari adedi degistirir', () => {
    cy.get('input.komut-kutu-girdi').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    cy.get('.sepet-item .adet-girdi').should('have.value', '1')
    cy.get('.sepet-item .sepet-tutar').should('contain', '100')
    // Adet 1 iken − devre disi (minimum), + acik olmali
    cy.get('.sepet-item [aria-label="Miktarı azalt"]').should('be.disabled')
    cy.get('.sepet-item [aria-label="Miktarı artır"]').should('not.be.disabled')
    cy.get('.sepet-item [aria-label="Miktarı artır"]').click()
    cy.get('.sepet-item .sepet-tutar').should('contain', '200')
    cy.get('.sepet-item .adet-girdi').should('have.value', '2')
    cy.get('.sepet-item [aria-label="Miktarı azalt"]').click()
    cy.get('.sepet-item .adet-girdi').should('have.value', '1')
  })

  it('adet penceresi + butonu sayiyi degistirir', () => {
    cy.get('.product-card').first().rightclick()
    cy.get('.adet-popover').should('be.visible')
    cy.get('.adet-popover .adet-btn').eq(1).click()
    cy.get('.adet-popover .adet-popover-girdi input').should('have.value', '2')
  })
})
