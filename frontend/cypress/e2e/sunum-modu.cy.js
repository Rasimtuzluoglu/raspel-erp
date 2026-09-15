describe('Sunum (Müşteri) Modu', () => {
  const banka = { id: 1, ad: 'Test Banka', hesapNo: '123', iban: 'TR120001', bakiye: 1234.56 }

  beforeEach(() => {
    cy.girisYap()
    cy.intercept('GET', '/api/bankalar*', { statusCode: 200, body: { content: [banka] } }).as('bankalar')
    cy.visit('/bankalar')
    cy.get('.gizli-veri').should('exist')
  })

  it('kenar çubuğu butonu ile açılır, hassas alan bulanıklaşır', () => {
    cy.get('html').should('have.attr', 'data-sunum', 'off')
    cy.get('button[title="Sunum modunu aç"]').click()

    cy.get('html').should('have.attr', 'data-sunum', 'on')
    cy.get('.sunum-banner').should('be.visible')
    cy.get('.gizli-veri').first().should('have.css', 'filter').and('contain', 'blur')
  })

  it('Ctrl+Shift+H ile açılır/kapanır', () => {
    cy.window().then((win) => {
      win.dispatchEvent(new KeyboardEvent('keydown', { key: 'H', ctrlKey: true, shiftKey: true, bubbles: true }))
    })
    cy.get('html').should('have.attr', 'data-sunum', 'on')

    cy.window().then((win) => {
      win.dispatchEvent(new KeyboardEvent('keydown', { key: 'H', ctrlKey: true, shiftKey: true, bubbles: true }))
    })
    cy.get('html').should('have.attr', 'data-sunum', 'off')
  })

  it('Gizle modunda hassas alan kaldırılır', () => {
    cy.get('button[title="Sunum modunu aç"]').click()
    cy.get('.sunum-banner-maske').contains('Gizle').click()

    cy.get('html').should('have.attr', 'data-sunum-maske', 'gizle')
    cy.get('.gizli-veri').first().should('have.css', 'display', 'none')
  })

  it('şeritten kapatınca mod kapanır', () => {
    cy.get('button[title="Sunum modunu aç"]').click()
    cy.get('.sunum-banner').should('be.visible')
    cy.get('.sunum-banner-kapat').click()
    cy.get('html').should('have.attr', 'data-sunum', 'off')
    cy.get('.sunum-banner').should('not.exist')
  })
})
