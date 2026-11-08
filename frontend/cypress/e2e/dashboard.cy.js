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
  cy.get('[data-test="finans-ozet-bandi"]', { timeout: 20000 }).should('be.visible')
}

const yatayTasmaYok = () => {
  cy.document().then((doc) => {
    const kok = doc.documentElement
    expect(kok.scrollWidth, 'yatay taşma').to.be.at.most(kok.clientWidth + 1)
  })
}

describe('Anasayfa finans kokpiti', () => {
  it('kokpit-ilk-yukleme: özet, kritik şerit ve dört bölüm görünür', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.contains('h1', 'Finans Masası').should('be.visible')
    cy.get('[data-test="finans-ozet-bandi"] [data-test^="kpi-"]').should('have.length', 8)
    cy.get('[data-test="kritik-durum-seridi"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-nakit"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-alacakVerecek"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-karMarj"]').should('be.visible')
    cy.get('[data-test="kokpit-bolum-faturaSiparis"]').should('be.visible')
    cy.get('[data-test="nakit-bolumu"]').should('be.visible')
    cy.get('[data-test="nakit-takvimi"]').should('be.visible')
    yatayTasmaYok()
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

  it('kokpit-390px: yatay taşma yok, bölümler tek kolon', () => {
    cy.viewport(390, 844)
    anasayfaAc()

    yatayTasmaYok()
    cy.get('[data-test="kokpit-bolumler"]').should(($grid) => {
      const kolonlar = window.getComputedStyle($grid[0]).gridTemplateColumns.split(' ').filter(Boolean)
      expect(kolonlar.length).to.eq(1)
    })
    cy.get('[data-test="vadesi-gecen-tablo"] tbody tr').first().should('be.visible')
    yatayTasmaYok()
  })

  it('kokpit-excel: dışa aktarma isteği gönderilir', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-excel"]').click()
    cy.wait('@excel')
  })
})
