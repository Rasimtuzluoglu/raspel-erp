/**
 * REDTEAM/Faz1.2: Taksit odeme penceresi icin i18n anahtarlarini ekler.
 * JSON-aware calisir (metin regex'i JSON'i bozuyordu).
 * Kullanim: node scripts/redteam/i18n-taksit-odeme.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..')

const EKLENECEK = {
  tr: {
    odemeBaslik: 'Taksit Ödemesi',
    odemeYontemi: 'Ödeme Yöntemi',
    kasa: 'Kasa',
    kasaSec: 'Kasa seçin',
    banka: 'Banka',
    bankaSec: 'Banka seçin',
    odemeTarihi: 'Ödeme Tarihi',
    vadeTarihi: 'Vade Tarihi',
    tahsilatKaydet: 'Tahsilatı Kaydet',
    tahsilatKaydedildi: 'Tahsilat kaydedildi, taksit ödendi olarak işaretlendi',
    taksitOdemesi: 'Taksit ödemesi',
    kasaZorunlu: 'Nakit ödemede kasa seçmelisiniz',
    bankaZorunlu: 'Kart/Havale ödemede banka seçmelisiniz',
    cariBulunamadi: 'Taksit kaydına bağlı cari hesap bulunamadı',
    vazgec: 'Vazgeç'
  },
  en: {
    odemeBaslik: 'Installment Payment',
    odemeYontemi: 'Payment Method',
    kasa: 'Cash Register',
    kasaSec: 'Select cash register',
    banka: 'Bank',
    bankaSec: 'Select bank',
    odemeTarihi: 'Payment Date',
    vadeTarihi: 'Due Date',
    tahsilatKaydet: 'Record Collection',
    tahsilatKaydedildi: 'Collection recorded, installment marked as paid',
    taksitOdemesi: 'Installment payment',
    kasaZorunlu: 'A cash register must be selected for cash payment',
    bankaZorunlu: 'A bank must be selected for card/transfer payment',
    cariBulunamadi: 'No customer account linked to this installment',
    vazgec: 'Cancel'
  }
}

for (const [locale, yeni] of Object.entries(EKLENECEK)) {
  const yol = resolve(ROOT, 'frontend', 'src', 'locales', `${locale}.json`)
  const ham = readFileSync(yol, 'utf8')
  const json = JSON.parse(ham)

  if (!json.taksitTakvimi) throw new Error(`${locale}: taksitTakvimi bolumu yok`)

  let eklendi = 0
  let gecersiz = 0
  for (const [k, v] of Object.entries(yeni)) {
    if (k in json.taksitTakvimi) {
      gecersiz++
      continue
    }
    json.taksitTakvimi[k] = v
    eklendi++
  }

  // 2 bosluk girintili, son satirda newline
  const yeniIcerik = JSON.stringify(json, null, 2) + '\n'
  writeFileSync(yol, yeniIcerik, 'utf8')
  console.log(`${locale}: ${eklendi} anahtar eklendi, ${gecersiz} zaten vardi, toplam ${Object.keys(json.taksitTakvimi).length}`)
}