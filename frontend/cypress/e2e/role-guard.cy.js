describe('Role Guard (USER)', () => {
  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: {
        id: 2, username: 'user', displayName: 'User', role: 'USER',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test'
      }
    }).as('login')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı adı"]').type('user')
    cy.get('input[type="password"]').type('123456')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
  })

  it('should not allow USER to access /kullanicilar', () => {
    cy.visit('/kullanicilar')
    cy.url().should('eq', Cypress.config().baseUrl + '/')
  })

  it('should not allow USER to access /sirketler', () => {
    cy.visit('/sirketler')
    cy.url().should('eq', Cypress.config().baseUrl + '/')
  })

  it('should not allow USER to access /yedekler', () => {
    cy.visit('/yedekler')
    cy.url().should('eq', Cypress.config().baseUrl + '/')
  })

  it('should not allow USER to access /butceler', () => {
    cy.visit('/butceler')
    cy.url().should('eq', Cypress.config().baseUrl + '/')
  })

  it('should not allow USER to access /denetim', () => {
    cy.visit('/denetim')
    cy.url().should('eq', Cypress.config().baseUrl + '/')
  })
})
