/**
 * Faz 11 — Depolar operasyon merkezi (E2E).
 *
 * Kapsam: filtreli liste + sayfalama, stok ekle/cikar (0 miktar engeli),
 * transfer onay/red (gerekce zorunlu), hareket gecmisi, dar ekran (1024px).
 * Tum API'ler mock'lanir; backend gerekmez.
 */
const DEPOLAR = [
  {
    id: 1, ad: 'Merkez Depo', subeId: 1, subeAdi: 'Merkez Şube',
    sorumluKullaniciId: 1, sorumluKullaniciAd: 'Ali Veli', yetkili: 'Ali Veli',
    kalemSayisi: 4, kritikStokSayisi: 2, sifirStokSayisi: 1,
    stokDegeri: 1200.5, dolulukYuzde: 40, kapasiteKalem: 10, aktif: true
  },
  {
    id: 2, ad: 'Saha Depo', subeId: 2, subeAdi: 'Şube 2',
    sorumluKullaniciId: null, sorumluKullaniciAd: null, yetkili: 'Ayşe Kaya',
    kalemSayisi: 3, kritikStokSayisi: 0, sifirStokSayisi: 0,
    stokDegeri: 50, dolulukYuzde: null, kapasiteKalem: null, aktif: true
  }
]

const STOKLAR = [
  {
    id: 10, depoId: 1, depoAdi: 'Merkez Depo', stokId: 100,
    stokAd: 'Vida', stokKodu: 'V-1', birim: 'Adet',
    miktar: 3, minMiktar: 5, ortalamaMaliyet: 10, deger: 30, kritikMi: true
  }
]

const HAREKETLER = [
  {
    id: 20, tarih: '2026-11-13T10:00:00', hareketTarihi: '2026-11-13',
    tur: 'GIRIS', kaynakTip: 'MANUEL', stokId: 100, stokKod: 'V-1', stokAd: 'Vida',
    miktar: 3, kullaniciAd: 'Ali Veli', aciklama: 'Sayım düzeltmesi'
  }
]

const TRANSFERLER = [
  {
    id: 5, kaynakDepoId: 1, kaynakDepoAd: 'Merkez Depo',
    hedefDepoId: 2, hedefDepoAd: 'Saha Depo',
    stokId: 100, stokAd: 'Vida', miktar: 2, durum: 'BEKLIYOR',
    olusturanKullaniciId: 1, olusturanKullaniciAd: 'Ali Veli',
    yasSaat: 6, slaDurumu: 'YAKLASIYOR', aciklama: 'Saha talebi'
  }
]

const sayfa = (content) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 20
})

describe('Depolar operasyon merkezi', () => {
  beforeEach(() => {
    cy.girisYap()

    // Depo listesi: `getAll` (parametresiz -> dizi) ve `sayfali` (page -> Page)
    // ayni pathname'i kullanir; tek handler iki sekli de karsilar.
    cy.intercept({ method: 'GET', pathname: '/api/depolar' }, (req) => {
      if (req.query.page === undefined) {
        req.reply({ statusCode: 200, body: DEPOLAR })
        return
      }
      const q = (req.query.q || '').toString().toLowerCase()
      const icerik = DEPOLAR.filter((d) => !q || d.ad.toLowerCase().includes(q))
      req.reply({
        statusCode: 200,
        body: { content: icerik, totalElements: 45, totalPages: 3, number: Number(req.query.page), size: Number(req.query.size) }
      })
    }).as('depolarListe')

    cy.intercept('GET', '/api/depolar/ozet', { statusCode: 200, body: { toplamDepo: 2, aktifDepo: 2, stokKalemiSayisi: 7, sifirStokluKalem: 1, bekleyenTalep: 1 } }).as('ozet')
    cy.intercept('GET', '/api/depolar/1/ozet', { statusCode: 200, body: { depoId: 1, kalemSayisi: 4, kritikStokSayisi: 2, sonHareketler: [] } }).as('depoOzet')
    cy.intercept('GET', '/api/depolar/1/stoklar/sayfali*', { statusCode: 200, body: sayfa(STOKLAR) }).as('stokSayfali')
    cy.intercept('GET', '/api/depolar/1/hareketler*', { statusCode: 200, body: sayfa(HAREKETLER) }).as('hareketler')
    cy.intercept('GET', '/api/depo-transferler/sayfali*', { statusCode: 200, body: sayfa(TRANSFERLER) }).as('transferSayfali')
    cy.intercept('GET', '/api/subeler', { statusCode: 200, body: [{ id: 1, ad: 'Merkez Şube' }, { id: 2, ad: 'Şube 2' }] }).as('subeler')
    cy.intercept('GET', '/api/kullanicilar*', { statusCode: 200, body: sayfa([{ id: 1, displayName: 'Ali Veli' }, { id: 2, displayName: 'Ayşe Kaya' }]) }).as('kullanicilar')
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: sayfa([{ id: 100, ad: 'Vida', stokKodu: 'V-1', birim: 'Adet' }]) }).as('stoklar')

    cy.visit('/depolar')
    cy.wait('@depolarListe')
  })

  it('depo listesini KPI ve tabloyla gosterir', () => {
    cy.contains('Depolar & Stok Yönetimi').should('be.visible')
    cy.get('[data-test="depo-tablo"]').should('be.visible')
    cy.contains('Merkez Depo').should('be.visible')
    cy.contains('Saha Depo').should('be.visible')
    // Sorumlu atanmamissa `yetkili` alanina duser.
    cy.contains('Ayşe Kaya').should('be.visible')
    cy.contains('Toplam Depo').should('be.visible')
  })

  it('yeni depo diyalogu acilir ve kompakt kalir', () => {
    cy.get('.depo-arac-cubugu').contains('button', 'Yeni Depo').click()
    cy.get('.p-dialog').should('be.visible').and('contain', 'Kapasite (kalem)')
    cy.get('.p-dialog').then(($d) => {
      const w = $d[0].getBoundingClientRect().width
      expect(w).to.be.lessThan(480)
    })
    cy.get('.p-dialog textarea').should('be.visible')
  })

  it('arama filtresini sunucuya iletir ve sayfayi sifirlar', () => {
    cy.intercept({ method: 'GET', pathname: '/api/depolar', query: { q: 'merkez' } }, (req) => {
      req.reply({
        statusCode: 200,
        body: { content: [DEPOLAR[0]], totalElements: 45, totalPages: 3, number: 0, size: 20 }
      })
    }).as('arama')
    cy.get('.depo-arac-cubugu input').first().type('merkez')
    cy.wait('@arama').its('request.query').should('deep.include', { q: 'merkez', page: '0' })
    cy.get('[data-test="depo-tablo"]').contains('Saha Depo').should('not.exist')
    cy.get('.p-paginator').should('be.visible')
  })

  it('kritik filtresi listeyi daraltir', () => {
    cy.get('.kritik-filtre input').check({ force: true })
    cy.wait('@depolarListe').its('request.query').should('deep.include', { kritik: 'true' })
  })

  it('stok paneli 0 miktarla islem yapmaz, gecerli miktarla ekler', () => {
    cy.get('[data-test="depo-tablo"] tbody tr').first().find('button').first().click()
    cy.get('.eylem-menu').contains('Stokları Gör').click()
    cy.wait('@stokSayfali')
    cy.get('[data-test="depo-stok-tablo"]').should('be.visible').contains('Vida')

    // Miktar bos/0 iken ekle-cikar kapali.
    cy.get('.stok-islem-form').contains('button', 'Ekle').should('be.disabled')
    cy.get('.stok-islem-form').contains('button', 'Çıkar').should('be.disabled')

    cy.get('.stok-islem-form .p-select').click()
    cy.get('.p-select-option').contains('Vida').click()
    cy.get('.stok-islem-form .p-inputnumber input').then(($i) => {
      $i.val('-5')
      $i[0].dispatchEvent(new Event('keyup', { bubbles: true }))
    })
    cy.get('.stok-islem-form').contains('button', 'Ekle').should('be.disabled')
    cy.get('.stok-islem-form .p-inputnumber input').clear().type('5')
    cy.get('.stok-islem-form .p-select-label').should('have.text', 'Vida')
    cy.get('.stok-islem-form .p-inputnumber input').should('have.value', '5')
    cy.get('.stok-islem-form').contains('button', 'Ekle').should('not.be.disabled')

    cy.intercept('POST', '/api/depolar/1/stok-ekle', { statusCode: 200, body: STOKLAR[0] }).as('stokEkle')
    cy.get('.stok-islem-form').contains('button', 'Ekle').click()
    cy.wait('@stokEkle').its('request.body').should('deep.include', { stokId: 100, miktar: 5 })
  })

  it('yetersiz stokta cikis hatasini kullaniciya gosterir', () => {
    cy.get('[data-test="depo-tablo"] tbody tr').first().find('button').first().click()
    cy.get('.eylem-menu').contains('Stokları Gör').click()
    cy.wait('@stokSayfali')

    cy.get('.stok-islem-form .p-select').click()
    cy.get('.p-select-option').contains('Vida').click()
    cy.get('.stok-islem-form .p-inputnumber input').clear().type('99')

    cy.intercept('POST', '/api/depolar/1/stok-cikar', {
      statusCode: 400,
      body: { message: 'Yetersiz stok! Mevcut: 3, Çıkış: 99' }
    }).as('stokCikar')
    cy.get('.stok-islem-form').contains('button', 'Çıkar').click()
    cy.wait('@stokCikar')
    cy.get('.p-toast-message-text').should('contain', 'Yetersiz stok')
  })

  it('transferi onaylar ve denetim ucuna gider', () => {
    cy.contains('[role="tab"]', 'Transferler').click()
    cy.wait('@transferSayfali')
    cy.get('[data-test="depo-transfer-tablo"]').contains('Merkez Depo').should('be.visible')
    cy.get('[data-test="depo-transfer-tablo"]').contains('Yaklaşıyor').should('be.visible')

    cy.intercept('POST', '/api/depo-transferler/5/onayla', { statusCode: 200, body: { id: 5, durum: 'ONAYLANDI' } }).as('onayla')
    cy.get('[data-test="depo-transfer-tablo"] tbody tr').first().find('button[aria-label="Onayla"]').click()
    cy.wait('@onayla')
  })

  it('red islemi en az 3 karakter gerekce ister', () => {
    cy.contains('[role="tab"]', 'Transferler').click()
    cy.wait('@transferSayfali')

    cy.get('[data-test="depo-transfer-tablo"] tbody tr').first().find('button[aria-label="Reddet"]').click()
    cy.get('.p-dialog').should('be.visible').contains('Red Gerekçesi')
    cy.get('.p-dialog').contains('button', 'Reddet').should('be.disabled')

    cy.get('.p-dialog textarea').type('ab')
    cy.get('.p-dialog').contains('button', 'Reddet').should('be.disabled')
    cy.get('.p-dialog textarea').clear().type('Stok hasarlı')
    cy.get('.p-dialog').contains('button', 'Reddet').should('not.be.disabled')

    cy.intercept('POST', '/api/depo-transferler/5/reddet', { statusCode: 200, body: { id: 5, durum: 'REDDEDILDI', redSebebi: 'Stok hasarlı' } }).as('reddet')
    cy.get('.p-dialog').contains('button', 'Reddet').click()
    cy.wait('@reddet').its('request.body').should('deep.include', { sebep: 'Stok hasarlı' })
  })

  it('stok satirindan hareket gecmisine gecer ve kullaniciyi gosterir', () => {
    cy.get('[data-test="depo-tablo"] tbody tr').first().find('button').first().click()
    cy.get('.eylem-menu').contains('Stokları Gör').click()
    cy.wait('@stokSayfali')

    cy.get('[data-test="depo-stok-tablo"] tbody tr').first().find('button').click()
    cy.wait('@hareketler')
    cy.get('[data-test="depo-hareket-tablo"]').should('be.visible')
    cy.get('[data-test="depo-hareket-tablo"]').contains('Ali Veli').should('be.visible')
    cy.get('[data-test="depo-hareket-tablo"]').contains('Sayım düzeltmesi').should('be.visible')
  })

  it('1024px ekranda arac cubugu ve tablo kullanilabilir kalir', () => {
    cy.viewport(1024, 768)
    cy.visit('/depolar')
    cy.wait('@depolarListe')
    cy.get('[data-test="depo-tablo"]').should('be.visible')
    cy.get('.depo-arac-cubugu input').first().should('be.visible')
    cy.get('.p-paginator').should('be.visible')
    cy.document().then((doc) => {
      expect(doc.documentElement.scrollWidth).to.be.lessThan(1024 + 24)
    })
  })
})

describe('Depolar — USER rolü', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: { girisToken: 'test-token', twoFactorGerekli: false, sirketler: [{ id: 1, ad: 'Test' }] }
    }).as('login')
    cy.intercept('POST', '/api/kullanicilar/giris-sirket', {
      statusCode: 200,
      body: {
        id: 7, username: 'user', displayName: 'User', role: 'USER',
        token: 'test-token', sirketId: 1, sirketAdi: 'Test', companyName: 'Test'
      }
    }).as('girisSirket')
    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: { id: 7, username: 'user', displayName: 'User', role: 'USER', sirketId: 1, companyName: 'Test' }
    }).as('ben')
    cy.intercept('GET', '/api/yetkiler/roller', ['USER']).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', ['STOK']).as('moduller')
    cy.intercept('GET', '/api/yetkiler', ['STOK_READ']).as('yetkiler')
    cy.intercept({ method: 'GET', pathname: '/api/depolar' }, (req) => {
      const body = req.query.page === undefined
        ? DEPOLAR
        : { content: DEPOLAR, totalElements: DEPOLAR.length, totalPages: 1, number: 0, size: 20 }
      req.reply({ statusCode: 200, body })
    }).as('depolarListeUser')
    cy.intercept('GET', '/api/depolar/ozet', { statusCode: 200, body: {} }).as('ozetUser')
    // Sorumlusu olmadigi depo: USER islem yapamaz.
    cy.intercept('GET', '/api/depolar/2/stoklar/sayfali*', { statusCode: 200, body: sayfa(STOKLAR) }).as('stokSayfaliUser')
    cy.intercept('GET', '/api/depo-transferler/sayfali*', { statusCode: 200, body: sayfa(TRANSFERLER) }).as('transferSayfaliUser')
    cy.intercept('GET', '/api/subeler', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/kullanicilar*', { statusCode: 200, body: sayfa([]) })
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: sayfa([]) })

    cy.visit('/giris')
    cy.get('[data-test="giris-username"]').type('user')
    cy.get('[data-test="giris-password"]').type('123456')
    cy.get('[data-test="giris-buton"]').click()
    cy.wait('@login')
    cy.wait('@girisSirket')
    cy.visit('/depolar')
    cy.wait('@depolarListeUser')
  })

  it('stok islem formunu gostermez (sorumlu degil)', () => {
    cy.get('[data-test="depo-tablo"] tbody tr').eq(1).find('button').first().click()
    cy.get('.eylem-menu').contains('Stokları Gör').click()
    cy.wait('@stokSayfaliUser')
    cy.get('[data-test="depo-stok-tablo"]').should('be.visible')
    cy.get('.stok-islem-form').should('not.exist')
  })

  it('transfer onay/red aksiyonlarini gostermez', () => {
    cy.contains('[role="tab"]', 'Transferler').click()
    cy.wait('@transferSayfaliUser')
    cy.get('[data-test="depo-transfer-tablo"]').should('be.visible')
    cy.get('[data-test="depo-transfer-tablo"] button[aria-label="Onayla"]').should('not.exist')
    cy.get('[data-test="depo-transfer-tablo"] button[aria-label="Reddet"]').should('not.exist')
  })
})
