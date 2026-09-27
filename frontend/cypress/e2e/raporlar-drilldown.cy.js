// Kârlılık Analizi ve Fatura Geçmişi sayfalarında grup değiştirme, satır
// drill-down ve belge zaman çizelgesi akışlarını doğrular (API mock'lu).
describe('Raporlar drill-down akislari', () => {
  const kirilim = (grup) => ([
    { id: grup === 'KATEGORI' ? null : 10, ad: grup === 'KATEGORI' ? 'Elektronik' : 'Ürün 10', ciro: 200, maliyet: 80, brutKar: 120, marj: 60, pay: 50 },
    { id: grup === 'KATEGORI' ? null : 11, ad: grup === 'KATEGORI' ? 'Gıda' : 'Ürün 11', ciro: 200, maliyet: 150, brutKar: 50, marj: 25, pay: 50 }
  ])

  beforeEach(() => {
    cy.girisYap()

    cy.intercept('GET', '/api/raporlar/karlilik-analizi*', (req) => {
      const grup = new URL(req.url).searchParams.get('grup') || 'KATEGORI'
      req.reply({
        statusCode: 200,
        body: {
          grup,
          ozet: { ciro: 400, maliyet: 230, brutKar: 170, brutKarMarji: 42.5, iadeTutari: 0, iadeMaliyeti: 0, kalemSayisi: 3, negatifMarjliAdet: 0 },
          aylikTrend: [{ ay: '2026-09', ciro: 400, maliyet: 230, brutKar: 170, marj: 42.5 }],
          kirilim: kirilim(grup),
          negatifMarjli: []
        }
      })
    }).as('karlilik')

    cy.intercept('GET', '/api/raporlar/karlilik-detay*', (req) => {
      const deger = new URL(req.url).searchParams.get('deger') || 'Elektronik'
      req.reply({
        statusCode: 200,
        body: {
          grup: 'KATEGORI', deger, altGrup: 'URUN',
          ciro: 200, maliyet: 80, brutKar: 120, marj: 60,
          altKirilim: [{ id: 10, ad: 'Ürün 10', ciro: 200, maliyet: 80, brutKar: 120, marj: 60, pay: 100 }],
          belgeler: [{
            faturaId: 5, faturaNumarasi: 'FTR-2026-000005', tarih: '2026-09-10',
            cariAd: 'Müşteri A', urunAd: 'Ürün 10', adet: 2,
            ciro: 200, maliyet: 80, brutKar: 120, iade: false
          }]
        }
      })
    }).as('karlilikDetay')

    cy.intercept('GET', '/api/raporlar/fatura-gecmis*', {
      statusCode: 200,
      body: {
        content: [{
          id: 1, faturaId: 5, faturaNumarasi: 'FTR-2026-000005', faturaTur: 'SATIS', faturaDurum: 'KESILDI',
          cariHesapAd: 'Müşteri A', olay: 'OLUSTUR', aciklama: 'Fatura oluşturuldu',
          kullaniciAdi: 'Admin', ipAdresi: '127.0.0.1', yazdirmaFormat: null, yaziciAdi: null, kopyaNo: 1,
          tarih: '2026-09-10T10:15:00'
        }],
        totalElements: 1, totalPages: 1, number: 0, size: 20
      }
    }).as('faturaGecmisRapor')

    cy.intercept('GET', '/api/faturalar/5/gecmis', {
      statusCode: 200,
      body: [{
        id: 1, olay: 'OLUSTUR', aciklama: 'Fatura oluşturuldu', kullaniciAdi: 'Admin',
        tarih: '2026-09-10T10:15:00', kopyaNo: 1
      }]
    }).as('faturaZamanCizelgesi')
  })

  it('karlilik: tablo gorunur, grup degisince yenilenir ve satir detayi acilir', () => {
    cy.visit('/raporlar/karlilik-analizi')
    cy.wait('@karlilik')
    cy.get('.karlilik-container h1').should('be.visible')
    // Card #content duzeltmesi: tablo gercekten render edilmeli
    cy.get('.kirilim-tablo .p-datatable-tbody tr').should('have.length', 2)
    cy.contains('.kirilim-tablo .p-datatable-tbody tr', 'Elektronik').should('be.visible')

    // Kategori -> Ürün geçişi yeni istek tetiklemeli
    cy.contains('.filtreler button', 'Ürün').click()
    cy.wait('@karlilik').its('request.url').should('include', 'grup=URUN')
    cy.contains('.kirilim-tablo .p-datatable-tbody tr', 'Ürün 10').should('be.visible')

    // Satıra tıklama detay diyaloğunu açmalı
    cy.contains('.kirilim-tablo .p-datatable-tbody tr', 'Ürün 10').click()
    cy.wait('@karlilikDetay')
    cy.get('.p-dialog').should('be.visible')
    cy.contains('.p-dialog', 'Alt Kırılım').should('be.visible')
    cy.contains('.p-dialog .p-datatable-tbody tr', 'Ürün 10').should('be.visible')

    // Belgeler sekmesi fatura kalemini göstermeli
    cy.contains('.p-dialog [role="tab"], .p-dialog .p-tab, .p-dialog .p-tabview-title', 'Belgeler').click()
    cy.contains('.p-dialog .p-datatable-tbody tr', 'FTR-2026-000005').should('be.visible')
    cy.screenshot('karlilik-detay-dialog')
  })

  it('fatura gecmisi: tablo gorunur ve satir tiklamasi zaman cizelgesini acar', () => {
    cy.visit('/raporlar/fatura-gecmis')
    cy.wait('@faturaGecmisRapor')
    cy.get('.gecmis-tablo .p-datatable-tbody tr').should('have.length', 1)
    cy.contains('.gecmis-tablo .p-datatable-tbody tr', 'FTR-2026-000005').should('be.visible')

    cy.contains('.gecmis-tablo .p-datatable-tbody tr', 'FTR-2026-000005').click()
    cy.wait('@faturaZamanCizelgesi')
    cy.get('.p-dialog').should('be.visible')
    cy.contains('.p-dialog', 'FTR-2026-000005').should('be.visible')
    cy.screenshot('fatura-gecmis-dialog')
  })
})
