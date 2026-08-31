describe('Take Screenshots for Documentation', () => {
  beforeEach(() => {
    // Intercept common APIs
    cy.intercept('POST', '/api/kullanicilar/giris', {
      statusCode: 200,
      body: {
        id: 1,
        username: 'admin',
        displayName: 'Ahmet Yılmaz',
        role: 'ADMIN',
        token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6OTk5OTk5OTk5OSwiZXhwIjo5OTk5OTk5OTk5fQ.mock',
        sirketId: 1,
        sirketAdi: 'RasPel Teknoloji A.Ş.'
      }
    }).as('login')

    cy.intercept('GET', '/api/kullanicilar/ben', {
      statusCode: 200,
      body: {
        id: 1,
        username: 'admin',
        displayName: 'Ahmet Yılmaz',
        role: 'ADMIN',
        sirketId: 1,
        companyName: 'RasPel Teknoloji A.Ş.'
      }
    }).as('ben')

    cy.intercept('GET', '/api/sirketler/aktif', {
      statusCode: 200,
      body: [{ id: 1, ad: 'RasPel Teknoloji A.Ş.', aktif: true }]
    }).as('sirketler')

    cy.intercept('GET', '/api/doviz', {
      statusCode: 200,
      body: [
        { kod: 'USD', alis: 36.45, satis: 36.52, degisim: 0.15 },
        { kod: 'EUR', alis: 38.10, satis: 38.22, degisim: -0.08 },
        { kod: 'GBP', alis: 45.80, satis: 45.95, degisim: 0.25 }
      ]
    }).as('doviz')

    cy.intercept('GET', '/api/yetkiler/roller', {
      statusCode: 200,
      body: ['ADMIN', 'MUHASEBE', 'SATIS', 'DEPO', 'PERSONEL']
    }).as('roller')

    cy.intercept('GET', '/api/yetkiler/moduller', {
      statusCode: 200,
      body: ['CARI', 'FATURA', 'STOK', 'BANKA', 'KASA', 'SIPARIS', 'SATINALMA', 'PERSONEL', 'MUHASEBE', 'RAPOR']
    }).as('moduller')

    cy.intercept('GET', '/api/yetkiler', {
      statusCode: 200,
      body: [
        { rol: 'ADMIN', modul: 'FATURA', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'FATURA', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'SATIS', modul: 'FATURA', okuma: true, yazma: true, silme: false, disaAktarim: false },
        { rol: 'DEPO', modul: 'FATURA', okuma: true, yazma: false, silme: false, disaAktarim: false },
        { rol: 'PERSONEL', modul: 'FATURA', okuma: false, yazma: false, silme: false, disaAktarim: false },
        { rol: 'ADMIN', modul: 'CARI', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'CARI', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'SATIS', modul: 'CARI', okuma: true, yazma: false, silme: false, disaAktarim: false },
        { rol: 'DEPO', modul: 'CARI', okuma: false, yazma: false, silme: false, disaAktarim: false },
        { rol: 'PERSONEL', modul: 'CARI', okuma: false, yazma: false, silme: false, disaAktarim: false },
        { rol: 'ADMIN', modul: 'STOK', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'DEPO', modul: 'STOK', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'SATIS', modul: 'STOK', okuma: true, yazma: false, silme: false, disaAktarim: false },
        { rol: 'ADMIN', modul: 'BANKA', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'BANKA', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'ADMIN', modul: 'KASA', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'KASA', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'ADMIN', modul: 'SIPARIS', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'SATIS', modul: 'SIPARIS', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'ADMIN', modul: 'SATINALMA', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'SATINALMA', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'ADMIN', modul: 'PERSONEL', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'PERSONEL', modul: 'PERSONEL', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'ADMIN', modul: 'MUHASEBE', okuma: true, yazma: true, silme: true, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'MUHASEBE', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'ADMIN', modul: 'RAPOR', okuma: true, yazma: true, silme: false, disaAktarim: true },
        { rol: 'MUHASEBE', modul: 'RAPOR', okuma: true, yazma: false, silme: false, disaAktarim: true },
        { rol: 'SATIS', modul: 'RAPOR', okuma: true, yazma: false, silme: false, disaAktarim: false }
      ]
    }).as('yetkiler')

    cy.intercept('GET', '/api/dashboard', {
      statusCode: 200,
      body: {
        toplamCariSayisi: 148,
        toplamBakiye: 1845200.50,
        pozitifBakiye: 2450000.00,
        negatifBakiye: -604799.50,
        toplamFatura: 524,
        kesilenFatura: 489,
        toplamBankaBakiye: 1250000.00,
        toplamKasaBakiye: 380450.00,
        toplamStok: 840,
        kritikStokSayisi: 6,
        bugunkuSiparis: 14,
        bekleyenTeslimat: 5,
        iadeOrani: 1.2,
        stokDevirHizi: 4.8,
        aktifCalisan: 32,
        bugunIzinli: 2,
        buAyIseBaslayacak: 1,
        bugunkuTahsilat: 94500.00,
        bugunkuOdeme: 32100.00,
        aylikGelirGider: [
          { ay: 'Eyl', gelir: 450000, gider: 310000 },
          { ay: 'Eki', gelir: 520000, gider: 360000 },
          { ay: 'Kas', gelir: 580000, gider: 410000 },
          { ay: 'Ara', gelir: 690000, gider: 450000 },
          { ay: 'Oca', gelir: 640000, gider: 420000 },
          { ay: 'Şub', gelir: 780000, gider: 490000 }
        ],
        vadesiGecenFaturalar: [
          { faturaId: 101, faturaNumarasi: 'FTR-2026-000412', cariHesapAd: 'Atlas Lojistik A.Ş.', cariTelefon: '0212 555 0192', vadeTarihi: '2026-02-10', kalanTutar: 48500.00 },
          { faturaId: 102, faturaNumarasi: 'FTR-2026-000418', cariHesapAd: 'Delta İnşaat Ltd.', cariTelefon: '0216 444 0122', vadeTarihi: '2026-02-14', kalanTutar: 72300.00 }
        ],
        vadesiYaklasanFaturalar: [
          { faturaId: 103, faturaNumarasi: 'FTR-2026-000489', cariHesapAd: 'Mavi Dağıtım San.', cariTelefon: '0312 222 0188', vadeTarihi: '2026-02-25', kalanTutar: 115000.00 }
        ],
        sonHareketler: [
          { id: 1, tur: 'TAHSILAT', aciklama: 'Banka Havalesi - Atlas Lojistik', tutar: 50000, tarih: '2026-02-21' },
          { id: 2, tur: 'SATIS_FATURASI', aciklama: 'Fatura #FTR-2026-000524 Kesildi', tutar: 84200, tarih: '2026-02-21' },
          { id: 3, tur: 'ODEME', aciklama: 'Tedarikçi Ödemesi - Ege Kimya', tutar: 22500, tarih: '2026-02-20' }
        ],
        enCokSatanlar: [
          { stokAdi: 'Ultra Dayanıklı Endüstriyel Palet', toplam: 320 },
          { stokAdi: 'Streç Film 50cm x 300m', toplam: 245 },
          { stokAdi: 'Koli Bandı Şeffaf 45x100', toplam: 180 }
        ]
      }
    }).as('dashboard')

    cy.intercept('GET', '/api/yonetici/kokpit', {
      statusCode: 200,
      body: {
        aylikCiro: 780000.00,
        ciroHedefi: 850000.00,
        hedefTamamlanmaYuzdesi: 91.8,
        gecenAyaGoreCiroDegisimYuzdesi: 14.5,
        aylikNetKar: 290000.00,
        netKarMarji: 37.2,
        gecenAyaGoreKarDegisimYuzdesi: 8.4,
        toplamLikidite: 1630450.00,
        bankaBakiye: 1250000.00,
        kasaBakiye: 380450.00,
        vadesiGecmisAlacak: 120800.00,
        vadesiGecmisBorc: 45000.00,
        toplamAlacak: 2450000.00,
        toplamBorc: 604799.50,
        cariRiskSkoru: 'DÜŞÜK',
        nakitAkimTahmini30Gun: 420000.00,
        nakitAkimTahmini60Gun: 890000.00,
        enYuksekCiroYapanCariler: [
          { cariId: 1, cariAd: 'Atlas Lojistik A.Ş.', toplamCiro: 240000.00, yuzde: 30.7 },
          { cariId: 2, cariAd: 'Mavi Dağıtım San.', toplamCiro: 185000.00, yuzde: 23.7 },
          { cariId: 3, cariAd: 'Delta İnşaat Ltd.', toplamCiro: 135000.00, yuzde: 17.3 }
        ]
      }
    }).as('kokpit')

    cy.intercept('GET', '/api/stoklar*', {
      statusCode: 200,
      body: [
        { id: 1, ad: 'Ultra Dayanıklı Endüstriyel Palet', stokKodu: 'PLT-001', barkod: '8690123456789', fiyat: 450.00, miktar: 120, kategori: 'Ambalaj' },
        { id: 2, ad: 'Streç Film 50cm x 300m', stokKodu: 'STR-050', barkod: '8690123456790', fiyat: 185.00, miktar: 340, kategori: 'Sarf' },
        { id: 3, ad: 'Koli Bandı Şeffaf 45x100', stokKodu: 'BND-045', barkod: '8690123456791', fiyat: 35.00, miktar: 600, kategori: 'Sarf' },
        { id: 4, ad: 'Çemberleme Tokası Sac', stokKodu: 'TKA-012', barkod: '8690123456792', fiyat: 12.50, miktar: 1500, kategori: 'Bağlantı' }
      ]
    }).as('stoklar')

    cy.intercept('GET', '/api/cari-hesaplar*', {
      statusCode: 200,
      body: [
        { id: 1, ad: 'Atlas Lojistik A.Ş.', bakiye: 240000.00, tur: 'MUSTERI', telefon: '0212 555 0192' },
        { id: 2, ad: 'Mavi Dağıtım San.', bakiye: 185000.00, tur: 'MUSTERI', telefon: '0312 222 0188' },
        { id: 3, ad: 'Delta İnşaat Ltd.', bakiye: -72300.00, tur: 'TEDARIKCI', telefon: '0216 444 0122' }
      ]
    }).as('cariHesaplar')

    cy.intercept('GET', '/api/faturalar*', {
      statusCode: 200,
      body: []
    }).as('faturalar')

    cy.intercept('GET', '/api/bankalar*', {
      statusCode: 200,
      body: []
    }).as('bankalar')

    cy.intercept('GET', '/api/kasalar*', {
      statusCode: 200,
      body: []
    }).as('kasalar')

    cy.intercept('GET', '/api/siparisler*', {
      statusCode: 200,
      body: []
    }).as('siparisler')

    cy.intercept('GET', '/api/personel-izin/bekleyen', {
      statusCode: 200,
      body: [
        { id: 1, personelAdi: 'Mustafa Kaya', izinTuru: 'YILLIK_IZIN', baslangicTarihi: '2026-02-24', bitisTarihi: '2026-02-28', gunSayisi: 5, aciklama: 'Yıllık izin talebi', durum: 'BEKLEMEDE' },
        { id: 2, personelAdi: 'Zeynep Çelik', izinTuru: 'MAZERET_IZNI', baslangicTarihi: '2026-02-23', bitisTarihi: '2026-02-23', gunSayisi: 1, aciklama: 'Resmi daire işlemi', durum: 'BEKLEMEDE' }
      ]
    }).as('izinler')

    cy.intercept('GET', '/api/masraflar*', {
      statusCode: 200,
      body: [
        { id: 1, baslik: 'Saha Müşteri Ziyareti Yakıt', personelAdi: 'Ali Yıldız', tutar: 1450.00, kategori: 'YAKIT', tarih: '2026-02-21', durum: 'BEKLEMEDE' },
        { id: 2, baslik: 'Müşteri Öğle Yemeği Ağırlama', personelAdi: 'Emre Şahin', tutar: 820.00, kategori: 'TEMSIL_AGIRLAMA', tarih: '2026-02-20', durum: 'BEKLEMEDE' }
      ]
    }).as('masraflar')

    cy.intercept('GET', '/api/satinalma*', {
      statusCode: 200,
      body: [
        { id: 1, talepNo: 'SAT-2026-000045', baslik: '100 Adet Ahşap Euro Palet', personelAdi: 'Kemal Ak', toplamTutar: 45000.00, durum: 'ONAY_BEKLIYOR', tarih: '2026-02-21' }
      ]
    }).as('satinalma')

    cy.intercept('GET', '/api/ai-config', {
      statusCode: 200,
      body: { provider: 'OPENAI', model: 'gpt-4o', apiKey: 'sk-proj-****...4f2a', aktif: true, durum: 'AKTIF' }
    }).as('aiConfig')

    // Catch-all for any other API calls to prevent 404s
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] }).as('catchAll')
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} }).as('catchAllPost')

    // Simulate logged in user in LocalStorage
    cy.window().then((win) => {
      win.localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: { id: 1, username: 'admin', displayName: 'Ahmet Yılmaz', role: 'ADMIN', avatarUrl: null },
        token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6OTk5OTk5OTk5OSwiZXhwIjo5OTk5OTk5OTk5fQ.mock',
        companyName: 'RasPel Teknoloji A.Ş.',
        sirketId: 1,
        sirketAdi: 'RasPel Teknoloji A.Ş.',
        yetkiler: ['ADMIN']
      }))
      win.localStorage.setItem('raspel_erp_theme', 'dark')
      win.localStorage.setItem('raspel_primary_color', '#3b82f6')
    })
  })

  it('01 - Dashboard', () => {
    cy.visit('/')
    // Dismiss any modal/dialog overlays that may appear (session warning, company selection etc.)
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        // Try clicking any close/cancel button in the dialog
        const closeBtn = $body.find('.p-dialog-mask .p-dialog-close-button, .p-dialog-mask button')
        if (closeBtn.length > 0) {
          closeBtn.first().trigger('click')
        }
        // Press Escape as fallback
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('01-dashboard', { overwrite: true, capture: 'fullPage' })
  })

  it('02 - Saha Portali', () => {
    cy.visit('/saha-portali')
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('02-saha-portali', { overwrite: true, capture: 'fullPage' })
  })

  it('03 - Yonetici Kokpiti', () => {
    cy.visit('/yonetici-kokpiti')
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('03-yonetici-kokpiti', { overwrite: true, capture: 'fullPage' })
  })

  it('04 - Onay Merkezi', () => {
    cy.visit('/onaylar')
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('04-onay-merkezi', { overwrite: true, capture: 'fullPage' })
  })

  it('05 - Hizli Satis POS', () => {
    cy.visit('/hizli-satis')
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('05-hizli-satis-pos', { overwrite: true, capture: 'fullPage' })
  })

  it('06 - RBAC Yetki Matrisi', () => {
    cy.visit('/yetki-yonetimi')
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('06-rbac-yetki-matrisi', { overwrite: true, capture: 'fullPage' })
  })

  it('07 - AI Sohbet Asistani', () => {
    cy.visit('/sohbet')
    cy.wait(2000)
    cy.get('body').then(($body) => {
      if ($body.find('.p-dialog-mask').length > 0) {
        cy.get('body').type('{esc}')
        cy.wait(500)
      }
    })
    cy.wait(1000)
    cy.screenshot('07-ai-sohbet-asistani', { overwrite: true, capture: 'fullPage' })
  })

  it('08 - Giris Ekrani', () => {
    // Clear auth to show login page
    cy.window().then((win) => {
      win.localStorage.removeItem('raspel_erp_auth')
    })
    cy.visit('/giris')
    cy.wait(2000)
    cy.screenshot('08-giris-ekrani', { overwrite: true, capture: 'fullPage' })
  })
})
