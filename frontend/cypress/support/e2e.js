// Cypress E2E destek dosyasi
// Tum API'ler mock'lanir (backend gerekmez). Ortak giris akisi tek komutta toplanmistir.

Cypress.Commands.add('apiMocklariKur', () => {
  // Genel yakalayici EN BASA tanimlanir. Cypress aynı isteğe birden fazla
  // intercept uygulandığında EN SON tanımlananı seçer (tanımlama sırasının
  // tersten çalışması). Bu satır sonda iken `/api/**` catch-all'i
  // `/api/kullanicilar/ben`, `/api/dashboard` ve `/api/yetkiler/*`
  // mock'larını gölgeliyor, hepsi `[]` dönüyor ve giriş akışı kırılıyordu.
  cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')

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
})

/**
 * POS (Hizli Satis) katalog mock'lari — TEK yerden.
 *
 * SURUKLENME ONARIMI: katalog sunucu-sayfali hâle geldiginde (`filtreli`),
 * barkod taramasi da yerel `stokStore` bossa `/api/stoklar/ara`'ya duser.
 * Spec'ler yalnizca `**\/api/stoklar*`'i mock'ladigi icin bu uclar catch-all'a
 * (`[]`) gidiyor, urun "bulunamadi" olup "Yeni Urun Olustur" diyalogu aciliyor
 * ve sonraki `cy.type` mask tarafindan engelleniyordu.
 *
 * Bu komut POS'un kullandigi TUM katalog uclarini tutarli mock'lar; kategori/
 * marka/stok grubu dagilimi urunlerden turetilir (elle senkron tutma yok).
 *
 * @param {Array<object>} urunler POS'ta gorunecek urunler
 */
Cypress.Commands.add('posKatalogMockla', (urunler = []) => {
  const sayfa = { content: urunler, totalElements: urunler.length, totalPages: 1 }
  const degerler = (alan) => [
    ...new Set(urunler.map((u) => u[alan]).filter(Boolean))
  ].map((deger) => ({ deger, sayi: urunler.filter((u) => u[alan] === deger).length }))

  cy.intercept('GET', '**/api/stoklar/filtreli*', { statusCode: 200, body: sayfa }).as('posFiltreli')
  cy.intercept('GET', '**/api/stoklar/ara*', { statusCode: 200, body: urunler }).as('posAra')
  cy.intercept('GET', '**/api/stoklar/gruplama-dagilimi*', {
    statusCode: 200,
    body: {
      kategoriler: degerler('kategori'),
      markalar: degerler('marka'),
      stokGruplari: degerler('stokGrubu')
    }
  }).as('posDagilim')
  cy.intercept('GET', '**/api/stoklar/en-cok-satanlar*', { statusCode: 200, body: [] }).as('posCokSatan')
  cy.intercept('GET', '**/api/stoklar/satis-onerileri*', { statusCode: 200, body: urunler }).as('posOneri')
  // Legacy global stok store (`stokStore.getAll`) icin: barkodTarandi yerel
  // arama yapabilsin. `**\/api/stoklar*` glob'u '/' gecmedigi icin yukaridaki
  // ozel uclari golgelemez.
  cy.intercept('GET', '**/api/stoklar*', { statusCode: 200, body: sayfa }).as('posStoklar')
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
