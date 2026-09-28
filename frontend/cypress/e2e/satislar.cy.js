describe('Satış İşlemleri', () => {
  const satis = {
    id: 1,
    faturaNumarasi: 'FTR-1-2026-000001',
    tarih: '2026-09-01',
    tur: 'SATIS',
    cariHesapId: 7,
    cariHesapAd: 'Acme Ltd.',
    kdv: 20,
    genelToplam: 120,
    kalanTutar: 120,
    odemeDurumu: 'ODENMEDI',
    durum: 'KESILDI',
    vadeTarihi: '2026-09-10',
    teslimatVar: true,
    teslimatDurum: 'YOLDA',
    driverAd: 'Ali Şoför'
  }

  const listeCevabi = { content: [satis], totalElements: 42, number: 0, size: 25, totalPages: 2 }

  beforeEach(() => {
    cy.girisYap()
    cy.intercept('GET', '/api/faturalar?*', { statusCode: 200, body: listeCevabi }).as('satisListesi')
    cy.visit('/satislar')
  })

  it('liste sunucu taraflı çekilir ve tur=SATIS gönderilir', () => {
    cy.wait('@satisListesi').its('request.url').should('include', 'tur=SATIS')
    cy.contains('FTR-1-2026-000001').should('exist')
    cy.contains('Acme Ltd.').should('exist')
    // Sunucu toplam kayıt sayısı sayfalamada görünür.
    cy.contains('42 kayıt').should('exist')
  })

  it('KDV/kalan/ödeme/teslimat kolonları ve gecikme rozeti görünür', () => {
    cy.wait('@satisListesi')
    cy.contains('th', 'KDV').should('exist')
    cy.contains('th', 'Kalan').should('exist')
    cy.contains('th', 'Ödeme').should('exist')
    cy.contains('th', 'Teslimat').should('exist')
    cy.get('.gecikme-rozet').should('exist')
    cy.contains('.p-datatable-tbody', 'Yolda').should('exist')
  })

  it('arama sunucuya gider', () => {
    cy.wait('@satisListesi')
    cy.get('input[placeholder="Fatura no veya cari ara..."]').type('acme')
    cy.wait('@satisListesi', { timeout: 5000 }).its('request.url').should('include', 'search=acme')
  })

  it('satır aksiyonları: tahsilat, iade, e-fatura, çoğalt, iptal', () => {
    cy.wait('@satisListesi')
    cy.get('.p-datatable-tbody tr').first().find('.satir-eylemler button').first().click()
    cy.contains('.eylem-item', 'Tahsilat Al').should('exist')
    cy.contains('.eylem-item', 'İade Oluştur').should('exist')
    cy.contains('.eylem-item', 'E-Fatura').should('exist')
    cy.contains('.eylem-item', 'Çoğalt').should('exist')
    cy.contains('.eylem-item', 'İptal').should('exist')
  })

  it('Tahsilat Al seçilince cari ön seçili tahsilat ekranı açılır', () => {
    cy.wait('@satisListesi')
    cy.get('.p-datatable-tbody tr').first().find('.satir-eylemler button').first().click()
    cy.contains('.eylem-item', 'Tahsilat Al').click()
    cy.url().should('include', '/tahsilat')
    cy.url().should('include', 'cariId=7')
  })
})
