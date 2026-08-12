/**
 * HTML ozel karakterlerini escape ederek XSS saldirilarini onler.
 * document.write / innerHTML / v-html icerisine kullanici verisi
 * gomulen yerlerde kullanilmalidir.
 */
export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
