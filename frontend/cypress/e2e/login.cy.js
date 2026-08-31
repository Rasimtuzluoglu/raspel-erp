describe('Login', () => {
  beforeEach(() => {
    cy.visit('/giris')
  })

  it('should display login page', () => {
    cy.contains('RasPel').should('be.visible')
    cy.get('input[placeholder="Kullanıcı Adı"]').should('be.visible')
    cy.get('input[type="password"]').should('be.visible')
    cy.contains('Giriş Yap').should('be.visible')
  })

  it('should show error with empty credentials', () => {
    cy.contains('Giriş Yap').click()
    cy.contains('Kullanıcı adı ve şifre giriniz').should('be.visible')
  })

  it('should login with valid admin credentials', () => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: { girisToken: 'test-token', twoFactorGerekli: false, sirketler: [{ id: 1, ad: 'Test Şirketi' }] }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', token: 'test-token', sirketId: 1, sirketAdi: 'Test Şirketi', companyName: 'Test Şirketi' }
    }).as('girisSirket')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')
    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', sirketId: 1, companyName: 'Test Şirketi' }
    }).as('ben')
    cy.intercept('GET', '/api/yetkiler/roller', ['ADMIN']).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', ['CARI', 'FATURA', 'STOK']).as('moduller')
    cy.intercept('GET', '/api/yetkiler', []).as('yetkiler')
    cy.intercept('GET', '/api/doviz', []).as('doviz')
    cy.intercept('GET', '/api/dashboard', {
      statusCode: 200,
      body: { toplamCariSayisi: 0, toplamBakiye: 0, toplamFatura: 0, toplamStok: 0, sonHareketler: [], enCokSatanlar: [] }
    }).as('dashboard')
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
    })

    cy.get('input[placeholder="Kullanıcı Adı"]').type('admin')
    cy.get('input[type="password"]').type('Admin123!')
    cy.contains('Giriş Yap').click()

    cy.wait('@login').its('response.statusCode').should('eq', 200)
    cy.wait('@girisSirket').its('response.statusCode').should('eq', 200)
    cy.url().should('not.include', '/giris')
    cy.get('.dashboard-container').should('be.visible')
  })

  it('should show error with wrong credentials', () => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 401,
      body: { message: 'Geçersiz kullanıcı adı veya şifre' }
    }).as('loginFail')

    cy.get('input[placeholder="Kullanıcı Adı"]').type('admin')
    cy.get('input[type="password"]').type('wrongpass')
    cy.contains('Giriş Yap').click()

    cy.wait('@loginFail')
    cy.contains('Geçersiz').should('be.visible')
  })
})
