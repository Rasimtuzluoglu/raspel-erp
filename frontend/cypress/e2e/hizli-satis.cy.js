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

  const stoksuzUrun = {
    id: 2,
    ad: 'Zzz Stoksuz Ürün',
    stokKodu: 'STK-2',
    barkod: '2222222222',
    fiyat: 50,
    satisFiyati: 50,
    miktar: 0,
    birim: 'Adet'
  }

  const tusGonder = (key, ek = {}) => {
    cy.window().then((win) => {
      win.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, ...ek }))
    })
  }

  beforeEach(() => {
    cy.girisYap()
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: { content: [urun, stoksuzUrun] } }).as('stoklar')
    cy.intercept('POST', '/api/faturalar', {
      statusCode: 200,
      body: { id: 99, faturaNumarasi: 'FTR-TEST-0001' }
    }).as('satisOlustur')
    // Fiş yazdırma izi: mock'lanmazsa 401 döner ve global oturum kapatma devreye girer.
    cy.intercept('POST', '/api/faturalar/*/yazdirma', { statusCode: 200, body: {} }).as('yazdirmaKaydet')
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

  it('ürün kartları fiyat, KDV notu ve stok durumunu gösterir', () => {
    cy.get('.product-card').should('have.length', 2)
    cy.contains('.product-card', 'Test Ürün').within(() => {
      cy.get('.product-price').should('contain', '100')
      cy.get('.kdv-not').should('be.visible')
      cy.get('.product-gorsel').should('exist')
    })
    // Stokta olmayan ürün görsel olarak işaretli olmalı
    cy.contains('.product-card', 'Zzz Stoksuz Ürün').should('have.class', 'stok-yok')
  })

  it('stokta olmayan ürün sepete eklenmez', () => {
    cy.contains('.product-card', 'Zzz Stoksuz Ürün').click()
    cy.get('.sepet-item').should('have.length', 0)
    cy.contains('stok kalmadı').should('exist')
  })

  it('fiş modu (fiyatlı/fiyatsız) satış için geçici değiştirilebilir', () => {
    // Fiş modu, sipariş özeti sağ kolonda görünürken erişilebilir olmalı.
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    cy.get('.fis-modu-satir').should('exist')
    cy.contains('.fis-modu-satir button', 'Fiyatlı').should('have.attr', 'aria-pressed', 'true')
    cy.contains('.fis-modu-satir button', 'Fiyatsız').scrollIntoView().click({ force: true })
    cy.contains('.fis-modu-satir button', 'Fiyatsız').should('have.attr', 'aria-pressed', 'true')
    cy.contains('.fis-modu-satir button', 'Fiyatlı').should('have.attr', 'aria-pressed', 'false')
  })
})
