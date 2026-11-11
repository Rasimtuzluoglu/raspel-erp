describe('Fatura Yönetimi', () => {
  const faturalarMock = [
    { id: 1, faturaNumarasi: 'FTR-2026-000001', tarih: '2026-08-15', tur: 'SATIS', cariHesapAd: 'Acme Ltd. Şti.', genelToplam: 15000, durum: 'TAMAMLANDI' },
    { id: 2, faturaNumarasi: 'FTR-2026-000002', tarih: '2026-08-20', tur: 'ALIS', cariHesapAd: 'Tedarikçi A.Ş.', genelToplam: 8000, durum: 'TASLAK' }
  ]

  beforeEach(() => {
    // Genel yakalayici EN BASA tanimlanir: Cypress aynı istege birden fazla
    // intercept uygulandığında EN SON tanımlananı seçer. Sonda kalsaydı
    // `/api/kullanicilar/ben` mock'unu gölgeler, `kullanici` bir DİZİ olur,
    // `isAdmin` false düşer ve `/faturalar/:id` yönlendirmesi `/yetki-reddi`ye
    // gider.
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
    cy.intercept('GET', '/api/yetkiler/roller', ['ADMIN']).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', ['CARI', 'FATURA', 'STOK']).as('moduller')
    cy.intercept('GET', '/api/yetkiler', []).as('yetkiler')
    cy.intercept('GET', '/api/doviz', []).as('doviz')
    cy.intercept('GET', '/api/sirketler/aktif', []).as('sirketler')
    cy.intercept('GET', '/api/dashboard', {
      statusCode: 200,
      body: { toplamCariSayisi: 0, toplamBakiye: 0, toplamFatura: 0, toplamStok: 0, sonHareketler: [], enCokSatanlar: [] }
    }).as('dashboard')
    cy.intercept('GET', '/api/faturalar?*', { statusCode: 200, body: faturalarMock }).as('faturalar')
    cy.intercept('GET', '/api/faturalar/1', {
      statusCode: 200,
      body: { id: 1, faturaNumarasi: 'FTR-2026-000001', tarih: '2026-08-15', tur: 'SATIS', cariHesapAd: 'Acme Ltd. Şti.', genelToplam: 15000, durum: 'TAMAMLANDI', kalemler: [] }
    }).as('faturaDetay')

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_gelismis_mod', 'true')
    })

    cy.visit('/giris')
    // Giris formu alanlari data-test ile isaretli; placeholder metni degisse de
    // secici kirilmaz.
    cy.get('[data-test="giris-username"]', { timeout: 15000 }).type('admin')
    cy.get('[data-test="giris-password"]').type('Admin123!')
    cy.get('[data-test="giris-buton"]').click()
    cy.wait('@login')
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(300)
      }
    })
    cy.visit('/faturalar')
  })

  it('fatura listesini gösterir', () => {
    cy.contains('Fatura Yönetimi').should('be.visible')
    cy.wait('@faturalar')
    cy.contains('FTR-2026-000001').should('be.visible')
    cy.contains('FTR-2026-000002').should('be.visible')
    cy.contains('Acme Ltd. Şti.').should('be.visible')
    cy.screenshot('fatura-liste-masaustu', { capture: 'viewport' })
  })

  it('fatura detayına gider', () => {
    cy.wait('@faturalar')
    cy.get('[title="Görüntüle"]').first().click()
    cy.url().should('include', '/faturalar/1')
    cy.wait('@faturaDetay')
  })

  it('Yeni Fatura butonunu gösterir', () => {
    cy.contains('Yeni Fatura').should('be.visible')
  })

  it('teslimatsız faturaya şoför atanır', () => {
    cy.wait('@faturalar')
    cy.intercept('GET', '/api/drivers', {
      statusCode: 200,
      body: [{ id: 5, ad: 'Ali Şoför', rol: 'DRIVER', bekleyenTeslimatSayisi: 0 }]
    }).as('suruculer')
    cy.intercept('POST', '/api/deliveries', { statusCode: 201, body: { id: 1 } }).as('teslimatOlustur')

    cy.get('.p-datatable-tbody tr').first().find('.satir-eylemler button').first().click()
    cy.contains('.eylem-item', 'Şoför Ata').click()
    cy.contains('.p-dialog', 'Fişe Şoför Ata').should('be.visible')

    cy.get('.p-dialog .p-select').click()
    cy.contains('.p-select-overlay .p-select-option', 'Ali Şoför').click()
    cy.get('.p-dialog textarea').type('Test Mah. 5. Sok No:1')
    cy.contains('.p-dialog button', 'Şoför Ata').click()

    cy.wait('@teslimatOlustur').its('request.body').then((body) => {
      expect(body.faturaId).to.eq(1)
      expect(body.driverId).to.eq(5)
      expect(body.teslimatAdresi).to.contain('Test Mah')
    })
  })

  it('tur ve durum filtreleri sunucuya gönderilir', () => {
    cy.wait('@faturalar')
    cy.contains('.durum-cip', 'Kesildi').click()
    cy.wait('@faturalar').its('request.url').should('include', 'durum=KESILDI')

    cy.get('.tur-filtre-secim').contains('button', 'Satış').click()
    cy.wait('@faturalar').its('request.url').should('include', 'tur=SATIS')
  })

  it('KPI özeti aktif filtrelerle yenilenir ve Temizle hepsini sıfırlar', () => {
    const ozetIstekleri = []
    cy.intercept('GET', '/api/faturalar/ozet*', (req) => {
      ozetIstekleri.push(req.url)
      req.reply({ statusCode: 200, body: { adet: 2, ciro: 23000, tahsilEdilen: 0, kalan: 23000 } })
    }).as('ozet')

    cy.wait('@faturalar')
    cy.get('.fatura-kpi').should('be.visible')

    cy.contains('.durum-cip', 'Kesildi').click()
    cy.wait('@faturalar').its('request.url').should('include', 'durum=KESILDI')
    cy.then(() => {
      expect(ozetIstekleri.some((u) => u.includes('durum=KESILDI'))).to.eq(true)
    })

    cy.get('[data-test="fatura-aktif-filtreler"]').should('be.visible')
    cy.get('[data-test="fatura-filtre-temizle"]').click()
    cy.wait('@faturalar').its('request.url').should('not.include', 'durum=')
    cy.get('[data-test="fatura-aktif-filtreler"]').should('not.exist')
    cy.then(() => {
      const son = ozetIstekleri[ozetIstekleri.length - 1] || ''
      expect(son).to.not.include('durum=')
    })
  })

  it('sıralama sunucuya sort parametresi gönderir', () => {
    cy.wait('@faturalar')
    cy.get('.p-datatable-thead th').contains('Fatura No').click()
    cy.wait('@faturalar').its('request.url').should('include', 'sort=faturaNumarasi,asc')
    cy.get('.p-datatable-thead th').contains('Fatura No').click()
    cy.wait('@faturalar').its('request.url').should('include', 'sort=faturaNumarasi,desc')
  })

  it('satır menüsü tetikleyicinin yanında açılır', () => {
    cy.wait('@faturalar')
    cy.get('.p-datatable-tbody tr').first().find('.satir-eylemler button').first().as('tetik')
    cy.get('@tetik').click()
    cy.get('body > .eylem-menu').should('be.visible').then(($menu) => {
      const menu = $menu[0].getBoundingClientRect()
      cy.get('@tetik').then(($btn) => {
        const btn = $btn[0].getBoundingClientRect()
        // Menü satirin hemen altinda/yaninda acilir; ekranin sol ustune kacmaz.
        expect(Math.abs(menu.left - btn.right)).to.be.lessThan(400)
        expect(menu.top).to.be.lessThan(btn.bottom + 420)
      })
    })
  })

  it('390px mobilde kart listesi taşmadan görünür', () => {
    cy.viewport(390, 844)
    cy.visit('/faturalar')
    cy.wait('@faturalar')
    cy.get('.fatura-kart').should('have.length.at.least', 1)
    cy.get('.fatura-kart').first().find('[data-test="fatura-kart-goruntule"]').should('be.visible')
    cy.get('.fatura-kart').first().find('.fatura-kart-tutar').should('be.visible')
    // Uygulama html uzerinde CSS zoom kullaniyor; tasma kontrolu zoom'lu
    // gorunur genislige gore yapilmalidir.
    cy.window().then((win) => {
      const doc = win.document.documentElement
      const zoom = Number.parseFloat(getComputedStyle(doc).zoom) || 1
      const gorunurGenislik = win.innerWidth / zoom
      expect(doc.scrollWidth).to.be.at.most(Math.ceil(gorunurGenislik) + 1)
    })
  })
})
