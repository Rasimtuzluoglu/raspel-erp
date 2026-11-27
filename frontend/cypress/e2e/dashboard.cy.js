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
      gunluk: [
        { tarih: '2026-10-28', kumulatif: 120000 },
        { tarih: '2026-10-29', kumulatif: 140000 }
      ]
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

const userOturumVerisi = {
  ...oturumVerisi,
  kullanici: {
    ...oturumVerisi.kullanici,
    role: 'USER'
  },
  yetkiler: []
}

const apiMockla = (rol = 'ADMIN', veri = kokpitVerisi) => {
  cy.intercept('GET', '**/api/**', { statusCode: 200, body: [] }).as('catchAll')
  cy.intercept('POST', '**/api/**', { statusCode: 200, body: {} }).as('catchAllPost')
  cy.intercept('PUT', '**/api/**', { statusCode: 200, body: {} }).as('catchAllPut')

  cy.intercept('GET', '**/api/kullanicilar/ben', {
    statusCode: 200,
    body: {
      id: 1,
      username: 'admin',
      displayName: 'Test Yönetici',
      role: rol,
      sirketId: 1,
      companyName: 'Test Şirketi',
      sahaKullanici: false
    }
  }).as('ben')

  cy.intercept('GET', '**/api/dashboard/kokpit*', (req) => {
    req.reply({ statusCode: 200, body: veri })
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

const anasayfaAc = (oturum = oturumVerisi, veri = kokpitVerisi) => {
  apiMockla(oturum?.kullanici?.role || 'ADMIN', veri)
  cy.visit('/', {
    onBeforeLoad(win) {
      win.localStorage.setItem(AUTH_ANAHTAR, JSON.stringify(oturum))
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

/** Tek sayıda kalan detay kartı iki kolonu kaplamalı (yarım satır boşluğu yok). */
const sonDetayKartiTamGenislik = (anahtar) => {
  cy.get('[data-test="kokpit-bolumler"]').then(($grid) => {
    const genislik = $grid[0].getBoundingClientRect().width
    cy.get(`[data-test="kokpit-bolum-${anahtar}"]`).should(($kart) => {
      expect($kart[0].getBoundingClientRect().width, 'son detay kartı genişliği')
        .to.be.closeTo(genislik, 2)
    })
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
  for (const secici of seciciler) {
    // Sinif adi degisirse denetim sessizce bos gecmesin: her secici en az bir oge bulmali.
    cy.get(secici)
      .should('have.length.at.least', 1)
      .each(($el) => {
        const kutu = $el[0].getBoundingClientRect()
        if (kutu.width <= 0 || kutu.height <= 0) return
        const genislik = $el[0].ownerDocument.documentElement.clientWidth
        const ad = `${$el[0].tagName.toLowerCase()}.${String($el[0].className).split(' ')[0]}`
        expect(kutu.left, `${ad} sol kenar`).to.be.at.least(-0.5)
        expect(kutu.right, `${ad} sağ kenar`).to.be.at.most(genislik + 0.5)
      })
  }
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
      .and('have.css', 'overflow', 'hidden')
      .and(($el) => {
        const stil = window.getComputedStyle($el[0])
        const kirpilmis = (stil.clip && stil.clip !== 'auto') ||
          (stil.clipPath && stil.clipPath !== 'none')
        expect(kirpilmis, 'canlı duyuru kırpma kuralı').to.eq(true)
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
    cy.get('[data-test="kokpit-bolumler"] > *').should('have.length', 3)
    sonDetayKartiTamGenislik('faturaSiparis')

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

  it('kokpit-hizli-islemler-user: onay gizlenir, son aksiyon iki kolon kaplar', () => {
    cy.viewport(1440, 900)
    anasayfaAc(userOturumVerisi, { ...kokpitVerisi, karMarj: null })

    cy.get('[data-test="hizli-onaylar"]').should('not.exist')
    cy.get('[data-test="kokpit-hizli-islemler"] .hizli-islem').should('have.length', 4)
    cy.get('[data-test="hizli-masraf"]').should(($el) => {
      const stil = window.getComputedStyle($el[0])
      expect(stil.gridColumnStart, 'grid-column-start').to.eq('span 2')
      const kart = $el[0].getBoundingClientRect()
      const liste = $el[0].closest('.hizli-liste').getBoundingClientRect()
      expect(kart.width, 'iki kolon genişliği').to.be.greaterThan(liste.width * 0.9)
    })
    // Yetkisiz rolde kar/marj gelmez: 3 kart kalir, son kart yarim satir boslugu birakmaz.
    cy.get('[data-test="kokpit-bolum-karMarj"]').should('not.exist')
    cy.get('[data-test="kokpit-bolumler"] > *').should('have.length', 3)
    sonDetayKartiTamGenislik('faturaSiparis')
  })

  it('kokpit-820px: ust bar statik, ikon hedefleri 44px, sabit bar cakismasi yok', () => {
    cy.viewport(820, 1180)
    anasayfaAc()

    cy.get('[data-test="kokpit-global-filtre"]').should('have.css', 'position', 'static')
    // Elle yazilan hedef 44 px; html { zoom: 0.9 } altinda hesaplanan deger
    // 1/64 px yuvarlamasiyla ~43.99'a duser (fiziksel ~39.6 px, proje geneli not).
    cy.get('[data-test="kokpit-global-filtre"] .kokpit-ikon-buton')
      .should('have.length.at.least', 2)
      .each(($el) => {
        const stil = window.getComputedStyle($el[0])
        expect(parseFloat(stil.width), 'ikon hedefi genişliği').to.be.at.least(43.9)
        expect(parseFloat(stil.height), 'ikon hedefi yüksekliği').to.be.at.least(43.9)
      })
    // 900 px altinda kabuk sabit ust bara gecer; sticky baslik barin altina kaymamali.
    cy.get('.sidebar').should('be.visible').then(($sidebar) => {
      cy.get('[data-test="kokpit-global-filtre"]').then(($filtre) => {
        const bar = $sidebar[0].getBoundingClientRect()
        const filtre = $filtre[0].getBoundingClientRect()
        expect(filtre.top, 'ust bar ile çakışma').to.be.at.least(bar.bottom - 0.5)
      })
    })
    yatayTasmaYok()
  })

  it('kokpit-kpi-kirilim: 1152 iki kolon, 1264 dort kolon', () => {
    const izgaraKolonlari = () => cy.get('[data-test="finans-ust"]').then(($el) => (
      window.getComputedStyle($el[0]).gridTemplateColumns.split(' ').filter(Boolean)
    ))

    cy.viewport(1152, 900)
    anasayfaAc()
    izgaraKolonlari().should('have.length', 2)

    cy.viewport(1264, 900)
    anasayfaAc()
    izgaraKolonlari().should('have.length', 4)
  })

  it('kokpit-sticky-davranis: 1440 ve 1024 kaydirinca ust bar yapisir', () => {
    for (const genislik of [1440, 1024]) {
      cy.viewport(genislik, 900)
      anasayfaAc()
      cy.scrollTo(0, 400)
      cy.window().its('scrollY').should('be.greaterThan', 0)
      cy.get('[data-test="kokpit-global-filtre"]').should(($el) => {
        const top = $el[0].getBoundingClientRect().top
        expect(top, `${genislik}px sticky top`).to.be.within(-1, 1)
      })
    }
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

  it('kokpit-genis-ekran: 1920, 2560 ve 3840ta icerik genisler, tasma olmaz', () => {
    const beklenen = [
      { genislik: 1920, enAz: 1650, enCok: 1750 },
      { genislik: 2560, enAz: 2100, enCok: 2250 },
      // 4K: 2800 CSS tavan -> ~2520 fiziksel (zoom 0.9)
      { genislik: 3840, enAz: 2450, enCok: 2600 }
    ]
    for (const { genislik, enAz, enCok } of beklenen) {
      cy.viewport(genislik, 1080)
      anasayfaAc()
      yatayTasmaYok()
      cy.get('.kokpit-sayfa').then(($el) => {
        const icerikGenisligi = $el[0].getBoundingClientRect().width
        expect(icerikGenisligi, `${genislik}px içerik genişliği`).to.be.within(enAz, enCok)
      })
      if (genislik >= 2300) {
        // ≥2300: akış iki iç kolona bölünür, hero sağ rayı 540px'te kapanır.
        cy.get('.akis-gruplar').should('have.css', 'column-count', '2')
        cy.get('.kokpit-hero-sag').should(($el) => {
          const stil = $el[0].ownerDocument.defaultView.getComputedStyle($el[0])
          expect(parseFloat(stil.width), 'hero sağ kolon genişliği').to.be.at.most(540.5)
        })
      }
    }
  })

  it('kokpit-nakit-serit: 1440x900 ilk ekranda projeksiyon seridi', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-nakit-serit"]')
      .should('be.visible')
      .then(($el) => {
        const kutu = $el[0].getBoundingClientRect()
        expect(kutu.bottom, 'nakit seridi fold icinde').to.be.at.most(900)
      })
    cy.get('[data-test="kokpit-nakit-serit-grafik"]').should('exist')
    cy.get('[data-test="kokpit-nakit-serit-detay"]')
      .should('be.visible')
      .and('have.attr', 'href', '/kasa')
  })

  it('kokpit-excel: dışa aktarma isteği gönderilir', () => {
    cy.viewport(1440, 900)
    anasayfaAc()

    cy.get('[data-test="kokpit-menu"]').click()
    cy.get('[data-test="kokpit-excel"]').click()
    cy.wait('@excel')
  })
})
