/* Programatik görsel denetim: layout ihlallerini ölçer ve JSON raporlar */
describe('Layout Violation Audit', () => {
  const viewports = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'mobile', width: 390, height: 844 }
  ]

  const sayfalar = [
    ['dashboard', '/'],
    ['cari-hesaplar', '/cari-hesaplar'],
    ['hareketler', '/hareketler'],
    ['faturalar', '/faturalar'],
    ['bankalar', '/bankalar'],
    ['kasa', '/kasa'],
    ['raporlar', '/raporlar'],
    ['kullanicilar', '/kullanicilar'],
    ['stoklar', '/stoklar'],
    ['toplu-stok', '/toplu-stok'],
    ['satinalma', '/satinalma'],
    ['personel', '/personel'],
    ['izinler', '/izinler'],
    ['puantaj', '/puantaj'],
    ['siparisler', '/siparisler'],
    ['teklifler', '/teklifler'],
    ['saha-portali', '/saha-portali'],
    ['cek-senet', '/cek-senet'],
    ['irsaliyeler', '/irsaliyeler'],
    ['projeler', '/projeler'],
    ['subeler', '/subeler'],
    ['depolar', '/depolar'],
    ['donemler', '/donemler'],
    ['iadeler', '/iadeler'],
    ['stok-seriler', '/stok-seriler'],
    ['stok-sayim', '/stok-sayim'],
    ['muhasebe', '/muhasebe'],
    ['yetki-yonetimi', '/yetki-yonetimi'],
    ['denetim', '/denetim'],
    ['masraflar', '/masraflar'],
    ['butceler', '/butceler'],
    ['crm', '/crm'],
    ['kritik-stok', '/kritik-stok'],
    ['ajanda', '/ajanda'],
    ['onaylar', '/onaylar'],
    ['notlar', '/notlar']
  ]

  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', { statusCode: 200, body: { id: 1, username: 'admin', role: 'ADMIN', token: 'mock', sirketId: 1, sirketAdi: 'RasPel' } })
    cy.intercept('GET', '/api/kullanicilar/ben', { statusCode: 200, body: { id: 1, username: 'admin', role: 'ADMIN', sirketId: 1, companyName: 'RasPel' } })
    cy.intercept('GET', '/api/sirketler/aktif', { statusCode: 200, body: [{ id: 1, ad: 'RasPel', aktif: true }] })
    cy.intercept('GET', '/api/yetkiler/roller', { statusCode: 200, body: ['ADMIN'] })
    cy.intercept('GET', '/api/yetkiler/moduller', { statusCode: 200, body: ['CARI', 'FATURA', 'STOK'] })
    cy.intercept('GET', '/api/yetkiler', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/doviz', { statusCode: 200, body: [] })

    const uzunAd = 'Ultra Dayanıklı Endüstriyel Palet Sistemi 120x100cm Çift Taraflı Ahşap Euro Palet'
    const uzunCari = 'Atlas Lojistik ve Taşımacılık Anonim Şirketi Uluslararası Nakliyat'

    cy.intercept('GET', '/api/dashboard', { statusCode: 200, body: {
      toplamCariSayisi: 148, toplamBakiye: 1845200.50, pozitifBakiye: 2450000.00, negatifBakiye: -604799.50,
      toplamFatura: 524, kesilenFatura: 489, toplamBankaBakiye: 1250000.00, toplamKasaBakiye: 380450.00,
      toplamStok: 840, kritikStokSayisi: 6, bugunkuSiparis: 14, bekleyenTeslimat: 5, iadeOrani: 1.2,
      stokDevirHizi: 4.8, aktifCalisan: 32, bugunIzinli: 2, buAyIseBaslayacak: 1,
      bugunkuTahsilat: 94500.00, bugunkuOdeme: 32100.00,
      aylikGelirGider: [{ ay: 'Eyl', gelir: 450000, gider: 310000 }, { ay: 'Şub', gelir: 780000, gider: 490000 }],
      vadesiGecenFaturalar: [{ faturaId: 101, faturaNumarasi: 'FTR-2026-000412', cariHesapAd: uzunCari, cariTelefon: '0212 555 0192', vadeTarihi: '2026-02-10', kalanTutar: 48500.00 }],
      sonHareketler: [{ id: 1, tur: 'TAHSILAT', aciklama: 'Banka Havalesi', tutar: 50000, tarih: '2026-02-21' }],
      enCokSatanlar: [{ stokAdi: uzunAd, toplam: 320 }]
    } })
    cy.intercept('GET', '/api/stoklar*', { statusCode: 200, body: [
      { id: 1, ad: uzunAd, stokKodu: 'PLT-001-END-ULUSLARARASI', barkod: '869012345678901234567890', fiyat: 450.00, miktar: 120, kategori: 'Ambalaj Malzemeleri', minMiktar: 10, birim: 'Adet' },
      { id: 2, ad: 'Streç Film 50cm x 300m', stokKodu: 'STR-050', barkod: '8690123456790', fiyat: 185.00, miktar: 340, kategori: 'Sarf', minMiktar: 5, birim: 'Rulo' },
      { id: 3, ad: 'Koli Bandı Şeffaf 45x100', stokKodu: 'BND-045', barkod: '8690123456791', fiyat: 35.00, miktar: 600, kategori: 'Sarf', minMiktar: 5, birim: 'Adet' },
      { id: 4, ad: 'Çemberleme Tokası Sac', stokKodu: 'TKA-012', barkod: '8690123456792', fiyat: 12.50, miktar: 1500, kategori: 'Bağlantı', minMiktar: 20, birim: 'Adet' }
    ] })
    cy.intercept('GET', '/api/cari-hesaplar*', { statusCode: 200, body: [
      { id: 1, ad: uzunCari, bakiye: 240000.00, tur: 'MUSTERI', telefon: '0212 555 0192', email: 'musteri.hizmetleri@atlas-lojistik.com.tr', il: 'İstanbul' },
      { id: 2, ad: 'Mavi Dağıtım San.', bakiye: 185000.00, tur: 'MUSTERI', telefon: '0312 222 0188', il: 'Ankara' },
      { id: 3, ad: 'Delta İnşaat Ltd.', bakiye: -72300.00, tur: 'TEDARIKCI', telefon: '0216 444 0122', il: 'İstanbul' }
    ] })
    cy.intercept('GET', '/api/faturalar*', { statusCode: 200, body: [
      { id: 1, faturaNumarasi: 'FTR-2026-000524', cariHesapAd: uzunCari, tarih: '2026-02-21', tur: 'SATIS', durum: 'KESILDI', araToplam: 70000.00, kdv: 14000.00, genelToplam: 84000.00, odenenTutar: 50000.00, kalanTutar: 34000.00, vadeTarihi: '2026-03-21', odemeDurumu: 'KISMI' },
      { id: 2, faturaNumarasi: 'FTR-2026-000525', cariHesapAd: 'Mavi Dağıtım San.', tarih: '2026-02-20', tur: 'SATIS', durum: 'TASLAK', genelToplam: 14400.00, odemeDurumu: 'ODENMEDI' },
      { id: 3, faturaNumarasi: 'FTR-2026-000526', cariHesapAd: 'Delta İnşaat Ltd.', tarih: '2026-02-19', tur: 'ALIS', durum: 'IPTAL', genelToplam: 36000.00, odemeDurumu: 'ODENDI' }
    ] })
    cy.intercept('GET', '/api/hareketler*', { statusCode: 200, body: [
      { id: 1, cariHesapAd: uzunCari, tur: 'TAHSILAT', tutar: 50000.00, hareketTarihi: '2026-02-21', aciklama: 'Banka havalesi ile tahsilat', odemeSekli: 'HAVALE' }
    ] })
    cy.intercept('GET', '/api/siparisler*', { statusCode: 200, body: [
      { id: 1, siparisNo: 'SPR-2026-000145', cariHesapAd: uzunCari, tarih: '2026-02-21', durum: 'ONAYLANDI', genelToplam: 84200.00 },
      { id: 2, siparisNo: 'SPR-2026-000146', cariHesapAd: 'Mavi Dağıtım San.', tarih: '2026-02-20', durum: 'TASLAK', genelToplam: 14400.00 }
    ] })
    cy.intercept('GET', '/api/personel*', { statusCode: 200, body: [
      { id: 1, ad: 'Ahmet', soyad: 'Yılmaz', tcKimlik: '12345678901', departman: 'Muhasebe', pozisyon: 'Genel Müdür Yardımcısı', maas: 85000, telefon: '0532 555 0101', aktif: true },
      { id: 2, ad: 'Zeynep', soyad: 'Çelik', departman: 'İnsan Kaynakları', pozisyon: 'İK Uzmanı', maas: 45000, aktif: true }
    ] })
    cy.intercept('GET', '/api/kasalar*', { statusCode: 200, body: [{ id: 1, ad: 'Ana Kasa - Merkez Şube', bakiye: 380450.00 }] })
    cy.intercept('GET', '/api/bankalar*', { statusCode: 200, body: [{ id: 1, ad: 'Garanti Bankası Ticari Hesap', iban: 'TR33 0006 2000 1234 5678 9012 34', bakiye: 850000.00 }] })
    cy.intercept('GET', '/api/teklifler*', { statusCode: 200, body: [
      { id: 1, teklifNo: 'TKF-2026-000089', cariHesapAd: uzunCari, tarih: '2026-02-21', durum: 'ONAY_BEKLIYOR', genelToplam: 125000.00 }
    ] })
    cy.intercept('GET', '/api/irsaliyeler*', { statusCode: 200, body: [
      { id: 1, irsaliyeNo: 'IRS-2026-000178', cariHesapAd: uzunCari, tarih: '2026-02-21', durum: 'KESILDI', genelToplam: 84200.00 }
    ] })
    cy.intercept('GET', '/api/cek-senet*', { statusCode: 200, body: [
      { id: 1, tur: 'CEK', cariHesapAd: uzunCari, tutar: 45000.00, vadeTarihi: '2026-03-15', durum: 'PORTFODYDE' }
    ] })
    cy.intercept('GET', '/api/subeler*', { statusCode: 200, body: [{ id: 1, ad: 'İstanbul Merkez Şubesi', sirketId: 1, aktif: true }] })
    cy.intercept('GET', '/api/depolar*', { statusCode: 200, body: [{ id: 1, ad: 'Ana Depo - İkitelli', subeId: 1, sirketId: 1 }] })
    cy.intercept('GET', '/api/donemler*', { statusCode: 200, body: [{ id: 1, ad: '2026 Dönemi', aktif: true }] })
    cy.intercept('GET', '/api/projeler*', { statusCode: 200, body: [{ id: 1, ad: 'Yeni Nesil ERP Geçiş Projesi', durum: 'DEVAM_EDIYOR' }] })
    cy.intercept('GET', '/api/masraflar*', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/butceler*', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/iadeler*', { statusCode: 200, body: [{ id: 1, faturaId: 1, tur: 'SATIS', tarih: '2026-02-21', tutar: 8400.00, durum: 'TAMAMLANDI', cariHesapAd: uzunCari }] })
    cy.intercept('GET', '/api/stok-sayim*', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/stok-seriler*', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/kullanicilar*', { statusCode: 200, body: [
      { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN', email: 'admin@raspel.com', aktif: true }
    ] })
    cy.intercept('GET', '/api/audit-log*', { statusCode: 200, body: [
      { id: 1, kullaniciAdi: 'admin', islem: 'Fatura oluşturdu', entityAdi: 'Fatura', entityId: 524, tarih: '2026-02-21T10:30:00' }
    ] })
    cy.intercept('GET', '/api/ai-config', { statusCode: 200, body: {} })
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} })

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN' },
        token: 'mock', companyName: 'RasPel', sirketId: 1, sirketAdi: 'RasPel', yetkiler: ['ADMIN']
      }))
      win.localStorage.setItem('raspel_erp_theme', 'light')
      win.localStorage.setItem('raspel_primary_color', '#3b82f6')
    })
  })

  const rapor = {}

  viewports.forEach((vp) => {
    sayfalar.forEach(([ad, yol]) => {
      it(`audit ${ad} @${vp.name}`, () => {
        cy.viewport(vp.width, vp.height)
        cy.visit(yol)
        cy.wait(1800)
        cy.get('body').then(($body) => {
          if ($body.find('.p-dialog-mask').length > 0) {
            cy.get('body').type('{esc}')
            cy.wait(400)
          }
        })
        cy.window().then((win) => {
          const vw = win.innerWidth
          const bulgular = []
          const doc = win.document

          if (doc.documentElement.scrollWidth > vw + 2) {
            bulgular.push({
              tip: 'document-yatay-tasma',
              scrollWidth: doc.documentElement.scrollWidth,
              viewport: vw
            })
          }

          const tasanlar = []
          doc.querySelectorAll('body *').forEach((el) => {
            if (el.closest('.p-dialog, .p-sidebar, [class*=toast], .p-overlay')) return
            const r = el.getBoundingClientRect()
            if (r.width < 30 || r.right <= vw + 2) return
            const cs = getComputedStyle(el)
            if (cs.position === 'fixed' && r.left >= vw) return
            if (cs.display === 'none' || cs.visibility === 'hidden') return
            const cls = (typeof el.className === 'string' ? el.className : '').split(' ').slice(0, 4).join('.')
            const id = el.id || ''
            tasanlar.push({
              tag: el.tagName.toLowerCase(),
              cls: cls || id || (el.getAttribute('data-v-') ? 'scoped' : ''),
              left: Math.round(r.left),
              right: Math.round(r.right),
              width: Math.round(r.width)
            })
          })
          if (tasanlar.length) bulgular.push({ tip: 'viewport-disi-elemanlar', sayi: tasanlar.length, ornekler: tasanlar.slice(0, 8) })

          const tablolar = []
          doc.querySelectorAll('.p-datatable-wrapper').forEach((w) => {
            if (w.scrollWidth > w.clientWidth + 2) {
              tablolar.push({
                wrapperWidth: w.clientWidth,
                scrollWidth: w.scrollWidth,
                kaydirilabilir: true,
                parent: (w.parentElement.className || '').toString().slice(0, 60)
              })
            }
          })
          doc.querySelectorAll('table').forEach((t) => {
            const pr = t.getBoundingClientRect()
            if (pr.width > vw + 2 && !t.closest('.p-datatable-wrapper')) {
              bulgular.push({ tip: 'tablo-viewport-tasması', width: Math.round(pr.width), viewport: vw })
            }
          })
          if (tablolar.length) bulgular.push({ tip: 'datatable-ic-yatay-kaydirma', tablolar })

          const dialoglar = []
          doc.querySelectorAll('.p-dialog').forEach((d) => {
            const r = d.getBoundingClientRect()
            if (r.width > vw + 2) dialoglar.push({ width: Math.round(r.width), viewport: vw, cls: (d.className || '').toString().slice(0, 50) })
          })
          if (dialoglar.length) bulgular.push({ tip: 'dialog-viewport-tasması', dialoglar })

          const secili = doc.querySelector('.main-content')
          if (secili) {
            const r = secili.getBoundingClientRect()
            if (r.left < -1) bulgular.push({ tip: 'main-content-sola-tasıyor', left: Math.round(r.left) })
          }

          rapor[`${vp.name}/${ad}`] = bulgular
        })
      })
    })
  })

  after(() => {
    cy.writeFile('cypress/layout-audit.json', rapor)
  })
})
