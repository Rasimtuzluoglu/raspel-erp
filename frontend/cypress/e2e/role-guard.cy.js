describe('Role Guard (USER)', () => {
  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: {
        id: 2, username: 'user', displayName: 'User', role: 'USER',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test',
        twoFactorGerekli: false,
        sirketler: [{ id: 1, ad: 'Test' }]
      }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: {
        id: 2, username: 'user', displayName: 'User', role: 'USER',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test', companyName: 'Test'
      }
    }).as('girisSirket')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')
    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: { id: 2, username: 'user', displayName: 'User', role: 'USER', sirketId: 1, companyName: 'Test' }
    }).as('ben')
    cy.intercept('GET', '/api/yetkiler/roller', ['USER']).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', []).as('moduller')
    cy.intercept('GET', '/api/yetkiler', []).as('yetkiler')
    cy.intercept('GET', '/api/doviz', []).as('doviz')
    cy.intercept('GET', '/api/dashboard', {
      statusCode: 200,
      body: { toplamCariSayisi: 0, toplamBakiye: 0, toplamFatura: 0, toplamStok: 0, aylikGelirGider: [], sonHareketler: [], enCokSatanlar: [] }
    }).as('dashboard')
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı Adı"]').type('user')
    cy.get('input[type="password"]').type('123456')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(300)
      }
    })
  })

  it('should not allow USER to access /kullanicilar', () => {
    cy.visit('/kullanicilar')
    cy.url().should('include', '/yetki-reddi')
  })

  it('should not allow USER to access /sirketler', () => {
    cy.visit('/sirketler')
    cy.url().should('include', '/yetki-reddi')
  })

  it('should not allow USER to access /yedekler', () => {
    cy.visit('/yedekler')
    cy.url().should('include', '/yetki-reddi')
  })

  it('should not allow USER to access /butceler', () => {
    cy.visit('/butceler')
    cy.url().should('include', '/yetki-reddi')
  })

  it('should not allow USER to access /denetim', () => {
    cy.visit('/denetim')
    cy.url().should('include', '/yetki-reddi')
  })
})
