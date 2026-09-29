describe('Hızlı Satış (POS)', () => {
  const urun = {
    id: 1,
    ad: 'Test Ürün',
    stokKodu: 'STK-1',
    barkod: '1234567890',
    fiyat: 100,
    satisFiyati: 100,
    miktar: 50,
    birim: 'Adet',
    kategori: 'Gıda',
    marka: 'Güneş',
    stokGrubu: 'Mamul'
  }

  const stoksuzUrun = {
    id: 2,
    ad: 'Zzz Stoksuz Ürün',
    stokKodu: 'STK-2',
    barkod: '2222222222',
    fiyat: 50,
    satisFiyati: 50,
    miktar: 0,
    birim: 'Adet',
    kategori: 'Ambalaj',
    marka: 'Marmara',
    stokGrubu: 'Aksesuar'
  }

  // Barkodu olmayan urun: etikette stok kodu kodlanir, tarama stok koduyla bulmali.
  const kodluUrun = {
    id: 3,
    ad: 'Kodlu Ürün',
    stokKodu: 'MDF-18',
    barkod: '',
    fiyat: 75,
    satisFiyati: 75,
    miktar: 10,
    birim: 'Adet',
    kategori: 'Yapı',
    marka: 'VidaSan',
    stokGrubu: 'Hammadde'
  }

  const tusGonder = (key, ek = {}) => {
    cy.window().then((win) => {
      win.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, ...ek }))
    })
  }

  beforeEach(() => {
    cy.girisYap()
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: { content: [urun, stoksuzUrun, kodluUrun] } }).as('stoklar')
    cy.intercept('POST', '/api/faturalar', {
      statusCode: 200,
      body: { id: 99, faturaNumarasi: 'FTR-TEST-0001' }
    }).as('satisOlustur')
    // Sofor listesi + teslimat kaydi (POS'tan sofor atama akisi)
    cy.intercept('GET', '/api/drivers', {
      statusCode: 200,
      body: [{ id: 5, ad: 'Ali Şoför', rol: 'DRIVER', bekleyenTeslimatSayisi: 0 }]
    }).as('suruculer')
    cy.intercept('POST', '/api/deliveries', { statusCode: 201, body: { id: 1 } }).as('teslimatOlustur')
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
    // Bölüm varsayılanları: müşteri, sepet ve ödeme AÇIK gelir (kasiyer her şeyi görür).
    cy.contains('.pos-bolum-baslik', 'Müşteri').should('have.attr', 'aria-expanded', 'true')
    cy.get('.sepet-baslik .katlanir-ikon-btn').should('have.attr', 'aria-expanded', 'true')
    cy.contains('.pos-bolum-baslik', 'Ödeme').should('have.attr', 'aria-expanded', 'true')
  })

  it('kısayol ipucu şeridi açılıp kapatılabilir', () => {
    // Şerit varsayılan olarak gizlidir; "?" ile açılır, çarpı ile kapanır.
    cy.get('.pos-ipucu').should('not.exist')
    cy.get('.pos-ipucu-btn').click()
    cy.get('.pos-ipucu').should('be.visible')
    cy.get('.pos-ipucu-kapat').click()
    cy.get('.pos-ipucu').should('not.exist')
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

  it('müşteri modu seçiliyken müşteri yoksa uyarı gösterir (F9)', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    // Varsayılan Perakende; uyarı senaryosu için Müşteri moduna geç.
    cy.get('.musteri-modu').contains('Müşteri').click()
    tusGonder('F9')
    cy.contains('Müşteri gerekli').should('exist')
    cy.get('@satisOlustur.all').should('have.length', 0)
  })

  it('ürün kartları stok adedi, fiyat ve KDV notunu gösterir', () => {
    cy.get('.product-card').should('have.length', 3)
    cy.contains('.product-card', 'Test Ürün').within(() => {
      cy.get('.product-stok-satir').should('be.visible').and('contain', 'Stok: 50 Adet')
      cy.get('.product-price').should('contain', '100')
      cy.get('.kdv-not').should('be.visible')
      cy.get('.product-gorsel').should('exist')
    })
    // Stokta olmayan ürün görsel olarak işaretli ve stok satırı kırmızı olmalı
    cy.contains('.product-card', 'Zzz Stoksuz Ürün').within(() => {
      cy.get('.product-stok-satir').should('contain', 'Stok yok').and('have.class', 'yok')
    })
    cy.contains('.product-card', 'Zzz Stoksuz Ürün').should('have.class', 'stok-yok')
  })

  it('stokta olmayan ürün sepete eklenmez', () => {
    cy.contains('.product-card', 'Zzz Stoksuz Ürün').click()
    cy.get('.sepet-item').should('have.length', 0)
    cy.contains('stok kalmadı').should('exist')
  })

  it('barkodsuz üründe stok kodu ile tarama ürünü bulur', () => {
    // Etiketlerde barkod yerine stok kodu kodlanır; arama stok kodunu da kapsar.
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('MDF-18{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    cy.contains('.sepet-item', 'Kodlu Ürün').should('exist')
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

  it('filtreler ve tercihler popover ile yönetilir', () => {
    // Filtreler tek bir popover'da toplanır (araç çubuğu sade kalır)
    cy.get('.filtre-btn').click()
    cy.get('.filtre-panel').should('be.visible')
    cy.contains('.filtre-panel', 'Kategori').should('exist')
    cy.contains('.filtre-panel', 'Marka').should('exist')
    cy.contains('.filtre-panel', 'Stok Grubu').should('exist')
    cy.get('body').type('{esc}')

    // Tercihler tek menüde: büyük yazı, onay iste, otomatik yazdırma, kısayol ipucu
    cy.get('.pos-tercih-btn').click()
    cy.get('.tercih-panel').should('be.visible')
    cy.get('.tercih-satir').should('have.length', 4)
  })

  it('filtre seçenekleri ürün verisinden türetilir (tanım gerekmez)', () => {
    // Kategori hızlı çipleri ürünlerde yazılı değerlerden gelir.
    cy.contains('.kategori-cipler .kategori-cip', 'Gıda').should('exist')
    cy.contains('.kategori-cipler .kategori-cip', 'Yapı').should('exist')

    // Marka filtresi de ürün alanından türetilir.
    cy.get('.filtre-btn').click()
    cy.get('.filtre-panel .p-select').eq(1).click()
    cy.contains('.p-select-overlay .p-select-option', 'VidaSan').should('exist')
    cy.get('body').type('{esc}')
    cy.get('body').type('{esc}')
  })

  it('şoför seçilmeden satış tamamlanır ve teslimat kaydı açılmaz', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.sepet-item').should('have.length', 1)
    cy.get('.musteri-modu').contains('Perakende').click()
    cy.contains('Satışı Tamamla').click()
    cy.wait('@satisOlustur')
    cy.get('@teslimatOlustur.all').should('have.length', 0)
  })

  it('şoför seçilince teslimat adresi zorunludur', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.musteri-modu').contains('Perakende').click()
    cy.contains('.pos-bolum-baslik', 'Teslimat').click()
    cy.get('#hizli-teslim-sofor').click()
    cy.contains('.p-select-overlay .p-select-option', 'Ali Şoför').click()
    cy.contains('Satışı Tamamla').click()
    cy.contains('teslimat adresi zorunludur').should('exist')
    cy.get('@satisOlustur.all').should('have.length', 0)
  })

  it('şoför ve adres ile satışta teslimat kaydı açılır', () => {
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('1234567890{enter}')
    cy.get('.musteri-modu').contains('Perakende').click()
    cy.contains('.pos-bolum-baslik', 'Teslimat').click()
    cy.get('#hizli-teslim-sofor').click()
    cy.contains('.p-select-overlay .p-select-option', 'Ali Şoför').click()
    cy.get('#hizli-teslim-adres').type('Test Mah. 1. Sok No:5')
    cy.contains('Satışı Tamamla').click()
    cy.wait('@satisOlustur')
    cy.wait('@teslimatOlustur').its('request.body').then((body) => {
      expect(body.faturaId).to.eq(99)
      expect(body.driverId).to.eq(5)
      expect(body.teslimatAdresi).to.contain('Test Mah')
      expect(body.durum).to.eq('BEKLEMEDE')
    })
  })

  it('POS dar ekranlarda yatay taşma yapmaz', () => {
    cy.viewport(1280, 800)
    cy.visit('/hizli-satis')
    cy.window().then((win) => {
      expect(win.document.documentElement.scrollWidth).to.be.at.most(win.document.documentElement.clientWidth + 1)
    })
    cy.viewport(1024, 768)
    cy.window().then((win) => {
      expect(win.document.documentElement.scrollWidth).to.be.at.most(win.document.documentElement.clientWidth + 1)
    })
  })
})
