describe('Dashboard', () => {
  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', token: 'test', sirketId: 1, sirketAdi: 'Test' }
    }).as('login')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')
    cy.intercept('GET', '/api/dashboard', {
      toplamCariSayisi: 10, toplamBakiye: 50000, pozitifBakiye: 80000, negatifBakiye: -30000,
      bugunkuSiparis: 5, bekleyenTeslimat: 3, iadeOrani: 2, stokDevirHizi: 1.5,
      aktifCalisan: 25, bugunIzinli: 2, buAyIseBaslayacak: 1,
      sonHareketler: [], enCokSatanlar: []
    }).as('dashboard')
    cy.intercept('GET', '/api/cari-hesaplar', []).as('cari')
    cy.intercept('GET', '/api/faturalar', []).as('fatura')
    cy.intercept('GET', '/api/bankalar', []).as('bankalar')
    cy.intercept('GET', '/api/kasalar', []).as('kasalar')
    cy.intercept('GET', '/api/stoklar', []).as('stoklar')

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı adı"]').type('admin')
    cy.get('input[type="password"]').type('admin123')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
  })

  it('should display dashboard widgets', () => {
    cy.contains('Toplam Cari').should('be.visible')
    cy.contains('Toplam Bakiye').should('be.visible')
    cy.contains('Hizli Islemler').should('be.visible')
  })

  it('should display stat cards', () => {
    cy.contains('10').should('be.visible')
    cy.contains('25').should('be.visible')
    cy.contains('5').should('be.visible')
  })

  it('should toggle widget settings', () => {
    cy.get('.pi-cog').click()
    cy.contains('Gösterilecek Widget').should('be.visible')
    cy.contains('İstatistik Kartları').should('be.visible')
    cy.contains('Uygula').click()
  })
})
