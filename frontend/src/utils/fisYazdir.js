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

  setTimeout(() => {
    try {
      pencere.focus()
      pencere.print()
    } catch (e) {
      console.error('Termal yazıcı hatası:', e)
    }
  }, gecikme)

  return pencere
}
