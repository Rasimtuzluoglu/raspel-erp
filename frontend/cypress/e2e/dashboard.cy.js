describe('Dashboard', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} })
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', token: 'test', twoFactorGerekli: false, sirketler: [{ id: 1, ad: 'Test' }], sirketId: 1, sirketAdi: 'Test' }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', token: 'test', sirketId: 1, sirketAdi: 'Test', companyName: 'Test' }
    }).as('girisSirket')
    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', sirketId: 1, companyName: 'Test' }
    }).as('ben')
    cy.intercept('GET', '/api/yetkiler/roller', ['ADMIN']).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', ['CARI', 'FATURA', 'STOK']).as('moduller')
    cy.intercept('GET', '/api/yetkiler', []).as('yetkiler')
    cy.intercept('GET', '/api/doviz', []).as('doviz')
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

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
    })

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı Adı"]').type('admin')
    cy.get('input[type="password"]').type('admin123')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(300)
      }
    })
  })

  it('should display dashboard widgets', () => {
    cy.contains('Toplam Cari').should('be.visible')
    cy.contains('Bakiye').should('be.visible')
    cy.contains('Toplam Likidite').should('be.visible')
  })

  it('should display stat cards', () => {
    cy.contains('10').should('be.visible')
    cy.contains('50.000,00').should('be.visible')
    cy.contains('0').should('be.visible')
  })

  it('should toggle widget settings', () => {
    cy.get('button[title="Widget Ayarları"]').click()
    cy.contains('Gösterilecek Widget').should('be.visible')
    cy.contains('İstatistik Kartları').should('be.visible')
    cy.contains('Uygula').click()
  })
})
