/**
 * Cari hesaplar E2E — gerçek backend gerekmez.
 * Oturum localStorage'dan kurulur; /api çağrıları mock'lanır.
 */

const AUTH_ANAHTAR = 'raspel_erp_auth'
const OTURUM_POLITIKA_ANAHTAR = 'raspel_erp_session_politika_v2'

const oturumVerisi = {
  kullanici: {
    id: 1,
    username: 'admin',
    displayName: 'Test Yönetici',
    role: 'ADMIN',
    sirketId: 1,
    companyName: 'Test Şirketi',
    sahaKullanici: false
  },
  companyName: 'Test Şirketi',
  sirketId: 1,
  sirketAdi: 'Test Şirketi',
  yetkiler: ['*'],
  tokenExpiresAt: Date.now() + 60 * 60 * 1000
}

const cariListesi = [
  {
    id: 4,
    ad: 'Pozitif Cari',
    tur: 'Musteri',
    yetkiliKisi: 'Ali Veli',
    telefon: '0555 111 22 33',
    krediLimiti: 500000,
    odemeVadesi: 28,
    temsilciAd: 'Temsilci A',
    bakiye: 939737.5,
    etiketler: 'vip',
    aktif: true
  },
  {
    id: 8,
    ad: 'Negatif Cari',
    tur: 'Musteri',
    yetkiliKisi: 'Ayşe Kaya',
    telefon: '0555 444 55 66',
    krediLimiti: 50000,
    odemeVadesi: 15,
    temsilciAd: null,
    bakiye: -3280,
    etiketler: 'bayi',
    aktif: true
  }
]

const kartVerisi = {
  cariId: 4,
  cariAd: 'Pozitif Cari',
  telefon: '0555 111 22 33',
  email: 'cari@example.com',
  tur: 'Musteri',
  aktif: true,
  temsilciAd: 'Temsilci A',
  kredi: { krediLimiti: 500000, bakiye: 939737.5, kullanilabilirKredi: -439737.5, riskOrani: 187.95, limitAsimi: true },
  ozet: {
    faturaSayisi: 2,
    faturaToplam: 10000,
    kalanTutar: 4000,
    siparisSayisi: 1,
    siparisToplam: 5000,
    iadeSayisi: 0,
    iadeToplam: 0,
    tahsilatToplam: 2500
  },
  sonFaturalar: [
    { id: 10, faturaNumarasi: 'FTR-10', tarih: '2026-10-01', durum: 'KESILDI', genelToplam: 10000, kalanTutar: 4000 }
  ],
  sonSiparisler: [],
  sonIadeler: [],
  sonHareketler: [
    { id: 70, tur: 'TAHSILAT', tarih: '2026-10-02', tutar: 2500, aciklama: 'Nakit tahsilat' }
  ],
  firsatlar: [],
  notlar: [{ id: 40, baslik: 'Görüşme', icerik: 'Fiyat görüşüldü', olusturmaTarihi: '2026-10-03T10:00:00' }],
  ozelFiyatlar: [],
  taksitler: []
}

const apiMockla = () => {
  // Vite dev sunucusu modülleri /src/api/... altından yükler; catch-all'ı
  // yalnızca gerçek /api yoluna sınırla, aksi halde modül istekleri kesilir.
  cy.intercept({ method: 'GET', pathname: '/api/**' }, { statusCode: 200, body: [] }).as('catchAll')
  cy.intercept({ method: 'POST', pathname: '/api/**' }, { statusCode: 200, body: {} }).as('catchAllPost')
  cy.intercept({ method: 'PUT', pathname: '/api/**' }, { statusCode: 200, body: {} }).as('catchAllPut')

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

  cy.intercept('GET', '**/api/cari-hesaplar/filtreli*', {
    statusCode: 200,
    body: { content: cariListesi, totalElements: cariListesi.length, number: 0, size: 10 }
  }).as('filtreli')

  cy.intercept('GET', '**/api/cari-hesaplar/ozet', {
    statusCode: 200,
    body: { toplamKayit: 2, alacakli: 3280, borclu: 939737.5 }
  }).as('ozet')

  cy.intercept('GET', '**/api/cari-hesaplar/etiketler', {
    statusCode: 200,
    body: ['vip', 'bayi']
  }).as('etiketler')

  cy.intercept('GET', '**/api/cari-hesaplar/riskli', { statusCode: 200, body: [] }).as('riskli')
  cy.intercept('GET', '**/api/cari-hesaplar/*/kart', { statusCode: 200, body: kartVerisi }).as('kart')
  cy.intercept('GET', '**/api/cari-hesaplar/*/fiyatlar', { statusCode: 200, body: [] }).as('fiyatlar')
  cy.intercept('GET', '**/api/cari-hesaplar/*/adresler', { statusCode: 200, body: [] }).as('adresler')
  cy.intercept('GET', '**/api/kullanicilar*', { statusCode: 200, body: { content: [] } }).as('kullanicilar')
  cy.intercept('GET', '**/api/fatura/*/cari-faturalari*', { statusCode: 200, body: [] }).as('cariFaturalari')
}

const cariAc = () => {
  apiMockla()
  cy.visit('/cari-hesaplar', {
    onBeforeLoad(win) {
      win.localStorage.setItem(AUTH_ANAHTAR, JSON.stringify(oturumVerisi))
      win.localStorage.setItem(OTURUM_POLITIKA_ANAHTAR, '1')
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
      win.localStorage.setItem('raspel_gosterilen_ipuclari', '[]')
    }
  })
  cy.url().should('not.include', '/giris')
  cy.wait('@filtreli', { timeout: 20000 })
  cy.get('.cari-hesaplar-container', { timeout: 20000 }).should('be.visible')
  cy.get('.table-container tbody tr').should('have.length', 2)
}

const yatayTasmaYok = () => {
  cy.document().then((doc) => {
    const kok = doc.documentElement
    expect(kok.scrollWidth, 'yatay taşma').to.be.at.most(kok.clientWidth + 1)
  })
}

describe('Cari hesaplar', () => {
  it('liste, KPI ve bakiye yönleri doğru gösterilir', () => {
    cy.viewport(1280, 900)
    cariAc()

    cy.contains('h1', 'Cari Hesaplar Yönetimi').should('be.visible')
    cy.contains('Tahsil Edilecek').should('be.visible')
    cy.contains('Ödenecek').should('be.visible')

    cy.get('.bakiye-rozet.borc').first().should('contain', '939.737,50')
    cy.get('.bakiye-rozet.alacak').first().should('contain', '3.280,00')
    yatayTasmaYok()
  })

  it('KPI kartı bakiye filtresini uygular', () => {
    cy.viewport(1280, 900)
    cariAc()

    cy.get('[data-test="kpi-alacak"]').click()
    cy.wait('@filtreli').its('request.query.bakiyeYonu').should('eq', 'alacak')
    cy.get('[data-test="cari-aktif-filtreler"]').should('contain', 'Tahsil Edilecek')
    cy.get('[data-test="kpi-alacak"]').should('have.attr', 'aria-pressed', 'true')
  })

  it('360 kartı özet, sekmeler ve hızlı aksiyonları gösterir', () => {
    cy.viewport(1280, 900)
    cariAc()

    cy.get('.satir-islemler').first().find('button').last().click()
    cy.contains('Detay (360)').click()
    cy.wait('@kart')
    cy.get('.kart-kimlik-kart').should('contain', 'Pozitif Cari')
    cy.get('.kart-ozet').should('be.visible')
    cy.get('[data-test="cari-360-tahsilat"]').should('be.visible')
    cy.get('[data-test="cari-360-fatura"]').should('be.visible')
    cy.get('.p-dialog').contains('Faturalar').should('be.visible')
  })

  it('yeni cari formu zorunlu alanı satır içi doğrular', () => {
    cy.viewport(1280, 900)
    cariAc()

    cy.contains('button', 'Yeni Cari Hesap').click()
    cy.get('#ad').should('be.visible')
    cy.contains('.p-dialog-footer button', 'Kaydet').click()
    cy.get('.form-hata-ozet').should('exist')
    cy.get('#ad-hata').should('exist')
  })

  it('390px yatay taşma yok ve kart listesi görünür', () => {
    cy.viewport(390, 844)
    cariAc()

    cy.get('.cari-kart-liste').should('be.visible')
    cy.get('.cari-kart').should('have.length', 2)
    yatayTasmaYok()
  })
})
