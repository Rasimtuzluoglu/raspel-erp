// Faz B: Yeni POS akisi (posYeniAkis) — odeme odakli katman (overlay).
// Bayrak KAPALIYKEN mevcut satir ici panel kullanilir (bkz. hizli-satis.cy.js).
describe('POS odeme overlay (posYeniAkis)', () => {
  const urunler = [
    { id: 1, ad: 'Test Ürün', stokKodu: 'STK-1', barkod: '1234567890', fiyat: 100, satisFiyati: 100, miktar: 50, birim: 'Adet', kategori: 'Gıda' }
  ]

  beforeEach(() => {
    cy.viewport(1600, 900)
    cy.girisYap()
    cy.posKatalogMockla(urunler)
    cy.intercept('POST', '/api/faturalar', { statusCode: 200, body: { id: 99, faturaNumarasi: 'FTR-TEST-0001' } }).as('satisOlustur')
    cy.intercept('GET', '/api/drivers', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/faturalar/*/yazdirma', { statusCode: 200, body: {} })
    cy.visit('/hizli-satis', {
      onBeforeLoad(win) {
        // Yeni akisi etkinlestir (safeStorage JSON boolean).
        win.localStorage.setItem('raspel_pos_yeni_akis', 'true')
      }
    })
    cy.get('.pos-container').should('exist')
  })

  it('yeni akista satir ici odeme paneli yerine tek "Ödeme Al" aksiyonu gorunur', () => {
    cy.get('.yeni-odeme-btn').should('be.visible').and('contain', 'Ödeme Al')
    // Eski katlanir odeme basligi YOK
    cy.get('.pos-bolum-baslik').filter(':contains("Ödeme")').should('not.exist')
  })

  it('"Ödeme Al" overlay i acar; tus takimi gorunur', () => {
    cy.get('.yeni-odeme-btn').click()
    cy.get('.p-dialog').should('be.visible')
    cy.get('.odeme-keypad').should('be.visible')
    cy.get('.odeme-keypad .keypad-tus').should('have.length', 13)
  })

  it('tus takimina basmak alinan nakit tutarini gunceller', () => {
    cy.get('.yeni-odeme-btn').click()
    cy.get('.odeme-keypad .keypad-tus').filter(':contains("1")').first().click()
    cy.get('.odeme-keypad .keypad-tus').filter(':contains("0")').first().click()
    // Ekranda 10 (₺10,00 / 10) gorunmeli
    cy.get('.keypad-ekran').should('contain', '10')
  })

  it('overlay den satis tamamlanir ve overlay kapanir', () => {
    cy.get('input.komut-kutu-girdi').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    // Perakende (musteri gerekmez) varsayilan.
    cy.get('.yeni-odeme-btn').click()
    cy.get('.p-dialog').should('be.visible')
    cy.get('.p-dialog').contains('button', 'Satışı Tamamla').click()
    cy.wait('@satisOlustur').its('response.statusCode').should('eq', 200)
    // Basarili satistan sonra overlay kapanir.
    cy.get('.p-dialog').should('not.be.visible')
  })
})
