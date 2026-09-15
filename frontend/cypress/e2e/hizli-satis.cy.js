describe('Hızlı Satış (POS)', () => {
  const urun = {
    id: 1,
    ad: 'Test Ürün',
    stokKodu: 'STK-1',
    barkod: '1234567890',
    fiyat: 100,
    satisFiyati: 100,
    miktar: 50,
    birim: 'Adet'
  }

  const tusGonder = (key, ek = {}) => {
    cy.window().then((win) => {
      win.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, ...ek }))
    })
  }

  beforeEach(() => {
    cy.girisYap()
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: { content: [urun] } }).as('stoklar')
    cy.intercept('POST', '/api/faturalar', {
      statusCode: 200,
      body: { id: 99, faturaNumarasi: 'FTR-TEST-0001' }
    }).as('satisOlustur')
    cy.visit('/hizli-satis')
    // Yazdirma penceresini etkisiz kil
    cy.window().then((win) => {
      cy.stub(win, 'print')
      cy.stub(win, 'open').returns({
        document: { write() {}, close() {} },
        focus() {},
        print() {}
      })
    })
  })

  it('POS ekranı yüklenir', () => {
    cy.get('.pos-container').should('exist')
    cy.contains('Hızlı Satış').should('exist')
  })

  it('kısayol ipucu şeridi kapatılıp yeniden açılabilir', () => {
    cy.get('.pos-ipucu').should('be.visible')
    cy.get('.pos-ipucu-kapat').click()
    cy.get('.pos-ipucu').should('not.exist')
    cy.get('.pos-ipucu-btn').click()
    cy.get('.pos-ipucu').should('be.visible')
  })

  it('F2 sepeti temizler', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    tusGonder('F2')
    cy.get('.sepet-item').should('have.length', 0)
    cy.contains('Sepete ürün ekleyin').should('exist')
  })

  it('barkod ile ürün ekleyip perakende satışı tamamlar', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)

    // Müşteri yerine Perakende moduna geç
    cy.get('.musteri-modu').contains('Perakende').click()
    cy.contains('Satışı Tamamla').click()

    cy.wait('@satisOlustur').its('response.statusCode').should('eq', 200)
    cy.get('.satis-ozet').should('be.visible')
    cy.contains('FTR-TEST-0001').should('exist')
  })

  it('müşteri seçili değilken uyarı gösterir (F9)', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    tusGonder('F9')
    cy.contains('Müşteri gerekli').should('exist')
    cy.get('@satisOlustur.all').should('have.length', 0)
  })
})
