/**
 * Termal fis / yazdirma penceresi yardimcilari.
 *
 * HizliSatis ve Satis ekranlarinda ayni "pencere ac -> HTML yaz -> yazdir"
 * akisi kopyalanmisti; tek noktadan yonetilir.
 */

/**
 * Verilen HTML'i yeni bir pencerede acar ve kisa bir gecikmeyle yazdirir.
 * Popup engellenmisse null doner (cagiran taraf kullaniciyi bilgilendirmelidir).
 *
 * @param {string} html Yazdirilacak tam HTML dokumani
 * @param {{ genislik?: number, yukseklik?: number, gecikme?: number }} [secenekler]
 * @returns {Window|null}
 */
export function fisPenceresiAcVeYazdir(html, secenekler = {}) {
  const { genislik = 400, yukseklik = 600, gecikme = 300 } = secenekler
  const pencere = window.open('', '_blank', `width=${genislik},height=${yukseklik}`)
  if (!pencere) return null

  pencere.document.open()
  pencere.document.write(html)
  pencere.document.close()

  // Görselleri (özellikle uzaktaki şirket logosu) yüklenmeden yazdırırsak
  // logo boş çıkar ve düzen kayar. Sabit gecikme yerine görselleri bekle.
  const yazdir = () => {
    try {
      pencere.focus()
      pencere.print()
    } catch (e) {
      console.error('Termal yazıcı hatası:', e)
    }
  }
  const resimleriBekleVeYazdir = () => {
    const doc = pencere.document
    const resimler = doc && doc.images ? Array.from(doc.images) : []
    const bekleyen = resimler.filter((img) => img && img.complete === false)
    if (!bekleyen.length) {
      yazdir()
      return
    }
    let kalan = bekleyen.length
    const azalt = () => {
      kalan -= 1
      if (kalan <= 0) yazdir()
    }
    bekleyen.forEach((img) => {
      img.addEventListener?.('load', azalt)
      img.addEventListener?.('error', azalt)
    })
    // Güvenlik: görsel takılırsa yazdırmayı askıya alma.
    setTimeout(yazdir, Math.max(gecikme, 1500))
  }

  setTimeout(resimleriBekleVeYazdir, gecikme)

  return pencere
}
