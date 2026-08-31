describe('Fatura Yönetimi', () => {
  const faturalarMock = [
    { id: 1, faturaNumarasi: 'FTR-2026-000001', tarih: '2026-08-15', tur: 'SATIS', cariHesapAd: 'Acme Ltd. Şti.', genelToplam: 15000, durum: 'TAMAMLANDI' },
    { id: 2, faturaNumarasi: 'FTR-2026-000002', tarih: '2026-08-20', tur: 'ALIS', cariHesapAd: 'Tedarikçi A.Ş.', genelToplam: 8000, durum: 'TASLAK' }
  ]

  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', token: 'test-token', twoFactorGerekli: false, sirketler: [{ id: 1, ad: 'Test Şirketi' }], sirketId: 1, sirketAdi: 'Test Şirketi' }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', token: 'test-token', sirketId: 1, sirketAdi: 'Test Şirketi', companyName: 'Test Şirketi' }
    }).as('girisSirket')
    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Admin', role: 'ADMIN', sirketId: 1, companyName: 'Test Şirketi' }
    }).as('ben')
    cy.intercept('GET', '/api/yetkiler/roller', ['ADMIN']).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', ['CARI', 'FATURA', 'STOK']).as('moduller')
    cy.intercept('GET', '/api/yetkiler', []).as('yetkiler')
    cy.intercept('GET', '/api/doviz', []).as('doviz')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')
    cy.intercept('GET', '/api/dashboard', {
      statusCode: 200,
      body: { toplamCariSayisi: 0, toplamBakiye: 0, toplamFatura: 0, toplamStok: 0, sonHareketler: [], enCokSatanlar: [] }
    }).as('dashboard')
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
    cy.intercept('GET', '/api/faturalar', { statusCode: 200, body: faturalarMock }).as('faturalar')
    cy.intercept('GET', '/api/faturalar/1', {
      statusCode: 200,
      body: { id: 1, faturaNumarasi: 'FTR-2026-000001', tarih: '2026-08-15', tur: 'SATIS', cariHesapAd: 'Acme Ltd. Şti.', genelToplam: 15000, durum: 'TAMAMLANDI', kalemler: [] }
    }).as('faturaDetay')

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_gelismis_mod', 'true')
    })

    cy.visit('/giris')
    cy.get('input[placeholder="Kullanıcı Adı"]').type('admin')
    cy.get('input[type="password"]').type('Admin123!')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(300)
      }
    })
    cy.visit('/faturalar')
  })

  it('fatura listesini gösterir', () => {
    cy.contains('Fatura Yönetimi').should('be.visible')
    cy.wait('@faturalar')
    cy.contains('FTR-2026-000001').should('be.visible')
    cy.contains('FTR-2026-000002').should('be.visible')
    cy.contains('Acme Ltd. Şti.').should('be.visible')
  })

  it('fatura detayına gider', () => {
    cy.wait('@faturalar')
    cy.get('[title="Görüntüle"]').first().click()
    cy.url().should('include', '/faturalar/1')
    cy.wait('@faturaDetay')
  })

  it('Yeni Fatura butonunu gösterir', () => {
    cy.contains('Yeni Fatura').should('be.visible')
  })
})
