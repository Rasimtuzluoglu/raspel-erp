describe('Login', () => {
  beforeEach(() => {
    cy.visit('/giris')
  })

  it('should display login page', () => {
    cy.contains('RasPel').should('be.visible')
    cy.get('input[placeholder="Kullanıcı adı"]').should('be.visible')
    cy.get('input[type="password"]').should('be.visible')
    cy.contains('Giriş Yap').should('be.visible')
  })

  it('should show error with empty credentials', () => {
    cy.contains('Giriş Yap').click()
    cy.contains('Kullanıcı adı ve şifre giriniz').should('be.visible')
  })

  it('should login with valid admin credentials', () => {
    cy.intercept('POST', '/api/kullanicilar/giris').as('login')
    cy.intercept('GET', '/api/sirketler/aktif').as('sirketler')

    cy.get('input[placeholder="Kullanıcı adı"]').type('admin')
    cy.get('input[type="password"]').type('admin123')
    cy.contains('Giriş Yap').click()

    cy.wait('@login', { timeout: 15000 }).its('response.statusCode').should('eq', 200)
    cy.url().should('not.include', '/giris')
    cy.contains('Raspel ERP Özeti').should('be.visible')
  })

  it('should show error with wrong credentials', () => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 401,
      body: { message: 'Geçersiz kullanıcı adı veya şifre' }
    }).as('loginFail')

    cy.get('input[placeholder="Kullanıcı adı"]').type('admin')
    cy.get('input[type="password"]').type('wrongpass')
    cy.contains('Giriş Yap').click()

    cy.wait('@loginFail')
    cy.contains('Geçersiz').should('be.visible')
  })
})
