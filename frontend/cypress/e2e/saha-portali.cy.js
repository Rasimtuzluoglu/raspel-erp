/**
 * Saha Portalı E2E — gerçek backend gerekmez.
 *
 * Oturum localStorage'dan kurulur; saha uçları `cy.intercept` ile mock'lanır.
 * Telefon görünümü (390x844) esas alınır.
 */

const AUTH_ANAHTAR = 'raspel_erp_auth'

const oturumVerisi = {
  kullanici: {
    id: 9,
    username: 'saha',
    displayName: 'Ali Sürücü',
    role: 'USER',
    sahaKullanici: true
  },
  companyName: 'Test Şirketi',
  sirketId: 1,
  sirketAdi: 'Test Şirketi',
  yetkiler: ['*'],
  tokenExpiresAt: Date.now() + 60 * 60 * 1000
}

const ozetVerisi = {
  tarih: '2026-11-17',
  teslimat: { bekleyen: 2, yolda: 1, bugunTeslimEdilen: 1, geciken: 1 },
  ziyaret: { bugun: 2, sonZiyaretTarihi: '2026-11-17T09:30:00' },
  talepler: { bekleyenIzin: 1, bekleyenMasraf: 2 }
}

const teslimatlar = [
  {
    id: 5,
    faturaNumarasi: 'FTR-2026-0005',
    teslimatAdresi: 'Atatürk Cad. No:5 Kadıköy / İstanbul',
    musteriAdi: 'Yılmaz Ticaret',
    musteriTelefon: '05001112233',
    durum: 'BEKLEMEDE',
    gecikti: false
  },
  {
    id: 6,
    faturaNumarasi: 'FTR-2026-0006',
    teslimatAdresi: 'Bostancı Mah. 12. Sok. No:3 / İstanbul',
    musteriAdi: 'Demir Gıda',
    durum: 'YOLDA',
    gecikti: true
  }
]

const apiMockla = () => {
  // Bilinmeyen çağrılar saha testini düşürmesin; özel intercept'ler sonra tanımlanır.
  cy.intercept('**/api/**', { statusCode: 200, body: {} })

  cy.intercept('GET', '**/api/saha/ozet*', { statusCode: 200, body: ozetVerisi }).as('ozet')
  cy.intercept('GET', '**/api/deliveries?driverId=*', {
    statusCode: 200,
    body: teslimatlar
  }).as('teslimatlar')
  cy.intercept('GET', '**/api/notlar*', { statusCode: 200, body: { content: [] } }).as('notlar')
  cy.intercept('GET', '**/api/personel-izin*', { statusCode: 200, body: { content: [] } }).as('izinler')
  cy.intercept('GET', '**/api/personel-masraf-talepler*', { statusCode: 200, body: [] }).as('masraflar')
  cy.intercept('GET', '**/api/kasalar*', { statusCode: 200, body: [] }).as('kasalar')
  cy.intercept('GET', '**/api/bankalar*', { statusCode: 200, body: [] }).as('bankalar')
  cy.intercept('POST', '**/api/deliveries/*/teslim*', { statusCode: 200, body: { id: 5 } }).as('teslim')
}

const sahaAc = (cevrimici = true) => {
  apiMockla()
  cy.visit('/saha-portali', {
    onBeforeLoad(win) {
      win.localStorage.setItem(AUTH_ANAHTAR, JSON.stringify(oturumVerisi))
      win.localStorage.setItem('raspel_erp_session_politika_v2', '1')
      win.localStorage.setItem('raspel_gorulen_surum', '1.1.0')
      win.localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
      win.localStorage.setItem('raspel_gosterilen_ipuclari', '[]')
      win.localStorage.removeItem('raspel_saha_kuyruk_depo')
      win.localStorage.removeItem('raspel_saha_kuyrugu')
      win.indexedDB?.deleteDatabase('raspel-saha')
      if (!cevrimici) {
        Object.defineProperty(win.navigator, 'onLine', { configurable: true, get: () => false })
      }
    }
  })
  cy.url().should('include', '/saha-portali')
  cy.wait('@ozet', { timeout: 20000 })
  cy.wait('@teslimatlar', { timeout: 20000 })
}

const yatayTasmaYok = () => {
  cy.document().then((doc) => {
    const kok = doc.documentElement
    expect(kok.scrollWidth, 'yatay taşma').to.be.at.most(kok.clientWidth + 1)
  })
}

/** IndexedDB kuyruğunu okur (yerel depo adaptörü). */
const kuyrukKayitlari = () =>
  cy.window().then(
    (win) =>
      new Promise((cozumle, reddet) => {
        const istek = win.indexedDB.open('raspel-saha', 1)
        istek.onerror = () => reddet(istek.error)
        istek.onsuccess = () => {
          const db = istek.result
          if (!db.objectStoreNames.contains('kuyruk')) {
            cozumle([])
            return
          }
          const islem = db.transaction('kuyruk', 'readonly').objectStore('kuyruk').getAll()
          islem.onsuccess = () => cozumle(islem.result || [])
          islem.onerror = () => reddet(islem.error)
        }
      })
  )

const imzaCiz = () => {
  cy.get('[data-test="saha-imza-sheet"] canvas', { timeout: 10000 })
    .should('be.visible')
    .trigger('mousedown', { offsetX: 20, offsetY: 30, force: true })
    .trigger('mousemove', { offsetX: 90, offsetY: 60, force: true })
    .trigger('mouseup', { force: true })
}

describe('Saha Portalı', () => {
  it('saha-ozet-ve-sekmeler: 3 kart, 4 sekme, taşma yok', () => {
    cy.viewport(390, 844)
    sahaAc()

    cy.get('[data-test="saha-ozet-teslimat"]').should('be.visible').and('contain', '3')
    cy.get('[data-test="saha-ozet-ziyaret"]').should('be.visible').and('contain', '2')
    cy.get('[data-test="saha-ozet-talepler"]').should('be.visible').and('contain', '3')

    cy.get('[data-test="saha-sekme-teslimat"]').should('be.visible').and('contain', 'Teslimatlar')
    cy.get('[data-test="saha-sekme-ziyaret"]').scrollIntoView().should('be.visible')
    cy.get('[data-test="saha-sekme-talepler"]').scrollIntoView().should('be.visible')
    cy.get('[data-test="saha-sekme-tahsilat"]').scrollIntoView().should('be.visible')

    cy.get('[data-test="saha-teslimat-karti"]').should('have.length', 2)
    cy.get('[data-test="saha-teslimat-siradaki"]').should('be.visible')
    yatayTasmaYok()
  })

  it('saha-ozet-karti: karta tıklama sekmeyi açar ve yalnız o sekmeyi yükler', () => {
    cy.viewport(390, 844)
    sahaAc()

    cy.get('@notlar.all').should('have.length', 0)
    cy.get('[data-test="saha-ozet-ziyaret"]').click()

    cy.get('[data-test="saha-sekme-icerik-ziyaret"]').should('be.visible')
    cy.wait('@notlar', { timeout: 20000 })
    cy.window().then((win) => {
      expect(win.localStorage.getItem('raspel_saha_sekme')).to.eq('ziyaret')
    })
    yatayTasmaYok()
  })

  it('saha-navigasyon: yol tarifi doğru URL ile açılır', () => {
    cy.viewport(390, 844)
    sahaAc()

    cy.window().then((win) => {
      cy.stub(win, 'open').as('haritaAc')
    })
    cy.get('[data-test="saha-navigasyon"]').first().click()

    cy.get('@haritaAc').should('have.been.calledWithMatch', (url) => {
      return url.startsWith('https://www.google.com/maps/dir/?api=1&destination=') &&
        url.includes('Kad%C4%B1k%C3%B6y')
    })
  })

  it('saha-imza-sheet: tek ekranda imza ve teslim (çevrimiçi)', () => {
    cy.viewport(390, 844)
    sahaAc()

    cy.get('[data-test="saha-teslimat-karti"] [data-test="saha-teslimat-imza"]').first().click()
    cy.get('[data-test="saha-imza-sheet"]').should('be.visible')
    cy.get('[data-test="saha-imza-gonder"]').should('be.disabled')

    cy.get('[data-test="saha-imza-ad"]').type('Ali Veli')
    imzaCiz()
    cy.get('[data-test="saha-imza-gonder"]').should('not.be.disabled').click()

    cy.wait('@teslim', { timeout: 20000 })
    cy.get('[data-test="saha-imza-sheet"]').should('not.exist')
    yatayTasmaYok()
  })

  it('saha-cevrimdisi-kuyruk: imza kuyruğa girer, online olunca gönderilir', () => {
    cy.viewport(390, 844)
    sahaAc(false)

    cy.get('[data-test="saha-teslimat-karti"] [data-test="saha-teslimat-imza"]').first().click()
    cy.get('[data-test="saha-imza-ad"]').type('Ayşe Yılmaz')
    imzaCiz()
    cy.get('[data-test="saha-imza-kuyruk"]').should('be.visible').click()

    cy.get('[data-test="saha-kuyruk-cubugu"]', { timeout: 10000 }).should('be.visible')
    kuyrukKayitlari().then((kayitlar) => {
      expect(kayitlar.length, 'kuyruk kaydı').to.be.greaterThan(0)
      expect(kayitlar[0].tur).to.eq('TESLIMAT')
      expect(kayitlar[0].dosyalar?.[0]?.dataUrl).to.contain('data:image/png')
    })

    cy.window().then((win) => {
      Object.defineProperty(win.navigator, 'onLine', { configurable: true, get: () => true })
      win.dispatchEvent(new win.Event('online'))
    })

    cy.wait('@teslim', { timeout: 20000 })
    cy.get('[data-test="saha-kuyruk-cubugu"]', { timeout: 10000 }).should('not.exist')
  })
})
