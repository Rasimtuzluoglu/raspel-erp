/* Ek viewport kontrolü: 1024, 1280, 1920 */
describe('Extra Viewport Audit', () => {
  const viewports = [
    { name: 'w1024', width: 1024, height: 768 },
    { name: 'w1280', width: 1280, height: 800 },
    { name: 'w1920', width: 1920, height: 1080 }
  ]
  const sayfalar = [
    ['dashboard', '/'], ['raporlar', '/raporlar'], ['muhasebe', '/muhasebe'], ['faturalar', '/faturalar'],
    ['stoklar', '/stoklar'], ['teklifler', '/teklifler'], ['hareketler', '/hareketler'], ['cari-hesaplar', '/cari-hesaplar'],
    ['siparisler', '/siparisler'], ['personel', '/personel'], ['kasa', '/kasa'], ['satinalma', '/satinalma'],
    ['saha-portali', '/saha-portali'], ['onaylar', '/onaylar'], ['denetim', '/denetim'], ['anomaliler', '/anomaliler'],
    ['kritik-stok', '/kritik-stok'], ['fiyat-listesi', '/fiyat-listesi'], ['vergi-raporlari', '/vergi-raporlari'],
    ['banka-mutabakat', '/banka-mutabakat'], ['e-fatura', '/e-fatura'], ['sohbet', '/sohbet'], ['sistem-durum', '/sistem-durum']
  ]

  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', { statusCode: 200, body: { id: 1, username: 'admin', role: 'ADMIN', token: 'mock', sirketId: 1 } })
    cy.intercept('GET', '/api/kullanicilar/ben', { statusCode: 200, body: { id: 1, username: 'admin', role: 'ADMIN', sirketId: 1 } })
    cy.intercept('GET', '/api/sirketler/aktif', { statusCode: 200, body: [{ id: 1, ad: 'RasPel', aktif: true }] })
    cy.intercept('GET', '/api/yetkiler/roller', { statusCode: 200, body: ['ADMIN'] })
    cy.intercept('GET', '/api/yetkiler/moduller', { statusCode: 200, body: ['CARI', 'FATURA', 'STOK'] })
    cy.intercept('GET', '/api/yetkiler', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/doviz', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/dashboard', { statusCode: 200, body: {
      toplamCariSayisi: 148, toplamBakiye: 1845200.50, toplamFatura: 524, toplamStok: 840,
      kritikStokSayisi: 6, bugunkuSiparis: 14, aktifCalisan: 32, aylikGelirGider: [{ ay: 'Oca', gelir: 780000, gider: 490000 }],
      vadesiGecenFaturalar: [{ faturaNumarasi: 'FTR-2026-000412', cariHesapAd: 'Atlas Lojistik ve Taşımacılık Anonim Şirketi Uluslararası Nakliyat', kalanTutar: 48500 }],
      sonHareketler: [{ id: 1, tur: 'TAHSILAT', aciklama: 'Banka Havalesi', tutar: 50000, tarih: '2026-02-21' }],
      enCokSatanlar: [{ stokAdi: 'Ultra Dayanıklı Endüstriyel Palet Sistemi 120x100cm Çift Taraflı Ahşap Euro Palet', toplam: 320 }]
    } })
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: [{ id: 1, ad: 'Ultra Dayanıklı Endüstriyel Palet Sistemi 120x100cm Çift Taraflı Ahşap Euro Palet', stokKodu: 'PLT-001', barkod: '8690123456789', fiyat: 450, miktar: 120 }] })
    cy.intercept('GET', '/api/cari-hesaplar*', { statusCode: 200, body: [{ id: 1, ad: 'Atlas Lojistik ve Taşımacılık Anonim Şirketi Uluslararası Nakliyat', bakiye: 240000, tur: 'MUSTERI' }] })
    cy.intercept('GET', '/api/faturalar*', { statusCode: 200, body: [{ id: 1, faturaNumarasi: 'FTR-2026-000524', cariHesapAd: 'Atlas Lojistik ve Taşımacılık Anonim Şirketi Uluslararası Nakliyat', tarih: '2026-02-21', tur: 'SATIS', durum: 'KESILDI', genelToplam: 84000 }] })
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} })
    cy.window().then((win) => {
      win.localStorage.setItem('raspel_erp_auth', JSON.stringify({ kullanici: { id: 1, username: 'admin', displayName: 'Ahmet', role: 'ADMIN' }, token: 'mock', sirketId: 1, yetkiler: ['ADMIN'] }))
      win.localStorage.setItem('raspel_erp_theme', 'light')
    })
  })

  const rapor = {}
  viewports.forEach((vp) => {
    sayfalar.forEach(([ad, yol]) => {
      it(`${ad} @${vp.name}`, () => {
        cy.viewport(vp.width, vp.height)
        cy.visit(yol)
        cy.wait(1500)
        cy.get('body').then(($body) => { if ($body.find('.p-dialog-mask').length) cy.get('body').type('{esc}') })
        cy.window().then((win) => {
          const vw = win.innerWidth
          const b = []
          if (win.document.documentElement.scrollWidth > vw + 2) {
            b.push({ tip: 'document-yatay-tasma', scrollWidth: win.document.documentElement.scrollWidth, viewport: vw })
          }
          rapor[`${vp.name}/${ad}`] = b
        })
      })
    })
  })
  after(() => cy.writeFile('cypress/extra-audit.json', rapor))
})
