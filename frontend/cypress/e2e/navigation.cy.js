describe('Navigation', () => {
  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: {
        id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test Şirketi',
        twoFactorGerekli: false,
        sirketler: [{ id: 1, ad: 'Test Şirketi' }]
      }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: {
        id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test Şirketi', companyName: 'Test Şirketi'
      }
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
      body: { toplamCariSayisi: 0, toplamBakiye: 0, toplamFatura: 0, toplamStok: 0, aylikGelirGider: [], sonHareketler: [], enCokSatanlar: [] }
    }).as('dashboard')
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_erp_gelismis_mod', 'true')
    })

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı Adı"]').type('admin')
    cy.get('input[type="password"]').type('Admin123!')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
    cy.url().should('not.include', '/giris')
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(300)
      }
    })
  })

  it('should navigate to Dashboard', () => {
    cy.contains('Ana Sayfa').click()
    cy.get('.dashboard-container').should('be.visible')
  })

  it('should navigate to Cari Hesaplar', () => {
    cy.contains('Cari').click()
    cy.url().should('include', '/cari-hesaplar')
  })

  it('should navigate to Faturalar', () => {
    cy.contains('Faturalar').click()
    cy.url().should('include', '/faturalar')
  })

  it('should navigate to Stok', () => {
    cy.contains('Stok').click()
    cy.url().should('include', '/stoklar')
  })

  it('should navigate to Banka', () => {
    cy.contains('Banka').click()
    cy.url().should('include', '/bankalar')
  })

  it('should navigate to Personel', () => {
    cy.contains('Personel').click()
    cy.url().should('include', '/personel')
  })

  it('should navigate to Yedekler (admin only)', () => {
    cy.contains('Yedek').click()
    cy.url().should('include', '/yedekler')
  })

  it('should logout successfully', () => {
    cy.get('.logout-icon-btn').click()
    cy.url().should('include', '/giris')
    cy.contains('Giriş Yap').should('be.visible')
  })
})
