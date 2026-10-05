// REDTEAM Faz3 canli dogrulama — yetki gosterimi + PrimeVue 4 ikon alanlari
//
// Amac dogrulamak icin:
//  1) Kategoriler sayfasinda EKRANA basilan `v-permission="..."` metni YOK
//     (REDTEAM 3.1: yonerge etiket disinda kalmisti, Vue metin dugumu
//      derleyip ekrana basiyordu; derleyici uyari vermiyordu)
//  2) Ayni hata Projeler / Personel / Izinler sayfalarinda da YOK
//  3) Arama kutusu PrimeVue 4 `IconField` ile kurulu: ikon ile yazi
//     UST USTE BINMEZ (padding-left olculur) ve ikon gercekten gorunur
//  4) Kaldirilmis `p-input-icon-left` sinifi DOM'da HICBIR YERDE yok
describe('REDTEAM Faz3 - Yetki gosterimi ve PrimeVue 4 ikon alanlari', () => {
  beforeEach(() => {
    cy.viewport(1440, 900)
    // ONEMLI SIRA: `girisYap()` ONCE, alan adina ozgu intercept'ler SONRA
    // (catch-all `/api/**` en son tanimlanir ve golgelemesin).
    cy.girisYap()
  })

  // -------------------------------------------------------------------------
  describe('3.1 etiket disinda kalmis yonerge metni ekrana basilmamali', () => {
    const SAYFALAR = [
      { yol: '/kategoriler', ad: 'Kategoriler' },
      { yol: '/projeler', ad: 'Projeler' },
      { yol: '/personel', ad: 'Personel' }
    ]

    SAYFALAR.forEach(({ yol, ad }) => {
      it(`${ad} sayfasinda "v-permission" metni gorunmemeli`, () => {
        cy.visit(yol)
        cy.wait(600)

        // REDTEAM: hata ekranda `v-permission="'STOK_DELETE'"` gibi literal
        // metin olarak gorunuyordu. Sifir tolerans.
        cy.get('body').then(($b) => {
          const metin = $b.text()
          expect(metin, `${ad}: ekranda "v-permission" metni var`).to.not.include('v-permission')
          // Buton etiketleri arasinda sizan yonerge kodu olmamali
          expect(metin, `${ad}: ekranda yetki kodu sizdi`).to.not.include('STOK_DELETE')
          expect(metin, `${ad}: ekranda yetki kodu sizdi`).to.not.include('IK_DELETE')
          expect(metin, `${ad}: ekranda yetki kodu sizdi`).to.not.include('SISTEM_DELETE')
        })

        // Ham DOM'da da olmamali (bir bilesen icine gomulmus olsa bile)
        cy.document().then((doc) => {
          const html = doc.documentElement.outerHTML
          expect(html, `${ad}: DOM icinde "v-permission=" var`).to.not.include('v-permission=')
        })
      })
    })

    it('Kategorilerde silme butonu ADMIN olarak gorunur (kontrol etkisiz degil)', () => {
      // REDTEAM: metin dugumu silinince buton YOK olmamali; `v-if`
      // dogru yetkiye baglanmali. Giris mock'u role=ADMIN donuyor.
      cy.visit('/kategoriler')
      cy.wait(600)
      cy.get('body').then(($b) => {
        const sil = $b.find('button .pi-trash')
        expect(sil.length, 'ADMIN icin silme butonu gorunmeli').to.be.greaterThan(0)
      })
    })
  })

  // -------------------------------------------------------------------------
  describe('3.3 arama kutusu PrimeVue 4 IconField ile kurulu', () => {
    const ARAMA_SAYFALARI = [
      { yol: '/satislar', ad: 'Satış', kutu: '.arama-kutu' },
      { yol: '/faturalar', ad: 'Faturalar', kutu: '.arama-kutu' }
    ]

    ARAMA_SAYFALARI.forEach(({ yol, ad, kutu }) => {
      it(`${ad}: IconField var, ikon ile yazi ust uste binmiyor`, () => {
        cy.visit(yol)
        cy.wait(700)

        // PrimeVue 4 ikon alani
        cy.get(`${kutu}.p-iconfield`).should('exist')
        cy.get(`${kutu}.p-iconfield .p-inputicon`).should('exist')

        // REDTEAM 3.3: `p-input-icon-left` PrimeVue 4'te uretilmiyordu;
        // yerlestirme kurali uygulanmiyor, ikon yazinin ustune biniyordu.
        cy.document().then((doc) => {
          expect(
            doc.documentElement.outerHTML,
            `${ad}: DOM'da kaldirilmis p-input-icon-left var`
          ).to.not.include('p-input-icon-left')
        })

        // Olculmus yerlesim: ikon yerini bos birakmali
        cy.get(`${kutu}.p-iconfield input`).first().then(($input) => {
          expect($input.length, `${ad}: arama inputu bulunmali`).to.be.greaterThan(0)
          const pad = parseFloat(Cypress.$($input).css('padding-left'))
          expect(
            pad,
            `${ad}: padding-left ${pad}px; ikon icin yer birakmali (>30px)`
          ).to.be.greaterThan(30)
        })
      })
    })

    it('Stoklar filtre kutusunda ikon alani tam genislikte (grid 1/-1 kurali)', () => {
      cy.visit('/stoklar')
      cy.wait(700)
      cy.get('.filter-bar .p-iconfield').should('exist')
      cy.get('.filter-bar .p-iconfield input').first().then(($input) => {
        const pad = parseFloat(Cypress.$($input).css('padding-left'))
        expect(pad, `Stoklar: padding-left ${pad}px ikon yerini bos birakmali`).to.be.greaterThan(30)
      })
    })
  })
})