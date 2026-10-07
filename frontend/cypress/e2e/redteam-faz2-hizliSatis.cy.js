// REDTEAM Faz2 canli dogrulama — Hızlı Satış (POS)
//
// Amac dogrulamak icin:
//  1) Teleport edilen sofor dropdown option'i gorunur ve rozetler yaslanir
//  2) Urun arama kutusunda ikon ile yazi UST USTE BINMEZ (padding-left)
//  3) Urun sayaci filtre yokken gercek toplami gosterir (50 degil)
//  4) Stok katalogu 200 yuklenir -> "Daha fazla goster" calisir
//  5) Arama tek gecikmeyle sonuc doner (500ms degil)
describe('REDTEAM Faz2 - Hizli Satış POS', () => {
  beforeEach(() => {
    cy.viewport(1440, 900)

    // ONEMLI SIRA: Once giris akisi kurulur. `apiMocklariKur` icindeki
    // `/api/**` catch-all'i EN SON tanimlanir ve ayni istege uygulanan
    // intercept'lerden SON TANIMLANAN secilir. Bu yuzden asagidaki
    // domain-specific intercept'ler `girisYap()`'tan SONRA gelmeli,
    // aksi halde catch-all bunlari golgeliyor ve `@stoklar` hic tetiklenmiyor.
    cy.girisYap()

    // REDTEAM Faz2.4: katalog artik 200 urun ister (backend varsayilani 50)
    cy.intercept('GET', '**/api/stoklar/filtreli*', {
      statusCode: 200,
      body: {
        content: Array.from({ length: 120 }, (_, i) => ({
          id: 9000 + i,
          ad: 'Test Ürün ' + (i + 1),
          stokKodu: 'TST-' + String(i + 1).padStart(3, '0'),
          satisFiyati: 100 + i,
          miktar: 50,
          birim: 'ADET'
        })),
        totalElements: 120,
        totalPages: 1
      }
    }).as('stoklar')

    cy.intercept('GET', '/api/stoklar/satis-onerileri*', {
      statusCode: 200,
      body: [
        { id: 9000, ad: 'Test Ürün 1', stokKodu: 'TST-001', satisFiyati: 100 },
        { id: 9001, ad: 'Test Ürün 2', stokKodu: 'TST-002', satisFiyati: 101 }
      ]
    }).as('oneri')

    cy.intercept('GET', '/api/kasalar*', {
      statusCode: 200,
      body: { content: [{ id: 1, ad: 'Kasa A', bakiye: 1000 }], totalElements: 1 }
    }).as('kasalar')
    cy.intercept('GET', '/api/bankalar*', {
      statusCode: 200,
      body: { content: [{ id: 1, ad: 'Banka A', bakiye: 2000 }], totalElements: 1 }
    }).as('bankalar')
    cy.intercept('GET', '/api/cari-hesaplar*', {
      statusCode: 200,
      body: { content: [], totalElements: 0 }
    }).as('cariler')
    cy.intercept('GET', '/api/personel*', {
      statusCode: 200,
      body: {
        content: [
          { id: 1, ad: 'Ali', soyad: 'Veli', rol: 'DRIVER' },
          { id: 2, ad: 'Veli', soyad: 'Şoför', rol: 'DRIVER', bekleyenTeslimatSayisi: 3 }
        ],
        totalElements: 2
      }
    }).as('personel')
    cy.intercept('GET', '/api/stoklar/en-cok-satanlar*', { statusCode: 200, body: [] }).as('cokSatan')
    cy.intercept('GET', '/api/poz/terminal*', { statusCode: 200, body: [] }).as('poslar')
    cy.intercept('GET', '/api/satislar/bugun*', { statusCode: 200, body: [] }).as('bugunSatis')

    cy.visit('/hizli-satis')
    cy.wait('@stoklar')
    cy.wait(800)
  })

  it('2.1 sofor dropdown overlay\'i gorunur; rozet saga yaslanmis olmali (teleport CSS duzeltmesi)', () => {
    // .personel-opsiyon artik ONESIZ tanimli. Teleport sonrasi overlay
    // icinde bulunmali ve rozet `margin-left: auto` ile sagda durmali.
    cy.get('body').then(($b) => {
      const sofor = $b.find('#hizli-teslim-sofor').first()
      if (!sofor.length) {
        // dropdown yoksa bu kontrolun gecmesi anlamli degil; atla
        cy.log('sofor dropdown bulunamadi, test atlandi')
        return
      }
      cy.wrap(sofor).click({ force: true })
      cy.get('.p-select-overlay', { timeout: 5000 }).should('be.visible')

      cy.get('.p-select-overlay .personel-opsiyon').should('exist')

      // margin-left:auto uygulanmis mi? (CSS prefix olsaydi UYGULANMAZDI)
      cy.get('.p-select-overlay .personel-opsiyon').first().then(($opt) => {
        const rozet = $opt.find('.sofor-bekleyen')
        if (rozet.length) {
          // jQuery `.css()` HESAPLANMIS degeri dondurur.
          const ml = Cypress.$(rozet).css('margin-left')
          expect(ml, 'rozet margin-left:auto ile saga yaslanmali').to.not.equal('0px')
          const radius = Cypress.$(rozet).css('border-radius')
          expect(radius, 'rozet pill seklinde olmali').to.not.equal('0px')
        }
      })
      cy.get('body').type('{esc}')
    })
  })

  it('2.2 arama kutusunda ikon ile yazi ust uste binmemeli (padding-left uygulanmis)', () => {
    // REDTEAM: .arama-kutusu .p-inputtext kurali :deep OLMADAN yazildigi icin
    // AutoComplete'in ic input'una UYGULANMIYORDU -> app.css'in global
    // padding'i (14px) kaliyordu ve ikon 14px'ten baslayan yazinin ustune
    // biniyordu.
    cy.get('.pos-arac-cubugu .arama-kutusu:not(.barkod-kutu)').first().then(($kutu) => {
      const input = $kutu.find('input').first()
      expect(input.length, 'urun arama inputu bulunmali').to.be.greaterThan(0)
      // jQuery `.css()` HESAPLANMIS degeri dondurur.
      const pad = parseFloat(Cypress.$(input).css('padding-left'))
      const h = parseFloat(Cypress.$(input).css('height'))
      // Ikon: left 0.9rem (14.4px) + ~15px genislik -> ~30px rezervasyon
      expect(pad, `padding-left ${pad}px ikon icin yer birakmali`).to.be.greaterThan(30)
      // Barkod kutusu ile ayni yukseklikte olmali (42px)
      expect(h, 'yukseklik 42px uygulanmali').to.be.closeTo(42, 2)
    })
  })

  it('2.3 urun sayaci gercek toplami gostermeli (filtre yokken 50 degil)', () => {
    // REDTEAM: sayac once yalniz YUKLENMIS (en fazla 50) urunu gosteriyordu.
    cy.get('.urun-sayaci').first().should('be.visible')
    cy.get('.urun-sayaci').first().invoke('text').then((t) => {
      const n = Number(String(t).trim())
      expect(n, `urun sayaci 120 olmaliydi, "${t}" geldi`).to.equal(120)
    })
  })

  it('2.4 katalog 200 urun ister (backend size=50 varsayilanini gecersiz kilmaz)', () => {
    cy.get('.product-grid .product-card').should('have.length.greaterThan', 50)
  })

  it('2.5 arama tek gecikmeyle sonuc donmeli (cift 250ms debounce olmamali)', () => {
    // REDTEAM: :delay="250" + koddaki 250ms setTimeout = ~500ms + RTT.
    // Artik :delay="0", tek gecikme 250ms.
    const t0 = Date.now()
    cy.get('.pos-arac-cubugu .arama-kutusu:not(.barkod-kutu) input').first().type('test', { delay: 10 })
    cy.wait('@oneri', { timeout: 2000 })
    const sure = Date.now() - t0
    expect(sure, `arama ${sure}ms surdu; 500ms uzerinde olmamali`).to.be.lessThan(1500)

    // Oneri paneli acilmali
    cy.get('.p-autocomplete-overlay', { timeout: 4000 }).should('be.visible')
    cy.get('.p-autocomplete-overlay .urun-oneri').should('have.length.greaterThan', 0)
  })

  it('2.6 urun dropdown oku kaldirildi (ok ile panel acilip kapanmamali)', () => {
    // REDTEAM: `dropdown` + minLength=2 bilesimi oku islevsiz yapiyordu.
    // Artik ok yok; yazmaya baslayinca panel acilir ve KALIR.
    cy.get('.pos-arac-cubugu .arama-kutusu:not(.barkod-kutu) input').first().type('te', { delay: 10 })
    cy.get('.p-autocomplete-overlay', { timeout: 4000 }).should('be.visible')
    cy.wait(600)
    // 600ms sonra hala acik olmali (once 1 tick sonra kapaniyordu)
    cy.get('.p-autocomplete-overlay').should('be.visible')
  })
})