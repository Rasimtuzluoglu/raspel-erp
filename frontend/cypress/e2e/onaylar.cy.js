describe('Onay Merkezi', () => {
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
    cy.intercept('GET', '/api/personel-izin', {
      statusCode: 200,
      body: [
        { id: 10, personelId: 5, personelAdi: 'Ayşe Yılmaz', izinTuru: 'Yıllık İzin', baslangic: '2026-09-01', bitis: '2026-09-05', gunSayisi: 5, durum: 'BEKLEMEDE' }
      ]
    }).as('izinler')
    cy.intercept('GET', '/api/personel-masraf-talepler/bekleyenler', { statusCode: 200, body: [] }).as('masraflar')
    cy.intercept('GET', '/api/satinalma-talepler', { statusCode: 200, body: [] }).as('talepler')
    cy.intercept('GET', '/api/siparisler?size=100', {
      statusCode: 200,
      body: [
        { id: 30, siparisNo: 'SIP-2026-000030', cariHesapAd: 'Saha Müşteri A.Ş.', aciklama: 'Saha siparişi', genelToplam: 7500, durum: 'BEKLIYOR' }
      ]
    }).as('siparisler')

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
    cy.visit('/onaylar')
  })

  it('onay merkezi başlığını gösterir', () => {
    cy.contains('Yönetici & Muhasebe Onay Merkezi').should('be.visible')
  })

  it('bekleyen izin talebini listeler', () => {
    cy.contains('Ayşe Yılmaz').should('be.visible')
    cy.contains('Yıllık İzin').should('be.visible')
    cy.contains('Onayla').should('be.visible')
    cy.contains('Reddet').should('be.visible')
  })

  it('izin talebini onaylar', () => {
    cy.intercept('PUT', '/api/personel-izin/10/durum', { statusCode: 200, body: {} }).as('izinOnay')
    cy.contains('Ayşe Yılmaz').should('be.visible')
    cy.contains('Ayşe Yılmaz').closest('.onay-kart').contains('Onayla').click()
    cy.wait('@izinOnay').its('request.body').should('deep.include', { durum: 'ONAYLANDI' })
  })

  it('saha siparişleri sekmesinde bekleyen siparişi gösterir', () => {
    cy.contains('Saha Siparişleri').click()
    cy.contains('SIP-2026-000030').should('be.visible')
    cy.contains('Saha Müşteri A.Ş.').should('be.visible')
  })

  it('saha siparişini onaylar (HAZIRLANIYOR durumuna)', () => {
    cy.intercept('PUT', '/api/siparisler/30/durum', { statusCode: 200, body: {} }).as('siparisOnay')
    cy.contains('Saha Siparişleri').click()
    cy.contains('SIP-2026-000030').should('be.visible')
    cy.contains('SIP-2026-000030').closest('.onay-kart').contains('Onayla').click()
    cy.wait('@siparisOnay').its('request.body').should('deep.include', { durum: 'HAZIRLANIYOR' })
  })
})
