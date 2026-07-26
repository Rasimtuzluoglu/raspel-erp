describe('Navigation', () => {
  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: {
        id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test Şirketi'
      }
    }).as('login')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı adı"]').type('admin')
    cy.get('input[type="password"]').type('admin123')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
    cy.url().should('not.include', '/giris')
  })

  it('should navigate to Dashboard', () => {
    cy.contains('Ana Sayfa').click()
    cy.contains('Raspel ERP Özeti').should('be.visible')
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
    cy.get('.cikis-btn').click()
    cy.url().should('include', '/giris')
    cy.contains('Giriş Yap').should('be.visible')
  })
})
