import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia } from 'pinia'
import i18n from '../../i18n.js'
import DashboardNotlarDrawer from '../dashboard/DashboardNotlarDrawer.vue'

const { notAPIMock } = vi.hoisted(() => ({
  notAPIMock: {
    getAll: vi.fn(),
    getById: vi.fn(),
    cariNotlari: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('../../api/index.js', () => ({ notAPI: notAPIMock }))

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const notlar = [
  {
    id: 1,
    baslik: 'Banka mutabakatı',
    icerik: 'Cari hesap kontrolü',
    onemDerecesi: 'YUKSEK',
    renk: 'MAVI',
    olusturmaTarihi: '2026-11-04T10:00:00'
  },
  {
    id: 2,
    baslik: 'Saha ziyareti',
    icerik: '',
    onemDerecesi: 'NORMAL',
    renk: 'MAVI',
    olusturmaTarihi: '2026-11-03T09:00:00'
  }
]

const sayfa = (content) => ({ data: { content, totalElements: content.length } })

const mountEt = (props = {}, options = {}) =>
  mount(DashboardNotlarDrawer, {
    props: { visible: true, ...props },
    ...options,
    global: {
      plugins: [createPinia(), i18n],
      stubs: { 'router-link': RouterLinkStub },
      ...(options.global || {})
    }
  })

const yukleVeBekle = async (wrapper) => {
  await flushPromises()
  await nextTick()
  return wrapper
}

describe('DashboardNotlarDrawer — /api/notlar', () => {
  beforeEach(() => {
    notAPIMock.getAll.mockReset()
    notAPIMock.create.mockReset()
    notAPIMock.getAll.mockResolvedValue(sayfa(notlar))
    notAPIMock.create.mockResolvedValue({ data: notlar[0] })
  })

  it('açılışta son 10 notu sunucudan ister ve listeler', async () => {
    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    expect(notAPIMock.getAll).toHaveBeenCalledTimes(1)
    expect(notAPIMock.getAll).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: 'olusturmaTarihi,desc'
    })

    const ogeler = wrapper.findAll('.notlar-oge')
    expect(ogeler).toHaveLength(2)
    expect(ogeler[0].text()).toContain('Banka mutabakatı')
    expect(ogeler[0].text()).toContain('Cari hesap kontrolü')
    expect(ogeler[1].text()).toContain('Saha ziyareti')
    // YUKSEK önem rozeti gösterilir; NORMAL gösterilmez.
    expect(ogeler[0].find('[class*="notlar-onem--yuksek"]').exists()).toBe(true)
    expect(ogeler[1].find('[class*="notlar-onem"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('başlık ve açıklamayı gösterir, tümünü gör /notlar adresine gider', async () => {
    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    expect(wrapper.find('#db-notlar-baslik').text()).toBe(i18n.global.t('dashboard.notlarBaslik'))
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.notlarAlt'))

    const tumu = wrapper.find('[data-test="notlar-tumu"]')
    expect(tumu.attributes('href')).toBe('/notlar')
    wrapper.unmount()
  })

  it('en fazla 10 not gösterir', async () => {
    notAPIMock.getAll.mockResolvedValue(
      sayfa(
        Array.from({ length: 14 }, (_, i) => ({
          id: i + 1,
          baslik: `Not ${i + 1}`,
          icerik: '',
          onemDerecesi: 'NORMAL',
          olusturmaTarihi: '2026-11-03T09:00:00'
        }))
      )
    )

    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    expect(wrapper.findAll('.notlar-oge')).toHaveLength(10)
    wrapper.unmount()
  })

  it('kayıt yoksa boş durum gösterir', async () => {
    notAPIMock.getAll.mockResolvedValue(sayfa([]))

    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    expect(wrapper.find('[data-test="notlar-bos"]').text()).toContain(
      i18n.global.t('dashboard.notlarBos')
    )
    expect(wrapper.find('[data-test="not-liste"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('yükleme hatasında yeniden dene sunar ve tekrar ister', async () => {
    notAPIMock.getAll.mockRejectedValueOnce(new Error('ağ hatası'))

    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    const hata = wrapper.find('[data-test="notlar-hata-durum"]')
    expect(hata.exists()).toBe(true)
    expect(hata.text()).toContain(i18n.global.t('dashboard.notlarYuklenemedi'))

    notAPIMock.getAll.mockResolvedValue(sayfa(notlar))
    await hata.find('button').trigger('click')
    await yukleVeBekle(wrapper)

    expect(notAPIMock.getAll).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('.notlar-oge')).toHaveLength(2)
    wrapper.unmount()
  })

  it('yenile düğmesi listeyi yeniden yükler', async () => {
    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    await wrapper.find('[data-test="notlar-yenile"]').trigger('click')
    await yukleVeBekle(wrapper)

    expect(notAPIMock.getAll).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('hızlı ekleme başlık ve içeriği /api/notlar üzerinden kaydeder', async () => {
    notAPIMock.getAll
      .mockResolvedValueOnce(sayfa(notlar))
      .mockResolvedValueOnce(
        sayfa([
          {
            id: 9,
            baslik: 'Yeni not',
            icerik: 'Detay',
            onemDerecesi: 'NORMAL',
            renk: 'MAVI',
            olusturmaTarihi: '2026-11-05T08:00:00'
          },
          ...notlar
        ])
      )

    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    await wrapper.find('[data-test="not-baslik"]').setValue('  Yeni not  ')
    await wrapper.find('[data-test="not-icerik"]').setValue('  Detay  ')
    await wrapper.find('.notlar-ekle').trigger('submit')
    await yukleVeBekle(wrapper)

    expect(notAPIMock.create).toHaveBeenCalledTimes(1)
    expect(notAPIMock.create).toHaveBeenCalledWith({
      baslik: 'Yeni not',
      icerik: 'Detay',
      onemDerecesi: 'NORMAL',
      renk: 'MAVI'
    })

    expect(wrapper.find('[data-test="not-baslik"]').element.value).toBe('')
    expect(wrapper.find('[data-test="not-icerik"]').element.value).toBe('')
    expect(wrapper.findAll('.notlar-oge')).toHaveLength(3)
    expect(wrapper.text()).toContain('Yeni not')
    wrapper.unmount()
  })

  it('Ctrl+Enter ile de kaydeder', async () => {
    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    await wrapper.find('[data-test="not-baslik"]').setValue('Klavye notu')
    await wrapper.find('[data-test="not-icerik"]').trigger('keydown', {
      key: 'Enter',
      ctrlKey: true
    })
    await yukleVeBekle(wrapper)

    expect(notAPIMock.create).toHaveBeenCalledWith(
      expect.objectContaining({ baslik: 'Klavye notu' })
    )
    wrapper.unmount()
  })

  it('boş başlığı reddeder ve istek atmaz', async () => {
    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    await wrapper.find('[data-test="not-baslik"]').setValue('   ')
    await wrapper.find('.notlar-ekle').trigger('submit')
    await nextTick()

    expect(wrapper.find('[data-test="not-hata"]').text()).toBe(
      i18n.global.t('dashboard.notlarBaslikZorunlu')
    )
    expect(notAPIMock.create).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="not-baslik"]').attributes('aria-invalid')).toBe('true')
    wrapper.unmount()
  })

  it('kaydetme hatasında sunucu mesajını gösterir', async () => {
    notAPIMock.create.mockRejectedValueOnce({
      response: { data: { message: 'Başlık zaten kullanılıyor' } }
    })

    const wrapper = mountEt()
    await yukleVeBekle(wrapper)

    await wrapper.find('[data-test="not-baslik"]').setValue('Tekrar eden')
    await wrapper.find('.notlar-ekle').trigger('submit')
    await yukleVeBekle(wrapper)

    expect(wrapper.find('[data-test="not-hata"]').text()).toBe('Başlık zaten kullanılıyor')
    wrapper.unmount()
  })

  it('Escape ile kapanır ve odağı tetikleyiciye döndürür', async () => {
    const tetik = document.createElement('button')
    document.body.appendChild(tetik)
    tetik.focus()

    const wrapper = mountEt({}, { attachTo: document.body })
    await yukleVeBekle(wrapper)
    expect(document.activeElement).toBe(wrapper.find('[data-test="not-baslik"]').element)

    await wrapper.find('[data-test="notlar-drawer"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('update:visible')).toEqual([[false]])

    await wrapper.setProps({ visible: false })
    await nextTick()
    expect(document.activeElement).toBe(tetik)

    wrapper.unmount()
    tetik.remove()
  })

  it('kapalıyken istek atmaz, açıldığında yükler', async () => {
    const wrapper = mountEt({ visible: false })
    await yukleVeBekle(wrapper)
    expect(notAPIMock.getAll).not.toHaveBeenCalled()

    await wrapper.setProps({ visible: true })
    await yukleVeBekle(wrapper)
    expect(notAPIMock.getAll).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})
