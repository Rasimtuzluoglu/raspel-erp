describe('Teslimatlar - Şoför Görünümü', () => {
  const surucu = { id: 5, ad: 'Ali Şoför', rol: 'DRIVER', bekleyenTeslimatSayisi: 1 }
  const bugun = (() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })()
  const teslimat = {
    id: 1,
    faturaId: 99,
    faturaNumarasi: 'FTR-2026-000099',
    musteriAdi: 'Acme Ltd.',
    teslimatAdresi: 'Test Mah. 1. Sok No:5',
    durum: 'BEKLEMEDE',
    gecikti: false,
    beklenenTeslimTarihi: bugun,
    notlar: null
  }

  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: {
        id: 5, username: 'sofor', displayName: 'Ali Şoför', role: 'DRIVER',
        token: 'test-token', twoFactorGerekli: false,
        sirketler: [{ id: 1, ad: 'Test Şirketi' }], sirketId: 1, sirketAdi: 'Test Şirketi'
      }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: {
        id: 5, username: 'sofor', displayName: 'Ali Şoför', role: 'DRIVER',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test Şirketi', companyName: 'Test Şirketi'
      }
    }).as('girisSirket')
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
    // Spesifik mock'lar catch-all'dan SONRA tanimlanir (son tanimlanan kazanir);
    // aksi halde /api/kullanicilar/ben [] doner ve rol bilgisi kaybolur.
    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: { id: 5, username: 'sofor', displayName: 'Ali Şoför', role: 'DRIVER', sirketId: 1, companyName: 'Test Şirketi' }
    }).as('ben')
    cy.intercept('GET', '/api/deliveries/by-driver', { statusCode: 200, body: [surucu] }).as('byDriver')
    cy.intercept('GET', '/api/deliveries?*', { statusCode: 200, body: [teslimat] }).as('teslimatlar')

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_gelismis_mod', 'true')
    })

    cy.visit('/giris')
    cy.get('[data-test="giris-username"]').type('sofor')
    cy.get('input[type="password"]').type('Sofor123!')
    cy.contains('Giriş Yap').click()
    cy.wait('@login')
    cy.wait('@girisSirket')
    cy.visit('/teslimatlar')
  })

  it('şoför yalnız kendi teslimatlarını sade görünümde görür', () => {
    cy.wait('@byDriver')
    cy.wait('@teslimatlar')
    // Yönetim rehberi ve durum geçmişi şoförde gizli.
    cy.get('.nasil-kart').should('not.exist')
    cy.get('.gecmis-toggle').should('not.exist')
    // Varsayılan filtre "Bugün".
    cy.contains('.filtre-sekme.aktif', 'Bugün').should('exist')
    // Sade görünüm + büyük aksiyonlar
    cy.get('.teslimat-kart.sofor-gorunum').should('have.length', 1)
    cy.contains('.teslimat-kart', 'Acme Ltd.').should('exist')
    cy.contains('.teslimat-kart', 'Test Mah.').should('exist')
    // Şoförde durum dropdown'u yok; aksiyonlar menüde ve "Teslim Et & İmzala" butonunda.
    cy.get('.teslimat-kart .durum-dropdown').should('not.exist')
    cy.contains('button', 'Teslim Et & İmzala').should('be.visible')
    // Şoför listesi (yönetici seçimi) gizli
    cy.get('.surucu-listesi').should('not.exist')
  })

  it('teslim et akışında imza modalı açılır', () => {
    cy.wait('@teslimatlar')
    cy.contains('button', 'Teslim Et & İmzala').first().click()
    cy.contains('Dijital Teslimat & İmza').should('be.visible')
    cy.get('.p-dialog input').first().should('be.visible')
  })
})
