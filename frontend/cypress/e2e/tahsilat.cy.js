describe('Tahsilat Merkezi', () => {
  const ozetMock = {
    toplamAlacak: 125000,
    vadesiGecmisToplam: 45000,
    vadesiYaklasanToplam: 30000,
    gecikmisCariSayisi: 3,
    acikFaturaSayisi: 8,
    cariler: [
      {
        cariId: 1,
        cariAd: 'Acme Ltd. Şti.',
        email: 'fatura@acme.com',
        telefon: '05321234567',
        faturaSayisi: 3,
        toplamAlacak: 60000,
        gecikmisAlacak: 45000,
        aralik: '61-90 Gün',
        faturalar: [
          { faturaId: 101, faturaNumarasi: 'FTR-2026-000101', vadeTarihi: '2026-05-10', kalanTutar: 30000, gecikmeGunu: 40 },
          { faturaId: 102, faturaNumarasi: 'FTR-2026-000102', vadeTarihi: '2026-06-01', kalanTutar: 30000, gecikmeGunu: 18 }
        ]
      },
      {
        cariId: 2,
        cariAd: 'Beta Ticaret',
        email: null,
        telefon: '05339876543',
        faturaSayisi: 1,
        toplamAlacak: 25000,
        gecikmisAlacak: 0,
        aralik: 'Vadesi Gelmemiş',
        faturalar: [
          { faturaId: 201, faturaNumarasi: 'FTR-2026-000201', vadeTarihi: '2026-09-20', kalanTutar: 25000, gecikmeGunu: -10 }
        ]
      }
    ]
  }

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
    cy.intercept('GET', '/api/tahsilat', { statusCode: 200, body: ozetMock }).as('tahsilatOzet')

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
  })

  it('özet kartlarını gösterir', () => {
    cy.visit('/tahsilat')
    cy.contains('Tahsilat Merkezi').should('be.visible')
    cy.contains('Toplam Alacak').should('be.visible')
    cy.contains('Vadesi Geçmiş').should('be.visible')
    cy.contains('Gecikmiş Cari').should('be.visible')
  })

  it('yaşlandırma tablosunda carileri listeler', () => {
    cy.visit('/tahsilat')
    cy.wait('@tahsilatOzet')
    cy.contains('Acme Ltd. Şti.').should('be.visible')
    cy.contains('Beta Ticaret').should('be.visible')
    cy.contains('61-90 Gün').should('be.visible')
  })

  it('fatura detayını genişletir', () => {
    cy.visit('/tahsilat')
    cy.wait('@tahsilatOzet')
    cy.contains('Acme Ltd. Şti.').closest('tr').find('.p-datatable-row-toggle-button').click()
    cy.contains('FTR-2026-000101').should('be.visible')
    cy.contains('40 gün gecikti').should('be.visible')
  })

  it('hatırlatma e-postası gönderir', () => {
    cy.intercept('POST', '/api/tahsilat/1/hatirlat', {
      statusCode: 200,
      body: { gonderilen: 3 }
    }).as('hatirlat')
    cy.visit('/tahsilat')
    cy.wait('@tahsilatOzet')
    cy.get('[title="E-posta ile hatırlat"]').first().click()
    cy.wait('@hatirlat')
  })

  it('e-posta tanımlı değilse hatırlatma butonu pasif olur', () => {
    cy.visit('/tahsilat')
    cy.wait('@tahsilatOzet')
    cy.contains('Beta Ticaret').should('be.visible')
    cy.get('[title="E-posta tanımlı değil"]').should('exist')
  })
})
