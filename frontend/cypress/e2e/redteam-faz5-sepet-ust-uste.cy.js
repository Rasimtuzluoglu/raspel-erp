// REDTEAM/Faz5 — POS'ta "malzeme kalemi ekleyince her sey birbirine giriyor".
//
// GERCEK KOK NEDEN (once yanlis varsayimda bulundu, ekran goruntusuyle
// duzeltildi): Cakisan sey sepet satirlari DEGIL, her urun eklemede cikan
// "Urun Eklendi" TOAST bildirimleriydi.
//
//   - `HizliSatis.vue.barkodTarandi` her eklemede `toast.add({severity:'success'})`
//     cagiriyordu. Hizli bir kasada 5-6 kalem eklenince 5-6 bildirim ust uste
//     diziliyordu.
//   - Toast `position="top-right"` (App.vue) ve POS'ta sag panel/sepet sag
//     tarafta oldugu icin bildirimler TUM SEPETI ortuyordu.
//   - Bildirimler ayrica sag kenardan tasip EKRAN DISINA cikiyor ve yatay
//     kaydirma cubugu doguyordu.
//
// Duzeltme: basarili ekleme toast'i kaldirildi. Geri bildirim zaten gorsel:
// satir aninda belirip pulse animasyonu oynuyor (`vurguluId` ->
// `.sepet-item.yeni-satir`) ve urun kartinda adet rozeti guncelleniyor.
// Hata/uyari bildirimleri (stok yok, barkod bulunamadi) KORUNDU.
//
// Ayrica ayni kapsamda flexbox ezme hatasi duzeltildi: kaydirma kabugu flex
// column (`HizliSatis.vue` `.siparis-kart :deep(.p-card-content)`) oldugu icin
// `.pos-bolum` / `.sepet-icerik` / `.sepet-item` ogelerinin `flex-shrink: 0`
// olmamasi icerigi tasmaya yol acabiliyordu.

describe('REDTEAM Faz5 — kalem eklerken sepet okunur kalmali', () => {
  const urunler = Array.from({ length: 8 }, (_, i) => ({
    id: i + 1,
    ad: `Malzeme Kalemi ${i + 1}`,
    stokKodu: `STK-${i + 1}`,
    barkod: `100000000${i}`,
    fiyat: 125.5,
    satisFiyati: 125.5,
    miktar: 50,
    birim: 'Adet',
    kategori: 'Gıda',
    marka: 'Test',
    stokGrubu: 'Mamul'
  }))

  const barkodEkle = (n) => {
    for (let i = 0; i < n; i++) {
      cy.get('input[placeholder="Barkod okutun (Enter)"]').type(`100000000${i}{enter}`)
    }
  }

  beforeEach(() => {
    // 1280'den GENIS viewport sart: `@media (max-width:1280px)` POS'ta viewport
    // kilidini kaldirir, bu yuzden varsayilan 1280'de hata gorunmez.
    cy.viewport(1600, 900)
    cy.girisYap()
    cy.intercept('GET', '**/api/stoklar*', { statusCode: 200, body: { content: urunler } }).as('stoklar')
    cy.intercept('POST', '/api/faturalar', {
      statusCode: 200,
      body: { id: 99, faturaNumarasi: 'FTR-TEST-0001' }
    }).as('satisOlustur')
    cy.intercept('GET', '/api/deliveries/drivers*', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/deliveries*', { statusCode: 200, body: {} })
    cy.intercept('POST', '/api/faturalar/*/yazdirma', { statusCode: 200, body: {} })
    cy.visit('/hizli-satis')
    cy.get('.pos-container').should('exist')
    cy.wait('@stoklar')
  })

  it('birden fazla kalem eklenince sepeti orten bildirim cikmaz', () => {
    // NOT: eski toast'in `life` degeri 2000ms idi. Test, kalem ekledikten sonra
    // beklemeden bakmazsa bildirim suresi dolup KAYBOLUR ve test yanlis
    // olarak gecer. O yuzden once ekle, sonra kisa sure icinde say.
    barkodEkle(3)
    cy.get('.sepet-item').should('have.length', 3)
    cy.get('body').then(($b) => {
      // Eklenen kalem basina "Urun Eklendi" bildirimi cikardi; hicbiri olmamali.
      const basarili = Array.from($b[0].querySelectorAll('.p-toast-message'))
        .filter((m) => (m.textContent || '').includes('Eklendi'))
      expect(basarili.length, 'kalem eklenirken "Eklendi" bildirimi cikiyor').to.eq(0)
    })
  })

  it('bildirimler sepet panelini kaplamaz', () => {
    barkodEkle(5)
    cy.get('.sepet-item').should('have.length', 5)

    cy.get('body').then(($b) => {
      const msgs = Array.from($b[0].querySelectorAll('.p-toast-message'))
      const panel = $b[0].querySelector('.pos-right')
      expect(panel, 'sag panel bulunamadi').to.not.eq(null)
      const pr = panel.getBoundingClientRect()
      const paneleBinen = msgs.filter((m) => {
        const r = m.getBoundingClientRect()
        return r.bottom > pr.top && r.top < pr.bottom && r.right > pr.left && r.left < pr.right
      })
      expect(paneleBinen.length, 'bildirim sepet panelini ortuyor').to.eq(0)
    })
  })

  it('bildirimler ekran disina tasmaz', () => {
    // Genel yerlesim guvenligi (bu regresyonun kendisi degil ama ayni yuzey):
    // bildirimler saga yasli, ekran disina cikarlarsa yatay kaydirma dogar.
    barkodEkle(3)
    cy.get('.sepet-item').should('have.length', 3)
    cy.get('body').then(($b) => {
      const vw = $b[0].ownerDocument.documentElement.clientWidth
      const msgs = Array.from($b[0].querySelectorAll('.p-toast-message'))
      const tasan = msgs.filter((m) => m.getBoundingClientRect().right > vw + 0.5)
      expect(tasan.length, 'bildirim ekran disina tasti').to.eq(0)
    })
  })

  it('kalem eklemek yatay tasma olusturmaz', () => {
    // Once "Urun Eklendi" bildirimleri sag kenardan tasip yatay kaydirma
    // cubugu doguruyordu.
    barkodEkle(5)
    cy.get('.sepet-item').should('have.length', 5)
    cy.get('body').then(($b) => {
      const doc = $b[0].ownerDocument.documentElement
      const tasma = doc.scrollWidth - doc.clientWidth
      expect(tasma, `yatay tasma: ${tasma}px`).to.be.at.most(1)
    })
  })

  it('sepet satirlari birbirine girmez ve ezilmez', () => {
    barkodEkle(6)
    cy.get('.sepet-item').should('have.length', 6)

    cy.get('.sepet-item').then(($satirlar) => {
      const satirlar = Array.from($satirlar)
      const kutular = satirlar.map((el) => el.getBoundingClientRect())

      // Komsu satirlar ust uste binmemeli
      const tasma = kutular.slice(0, -1).filter((k, i) => k.bottom > kutular[i + 1].top + 0.5)
      expect(tasma.length, 'sepet satirlari ust uste biniyor').to.eq(0)

      // Satir kutusu kendi iceriginden kisalmis olmamali (flex-shrink)
      satirlar.forEach((el, i) => {
        const kontrol = el.querySelector('.sepet-kontroller')
        if (!kontrol) return
        const icerik = kontrol.getBoundingClientRect()
        expect(
          kutular[i].height + 1,
          `satir ${i + 1} iceriginden kisa (${Math.round(kutular[i].height)}px < ${Math.round(icerik.height)}px)`
        ).to.be.greaterThan(icerik.height)
      })
    })
  })

  it('toplam cubugu ezilmez ve Satisi Tamamla gorunur kalir', () => {
    barkodEkle(5)
    cy.get('.sepet-item').should('have.length', 5)
    cy.get('.sticky-tamamla').then(($k) => {
      const r = $k[0].getBoundingClientRect()
      expect(r.height, 'toplam cubugu ezilmis').to.be.greaterThan(60)
    })
    cy.contains('.sticky-tamamla button', 'Satışı Tamamla').should('be.visible')
  })

  it('stok yok uyarisi HALEN gosterilir (bildirimleri tamamen kaldirmadik)', () => {
    // Yanlis yere gidilmemis olmak icin: hata bildirimleri korunmali.
    // Barkod bulunamadi -> uyari + "Hizli Urun" dialogu
    cy.get('input[placeholder="Barkod okutun (Enter)"]').type('YOKBARKOD999{enter}')
    cy.get('.p-toast-message', { timeout: 4000 }).should('exist')
  })
})