// Diyalog/sayfa duzen regresyonlari: acilista yatay tasma yok, footer erisilebilir.
describe('Diyalog ve sayfa duzeni', () => {
  beforeEach(() => {
    cy.girisYap()
    // Veri uclarini sabitle: sayfa yuklemeleri hata vermesin, diyaloglar acilsin.
    cy.intercept('GET', '/api/**', { statusCode: 200, body: { content: [], totalElements: 0 } })
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} })
  })

  const yatayTasmaYok = (sel) => {
    cy.get(sel).should('be.visible').then(($el) => {
      const el = $el[0]
      expect(el.scrollWidth, sel + ' yatay tasma').to.be.at.most(el.clientWidth + 2)
    })
  }

  it('Yeni Satis penceresi duzgun acilir ve tascmaz', () => {
    cy.visit('/satislar')
    cy.contains('button', 'Yeni Satış').click()
    cy.get('.app-dialog').should('be.visible')
    yatayTasmaYok('.app-dialog .p-dialog-content')
    cy.get('.app-dialog .urun-ekle-satir').should('exist')
    cy.get('.app-dialog .p-dialog-footer').should('be.visible')
    cy.get('.app-dialog .p-dialog-footer button').should('have.length.at.least', 2)
  })

  it('Yeni Teklif penceresi duzgun acilir ve tascmaz', () => {
    cy.visit('/teklifler')
    cy.contains('button', 'Yeni Teklif').click()
    cy.get('.app-dialog').should('be.visible')
    yatayTasmaYok('.app-dialog .p-dialog-content')
    cy.get('.app-dialog .p-dialog-footer').should('be.visible')
    cy.contains('.app-dialog', 'Kalemler').should('exist')
  })

  it('Hesap Ayarlari sayfasi yatay tasma yapmaz', () => {
    cy.visit('/hesap-ayarlari')
    cy.contains('.page-header', 'Hesap Ayarları').should('exist')
    cy.window().then((win) => {
      expect(win.document.documentElement.scrollWidth).to.be.at.most(win.document.documentElement.clientWidth + 1)
    })
  })
})
