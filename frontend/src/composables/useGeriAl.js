import { reactive } from 'vue'

const durum = reactive({
  gorunur: false,
  metin: '',
  veri: null,
  geriYukle: null,
  zamanlayici: null
})

/**
 * Yıkıcı işlem (silme) sonrası "Geri Al" çubuğu gösterir.
 * kullanım: silVeGeriAl({ veri: silinenKayit, metin: 'Cari hesap silindi', geriYukle: () => cariAPI.create(veri) })
 */
export function useGeriAl() {
  const silVeGeriAl = ({ veri, metin = 'Kayıt silindi', geriYukle }) => {
    if (durum.zamanlayici) clearTimeout(durum.zamanlayici)
    durum.veri = veri
    durum.geriYukle = geriYukle
    durum.metin = metin
    durum.gorunur = true
    durum.zamanlayici = setTimeout(() => gizle(), 6000)
  }

  const geriAl = async () => {
    if (durum.geriYukle) {
      try {
        await durum.geriYukle(durum.veri)
      } catch { /* geri alma başarısızsa sessiz geç */ }
    }
    gizle()
  }

  const gizle = () => {
    if (durum.zamanlayici) clearTimeout(durum.zamanlayici)
    durum.gorunur = false
    durum.veri = null
    durum.geriYukle = null
  }

  return { durum, silVeGeriAl, geriAl, gizle }
}
