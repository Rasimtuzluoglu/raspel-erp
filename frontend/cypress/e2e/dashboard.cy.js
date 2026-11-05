/**
 * Anasayfa (dashboard) E2E — gercek backend GEREKMEZ.
 *
 * Oturum localStorage'dan kurulur; tum `/api/**` cagrilari `cy.intercept` ile
 * mock'lanir. Boylece testler deploy edilmis uygulamada (:80) da calisir:
 *
 *   npx cypress run --spec "cypress/e2e/dashboard.cy.js" \
 *     --config baseUrl=http://localhost --browser electron
 */

const AUTH_ANAHTAR = 'raspel_erp_auth'
const OTURUM_POLITIKA_ANAHTAR = 'raspel_erp_session_politika_v2'
const KISAYOL_ANAHTAR = 'raspel_dashboard_kisayollar'

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

const ozetVerisi = {
  hero: { kullaniciAd: 'Test Yönetici', sirketAd: 'Test Şirketi', canli: true },
  bugun: { tahsilat: 15000, odeme: 4000, siparis: 3, teslimat: 2 },
  kpi: {
    likidite: { deger: 120000, kasa: 20000, banka: 100000, sparkline: [1, 2, 3, 4] },
    netCari: { deger: 50000, alacak: 90000, borc: 40000 },
    ciro: { deger: 300000, hedef: 500000, yuzde: 60 },
    kar: { deger: 80000, hedef: 100000, marj: 26.6 }
  },
  stok: { cesit: 42, deger: 250000, kritikAdet: 5 },
  gorevSayilari: {
    onayBekleyen: 2,
    vadesiGecen: 1,
    kritikStok: 3,
    bekleyenTeslimat: 0,
    vadesiYaklasan: 2
  }
}

const gorevVerisi = [
  {
    tur: 'ONAY_SIPARIS',
    baslik: 'Sipariş onayı bekliyor',
    aciklama: '4 sipariş',
    adet: 4,
    tutar: 0,
    oncelik: 80,
    route: '/onaylar?tur=SIPARIS',
    ikon: 'pi pi-shopping-cart',
    renk: '#f59e0b'
  },
  {
    tur: 'ONAY_IZIN',
    baslik: 'İzin onayı bekliyor',
    aciklama: '2 personel',
    adet: 2,
    tutar: 0,
    oncelik: 45,
    route: '/izinler',
    ikon: 'pi pi-calendar',
    renk: '#3b82f6'
  },
  {
    tur: 'VADESI_GECEN',
    baslik: 'Vadesi geçen alacak',
    aciklama: 'Gecikmiş',
    adet: 1,
    tutar: 12500,
    oncelik: 90,
    route: null,
    ikon: 'pi pi-exclamation-triangle',
    renk: '#ef4444'
  }
]

const akisVerisi = [
  {
    tur: 'TAHSILAT',
    baslik: 'Tahsilat alındı',
    aciklama: 'ABC Ltd.',
    tutar: 5000,
    tarih: new Date().toISOString(),
    route: '/hareketler',
    ikon: null,
    renk: null
  },
  {
    tur: 'FATURA',
    baslik: 'Fatura kesildi',
    aciklama: 'FTR-2026-0001',
    tutar: 12000,
    tarih: new Date().toISOString(),
    route: null,
    ikon: null,
    renk: null
  }
]

const grafikVerisi = {
  aylikGelirGider: [
    { ay: '2026-09', gelir: 100, gider: 60 },
    { ay: '2026-10', gelir: 120, gider: 70 }
  ],
  gunlukNakitAkisi: [
    { gun: '2026-10-28', gelir: 1000, gider: 500 },
    { gun: '2026-10-29', gelir: 1200, gider: 700 }
  ],
  enCokSatanlar: [{ stokAd: 'Kalem A', stokKodu: 'KA', satisMiktari: 5 }],
  kategoriSatislari: [{ kategori: 'Kırtasiye', tutar: 900 }],
  alacakYaslandirma: [{ aralik: '0-30', tutar: 500 }],
  enCokBorcCariler: [{ cariAd: 'Borçlu Ltd.', tutar: 700 }],
  enCokAlacakCariler: [{ cariAd: 'Alacaklı A.Ş.', tutar: 900 }],
  kasaBanka: { kasa: 1000, banka: 4000 },
  siparisDurumlari: [
    { durum: 'TEKLIF', adet: 2 },
    { durum: 'SIPARIS', adet: 5 }
  ],
  butceGerceklesen: [{ kategori: 'Pazarlama', butce: 10000, gerceklesen: 7000 }],
  stokDegeri: {
    toplamDeger: 1250000,
    kritikAdet: 7,
    kategoriler: [{ kategori: 'Elektronik', deger: 900000, adet: 4 }]
  }
}

const icgoruVerisi = [
  { seviye: 'KRITIK', baslik: 'Geciken alacak arttı', mesaj: '30 günü aşan alacak var.', route: '/tahsilat', ikon: null },
  { seviye: 'INFO', baslik: 'Nakit akışı', mesaj: 'Bu hafta pozitif.', route: '/kasa', ikon: null }
]

/**
 * Tum API'leri mock'lar. Genel yakalayici EN ONCE tanimlanir; Cypress ayni
 * istege birden fazla intercept uygulandiginda SON tanimlanani secer.
 */
const apiMockla = () => {
  cy.intercept('GET', '**/api/**', { statusCode: 200, body: [] }).as('catchAll')
  cy.intercept('POST', '**/api/**', { statusCode: 200, body: {} }).as('catchAllPost')

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

  cy.intercept('GET', '**/api/dashboard/ozet*', { statusCode: 200, body: ozetVerisi }).as('ozet')
  cy.intercept('GET', '**/api/dashboard/gorevler*', { statusCode: 200, body: gorevVerisi }).as('gorevler')
  cy.intercept('GET', '**/api/dashboard/grafikler*', { statusCode: 200, body: grafikVerisi }).as('grafikler')
  cy.intercept('GET', '**/api/dashboard/akis*', { statusCode: 200, body: akisVerisi }).as('akis')
  cy.intercept('GET', '**/api/dashboard/icgoruler*', { statusCode: 200, body: icgoruVerisi }).as('icgoruler')
  cy.intercept('GET', '**/api/dashboard/tercihler*', { statusCode: 200, body: { duzen: null } }).as('tercihler')
  cy.intercept('GET', '**/api/dashboard/vadeler*', { statusCode: 200, body: [] }).as('vadeler')
  cy.intercept('GET', '**/api/doviz*', { statusCode: 200, body: [] }).as('doviz')
  cy.intercept('GET', '**/api/stoklar/kritik*', { statusCode: 200, body: [] }).as('kritikStok')
  cy.intercept('GET', '**/api/exports/dashboard*', {
    statusCode: 200,
    headers: { 'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
    body: 'xlsx'
  }).as('excel')
}

/** Uygulamayi oturum acmis olarak acar; anasayfa verisi gelene kadar bekler. */
const anasayfaAc = () => {
  apiMockla()
  cy.visit('/', {
    onBeforeLoad(win) {
      win.localStorage.setItem(AUTH_ANAHTAR, JSON.stringify(oturumVerisi))
      win.localStorage.setItem(OTURUM_POLITIKA_ANAHTAR, '1')
      // Acilis popup'larini (surum notlari / ipuclari / onboarding) bastir.
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
      win.localStorage.setItem('raspel_gosterilen_ipuclari', '[]')
      win.localStorage.removeItem(KISAYOL_ANAHTAR)
    }
  })
  cy.url().should('not.include', '/giris')
  cy.get('[data-test="kpi-grid"]', { timeout: 20000 }).should('be.visible')
  cy.get('[data-test="kpi-deger-likidite"]', { timeout: 20000 }).should('be.visible')
}

const yatayTasmaYok = () => {
  cy.document().then((doc) => {
    const kok = doc.documentElement
    expect(kok.scrollWidth, 'yatay taşma').to.be.at.most(kok.clientWidth + 1)
  })
}

describe('Anasayfa (dashboard) — yerleşim, etkileşim ve dışa aktarma', () => {
  it('1280px: 9 KPI, sol 10 / sağ 6 widget, yatay taşma yok', () => {
    cy.viewport(1280, 900)
    anasayfaAc()

    cy.get('[data-test="kpi-grid"] .kpi-kart').should('have.length', 9)
    cy.get('[data-bolge="SOL"] > [data-widget]').should('have.length', 10)
    cy.get('[data-bolge="SAG"] > [data-widget]').should('have.length', 6)

    yatayTasmaYok()
  })

  it('390px: iki kolon KPI, tek kolon gövde, yatay taşma yok', () => {
    cy.viewport(390, 844)
    anasayfaAc()

    cy.get('[data-test="kpi-grid"]').should(($grid) => {
      const kolonlar = window.getComputedStyle($grid[0]).gridTemplateColumns.split(' ').filter(Boolean)
      expect(kolonlar, 'KPI iki kolon (360-699)').to.have.length(2)
    })
    cy.get('[data-test="dashboard-govde"]').should(($govde) => {
      const kolonlar = window.getComputedStyle($govde[0]).gridTemplateColumns.split(' ').filter(Boolean)
      expect(kolonlar, 'gövde tek kolon').to.have.length(1)
    })
    cy.get('.db-sol').should(($sol) => {
      const kolonlar = window.getComputedStyle($sol[0]).gridTemplateColumns.split(' ').filter(Boolean)
      expect(kolonlar, 'grafik duvarı tek kolon').to.have.length(1)
    })

    // Mobil üst bar: dönem seçici tam satır, üç ikon taşmadan sığar.
    cy.get('.ust-sag').should(($sag) => {
      expect($sag[0].scrollWidth, 'üst bar taşması').to.be.at.most($sag[0].clientWidth + 1)
    })
    cy.get('[data-test="donem-secici"]').should('be.visible')
    cy.get('[data-test="mobil-daha"]').should('be.visible')

    yatayTasmaYok()
  })

  it('390px: "Daha fazla" menüsü açılır, satırlar görünür ve Escape kapatır', () => {
    cy.viewport(390, 844)
    anasayfaAc()

    cy.get('[data-test="mobil-daha"]')
      .should('have.attr', 'aria-expanded', 'false')
      .click()
    cy.get('[data-test="mobil-daha"]').should('have.attr', 'aria-expanded', 'true')
    cy.get('[data-test="mobil-menu"]').should('be.visible')

    const satirlar = [
      'notlar',
      'takvim',
      'kar',
      'excel',
      'yazdir',
      'canli',
      'yenile',
      'duzenle'
    ]
    satirlar.forEach((ad) => {
      cy.get(`[data-test="mobil-aksiyon-${ad}"]`).should('be.visible')
    })

    // 44px dokunma hedefi (Cypress AUT'u olceklebilecegi icin hesaplanan
    // yukseklik yerine CSS kurali dogrulanir).
    cy.get('[data-test="mobil-aksiyon-notlar"]').should(($satir) => {
      expect(window.getComputedStyle($satir[0]).minHeight).to.eq('44px')
    })

    cy.get('[data-test="mobil-menu"]').trigger('keydown', { key: 'Escape' })
    cy.get('[data-test="mobil-menu"]').should('not.exist')

    // Masaüstü eylemleri mobilde gizli; emit yolu menüden işler.
    cy.get('[data-test="excel-indir"]').should('not.be.visible')
  })

  it('dönem seçici "Bu Yıl" isteği donem=BU_YIL ile gönderir', () => {
    cy.viewport(1280, 900)
    anasayfaAc()

    // Yeni alias: ilk yukleme istegiyle karismasin, yalniz secim sonrasi istek.
    cy.intercept('GET', '**/api/dashboard/ozet*', { statusCode: 200, body: ozetVerisi }).as('ozetDonem')
    cy.get('[data-test="donem-secici"]').select('BU_YIL')
    cy.wait('@ozetDonem').then(({ request }) => {
      expect(request.query.donem).to.eq('BU_YIL')
    })
  })

  it('bildirim zili açılır ve üç satır gösterir', () => {
    cy.viewport(1280, 900)
    anasayfaAc()

    cy.get('[data-test="bildirim-zili"]').should('have.attr', 'aria-expanded', 'false').click()
    cy.get('[data-test="bildirim-zili"]').should('have.attr', 'aria-expanded', 'true')

    const menuler = ['onayBekleyen', 'vadesiGecen', 'kritikStok']
    cy.get('[data-test="bildirim-menusu"]').should('be.visible')
    cy.get('[data-test="bildirim-menusu"] [role="menuitem"]').should('have.length', 3)
    menuler.forEach((kod) => {
      cy.get(`[data-test="bildirim-${kod}"]`).should('be.visible')
    })
  })

  it('notlar çekmecesi: not eklenir ve listede görünür', () => {
    cy.viewport(1280, 900)
    anasayfaAc()

    cy.get('[data-test="not-buton"]').click()
    cy.get('[data-test="notlar-drawer"]').should('be.visible')

    cy.get('[data-test="not-girdi"]').type('Cypress kontrol notu')
    cy.get('[data-test="not-ekle"]').click()

    cy.get('[data-test="not-liste"]').should('contain.text', 'Cypress kontrol notu')
    cy.get('[data-test="not-girdi"]').should('have.value', '')
  })

  it('kısayol ayarları: 8 seçim sınırı korunur ve kaydet çalışır', () => {
    cy.viewport(1280, 900)
    anasayfaAc()

    cy.get('[data-test="kisayol-duzenle"]').click()
    cy.get('[data-test="kisayol-ayarlari"]').should('be.visible')

    // Varsayilan 8 secim dolu; kalan onay kutulari devre disi.
    cy.get('[data-test="kisayol-ayar-sayac"]').should('contain.text', '8 / 8')
    cy.get('.kisayol-ayar-onay:checked').should('have.length', 8)
    cy.get('.kisayol-ayar-onay:not(:checked)').each(($kutu) => {
      expect($kutu.is(':disabled'), 'sınır dolunca devre dışı').to.eq(true)
    })

    // Bir secimi kaldirip yerine baska bir secim yapilabilir; toplam 8 kalir.
    cy.get('[data-test="kisayol-secim-odeme"]').uncheck()
    cy.get('.kisayol-ayar-onay:checked').should('have.length', 7)
    cy.get('[data-test="kisayol-secim-ajanda"]').check()
    cy.get('.kisayol-ayar-onay:checked').should('have.length', 8)
    cy.get('[data-test="kisayol-ayar-sayac"]').should('contain.text', '8 / 8')

    cy.get('[data-test="kisayol-kaydet"]').click()
    cy.get('[data-test="kisayol-ayarlari"]').should('not.exist')
    cy.get('[data-test="kisayol-seridi"] a.kisayol').should('have.length', 8)

    cy.window().then((win) => {
      const secim = JSON.parse(win.localStorage.getItem(KISAYOL_ANAHTAR))
      expect(secim, 'kaydedilen kısayollar').to.have.length(8)
      expect(secim).to.include('ajanda')
      expect(secim).not.to.include('odeme')
    })
  })

  it('Excel butonu /exports/dashboard isteğini tetikler', () => {
    cy.viewport(1280, 900)
    anasayfaAc()

    cy.get('[data-test="excel-indir"]').click()
    cy.wait('@excel').then(({ request }) => {
      expect(request.query.donem).to.eq('BU_AY')
    })
  })
})
