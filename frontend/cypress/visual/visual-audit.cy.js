/* Görsel denetim için çoklu viewport ekran görüntüsü alma spec'i */
describe('Visual Audit Screenshots', () => {
  const viewports = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'mobile', width: 390, height: 844 }
  ]

  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN', token: 'mock', sirketId: 1, sirketAdi: 'RasPel Teknoloji A.Ş.' }
    }).as('login')
    cy.intercept('GET', '/api/kullanicilar/ben', { statusCode: 200, body: { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN', sirketId: 1, companyName: 'RasPel Teknoloji A.Ş.' } }).as('ben')
    cy.intercept('GET', '/api/sirketler/aktif', { statusCode: 200, body: [{ id: 1, ad: 'RasPel Teknoloji A.Ş.', aktif: true }] }).as('sirketler')
    cy.intercept('GET', '/api/yetkiler/roller', { statusCode: 200, body: ['ADMIN', 'MUHASEBE', 'SATIS', 'DEPO', 'PERSONEL'] }).as('roller')
    cy.intercept('GET', '/api/yetkiler/moduller', { statusCode: 200, body: ['CARI', 'FATURA', 'STOK', 'BANKA', 'KASA', 'SIPARIS', 'SATINALMA', 'PERSONEL', 'MUHASEBE', 'RAPOR'] }).as('moduller')
    cy.intercept('GET', '/api/yetkiler', { statusCode: 200, body: [{ rol: 'ADMIN', modul: 'FATURA', okuma: true, yazma: true, silme: true, disaAktarim: true }] }).as('yetkiler')
    cy.intercept('GET', '/api/doviz', { statusCode: 200, body: [{ kod: 'USD', alis: 36.45, satis: 36.52, degisim: 0.15 }] }).as('doviz')

    const uzunAd = 'Ultra Dayanıklı Endüstriyel Palet Sistemi 120x100cm Çift Taraflı Ahşap Euro Palet'
    const uzunCari = 'Atlas Lojistik ve Taşımacılık Anonim Şirketi Uluslararası Nakliyat'

    cy.intercept('GET', '/api/dashboard', {
      statusCode: 200,
      body: {
        toplamCariSayisi: 148, toplamBakiye: 1845200.50, pozitifBakiye: 2450000.00, negatifBakiye: -604799.50,
        toplamFatura: 524, kesilenFatura: 489, toplamBankaBakiye: 1250000.00, toplamKasaBakiye: 380450.00,
        toplamStok: 840, kritikStokSayisi: 6, bugunkuSiparis: 14, bekleyenTeslimat: 5, iadeOrani: 1.2,
        stokDevirHizi: 4.8, aktifCalisan: 32, bugunIzinli: 2, buAyIseBaslayacak: 1,
        bugunkuTahsilat: 94500.00, bugunkuOdeme: 32100.00,
        aylikGelirGider: [
          { ay: 'Eyl', gelir: 450000, gider: 310000 }, { ay: 'Eki', gelir: 520000, gider: 360000 },
          { ay: 'Kas', gelir: 580000, gider: 410000 }, { ay: 'Ara', gelir: 690000, gider: 450000 },
          { ay: 'Oca', gelir: 640000, gider: 420000 }, { ay: 'Şub', gelir: 780000, gider: 490000 }
        ],
        vadesiGecenFaturalar: [
          { faturaId: 101, faturaNumarasi: 'FTR-2026-000412', cariHesapAd: uzunCari, cariTelefon: '0212 555 0192', vadeTarihi: '2026-02-10', kalanTutar: 48500.00 }
        ],
        sonHareketler: [
          { id: 1, tur: 'TAHSILAT', aciklama: 'Banka Havalesi - Atlas Lojistik', tutar: 50000, tarih: '2026-02-21' }
        ],
        enCokSatanlar: [{ stokAdi: uzunAd, toplam: 320 }]
      }
    }).as('dashboard')

    cy.intercept('GET', '/api/stoklar*', {
      statusCode: 200,
      body: [
        { id: 1, ad: uzunAd, stokKodu: 'PLT-001-END-ULUSLARARASI', barkod: '869012345678901234567890', fiyat: 450.00, miktar: 120, kategori: 'Ambalaj Malzemeleri ve Sarf', minMiktar: 10, birim: 'Adet' },
        { id: 2, ad: 'Streç Film 50cm x 300m Rulo', stokKodu: 'STR-050', barkod: '8690123456790', fiyat: 185.00, miktar: 340, kategori: 'Sarf', minMiktar: 5, birim: 'Rulo' },
        { id: 3, ad: 'Koli Bandı Şeffaf 45x100', stokKodu: 'BND-045', barkod: '8690123456791', fiyat: 35.00, miktar: 600, kategori: 'Sarf', minMiktar: 5, birim: 'Adet' },
        { id: 4, ad: 'Çemberleme Tokası Sac 32mm', stokKodu: 'TKA-012', barkod: '8690123456792', fiyat: 12.50, miktar: 1500, kategori: 'Bağlantı', minMiktar: 20, birim: 'Adet' }
      ]
    }).as('stoklar')

    cy.intercept('GET', '/api/cari-hesaplar*', {
      statusCode: 200,
      body: [
        { id: 1, ad: uzunCari, bakiye: 240000.00, tur: 'MUSTERI', telefon: '0212 555 0192 1234', email: 'musteri.hizmetleri@atlas-lojistik-nakliyat.com.tr', vergiNumarasi: '1234567890', il: 'İstanbul' },
        { id: 2, ad: 'Mavi Dağıtım San.', bakiye: 185000.00, tur: 'MUSTERI', telefon: '0312 222 0188', il: 'Ankara' },
        { id: 3, ad: 'Delta İnşaat Ltd.', bakiye: -72300.00, tur: 'TEDARIKCI', telefon: '0216 444 0122', il: 'İstanbul' }
      ]
    }).as('cariHesaplar')

    const faturalar = [
      { id: 1, faturaNumarasi: 'FTR-2026-000524', cariHesapAd: uzunCari, tarih: '2026-02-21', tur: 'SATIS', durum: 'KESILDI', araToplam: 70000.00, kdv: 14000.00, genelToplam: 84000.00, odenenTutar: 50000.00, kalanTutar: 34000.00, vadeTarihi: '2026-03-21', odemeDurumu: 'KISMI' },
      { id: 2, faturaNumarasi: 'FTR-2026-000525', cariHesapAd: 'Mavi Dağıtım San.', tarih: '2026-02-20', tur: 'SATIS', durum: 'TASLAK', araToplam: 12000.00, kdv: 2400.00, genelToplam: 14400.00, odenenTutar: 0, kalanTutar: 14400.00, odemeDurumu: 'ODENMEDI' },
      { id: 3, faturaNumarasi: 'FTR-2026-000526', cariHesapAd: 'Delta İnşaat Ltd.', tarih: '2026-02-19', tur: 'ALIS', durum: 'IPTAL', araToplam: 30000.00, kdv: 6000.00, genelToplam: 36000.00, odemeDurumu: 'ODENDI' }
    ]
    cy.intercept('GET', '/api/faturalar*', { statusCode: 200, body: faturalar }).as('faturalar')
    cy.intercept('GET', '/api/hareketler*', { statusCode: 200, body: [
      { id: 1, cariHesapAd: uzunCari, tur: 'TAHSILAT', tutar: 50000.00, hareketTarihi: '2026-02-21', aciklama: 'Banka havalesi ile tahsilat yapıldı', odemeSekli: 'HAVALE' },
      { id: 2, cariHesapAd: 'Mavi Dağıtım San.', tur: 'ODEME', tutar: 22500.00, hareketTarihi: '2026-02-20', aciklama: 'Tedarikçi ödemesi', odemeSekli: 'NAKIT' }
    ] }).as('hareketler')
    cy.intercept('GET', '/api/siparisler*', { statusCode: 200, body: [
      { id: 1, siparisNo: 'SPR-2026-000145', cariHesapAd: uzunCari, tarih: '2026-02-21', durum: 'ONAYLANDI', genelToplam: 84200.00, kalemSayisi: 12 },
      { id: 2, siparisNo: 'SPR-2026-000146', cariHesapAd: 'Mavi Dağıtım San.', tarih: '2026-02-20', durum: 'TASLAK', genelToplam: 14400.00, kalemSayisi: 3 }
    ] }).as('siparisler')
    cy.intercept('GET', '/api/personel*', { statusCode: 200, body: [
      { id: 1, ad: 'Ahmet', soyad: 'Yılmaz', tcKimlik: '12345678901', departman: 'Muhasebe', pozisyon: 'Genel Müdür Yardımcısı', maas: 85000, telefon: '0532 555 0101', email: 'ahmet.yilmaz@raspel.com', aktif: true },
      { id: 2, ad: 'Zeynep', soyad: 'Çelik', tcKimlik: '98765432101', departman: 'İnsan Kaynakları', pozisyon: 'İK Uzmanı', maas: 45000, telefon: '0533 555 0102', aktif: true }
    ] }).as('personel')
    cy.intercept('GET', '/api/kasalar*', { statusCode: 200, body: [
      { id: 1, ad: 'Ana Kasa - Merkez Şube', bakiye: 380450.00 },
      { id: 2, ad: 'Saha Satış Kasası', bakiye: 12500.00 }
    ] }).as('kasalar')
    cy.intercept('GET', '/api/bankalar*', { statusCode: 200, body: [
      { id: 1, ad: 'Garanti Bankası Ticari Hesap', iban: 'TR33 0006 2000 1234 5678 9012 34', bakiye: 850000.00 },
      { id: 2, ad: 'İş Bankası Kobi Hesabı', iban: 'TR64 0006 4000 1234 5678 9012 34', bakiye: 400000.00 }
    ] }).as('bankalar')
    cy.intercept('GET', '/api/tekliifler*', { statusCode: 200, body: [] }).as('teklifler')
    cy.intercept('GET', '/api/teklifler*', { statusCode: 200, body: [
      { id: 1, teklifNo: 'TKF-2026-000089', cariHesapAd: uzunCari, tarih: '2026-02-21', durum: 'ONAY_BEKLIYOR', genelToplam: 125000.00 },
      { id: 2, teklifNo: 'TKF-2026-000090', cariHesapAd: 'Mavi Dağıtım San.', tarih: '2026-02-19', durum: 'TASLAK', genelToplam: 22000.00 }
    ] }).as('teklifler')
    cy.intercept('GET', '/api/irsaliyeler*', { statusCode: 200, body: [
      { id: 1, irsaliyeNo: 'IRS-2026-000178', cariHesapAd: uzunCari, tarih: '2026-02-21', durum: 'KESILDI', genelToplam: 84200.00 },
      { id: 2, irsaliyeNo: 'IRS-2026-000179', cariHesapAd: 'Mavi Dağıtım San.', tarih: '2026-02-20', durum: 'TASLAK', genelToplam: 14400.00 }
    ] }).as('irsaliyeler')
    cy.intercept('GET', '/api/cek-senet*', { statusCode: 200, body: [
      { id: 1, tur: 'CEK', cariHesapAd: uzunCari, tutar: 45000.00, vadeTarihi: '2026-03-15', durum: 'PORTFODYDE' }
    ] }).as('ceksenet')
    cy.intercept('GET', '/api/subeler*', { statusCode: 200, body: [{ id: 1, ad: 'İstanbul Merkez Şubesi', sirketId: 1, aktif: true }] }).as('subeler')
    cy.intercept('GET', '/api/depolar*', { statusCode: 200, body: [{ id: 1, ad: 'Ana Depo - İkitelli', subeId: 1, sirketId: 1 }] }).as('depolar')
    cy.intercept('GET', '/api/donemler*', { statusCode: 200, body: [{ id: 1, ad: '2026 Dönemi', aktif: true }] }).as('donemler')
    cy.intercept('GET', '/api/projeler*', { statusCode: 200, body: [{ id: 1, ad: 'Yeni Nesil ERP Geçiş Projesi', durum: 'DEVAM_EDIYOR' }] }).as('projeler')
    cy.intercept('GET', '/api/masraflar*', { statusCode: 200, body: [
      { id: 1, baslik: 'Saha Müşteri Ziyareti Yakıt Masrafı', personelAdi: 'Ali Yıldız', tutar: 1450.00, kategori: 'YAKIT', tarih: '2026-02-21', durum: 'BEKLEMEDE' }
    ] }).as('masraflar')
    cy.intercept('GET', '/api/butceler*', { statusCode: 200, body: [{ id: 1, yil: 2026, ay: 2, tutar: 500000.00, tur: 'GIDER' }] }).as('butceler')
    cy.intercept('GET', '/api/iadeler*', { statusCode: 200, body: [
      { id: 1, faturaId: 1, tur: 'SATIS', tarih: '2026-02-21', tutar: 8400.00, durum: 'TAMAMLANDI', cariHesapAd: uzunCari }
    ] }).as('iadeler')
    cy.intercept('GET', '/api/stok-sayim*', { statusCode: 200, body: [
      { id: 1, stokAdi: uzunAd, beklenenMiktar: 120, sayilanMiktar: 118, fark: -2, durum: 'TAMAMLANDI', tarih: '2026-02-21' }
    ] }).as('stoksayim')
    cy.intercept('GET', '/api/stok-seriler*', { statusCode: 200, body: [
      { id: 1, stokAdi: uzunAd, seriNo: 'SN-2026-000001-ULUSLARARASI', lotNo: 'LOT-2026-01', sonKullanmaTarihi: '2027-01-01' }
    ] }).as('stokseri')
    cy.intercept('GET', '/api/kullanicilar*', { statusCode: 200, body: [
      { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN', email: 'admin@raspel.com', aktif: true },
      { id: 2, username: 'muhasebe', displayName: 'Zeynep Çelik', role: 'USER', email: 'muhasebe@raspel.com', aktif: true }
    ] }).as('kullanicilar')
    cy.intercept('GET', '/api/audit-log*', { statusCode: 200, body: [
      { id: 1, kullaniciAdi: 'admin', islem: 'Fatura oluşturdu', entityAdi: 'Fatura', entityId: 524, tarih: '2026-02-21T10:30:00', detay: 'FTR-2026-000524 numaralı fatura oluşturuldu' }
    ] }).as('auditlog')
    cy.intercept('GET', '/api/raporlar*', { statusCode: 200, body: [] }).as('raporlar')
    cy.intercept('GET', '/api/ai-config', { statusCode: 200, body: { provider: 'OPENAI', model: 'gpt-4o', aktif: true, durum: 'AKTIF' } }).as('aiConfig')
    cy.intercept('GET', '/api/sistem/durum', { statusCode: 200, body: { durum: 'OK' } }).as('sistemDurum')
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} }).as('catchAllPost')

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN', avatarUrl: null },
        token: 'mock', companyName: 'RasPel Teknoloji A.Ş.', sirketId: 1, sirketAdi: 'RasPel Teknoloji A.Ş.', yetkiler: ['ADMIN']
      }))
      win.localStorage.setItem('raspel_erp_theme', 'light')
      win.localStorage.setItem('raspel_primary_color', '#3b82f6')
    })
  })

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

  viewports.forEach((vp) => {
    sayfalar.forEach(([ad, yol]) => {
      it(`${ad} @${vp.name}`, () => {
        cy.viewport(vp.width, vp.height)
        cy.visit(yol)
        cy.wait(1800)
        cy.get('body').then(($body) => {
          if ($body.find('.p-dialog-mask').length > 0) {
            cy.get('body').type('{esc}')
            cy.wait(400)
          }
        })
        cy.screenshot(`audit/${vp.name}/${ad}`, { overwrite: true, capture: 'viewport' })
      })
    })
  })

  it('giris @all', () => {
    cy.window().then((win) => win.localStorage.removeItem('raspel_erp_auth'))
    viewports.forEach((vp) => {
      cy.viewport(vp.width, vp.height)
      cy.visit('/giris')
      cy.wait(1200)
      cy.screenshot(`audit/${vp.name}/giris`, { overwrite: true, capture: 'viewport' })
    })
  })
})
