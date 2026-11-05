import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import DashboardNotlarDrawer from '../dashboard/DashboardNotlarDrawer.vue'
import { MAX_NOT, notlarAnahtari, notlarOku } from '../../utils/dashboardNotlar.js'

const mountEt = (props = {}, options = {}) =>
  mount(DashboardNotlarDrawer, {
    props: { visible: true, kullaniciId: 7, ...props },
    global: { plugins: [i18n] },
    ...options
  })

const notEkle = async (wrapper, metin) => {
  const girdi = wrapper.find('[data-test="not-girdi"]')
  await girdi.setValue(metin)
  await girdi.trigger('keydown.enter')
  await nextTick()
}

describe('DashboardNotlarDrawer — ekle / tamamla / sil', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('boş durumda boş mesaj gösterir', () => {
    const wrapper = mountEt()
    expect(wrapper.find('[data-test="notlar-bos"]').text()).toContain(
      i18n.global.t('dashboard.notlarBos')
    )
    expect(wrapper.find('[data-test="not-liste"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('Enter ile not ekler ve kullanıcı anahtarına yazar', async () => {
    const wrapper = mountEt()
    await notEkle(wrapper, 'Tedarikçiyi ara')

    const ogeler = wrapper.findAll('.notlar-oge')
    expect(ogeler).toHaveLength(1)
    expect(ogeler[0].text()).toContain('Tedarikçiyi ara')
    expect(wrapper.find('[data-test="not-girdi"]').element.value).toBe('')

    const kayit = JSON.parse(localStorage.getItem(notlarAnahtari(7)))
    expect(kayit).toHaveLength(1)
    expect(kayit[0]).toMatchObject({ metin: 'Tedarikçiyi ara', tamam: false })
    expect(kayit[0].id).toBeTruthy()
    expect(kayit[0].tarih).toBeTruthy()
    wrapper.unmount()
  })

  it('notu tamamlar ve açık sayaç rozetini günceller', async () => {
    const wrapper = mountEt()
    await notEkle(wrapper, 'Banka mutabakatı')
    expect(wrapper.find('[data-test="not-sayac"]').text()).toContain('1')

    await wrapper.find('[data-test="not-tamam"]').setValue(true)
    await nextTick()

    expect(wrapper.findAll('.notlar-oge--tamam')).toHaveLength(1)
    expect(wrapper.find('[data-test="not-sayac"]').text()).toContain('0')
    const kayit = JSON.parse(localStorage.getItem(notlarAnahtari(7)))
    expect(kayit[0].tamam).toBe(true)
    wrapper.unmount()
  })

  it('notu siler ve depodan kaldırır', async () => {
    const wrapper = mountEt()
    await notEkle(wrapper, 'Silinecek not')

    await wrapper.find('[data-test="not-sil"]').trigger('click')
    await nextTick()

    expect(wrapper.find('.notlar-oge').exists()).toBe(false)
    expect(wrapper.find('[data-test="notlar-bos"]').exists()).toBe(true)
    expect(notlarOku(7)).toEqual([])
    wrapper.unmount()
  })

  it('tamamlananları toplu temizler, açık notları korur', async () => {
    const wrapper = mountEt()
    await notEkle(wrapper, 'Birinci')
    await notEkle(wrapper, 'İkinci')

    // Yeni not başa eklendiği için ilk satır "İkinci"dir.
    await wrapper.findAll('[data-test="not-tamam"]')[0].setValue(true)
    await nextTick()
    expect(wrapper.findAll('.notlar-oge')).toHaveLength(2)

    await wrapper.find('[data-test="not-temizle"]').trigger('click')
    await nextTick()

    const kalan = wrapper.findAll('.notlar-oge')
    expect(kalan).toHaveLength(1)
    expect(kalan[0].text()).toContain('Birinci')
    expect(notlarOku(7)).toHaveLength(1)
    wrapper.unmount()
  })

  it('boş metni reddeder ve kayıt oluşturmaz', async () => {
    const wrapper = mountEt()
    const girdi = wrapper.find('[data-test="not-girdi"]')
    await girdi.setValue('   ')
    await girdi.trigger('keydown.enter')
    await nextTick()

    expect(wrapper.find('[data-test="not-hata"]').text()).toBe(
      i18n.global.t('dashboard.notlarBosHata')
    )
    expect(wrapper.find('.notlar-oge').exists()).toBe(false)
    expect(localStorage.getItem(notlarAnahtari(7))).toBeNull()
    wrapper.unmount()
  })

  it('50 not sınırını aşmaz', async () => {
    const dolu = Array.from({ length: MAX_NOT }, (_, i) => ({
      id: `n-${i}`,
      metin: `Not ${i}`,
      tamam: false,
      tarih: '2026-11-05T08:00:00.000Z'
    }))
    localStorage.setItem(notlarAnahtari(7), JSON.stringify(dolu))

    const wrapper = mountEt()
    expect(wrapper.findAll('.notlar-oge')).toHaveLength(MAX_NOT)

    await notEkle(wrapper, 'Fazla not')
    expect(wrapper.find('[data-test="not-hata"]').text()).toBe(
      i18n.global.t('dashboard.notlarLimitHata', { limit: MAX_NOT })
    )
    expect(wrapper.findAll('.notlar-oge')).toHaveLength(MAX_NOT)
    expect(notlarOku(7)).toHaveLength(MAX_NOT)
    wrapper.unmount()
  })
})

describe('DashboardNotlarDrawer — kalıcılık ve klavye', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('notlar kapatıp yeniden açıldığında korunur', async () => {
    const ilk = mountEt()
    await notEkle(ilk, 'Kalıcı not')
    ilk.unmount()

    const ikinci = mountEt()
    expect(ikinci.find('.notlar-oge').text()).toContain('Kalıcı not')
    ikinci.unmount()
  })

  it('Escape kapatma olayı yayar ve odak geri döner', async () => {
    const tetik = document.createElement('button')
    document.body.appendChild(tetik)
    tetik.focus()

    const wrapper = mountEt({}, { attachTo: document.body })
    await nextTick()
    expect(document.activeElement).toBe(wrapper.find('[data-test="not-girdi"]').element)

    await wrapper.find('[data-test="notlar-drawer"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('update:visible')).toEqual([[false]])

    await wrapper.setProps({ visible: false })
    await nextTick()
    expect(document.activeElement).toBe(tetik)

    wrapper.unmount()
    tetik.remove()
  })

  it('farklı kullanıcı kimliği ayrı depo anahtarı kullanır', async () => {
    const wrapper = mountEt({ kullaniciId: 7 })
    await notEkle(wrapper, 'Kullanıcı 7 notu')
    wrapper.unmount()

    const diger = mountEt({ kullaniciId: 9 })
    expect(diger.find('[data-test="notlar-bos"]').exists()).toBe(true)
    diger.unmount()

    expect(localStorage.getItem(notlarAnahtari(7))).toBeTruthy()
    expect(localStorage.getItem(notlarAnahtari(9))).toBeNull()
  })

  it('kullanıcı kimliği yoksa genel anahtara yazar', async () => {
    const wrapper = mountEt({ kullaniciId: null })
    await notEkle(wrapper, 'Genel not')
    expect(localStorage.getItem('raspel_dashboard_notlar_genel')).toBeTruthy()
    wrapper.unmount()
  })
})
