describe('Fatura form ekranı', () => {
  const cariMock = [{ id: 3, ad: 'Acme Ltd. Şti.', vergiNumarasi: '1234567890' }]
  const faturaMock = {
    id: 2,
    faturaNumarasi: 'FTR-2026-000002',
    tarih: '2026-08-20',
    tur: 'SATIS',
    durum: 'TASLAK',
    cariHesapId: 3,
    cariHesapAd: 'Acme Ltd. Şti.',
    paraBirimi: 'TRY',
    odemeDurumu: 'ODENMEDI',
    kalanTutar: 200,
    kalemler: [
      { id: 1, aciklama: 'Kalem A', adet: 2, birimFiyat: 100, kdvOrani: 20, iskontoOrani: null, stokId: null }
    ]
  }

  beforeEach(() => {
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
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
    cy.intercept('GET', '/api/yetkiler/roller', ['ADMIN'])
    cy.intercept('GET', '/api/yetkiler/moduller', ['CARI', 'FATURA', 'STOK'])
    cy.intercept('GET', '/api/yetkiler', [])
    cy.intercept('GET', '/api/doviz', [])
    cy.intercept('GET', '/api/cari-hesaplar/filtreli*', {
      statusCode: 200,
      body: { content: cariMock, totalElements: cariMock.length }
    }).as('cariAra')
    cy.intercept('GET', '/api/cari-hesaplar/3', { statusCode: 200, body: cariMock[0] })
    cy.intercept('GET', '/api/faturalar/ozet*', {
      statusCode: 200,
      body: { adet: 0, ciro: 0, tahsilEdilen: 0, kalan: 0 }
    })
    cy.intercept('GET', '/api/faturalar/2', { statusCode: 200, body: faturaMock }).as('faturaDetay')
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/personel*', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/depolar*', { statusCode: 200, body: [] })

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_gelismis_mod', 'true')
      win.localStorage.removeItem('raspel_taslak_fatura_yeni')
    })

    cy.visit('/giris')
    cy.get('[data-test="giris-username"]').type('admin')
    cy.get('[data-test="giris-password"]').type('Admin123!')
    cy.get('[data-test="giris-buton"]').click()
    cy.wait('@login')
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(300)
      }
    })
  })

  it('yeni faturada 3 panel, sifir toplam ve zorunlu alan dogrulamasi', () => {
    cy.visit('/faturalar/yeni')
    cy.get('.form-panel-sol').should('be.visible')
    cy.get('.form-panel-orta').should('be.visible')
    cy.get('.form-panel-sag').should('be.visible')
    cy.get('[data-test="fatura-genel-toplam"]').should('contain', '0')
    cy.get('[data-test="fatura-kaydet"]').click()
    cy.contains('.p-toast-message', 'Fatura türü seçiniz').should('be.visible')
  })

  it('duzenlemede kayitli kalemler ve toplamlar yuklenir', () => {
    cy.visit('/faturalar/2/duzenle')
    cy.wait('@faturaDetay')
    cy.get('[data-test="fatura-cari"]').find('input').should('have.value', 'Acme Ltd. Şti.')
    cy.get('[data-test="fatura-genel-toplam"]').should('contain', '200')
    cy.get('[data-test="fatura-kaydet-kes"]').should('be.visible')
    cy.get('[data-test="fatura-kaydet"]').should('contain', 'Taslak Kaydet')
  })

  it('390px mobilde paneller yigilir ve sticky kaydet cubugu gorunur', () => {
    cy.viewport(390, 844)
    cy.visit('/faturalar/2/duzenle')
    cy.wait('@faturaDetay')
    cy.get('.fatura-form-mobil-bar').should('be.visible')
    cy.get('.form-panel-sol').should('be.visible')
    cy.get('.form-panel-sag').should('be.visible')
    cy.window().then((win) => {
      const doc = win.document.documentElement
      const zoom = Number.parseFloat(getComputedStyle(doc).zoom) || 1
      expect(doc.scrollWidth).to.be.at.most(Math.ceil(win.innerWidth / zoom) + 1)
    })
  })

  // --- Kaydetme akislari ---------------------------------------------------
  const cariVeKalemDoldur = () => {
    cy.intercept('GET', '/api/stoklar/barkod/12345', {
      statusCode: 200,
      body: { id: 10, ad: 'Kalem A', satisFiyati: 100, kdvOrani: 20, miktar: 50 }
    }).as('barkod')
    cy.get('[data-test="fatura-tur"]').click()
    cy.get('.p-select-option').contains('Satış').click()
    cy.get('[data-test="fatura-cari"] .p-autocomplete-dropdown').click()
    cy.get('.p-autocomplete-option').contains('Acme Ltd. Şti.').click()
    cy.get('#hizli-barkod-input').type('12345{enter}')
    cy.wait('@barkod')
    cy.get('[data-test="fatura-genel-toplam"]').should('contain', '100')
  }

  it('taslak kaydet POST govdesini dogru uretir', () => {
    let govde = null
    cy.intercept('POST', '/api/faturalar', (req) => {
      govde = req.body
      req.reply({ statusCode: 201, body: { id: 9, ...req.body } })
    }).as('kaydet')

    cy.visit('/faturalar/yeni')
    cariVeKalemDoldur()
    cy.get('[data-test="fatura-kaydet"]').click()
    cy.wait('@kaydet')

    cy.then(() => {
      expect(govde.durum).to.eq('TASLAK')
      expect(govde.cariHesapId).to.eq(3)
      expect(govde.tur).to.eq('SATIS')
      expect(govde.kalemler[0].aciklama).to.eq('Kalem A')
      expect(govde.kalemler[0].iskontoOrani).to.eq(undefined)
      expect(govde.kalemler[0].tutar).to.eq(100)
    })
    cy.url().should('include', '/faturalar')
  })

  it('Kaydet ve Kes once onay ister ve KESILDI gonderir', () => {
    let govde = null
    cy.intercept('POST', '/api/faturalar', (req) => {
      govde = req.body
      req.reply({ statusCode: 201, body: { id: 10, ...req.body } })
    }).as('kaydet')

    cy.visit('/faturalar/yeni')
    cariVeKalemDoldur()
    cy.get('[data-test="fatura-kaydet-kes"]').click()
    cy.get('.p-confirmdialog').should('be.visible')
    cy.get('.p-confirmdialog button').contains('Kaydet ve Kes').click()
    cy.wait('@kaydet')
    cy.then(() => expect(govde.durum).to.eq('KESILDI'))
  })

  it('duzenlemede odeme alanlari ve durum gonderilmez', () => {
    let govde = null
    cy.intercept('PUT', '/api/faturalar/2', (req) => {
      govde = req.body
      req.reply({ statusCode: 200, body: { ...faturaMock, ...req.body } })
    }).as('guncelle')

    cy.visit('/faturalar/2/duzenle')
    cy.wait('@faturaDetay')
    cy.get('[data-test="fatura-kaydet"]').click()
    cy.wait('@guncelle')
    cy.then(() => {
      expect(govde).to.not.have.property('odenenTutar')
      expect(govde).to.not.have.property('odemeDurumu')
      expect(govde).to.not.have.property('durum')
    })
  })

  it('kredi limiti asiminda onay alip krediLimitiGormezdenGel ile tekrar dener', () => {
    const gonderilenler = []
    cy.intercept('POST', '/api/faturalar', (req) => {
      gonderilenler.push(req.body)
      if (gonderilenler.length === 1) {
        req.reply({ statusCode: 400, body: { message: 'Kredi limiti aşıldı' } })
      } else {
        req.reply({ statusCode: 201, body: { id: 11, ...req.body } })
      }
    }).as('kaydet')

    cy.visit('/faturalar/yeni')
    cariVeKalemDoldur()
    cy.get('[data-test="fatura-kaydet-kes"]').click()
    cy.get('.p-confirmdialog button').contains('Kaydet ve Kes').click()
    cy.contains('.p-confirmdialog', 'Kredi limiti').should('be.visible')
    cy.get('.p-confirmdialog button').contains('Onayla ve Kaydet').click()
    cy.wait('@kaydet')
    cy.then(() => {
      expect(gonderilenler.length).to.eq(2)
      expect(gonderilenler[1].krediLimitiGormezdenGel).to.eq(true)
      expect(gonderilenler[1].durum).to.eq('KESILDI')
    })
  })

  it('cift tiklama tek kayit istegi uretir', () => {
    let sayac = 0
    cy.intercept('POST', '/api/faturalar', (req) => {
      sayac += 1
      req.reply({ delay: 400, statusCode: 201, body: { id: 12, ...req.body } })
    }).as('kaydet')

    cy.visit('/faturalar/yeni')
    cariVeKalemDoldur()
    cy.get('[data-test="fatura-kaydet"]').dblclick()
    cy.wait(1200)
    cy.then(() => expect(sayac).to.eq(1))
  })

  it('depo, manuel kur ve teslimat bilgilerini kaydeder', () => {
    cy.intercept('GET', '/api/depolar*', {
      statusCode: 200,
      body: { content: [{ id: 1, ad: 'Merkez', aktif: true }, { id: 2, ad: 'Yedek', aktif: true }], totalElements: 2 }
    }).as('depolar')
    cy.intercept('GET', '/api/drivers', {
      statusCode: 200,
      body: [{ id: 5, ad: 'Ali Şoför', rol: 'DRIVER' }]
    }).as('suruculer')

    let faturaGovde = null
    let teslimatGovde = null
    cy.intercept('POST', '/api/faturalar', (req) => {
      faturaGovde = req.body
      req.reply({ statusCode: 201, body: { id: 21, ...req.body } })
    }).as('kaydet')
    cy.intercept('POST', '/api/deliveries', (req) => {
      teslimatGovde = req.body
      req.reply({ statusCode: 201, body: { id: 31, ...req.body } })
    }).as('teslimat')

    cy.visit('/faturalar/yeni')
    cy.wait('@depolar')
    cy.get('[data-test="fatura-para-birimi"]').click()
    cy.get('.p-select-option').contains('USD').click()
    cy.get('[data-test="fatura-kur"]').find('input').clear().type('40')
    cy.get('[data-test="fatura-depo"]').click()
    cy.get('.p-select-option').contains('Merkez').click()

    cariVeKalemDoldur()

    cy.get('[data-test="fatura-sofor"]').click()
    cy.get('.p-select-option').contains('Ali Şoför').click()
    cy.get('[data-test="fatura-teslimat-adresi"]').type('Test Mah. 1')
    cy.get('[data-test="fatura-kaydet"]').click()
    cy.wait('@kaydet')
    cy.wait('@teslimat')

    cy.then(() => {
      expect(faturaGovde.paraBirimi).to.eq('USD')
      expect(Number(faturaGovde.kur)).to.eq(40)
      expect(faturaGovde.depoId).to.eq(1)
      expect(teslimatGovde.faturaId).to.eq(21)
      expect(teslimatGovde.driverId).to.eq(5)
      expect(teslimatGovde.teslimatAdresi).to.contain('Test Mah')
    })
  })

  it('duzenlemede depo kilitlidir ve mevcut teslimat gosterilir', () => {
    cy.intercept('GET', '/api/faturalar/2', {
      statusCode: 200,
      body: {
        ...faturaMock,
        depoId: 1,
        depoAd: 'Merkez',
        teslimatVar: true,
        teslimatDurum: 'YOLDA',
        driverAd: 'Ali Şoför'
      }
    }).as('faturaDetayKilitli')

    cy.visit('/faturalar/2/duzenle')
    cy.wait('@faturaDetayKilitli')
    cy.get('[data-test="fatura-depo"]').should('have.class', 'p-disabled')
    cy.get('[data-test="fatura-mevcut-teslimat"]').should('be.visible').and('contain', 'Ali Şoför')
    cy.get('[data-test="fatura-sofor"]').should('not.exist')
  })
})
