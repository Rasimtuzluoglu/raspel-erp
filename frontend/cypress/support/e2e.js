// Cypress E2E destek dosyasi
// Tum API'ler mock'lanir (backend gerekmez). Ortak giris akisi tek komutta toplanmistir.

Cypress.Commands.add('apiMocklariKur', () => {
  cy.intercept('POST', '/api/kullanicilar/giris', {
    statusCode: 200,
    body: { girisToken: 'test-token', twoFactorGerekli: false, sirketler: [{ id: 1, ad: 'Test Şirketi' }] }
  }).as('login')
  cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
    statusCode: 200,
    body: {
      id: 1,
      username: 'admin',
      displayName: 'Admin',
      role: 'ADMIN',
      token: 'test-token',
      sirketId: 1,
      sirketAdi: 'Test Şirketi',
      companyName: 'Test Şirketi'
    }
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
  // Genel yakalayici: tanimlanmamis GET istekleri bos dizi donsun
  cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
})

Cypress.Commands.add('girisYap', () => {
  cy.apiMocklariKur()
  cy.visit('/giris', {
    onBeforeLoad(win) {
      // Acilis popup'larini (surum notlari / ilk ziyaret ipuclari) bastir
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
      win.localStorage.setItem('raspel_gosterilen_ipuclari', '[]')
    }
  })
  cy.get('input[placeholder="Kullanıcı Adı"]').type('admin')
  cy.get('input[type="password"]').type('Admin123!')
  cy.contains('Giriş Yap').click()
  cy.wait('@login')
  cy.wait('@girisSirket')
  cy.url().should('not.include', '/giris')
})
