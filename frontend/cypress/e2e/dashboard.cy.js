/**
 * Anasayfa finans kokpiti E2E — gerçek backend GEREKMEZ.
 *
 * Oturum localStorage'dan kurulur; tüm `/api/**` çağrıları `cy.intercept` ile
 * mock'lanır. Testler deploy edilmiş uygulamada (:80) da çalışır.
 */

const AUTH_ANAHTAR = 'raspel_erp_auth'
const OTURUM_POLITIKA_ANAHTAR = 'raspel_erp_session_politika_v2'

const oturumVerisi = {
  kullanici: {
    id: 1,
    username: 'admin',
    displayName: 'Test Yönetici',
    role: 'ADMIN',
    sahaKullanici: false
  },
  companyName: 'Test Şirketi',
  sirketId: 1,
  sirketAdi: 'Test Şirketi',
  yetkiler: ['*'],
  tokenExpiresAt: Date.now() + 60 * 60 * 1000
}

const kokpitVerisi = {
  donem: { kod: 'BU_AY', etiket: 'Bu Ay' },
  ozet: [
    { anahtar: 'netNakit', etiket: 'Net Nakit', deger: 120000, degisimYuzde: 4.2, sparkline: [1, 2, 3], kritik: false, hedef: { route: '/kasa', query: {} } },
    { anahtar: 'alacak', etiket: 'Alacak', deger: 90000, kritik: false, hedef: { route: '/cari-hesaplar', query: { bakiyeYonu: 'alacak' } } },
    { anahtar: 'verecek', etiket: 'Verecek', deger: 40000, kritik: false, hedef: { route: '/cari-hesaplar', query: { bakiyeYonu: 'borc' } } },
    { anahtar: 'netCalismaSermayesi', etiket: 'Net Çalışma Sermayesi', deger: 170000, kritik: false, hedef: { route: '/cari-hesaplar', query: {} } },
    { anahtar: 'ciro', etiket: 'Dönem Cirosu', deger: 300000, degisimYuzde: 12, sparkline: [4, 5, 6], kritik: false, hedef: { route: '/faturalar', query: {} } },
    { anahtar: 'brutKar', etiket: 'Dönem Kârı', deger: 80000, degisimYuzde: 8, sparkline: [7, 8, 9], kritik: false, hedef: { route: '/raporlar', query: {} } },
    { anahtar: 'marj', etiket: 'Kâr Marjı', deger: 26.6, kritik: false, hedef: { route: '/raporlar', query: {} } },
    { anahtar: 'vadesiGecen', etiket: 'Vadesi Geçen', deger: 15000, kritik: true, hedef: { route: '/tahsilat', query: {} } }
  ],
  kritikDurum: [
    { anahtar: 'vadesiGecen', etiket: 'Vadesi Geçen Fatura', seviye: 'KRITIK', tutar: 15000, adet: 3, hedef: { route: '/tahsilat', query: {} } },
    { anahtar: 'kritikStok', etiket: 'Kritik Stok', seviye: 'WARN', tutar: null, adet: 5, hedef: { route: '/stoklar', query: { kritik: '1' } } },
    { anahtar: 'likiditeTrendi', etiket: 'Likidite Trendi', seviye: 'BILGI', tutar: 4.2, adet: null, hedef: { route: '/kasa', query: {} } }
  ],
  nakit: {
    kasa: 20000,
    banka: 100000,
    toplam: 120000,
    trendYuzde: 4.2,
    sparkline: [90000, 100000, 120000],
    gunlukAkis: [
      { tarih: '2026-10-28', giris: 1000, cikis: 500, net: 500 },
      { tarih: '2026-10-29', giris: 1200, cikis: 700, net: 500 }
    ],
    projeksiyon: {
      baslangicBakiyesi: 120000,
      toplamBeklenenGiris: 5000,
      toplamBeklenenCikis: 3000,
      tahminiBitisBakiyesi: 140000,
      gunSayisi: 30,
      gunluk: [{ kumulatif: 120000 }, { kumulatif: 140000 }]
    },
    hedef: { route: '/kasa', query: {} }
  },
  alacakVerecek: {
    alacakToplam: 60000,
    verecekToplam: 20000,
    net: 40000,
    tahsilatOrani: 70,
    yaslanma: [
      { anahtar: 'VADEDI_GELMEMIS', etiket: 'Vadesi Gelmemiş', tutar: 25000 },
      { anahtar: 'GUN_0_30', etiket: '0-30 Gün', tutar: 15000 },
      { anahtar: 'GUN_31_60', etiket: '31-60 Gün', tutar: 10000 },
      { anahtar: 'GUN_61_90', etiket: '61-90 Gün', tutar: 6000 },
      { anahtar: 'GUN_90_PLUS', etiket: '90+ Gün', tutar: 4000 }
    ],
    borclular: [{ cariId: 1, ad: 'Müşteri A', tutar: 5000 }],
    tedarikciler: [{ cariId: 2, ad: 'Tedarikçi A', tutar: 3000 }],
    hedef: { route: '/cari-hesaplar', query: {} }
  },
  karMarj: {
    ciro: 100000,
    maliyet: 70000,
    kar: 30000,
    marj: 30,
    maliyetsizKalem: 3,
    trend: [
      { ay: '2026-10', ciro: 90000, maliyet: 65000, kar: 25000, marj: 27.8 },
      { ay: '2026-11', ciro: 100000, maliyet: 70000, kar: 30000, marj: 30 }
    ],
    urunler: [{ id: 1, ad: 'Ürün A', ciro: 10000, maliyet: 6000, kar: 4000, marj: 40 }],
    cariler: [{ id: 2, ad: 'Cari A', ciro: 12000, maliyet: 7000, kar: 5000, marj: 41.7 }],
    hedef: { route: '/raporlar', query: {} }
  },
  faturaSiparis: {
    durumlar: [
      { anahtar: 'KESILDI', etiket: 'Kesildi', adet: 10, tutar: 10000 },
      { anahtar: 'VADESI_GECEN', etiket: 'Vadesi Geçen', adet: 3, tutar: 1500 }
    ],
    vadesiGecenTutar: 1500,
    vadesiGecenAdet: 3,
    vadesiGecenFaturalar: [
      { faturaId: 1, faturaNumarasi: 'FTR-1', cariAd: 'Müşteri A', vadeTarihi: '2026-10-01', kalanTutar: 500, gecikmeGun: 10 }
    ],
    tahsilatOrani: 70,
    siparisDurumlari: [
      { anahtar: 'TEKLIF', etiket: 'Teklif', adet: 4, tutar: null },
      { anahtar: 'TESLIM_EDILDI', etiket: 'Teslim edildi', adet: 2, tutar: null }
    ],
    hedef: { route: '/tahsilat', query: {} }
  }
}

const apiMockla = () => {
  cy.intercept('GET', '**/api/**', { statusCode: 200, body: [] }).as('catchAll')
  cy.intercept('POST', '**/api/**', { statusCode: 200, body: {} }).as('catchAllPost')
  cy.intercept('PUT', '**/api/**', { statusCode: 200, body: {} }).as('catchAllPut')

  cy.intercept('GET', '**/api/kullanicilar/ben', {
    statusCode: 200,
    body: {
      id: 1,
      username: 'admin',
      displayName: 'Test Yönetici',
      role: 'ADMIN',
      sirketId: 1,
      companyName: 'Test Şirketi',
      sahaKullanici: false
    }
  }).as('ben')

  cy.intercept('GET', '**/api/dashboard/kokpit*', (req) => {
    req.reply({ statusCode: 200, body: kokpitVerisi })
  }).as('kokpit')

  cy.intercept('GET', '**/api/dashboard/tercihler*', {
    statusCode: 200,
    body: { duzen: null }
  }).as('tercihler')

  const akisVerisi = Array.from({ length: 12 }, (_, i) => {
    const turler = ['TAHSILAT', 'FATURA', 'ODEME', 'SIPARIS']
    const tur = turler[i % turler.length]
    return {
      tur,
      baslik: `${tur} kaydı ${i + 1}`,
      aciklama: i % 3 === 0 ? 'Açıklama' : null,
      tutar: 1000 + i * 100,
      tarih: new Date(Date.now() - i * 600000).toISOString(),
      route: '/faturalar',
      ikon: 'pi pi-file',
      renk: '#3b82f6'
    }
  })
  cy.intercept('GET', '**/api/dashboard/akis*', { statusCode: 200, body: akisVerisi }).as('akis')

  cy.intercept('GET', '**/api/dashboard/gorevler*', {
    statusCode: 200,
    body: [
      { tur: 'ONAY_IZIN', baslik: 'İzin Onayı', adet: 2, tutar: null, oncelik: 90, route: '/onaylar?tur=IZIN', ikon: 'pi pi-calendar', renk: '#3b82f6' },
      { tur: 'ONAY_MASRAF', baslik: 'Masraf Onayı', adet: 1, tutar: 2500, oncelik: 85, route: '/onaylar?tur=MASRAF', ikon: 'pi pi-wallet', renk: '#f59e0b' },
      { tur: 'VADESI_GECEN', baslik: 'Vadesi Geçen', adet: 3, tutar: 15000, oncelik: 95, route: '/tahsilat', ikon: 'pi pi-exclamation-triangle', renk: '#ef4444' },
      { tur: 'KRITIK_STOK', baslik: 'Kritik Stok', adet: 5, tutar: null, oncelik: 82, route: '/stoklar?kritik=1', ikon: 'pi pi-box', renk: '#8b5cf6' }
    ]
  }).as('gorevler')

  cy.intercept('PUT', '**/api/dashboard/tercihler*', (req) => {
    req.reply({ statusCode: 200, body: { duzen: req.body?.duzen || null } })
  }).as('tercihKaydet')

  cy.intercept('GET', '**/api/exports/dashboard*', {
    statusCode: 200,
    headers: { 'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
    body: 'xlsx'
  }).as('excel')
}

const anasayfaAc = () => {
  apiMockla()
  cy.visit('/', {
    onBeforeLoad(win) {
      win.localStorage.setItem(AUTH_ANAHTAR, JSON.stringify(oturumVerisi))
      win.localStorage.setItem(OTURUM_POLITIKA_ANAHTAR, '1')
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
      win.localStorage.setItem('raspel_gosterilen_ipuclari', '[]')
    }
  })
  cy.url().should('not.include', '/giris')
  cy.wait('@kokpit', { timeout: 20000 })
  cy.wait('@akis', { timeout: 20000 })
  cy.wait('@gorevler', { timeout: 20000 })
  cy.get('[data-test="finans-ozet-bandi"]', { timeout: 20000 }).should('be.visible')
}

const yatayTasmaYok = () => {
  cy.document().then((doc) => {
    const kok = doc.documentElement
    expect(kok.scrollWidth, 'yatay taşma').to.be.at.most(kok.clientWidth + 1)
  })
}

/**
 * 390 px yatay taşma denetimi.
 * Atalardaki `overflow-x` kırpması `documentElement.scrollWidth` değerini
 * viewport genişliğine sabitlediği için görünür düzen kaplarının kutuları ayrıca
 * viewport ile karşılaştırılır. İç kaydırması bilinçli olan `.kokpit-donem` ve
 * `.akis-filtreler` yalnız dış kutusuyla denetlenir; çocukları listeye alınmaz.
 */
const yatayTasmaDenetimi = () => {
  const seciciler = [
    '.kokpit-sayfa',
    '.kokpit-filtre',
    '.kokpit-hero',
    '.kokpit-orta',
    '.kokpit-bolumler',
    '.kokpit-bolum',
    '.finans-ozet-bandi',
    '.kokpit-hizli-islemler',
    '.kokpit-akis',
    '.kokpit-bekleyen',
    '.fin-kart'
  ]
  cy.window().then((win) => {
    const genislik = win.document.documentElement.clientWidth
    cy.get(seciciler.join(', ')).each(($el) => {
      const kutu = $el[0].getBoundingClientRect()
      if (kutu.width <= 0 || kutu.height <= 0) return
      const ad = `${$el[0].tagName.toLowerCase()}.${String($el[0].className).split(' ')[0]}`
      expect(kutu.left, `${ad} sol kenar`).to.be.at.least(-0.5)
      expect(kutu.right, `${ad} sağ kenar`).to.be.at.most(genislik + 0.5)
    })
  })
}

describe('Anasayfa finans kokpiti', () => {
  it('kokpit-ilk-yukleme: oncelikli metrikler, akis ve bekleyen isler gorunur', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.contains('h1', 'Finans Masası').should('be.visible')
    cy.get('[data-test="kokpit-global-filtre"]').should('have.css', 'position', 'sticky')
    // Öncelikli 4 KPI büyük kart; kalan 4 metrik kompakt çip.
    cy.get('[data-test="finans-ozet-bandi"] .finans-ust [data-test^="kpi-"]').should('have.length', 4)
    cy.get('[data-test="finans-ikincil"] [data-test^="kpi-alt-"]').should('have.length', 4)
    cy.get('[data-test="kritik-durum-seridi"]').should('be.visible')

    // İlk ekran öncelikleri: hızlı işlemler, son işlemler, bekleyen işler.
    cy.get('[data-test="kokpit-hizli-islemler"]').should('be.visible')
    cy.get('[data-test="kokpit-akis"]').should('be.visible')
    cy.contains('[data-test="kokpit-akis"]', 'TAHSILAT kaydı 1').should('be.visible')
    cy.get('[data-test="kokpit-akis"] [data-test^="akis-oge-"]').should('have.length', 8)
    cy.get('[data-test="akis-tumunu-gor"]').should('contain', '12')
    cy.get('[data-test="kokpit-bekleyen-isler"]').should('be.visible')
    cy.get('[data-test="bekleyen-oge-ONAY_IZIN"]').should('be.visible')
    cy.get('[data-test="bekleyen-oge-VADESI_GECEN"]').should('be.visible')

    cy.get('[data-test="kokpit-bolum-nakit"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-alacakVerecek"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-karMarj"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-faturaSiparis"]').should('be.visible')
    // Nakit takvimi varsayılan kapalı (ikincil bölge).
    cy.get('[data-test="nakit-takvimi"]').should('not.exist')
    yatayTasmaYok()
    // Canlı duyuru alanı görsel olarak gizli olmalı; yoksa "Son güncelleme" iki kez görünür.
    // Not: Cypress 1×1 kırpılmış (.sr-only) öğeleri de "görünür" saydığı için
    // görünürlük yerine gerçek kutu ölçüsü ve kırpma stili doğrulanır.
    cy.get('[data-test="dashboard-kokpit"] [role="status"]')
      .should('have.class', 'sr-only')
      .and('have.css', 'position', 'absolute')
      .and(($el) => {
        const kutu = $el[0].getBoundingClientRect()
        expect(kutu.width, 'canlı duyuru genişliği').to.be.at.most(1)
        expect(kutu.height, 'canlı duyuru yüksekliği').to.be.at.most(1)
      })
  })

  it('kokpit-akis: satira tiklayinca ilgili ekrana gider', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="akis-oge-0"]').click()
    cy.url().should('include', '/faturalar')
  })

  it('kokpit-akis: tumunu gor/ac ve tur filtresi', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="akis-tumunu-gor"]').click()
    cy.get('[data-test="kokpit-akis"] [data-test^="akis-oge-"]').should('have.length', 12)
    cy.get('[data-test="akis-tumunu-gor"]').click()
    cy.get('[data-test="kokpit-akis"] [data-test^="akis-oge-"]').should('have.length', 8)

    cy.get('[data-test="akis-filtre-FATURA"]').click()
    cy.get('[data-test="kokpit-akis"] [data-test^="akis-oge-"]').should('have.length', 3)
    cy.get('[data-test="kokpit-akis"]').should('contain', 'FATURA kaydı')
  })

  it('kokpit-bekleyen: gorev satiri sorguyla yonlendirir', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="bekleyen-oge-ONAY_IZIN"]').click()
    cy.url().should('include', '/onaylar')
    cy.url().should('include', 'tur=IZIN')
  })

  it('kokpit-yogunluk: kompakt/ferah gecisi kalici', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="dashboard-kokpit"]').should('have.class', 'kokpit-sayfa--kompakt')
    cy.get('[data-test="kokpit-yogunluk"]').click()
    cy.get('[data-test="dashboard-kokpit"]').should('have.class', 'kokpit-sayfa--ferah')
    cy.window().then((win) => {
      expect(win.localStorage.getItem('raspel_dashboard_yogunluk')).to.eq('ferah')
    })
  })

  it('kokpit-takvim: varsayilan kapali, acilinca takvim gorunur', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-takvim-ac"]').scrollIntoView().click()
    cy.get('[data-test="nakit-takvimi"]').should('be.visible')
    cy.get('[data-test="kokpit-takvim-ac"]').should('have.attr', 'aria-expanded', 'true')
  })

  it('kokpit-donem-filtresi: dönem değişince yeni istek gider', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="donem-GECEN_AY"]').click()
    cy.wait('@kokpit').then(({ request }) => {
      expect(request.query.donem).to.eq('GECEN_AY')
    })
  })

  it('kokpit-kritik-serit-detay: kritik öğe tahsilat diyaloğunu açar', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kritik-oge-vadesiGecen"]').click()
    cy.get('[data-test="hizli-tahsilat-katman"]').should('be.visible')
    cy.get('[data-test="hizli-tahsilat-kapat"]').click()
  })

  it('kokpit-ozellestirme: bölüm gizlenir ve düzen kaydedilir', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-menu"]').click()
    cy.get('[data-test="kokpit-ozellestir"]').click()
    cy.get('[data-test="kokpit-ozellestirme"]').should('be.visible')

    cy.get('[data-test="ozellestirme-gorunurluk-karMarj"]').uncheck({ force: true })
    cy.get('[data-test="kokpit-bolum-karMarj"]').should('not.exist')

    cy.get('[data-test="ozellestirme-kaydet"]').click()
    cy.wait('@tercihKaydet').then(({ request }) => {
      expect(request.body.duzen.gorunurBolumler).not.to.include('karMarj')
    })
    cy.get('[data-test="kokpit-ozellestirme"]').should('not.exist')
  })

  it('kokpit-canli-yenileme: yenile düğmesi kokpiti tazeler', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-yenile"]').click()
    cy.wait('@kokpit')
    cy.get('[data-test="finans-ozet-bandi"]').should('be.visible')
  })

  it('kokpit-hizli-islemler: tahsilat ve onay diyalogları açılır', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="hizli-tahsilat"]').click()
    cy.get('[data-test="hizli-tahsilat-katman"]').should('be.visible')
    cy.get('[data-test="hizli-tahsilat-kapat"]').click()
    cy.get('[data-test="hizli-tahsilat-katman"]').should('not.exist')

    cy.get('[data-test="hizli-onaylar"]').click()
    cy.get('[data-test="kokpit-onay-katman"]').should('be.visible')
    cy.get('[data-test="kokpit-onay-kapat"]').click()
    cy.get('[data-test="kokpit-onay-katman"]').should('not.exist')
  })

  it('kokpit-390px: yatay tasma yok, mobil sikistirma uygulanir', () => {
    cy.viewport(390, 844)
    anasayfaAc()

    yatayTasmaYok()
    cy.get('[data-test="kokpit-bolumler"]').should(($grid) => {
      const kolonlar = window.getComputedStyle($grid[0]).gridTemplateColumns.split(' ').filter(Boolean)
      expect(kolonlar.length).to.eq(1)
    })
    // Mobil: akış 5 kayıt, bekleyen işler 3 kayıt + "tümünü gör".
    cy.get('[data-test="kokpit-akis"] [data-test^="akis-oge-"]').should('have.length', 5)
    cy.get('[data-test="kokpit-bekleyen-isler"] [data-test^="bekleyen-oge-"]').should('have.length', 3)
    cy.get('[data-test="bekleyen-tumunu-gor"]').should('be.visible')
    // Hızlı işlemler mobilde 2 kolon.
    cy.get('[data-test="kokpit-hizli-islemler"] .hizli-liste').should(($liste) => {
      const kolonlar = window.getComputedStyle($liste[0]).gridTemplateColumns.split(' ').filter(Boolean)
      expect(kolonlar.length).to.eq(2)
    })
    yatayTasmaYok()
  })

  it('390px viewportta yatay tasma olmaz', () => {
    cy.viewport(390, 844)
    anasayfaAc()
    cy.get('[data-test="dashboard-kokpit"]', { timeout: 20000 }).should('exist')
    cy.document().then((doc) => {
      expect(doc.documentElement.scrollWidth).to.be.at.most(390)
    })
    yatayTasmaDenetimi()
    cy.get('[data-test="kokpit-global-filtre"]').should('have.css', 'position', 'static')
  })

  it('kokpit-excel: dışa aktarma isteği gönderilir', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-menu"]').click()
    cy.get('[data-test="kokpit-excel"]').click()
    cy.wait('@excel')
  })
})
