/* Login ve kurulum ekranı görsel denetimi */
describe('Giris Layout Audit', () => {
  const viewports = [
    { name: 'w1920', width: 1920, height: 1080 },
    { name: 'w1440', width: 1440, height: 900 },
    { name: 'w1366', width: 1366, height: 768 },
    { name: 'w1280', width: 1280, height: 800 },
    { name: 'w1024', width: 1024, height: 768 },
    { name: 'w768', width: 768, height: 1024 },
    { name: 'w390', width: 390, height: 844 },
    { name: 'w375', width: 375, height: 812 }
  ]

  beforeEach(() => {
    cy.intercept('GET', '/api/**', { statusCode: 200, body: [] })
    cy.intercept('POST', '/api/**', { statusCode: 200, body: {} })
    cy.window().then((win) => win.localStorage.removeItem('raspel_erp_auth'))
  })

  const rapor = {}
  viewports.forEach((vp) => {
    it(`giris @${vp.name}`, () => {
      cy.viewport(vp.width, vp.height)
      cy.visit('/giris')
      cy.wait(1500)
      cy.window().then((win) => {
        const vw = win.innerWidth
        const b = []
        const doc = win.document
        if (doc.documentElement.scrollWidth > vw + 2) {
          b.push({ tip: 'document-yatay-tasma', scrollWidth: doc.documentElement.scrollWidth, viewport: vw })
        }
        doc.querySelectorAll('input, button').forEach((el) => {
          const r = el.getBoundingClientRect()
          if (r.width > 20 && (r.left < -1 || r.right > vw + 2)) {
            b.push({ tip: 'eleman-tasması', cls: (el.className || '').toString().slice(0, 40), left: Math.round(r.left), right: Math.round(r.right) })
          }
        })
        const form = doc.querySelector('.giris-form, .login-card, form')
        if (form) {
          const fr = form.getBoundingClientRect()
          if (fr.height > vp.height + 2) b.push({ tip: 'form-viewport-daha-yuksek', formHeight: Math.round(fr.height), viewport: vp.height })
        }
        rapor[`giris/${vp.name}`] = b
      })
    })
  })
  after(() => cy.writeFile('cypress/giris-audit.json', rapor))
})
