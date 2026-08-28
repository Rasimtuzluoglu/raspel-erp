/* Dialog/form denetimi: dialog aç, taşma kontrolü yap */
describe('Dialog Layout Audit', () => {
  const viewports = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'mobile', width: 390, height: 844 }
  ]

  const dialoglar = [
    ['faturalar', '/faturalar', 'Yeni Fatura'],
    ['cari-hesaplar', '/cari-hesaplar', 'İlk Cari Hesabı Ekle'],
    ['stoklar', '/stoklar', 'İlk Ürünü Ekle'],
    ['kullanicilar', '/kullanicilar', 'Yeni Kullanıcı'],
    ['personel', '/personel', 'Yeni Personel'],
    ['depolar', '/depolar', 'Yeni Depo'],
    ['siparisler', '/siparisler', 'Yeni Teklif'],
    ['kasa', '/kasa', 'Yeni Kasa'],
    ['masraflar', '/masraflar', 'Yeni Masraf'],
    ['donemler', '/donemler', 'Yeni Dönem'],
    ['projeler', '/projeler', 'Yeni Proje'],
    ['kategoriler', '/kategoriler', 'Yeni Kategori']
  ]

  beforeEach(() => {
    cy.intercept('POST', '/api/kullanicilar/giris', { statusCode: 200, body: { id: 1, username: 'admin', role: 'ADMIN', token: 'mock', sirketId: 1, sirketAdi: 'RasPel' } })
    cy.intercept('GET', '/api/kullanicilar/ben', { statusCode: 200, body: { id: 1, username: 'admin', role: 'ADMIN', sirketId: 1, companyName: 'RasPel' } })
    cy.intercept('GET', '/api/sirketler/aktif', { statusCode: 200, body: [{ id: 1, ad: 'RasPel', aktif: true }] })
    cy.intercept('GET', '/api/yetkiler/roller', { statusCode: 200, body: ['ADMIN'] })
    cy.intercept('GET', '/api/yetkiler/moduller', { statusCode: 200, body: ['CARI', 'FATURA', 'STOK'] })
    cy.intercept('GET', '/api/yetkiler', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/doviz', { statusCode: 200, body: [] })
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} })

    cy.window().then((win) => {
      win.localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: { id: 1, username: 'admin', displayName: 'Ahmet', role: 'ADMIN' },
        token: 'mock', companyName: 'RasPel', sirketId: 1, sirketAdi: 'RasPel', yetkiler: ['ADMIN']
      }))
      win.localStorage.setItem('raspel_erp_theme', 'light')
      win.localStorage.setItem('raspel_primary_color', '#3b82f6')
    })
  })

  const rapor = {}

  viewports.forEach((vp) => {
    dialoglar.forEach(([ad, yol, buton]) => {
      it(`dialog ${ad} @${vp.name}`, () => {
        cy.viewport(vp.width, vp.height)
        cy.visit(yol)
        cy.wait(1500)
        cy.get('body').then(($body) => {
          if ($body.find('.p-dialog-mask').length > 0) cy.get('body').type('{esc}')
        })
        cy.contains('button', buton, { timeout: 10000 }).first().click({ force: true })
        cy.wait(800)
        cy.window().then((win) => {
          const vw = win.innerWidth
          const bulgular = []
          const doc = win.document
          const dlg = doc.querySelector('.p-dialog')
          if (!dlg) {
            bulgular.push({ tip: 'dialog-acilmadi', buton })
          } else {
            const r = dlg.getBoundingClientRect()
            if (r.width > vw + 2) bulgular.push({ tip: 'dialog-viewport-tasması', width: Math.round(r.width), viewport: vw })
            if (r.left < -1 || r.right > vw + 2) bulgular.push({ tip: 'dialog-konum-tasması', left: Math.round(r.left), right: Math.round(r.right) })
            if (doc.documentElement.scrollWidth > vw + 2) bulgular.push({ tip: 'document-yatay-tasma', scrollWidth: doc.documentElement.scrollWidth })

            const icerik = dlg.querySelector('.p-dialog-content')
            if (icerik && icerik.scrollWidth > icerik.clientWidth + 2) {
              bulgular.push({ tip: 'dialog-icerik-yatay-tasma', icerikWidth: icerik.clientWidth, scrollWidth: icerik.scrollWidth })
            }
            const tasanlar = []
            dlg.querySelectorAll('input, button, .p-component').forEach((el) => {
              const er = el.getBoundingClientRect()
              const dr = dlg.getBoundingClientRect()
              if (er.width < 20) return
              if (er.right > dr.right + 2 || er.left < dr.left - 2) {
                const cls = (typeof el.className === 'string' ? el.className : '').split(' ').slice(0, 3).join('.')
                tasanlar.push({ tag: el.tagName.toLowerCase(), cls: cls || el.getAttribute('placeholder') || '', right: Math.round(er.right), dlgRight: Math.round(dr.right) })
              }
            })
            if (tasanlar.length) bulgular.push({ tip: 'dialog-icinde-tasan-elemanlar', ornekler: tasanlar.slice(0, 6) })
          }
          rapor[`${vp.name}/${ad}`] = bulgular
        })
      })
    })
  })

  after(() => {
    cy.writeFile('cypress/dialog-audit.json', rapor)
  })
})
