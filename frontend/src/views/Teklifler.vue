<template>
  <div class="teklifler-sayfasi">
    <!-- Üst Başlık & İstatistikler -->
    <div class="sayfa-header mb-4">
      <div class="baslik-kutu">
        <h1 class="page-title">
          <i class="pi pi-file-edit text-primary mr-2" />
          Satış Teklifleri & Proforma
        </h1>
        <p class="text-muted">
          Müşterilerinize profesyonel teklifler hazırlayın, revizyonları takip edin ve tek tıkla siparişe veya faturaya dönüştürün.
        </p>
      </div>
      <div class="aksiyon-kutu">
        <Button
          label="Yeni Teklif Oluştur"
          icon="pi pi-plus"
          class="p-button-primary"
          @click="yeniTeklifAc"
        />
      </div>
    </div>

    <!-- Özet İstatistik Kartları -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
      <div class="istatistik-kart kart-toplam">
        <div class="kart-ikon">
          <i class="pi pi-folder" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">Toplam Teklif</span>
          <span class="kart-deger">{{ toplamTeklifSayisi }}</span>
        </div>
      </div>
      <div class="istatistik-kart kart-bekleyen">
        <div class="kart-ikon">
          <i class="pi pi-send" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">Gönderilen / Bekleyen</span>
          <span class="kart-deger">{{ gonderilenTeklifSayisi }}</span>
        </div>
      </div>
      <div class="istatistik-kart kart-onayli">
        <div class="kart-ikon">
          <i class="pi pi-check-circle" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">Onaylanan Teklifler</span>
          <span class="kart-deger">{{ onaylananTeklifSayisi }}</span>
        </div>
      </div>
      <div class="istatistik-kart kart-donusen">
        <div class="kart-ikon">
          <i class="pi pi-sync" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">Dönüşen Toplam Hacim</span>
          <span class="kart-deger">{{ formatCurrency(donusenHacim) }}</span>
        </div>
      </div>
    </div>

    <!-- Teklifler Listesi Tablosu -->
    <div class="kart-kutu">
      <DataTable
        state-storage="session"
        state-key="teklifler-table-state"
        :value="teklifler"
        :loading="yukleniyor"
        paginator
        :rows="10"
        :rows-per-page-options="[10, 25, 50]"
        striped-rows
        responsive-layout="scroll"
        class="p-datatable-sm"
      >
        <template #header>
          <div class="tablo-toolbar flex justify-between items-center">
            <div class="durum-filtre-chips flex gap-2">
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'HEPSI' }]"
                @click="durumFiltrele('HEPSI')"
              >
                Tümü ({{ teklifler.length }})
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'TASLAK' }]"
                @click="durumFiltrele('TASLAK')"
              >
                Taslak
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'GONDERILDI' }]"
                @click="durumFiltrele('GONDERILDI')"
              >
                Gönderildi
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'ONAYLANDI' }]"
                @click="durumFiltrele('ONAYLANDI')"
              >
                Onaylandı
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'SIPARISE_DONUSTU' }]"
                @click="durumFiltrele('SIPARISE_DONUSTU')"
              >
                Siparişe Dönüştü
              </button>
            </div>
            <span class="p-input-icon-left">
              <i class="pi pi-search" />
              <InputText
                v-model="aramaMetni"
                placeholder="Teklif No veya Cari Ara..."
                class="p-inputtext-sm"
              />
            </span>
          </div>
        </template>

        <Column
          field="teklifNo"
          header="Teklif No"
          sortable
        >
          <template #body="{ data }">
            <div class="flex items-center gap-2">
              <span class="font-bold text-primary">{{ data.teklifNo }}</span>
              <span
                v-if="data.revizyonNo > 0"
                class="badge-rev"
              >Rev.{{ data.revizyonNo }}</span>
            </div>
          </template>
        </Column>

        <Column
          field="tarih"
          header="Tarih"
          sortable
        >
          <template #body="{ data }">
            {{ formatDate(data.tarih) }}
          </template>
        </Column>

        <Column
          field="gecerlilikTarihi"
          header="Geçerlilik"
          sortable
        >
          <template #body="{ data }">
            <div v-if="data.gecerlilikTarihi">
              <span>{{ formatDate(data.gecerlilikTarihi) }}</span>
              <small
                v-if="isGecmis(data.gecerlilikTarihi) && data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                class="text-red-500 block"
              >(Süresi Doldu)</small>
            </div>
            <span
              v-else
              class="text-muted"
            >-</span>
          </template>
        </Column>

        <Column
          field="cariHesapAdi"
          header="Müşteri / Cari"
          sortable
        >
          <template #body="{ data }">
            <div class="font-medium">
              {{ data.cariHesapAdi || 'Genel Müşteri' }}
            </div>
            <small
              v-if="data.cariVergiNo"
              class="text-muted"
            >VKN/TC: {{ data.cariVergiNo }}</small>
          </template>
        </Column>

        <Column
          field="kalemler"
          header="Kalem"
        >
          <template #body="{ data }">
            <span class="badge-kalem">{{ data.kalemler?.length || 0 }} Kalem</span>
          </template>
        </Column>

        <Column
          field="genelToplam"
          header="Genel Toplam"
          sortable
        >
          <template #body="{ data }">
            <span class="font-bold text-base text-gray-900 dark:text-gray-100">
              {{ formatCurrency(data.genelToplam) }} {{ data.paraBirimi || 'TRY' }}
            </span>
          </template>
        </Column>

        <Column
          field="durum"
          header="Durum"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="durumLabel(data.durum)"
              :severity="durumSeverity(data.durum)"
            />
          </template>
        </Column>

        <Column
          header="İşlemler"
          class="text-right"
          style="min-width: 220px;"
        >
          <template #body="{ data }">
            <div class="flex justify-end gap-1">
              <!-- Önizle & Mektup / Proforma PDF -->
              <Button
                icon="pi pi-print"
                class="p-button-text p-button-sm p-button-secondary"
                title="Teklif Mektubu & Proforma Önizle / Yazdır"
                @click="onizlemeAc(data)"
              />

              <!-- Revizyon Oluştur -->
              <Button
                icon="pi pi-copy"
                class="p-button-text p-button-sm p-button-info"
                title="Yeni Revizyon Oluştur"
                @click="revizyonOlustur(data)"
              />

              <!-- Siparişe Dönüştür -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-shopping-cart"
                class="p-button-text p-button-sm p-button-success"
                title="Siparişe Dönüştür"
                @click="sipariseDonustur(data)"
              />

              <!-- Faturaya Dönüştür -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-file"
                class="p-button-text p-button-sm p-button-warning"
                title="Faturaya Dönüştür"
                @click="faturayaDonustur(data)"
              />

              <!-- Düzenle -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-pencil"
                class="p-button-text p-button-sm"
                title="Düzenle"
                @click="duzenle(data)"
              />

              <!-- Sil -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-trash"
                class="p-button-text p-button-sm p-button-danger"
                title="Sil"
                @click="silOnay(data)"
              />
            </div>
          </template>
        </Column>

        <template #empty>
          <div class="text-center py-6 text-muted">
            <i class="pi pi-inbox text-4xl mb-2 block text-gray-400" />
            Henüz kayıtlı bir satış teklifi bulunmuyor.
          </div>
        </template>
      </DataTable>
    </div>

    <!-- TEKLİF OLUŞTURMA & DÜZENLEME DIALOG -->
    <Dialog
      v-model:visible="formDialog"
      :modal="true"
      :header="duzenlemeModu ? 'Teklifi Düzenle' : 'Yeni Satış Teklifi Hazırla'"
      class="teklif-form-dialog"
      :style="{ width: '88vw', maxWidth: '1200px' }"
    >
      <div class="p-fluid grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
        <div class="col-span-2">
          <label class="form-label">Müşteri / Cari Hesap *</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariHesaplar"
            option-label="ad"
            option-value="id"
            placeholder="Müşteri Seçin"
            filter
            class="w-full"
          />
        </div>
        <div>
          <label class="form-label">Teklif Tarihi *</label>
          <InputText
            v-model="form.tarih"
            type="date"
            class="w-full"
          />
        </div>
        <div>
          <label class="form-label">Geçerlilik Tarihi</label>
          <InputText
            v-model="form.gecerlilikTarihi"
            type="date"
            class="w-full"
          />
        </div>
      </div>

      <div class="p-fluid grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
        <div>
          <label class="form-label">Durum</label>
          <Dropdown
            v-model="form.durum"
            :options="durumSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div>
          <label class="form-label">Para Birimi</label>
          <Dropdown
            v-model="form.paraBirimi"
            :options="['TRY', 'USD', 'EUR', 'GBP']"
            class="w-full"
          />
        </div>
        <div>
          <label class="form-label">Teslimat Şartı</label>
          <InputText
            v-model="form.teslimatSarti"
            placeholder="Örn: 3 İş Günü / Depo Teslim"
          />
        </div>
        <div>
          <label class="form-label">Ödeme Şartı</label>
          <InputText
            v-model="form.odemeSarti"
            placeholder="Örn: %50 Peşin, Kalan 30 Gün"
          />
        </div>
      </div>

      <!-- Kalemler Bölümü -->
      <div class="kalemler-baslik flex justify-between items-center mb-2 mt-4">
        <h3 class="text-base font-bold">
          <i class="pi pi-list mr-1" /> Teklif Kalemleri
        </h3>
        <Button
          label="Yeni Kalem Ekle"
          icon="pi pi-plus"
          size="small"
          class="p-button-outlined p-button-sm"
          @click="kalemEkle"
        />
      </div>

      <div class="kalem-tablo-wrapper mb-4">
        <table class="w-full kalem-tablosu">
          <thead>
            <tr>
              <th style="width: 25%;">
                Ürün / Stok
              </th>
              <th style="width: 25%;">
                Açıklama
              </th>
              <th style="width: 10%;">
                Miktar
              </th>
              <th style="width: 10%;">
                Birim
              </th>
              <th style="width: 12%;">
                Birim Fiyat
              </th>
              <th style="width: 8%;">
                İsk.%
              </th>
              <th style="width: 8%;">
                KDV%
              </th>
              <th style="width: 12%;">
                Tutar
              </th>
              <th style="width: 5%;">
                Sil
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(k, idx) in form.kalemler"
              :key="idx"
            >
              <td>
                <Dropdown
                  v-model="k.stokId"
                  :options="stoklar"
                  option-label="ad"
                  option-value="id"
                  placeholder="Stok Seçin"
                  filter
                  class="w-full p-inputtext-sm"
                  @change="stokSecildi(k)"
                />
              </td>
              <td>
                <InputText
                  v-model="k.aciklama"
                  placeholder="Açıklama"
                  class="w-full p-inputtext-sm"
                />
              </td>
              <td>
                <input
                  v-model.number="k.miktar"
                  type="number"
                  min="1"
                  class="p-inputtext p-inputtext-sm w-full"
                  @input="kalemHesapla(k)"
                >
              </td>
              <td>
                <Dropdown
                  v-model="k.birim"
                  :options="['Adet', 'Kg', 'Metre', 'Paket', 'Koli', 'Saat', 'Ay', 'Yıl']"
                  class="w-full p-inputtext-sm"
                />
              </td>
              <td>
                <input
                  v-model.number="k.birimFiyat"
                  type="number"
                  min="0"
                  step="0.01"
                  class="p-inputtext p-inputtext-sm w-full"
                  @input="kalemHesapla(k)"
                >
              </td>
              <td>
                <input
                  v-model.number="k.iskontoOrani"
                  type="number"
                  min="0"
                  max="100"
                  class="p-inputtext p-inputtext-sm w-full"
                  @input="kalemHesapla(k)"
                >
              </td>
              <td>
                <Dropdown
                  v-model.number="k.kdvOrani"
                  :options="[0, 1, 10, 20]"
                  class="w-full p-inputtext-sm"
                  @change="kalemHesapla(k)"
                />
              </td>
              <td class="font-bold text-right">
                {{ formatCurrency(k.tutar) }}
              </td>
              <td class="text-center">
                <button
                  type="button"
                  class="text-red-500 hover:text-red-700"
                  @click="kalemSil(idx)"
                >
                  <i class="pi pi-times" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Alt Özet & Notlar -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="form-label">Garanti & Ek Şartlar / Notlar</label>
          <Textarea
            v-model="form.notlar"
            rows="4"
            placeholder="Teklife ait özel koşullar, teslim detayları, banka bilgileri vb."
            class="w-full"
          />
        </div>
        <div class="ozet-kutusu p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
          <div class="flex justify-between py-1">
            <span>Ara Toplam:</span>
            <span class="font-semibold">{{ formatCurrency(hesaplananAraToplam) }}</span>
          </div>
          <div class="flex justify-between py-1 items-center">
            <span>Genel İskonto (%):</span>
            <input
              v-model.number="form.iskontoOrani"
              type="number"
              min="0"
              max="100"
              class="p-inputtext p-inputtext-sm w-24 text-right"
            >
          </div>
          <div class="flex justify-between py-1">
            <span>Hesaplanan KDV:</span>
            <span class="font-semibold">{{ formatCurrency(hesaplananKdv) }}</span>
          </div>
          <div class="flex justify-between py-2 border-t mt-2 text-lg font-bold text-primary">
            <span>GENEL TOPLAM:</span>
            <span>{{ formatCurrency(hesaplananGenelToplam) }} {{ form.paraBirimi || 'TRY' }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="formDialog = false"
        />
        <Button
          :label="duzenlemeModu ? 'Değişiklikleri Kaydet' : 'Teklifi Kaydet'"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="kaydediliyor"
          @click="teklifKaydet"
        />
      </template>
    </Dialog>

    <!-- TEKLİF MEKTUBU & PROFORMA ÖNİZLEME MODAL -->
    <Dialog
      v-model:visible="onizlemeDialog"
      :modal="true"
      header="Satış Teklifi / Proforma Mektubu"
      :style="{ width: '850px', maxWidth: '95vw' }"
    >
      <div
        id="teklif-mektubu-alani"
        class="teklif-mektup-kagit"
      >
        <div class="mektup-header flex justify-between items-start border-b pb-4 mb-4">
          <div>
            <h2 class="text-xl font-bold text-primary">
              {{ sirket?.ad || 'RasPel ERP Ticaret A.Ş.' }}
            </h2>
            <p class="text-xs text-muted">
              {{ sirket?.adres || 'Örnek Mah. Sanayi Cad. No: 12 Kadıköy / İstanbul' }}
            </p>
            <p class="text-xs text-muted">
              Tel: {{ sirket?.telefon || '0216 555 0000' }} | E-Posta: {{ sirket?.email || 'info@raspel.com' }}
            </p>
          </div>
          <div class="text-right">
            <h3 class="text-lg font-bold text-gray-800 tracking-wider">
              SATIŞ TEKLİFİ
            </h3>
            <div class="text-xs text-muted mt-1">
              <strong>Teklif No:</strong> {{ seciliTeklif?.teklifNo }}
            </div>
            <div
              v-if="seciliTeklif?.revizyonNo > 0"
              class="text-xs text-muted"
            >
              <strong>Revizyon:</strong> Rev.{{ seciliTeklif?.revizyonNo }}
            </div>
            <div class="text-xs text-muted">
              <strong>Tarih:</strong> {{ formatDate(seciliTeklif?.tarih) }}
            </div>
            <div
              v-if="seciliTeklif?.gecerlilikTarihi"
              class="text-xs text-muted"
            >
              <strong>Geçerlilik:</strong> {{ formatDate(seciliTeklif?.gecerlilikTarihi) }}
            </div>
          </div>
        </div>

        <div class="musteri-kutusu bg-gray-50 p-3 rounded mb-4 border">
          <span class="text-xs font-bold text-gray-600 block mb-1">SAYIN (MÜŞTERİ / ALICI):</span>
          <div class="font-bold text-gray-800">
            {{ seciliTeklif?.cariHesapAdi || 'Müşteri' }}
          </div>
          <div class="text-xs text-gray-600">
            {{ seciliTeklif?.cariAdres || 'Adres bilgisi girilmedi' }}
          </div>
          <div class="text-xs text-gray-600">
            VKN/TC: {{ seciliTeklif?.cariVergiNo || '-' }} | Tel: {{ seciliTeklif?.cariTelefon || '-' }}
          </div>
        </div>

        <table class="w-full text-xs mb-4 mektup-tablo">
          <thead>
            <tr class="bg-gray-100 border-b border-t">
              <th class="p-2 text-left">
                #
              </th>
              <th class="p-2 text-left">
                Ürün / Hizmet Açıklaması
              </th>
              <th class="p-2 text-center">
                Miktar
              </th>
              <th class="p-2 text-right">
                Birim Fiyat
              </th>
              <th class="p-2 text-center">
                İsk.%
              </th>
              <th class="p-2 text-center">
                KDV%
              </th>
              <th class="p-2 text-right">
                Tutar
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(k, idx) in seciliTeklif?.kalemler"
              :key="idx"
              class="border-b"
            >
              <td class="p-2">
                {{ idx + 1 }}
              </td>
              <td class="p-2">
                <strong>{{ k.stokKodu ? '[' + k.stokKodu + '] ' : '' }}</strong>{{ k.aciklama }}
              </td>
              <td class="p-2 text-center">
                {{ k.miktar }} {{ k.birim }}
              </td>
              <td class="p-2 text-right">
                {{ formatCurrency(k.birimFiyat) }}
              </td>
              <td class="p-2 text-center">
                {{ k.iskontoOrani ? '%' + k.iskontoOrani : '-' }}
              </td>
              <td class="p-2 text-center">
                %{{ k.kdvOrani }}
              </td>
              <td class="p-2 text-right font-semibold">
                {{ formatCurrency(k.tutar) }}
              </td>
            </tr>
          </tbody>
        </table>

        <div class="flex justify-between items-start mb-6">
          <div class="sartlar-alani w-7/12 text-xs">
            <h4 class="font-bold mb-1">
              Teklif Koşulları & Şartlar:
            </h4>
            <ul class="list-disc pl-4 space-y-1 text-gray-700">
              <li v-if="seciliTeklif?.teslimatSarti">
                <strong>Teslimat:</strong> {{ seciliTeklif.teslimatSarti }}
              </li>
              <li v-if="seciliTeklif?.odemeSarti">
                <strong>Ödeme:</strong> {{ seciliTeklif.odemeSarti }}
              </li>
              <li v-if="seciliTeklif?.garantiSarti">
                <strong>Garanti:</strong> {{ seciliTeklif.garantiSarti }}
              </li>
              <li v-if="seciliTeklif?.notlar">
                <strong>Notlar:</strong> {{ seciliTeklif.notlar }}
              </li>
            </ul>
          </div>
          <div class="toplamlar-alani w-4/12 text-xs bg-gray-50 p-3 rounded border">
            <div class="flex justify-between py-1">
              <span>Ara Toplam:</span>
              <span class="font-semibold">{{ formatCurrency(seciliTeklif?.araToplam) }}</span>
            </div>
            <div
              v-if="seciliTeklif?.iskontoTutari > 0"
              class="flex justify-between py-1 text-red-600"
            >
              <span>İskonto (%{{ seciliTeklif?.iskontoOrani }}):</span>
              <span>-{{ formatCurrency(seciliTeklif?.iskontoTutari) }}</span>
            </div>
            <div class="flex justify-between py-1">
              <span>KDV Toplamı:</span>
              <span class="font-semibold">{{ formatCurrency(seciliTeklif?.kdv) }}</span>
            </div>
            <div class="flex justify-between py-2 border-t mt-1 font-bold text-sm text-primary">
              <span>GENEL TOPLAM:</span>
              <span>{{ formatCurrency(seciliTeklif?.genelToplam) }} {{ seciliTeklif?.paraBirimi }}</span>
            </div>
          </div>
        </div>

        <div class="imza-kutulari grid grid-cols-2 gap-8 pt-4 border-t text-center text-xs">
          <div class="border p-4 rounded min-h-24 flex flex-col justify-between">
            <span class="font-bold text-gray-700">Teklifi Hazırlayan (Firma Yetkilisi)</span>
            <span class="text-gray-400">İmza & Kaşe</span>
          </div>
          <div class="border p-4 rounded min-h-24 flex flex-col justify-between">
            <span class="font-bold text-gray-700">Teklifi Onaylayan (Müşteri Yetkilisi)</span>
            <span class="text-gray-400">Onay Tarihi & İmza</span>
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          label="WhatsApp ile Paylaş"
          icon="pi pi-whatsapp"
          class="p-button-success p-button-outlined"
          @click="whatsAppPaylas"
        />
        <Button
          label="Yazdır / PDF İndir"
          icon="pi pi-print"
          class="p-button-primary"
          @click="yazdirTeklif"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teklifAPI, cariHesapAPI, stokAPI, sirketAPI } from '../api/index.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import { useToast } from 'primevue/usetoast'

const toast = useToast()

const teklifler = ref([])
const cariHesaplar = ref([])
const stoklar = ref([])
const sirket = ref(null)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const aramaMetni = ref('')
const seciliDurumFiltre = ref('HEPSI')

const formDialog = ref(false)
const duzenlemeModu = ref(false)
const onizlemeDialog = ref(false)
const seciliTeklif = ref(null)

const durumSecenekleri = [
  { label: 'Taslak', value: 'TASLAK' },
  { label: 'Gönderildi', value: 'GONDERILDI' },
  { label: 'Onaylandı', value: 'ONAYLANDI' },
  { label: 'Reddedildi', value: 'REDDEDILDI' },
  { label: 'Siparişe Dönüştü', value: 'SIPARISE_DONUSTU' },
  { label: 'Faturalaştı', value: 'FATURALASTI' }
]

const varsayilanForm = {
  id: null,
  cariHesapId: null,
  tarih: new Date().toISOString().substring(0, 10),
  gecerlilikTarihi: '',
  durum: 'TASLAK',
  paraBirimi: 'TRY',
  iskontoOrani: 0,
  teslimatSarti: '3 İş Günü',
  odemeSarti: 'Nakit / Havale',
  garantiSarti: '2 Yıl Resmi Garanti',
  notlar: '',
  kalemler: [
    { stokId: null, aciklama: '', miktar: 1, birim: 'Adet', birimFiyat: 0, iskontoOrani: 0, kdvOrani: 20, tutar: 0 }
  ]
}

const form = ref({ ...varsayilanForm })

onMounted(async () => {
  await Promise.all([
    teklifleriGetir(),
    cariHesaplariGetir(),
    stoklariGetir(),
    sirketBilgisiGetir()
  ])
})

const teklifleriGetir = async () => {
  yukleniyor.value = true
  try {
    const res = await teklifAPI.getAll({ size: 100 })
    teklifler.value = res.data?.content || res.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Teklifler yüklenemedi: ' + err.message, life: 3000 })
  } finally {
    yukleniyor.value = false
  }
}

const cariHesaplariGetir = async () => {
  try {
    const res = await cariHesapAPI.getAll({ size: 500 })
    cariHesaplar.value = res.data?.content || res.data || []
  } catch {
    /* empty */
  }
}

const stoklariGetir = async () => {
  try {
    const res = await stokAPI.getAll({ size: 500 })
    stoklar.value = res.data?.content || res.data || []
  } catch {
    /* empty */
  }
}

const sirketBilgisiGetir = async () => {
  try {
    const res = await sirketAPI.getAktif()
    sirket.value = res.data || null
  } catch {
    /* empty */
  }
}

// İstatistikler
const toplamTeklifSayisi = computed(() => teklifler.value.length)
const gonderilenTeklifSayisi = computed(() => teklifler.value.filter(t => t.durum === 'GONDERILDI' || t.durum === 'TASLAK').length)
const onaylananTeklifSayisi = computed(() => teklifler.value.filter(t => t.durum === 'ONAYLANDI').length)
const donusenHacim = computed(() =>
  teklifler.value
    .filter(t => t.durum === 'SIPARISE_DONUSTU' || t.durum === 'FATURALASTI')
    .reduce((acc, t) => acc + (t.genelToplam || 0), 0)
)

// Filtreleme
const durumFiltrele = (durum) => {
  seciliDurumFiltre.value = durum
}

const isGecmis = (tarihStr) => {
  if (!tarihStr) return false
  return new Date(tarihStr) < new Date(new Date().setHours(0,0,0,0))
}

const durumLabel = (durum) => {
  const map = {
    TASLAK: 'Taslak',
    GONDERILDI: 'Gönderildi',
    ONAYLANDI: 'Onaylandı',
    REDDEDILDI: 'Reddedildi',
    SIPARISE_DONUSTU: 'Siparişe Dönüştü',
    FATURALASTI: 'Faturalaştı'
  }
  return map[durum] || durum
}

const durumSeverity = (durum) => {
  const map = {
    TASLAK: 'secondary',
    GONDERILDI: 'info',
    ONAYLANDI: 'success',
    REDDEDILDI: 'danger',
    SIPARISE_DONUSTU: 'warning',
    FATURALASTI: 'help'
  }
  return map[durum] || 'info'
}

// Hesaplamalar
const hesaplananAraToplam = computed(() => {
  return form.value.kalemler.reduce((sum, k) => sum + (k.tutar || 0), 0)
})

const hesaplananKdv = computed(() => {
  return form.value.kalemler.reduce((sum, k) => {
    const kdv = ((k.tutar || 0) * (k.kdvOrani || 0)) / 100
    return sum + kdv
  }, 0)
})

const hesaplananGenelToplam = computed(() => {
  const iskontoTutari = (hesaplananAraToplam.value * (form.value.iskontoOrani || 0)) / 100
  return (hesaplananAraToplam.value - iskontoTutari) + hesaplananKdv.value
})

const stokSecildi = (kalem) => {
  const s = stoklar.value.find(item => item.id === kalem.stokId)
  if (s) {
    kalem.aciklama = s.ad
    kalem.birimFiyat = s.fiyat || s.satisFiyati || 0
    kalem.birim = s.birim || 'Adet'
    kalem.kdvOrani = s.kdvOrani || 20
    kalemHesapla(kalem)
  }
}

const kalemHesapla = (kalem) => {
  const miktar = kalem.miktar || 0
  const fiyat = kalem.birimFiyat || 0
  const isk = kalem.iskontoOrani || 0
  const brut = miktar * fiyat
  const iskTutar = (brut * isk) / 100
  kalem.tutar = brut - iskTutar
}

const kalemEkle = () => {
  form.value.kalemler.push({
    stokId: null,
    aciklama: '',
    miktar: 1,
    birim: 'Adet',
    birimFiyat: 0,
    iskontoOrani: 0,
    kdvOrani: 20,
    tutar: 0
  })
}

const kalemSil = (idx) => {
  if (form.value.kalemler.length > 1) {
    form.value.kalemler.splice(idx, 1)
  }
}

const yeniTeklifAc = () => {
  duzenlemeModu.value = false
  form.value = JSON.parse(JSON.stringify(varsayilanForm))
  form.value.tarih = new Date().toISOString().substring(0, 10)
  formDialog.value = true
}

const duzenle = (teklif) => {
  duzenlemeModu.value = true
  form.value = {
    id: teklif.id,
    cariHesapId: teklif.cariHesapId,
    tarih: teklif.tarih,
    gecerlilikTarihi: teklif.gecerlilikTarihi || '',
    durum: teklif.durum,
    paraBirimi: teklif.paraBirimi || 'TRY',
    iskontoOrani: teklif.iskontoOrani || 0,
    teslimatSarti: teklif.teslimatSarti || '',
    odemeSarti: teklif.odemeSarti || '',
    garantiSarti: teklif.garantiSarti || '',
    notlar: teklif.notlar || '',
    kalemler: (teklif.kalemler || []).map(k => ({ ...k }))
  }
  formDialog.value = true
}

const teklifKaydet = async () => {
  if (!form.value.cariHesapId) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Lütfen bir müşteri/cari seçin.', life: 3000 })
    return
  }
  if (!form.value.kalemler || form.value.kalemler.length === 0 || !form.value.kalemler[0].aciklama) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'En az bir kalem açıklaması girilmelidir.', life: 3000 })
    return
  }

  kaydediliyor.value = true
  try {
    if (duzenlemeModu.value) {
      await teklifAPI.update(form.value.id, form.value)
      toast.add({ severity: 'success', summary: 'Güncellendi', detail: 'Teklif başarıyla güncellendi.', life: 3000 })
    } else {
      await teklifAPI.create(form.value)
      toast.add({ severity: 'success', summary: 'Oluşturuldu', detail: 'Yeni satış teklifi kaydedildi.', life: 3000 })
    }
    formDialog.value = false
    await teklifleriGetir()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Kaydedilemedi: ' + err.message, life: 3000 })
  } finally {
    kaydediliyor.value = false
  }
}

const silOnay = async (teklif) => {
  if (confirm(`"${teklif.teklifNo}" numaralı teklifi silmek istediğinize emin misiniz?`)) {
    try {
      await teklifAPI.delete(teklif.id)
      toast.add({ severity: 'success', summary: 'Silindi', detail: 'Teklif silindi.', life: 3000 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
    }
  }
}

const revizyonOlustur = async (teklif) => {
  if (confirm(`"${teklif.teklifNo}" için yeni bir revizyon (Rev.${teklif.revizyonNo + 1}) oluşturulsun mu?`)) {
    try {
      await teklifAPI.revizyonOlustur(teklif.id)
      toast.add({ severity: 'success', summary: 'Revizyon Oluşturuldu', detail: 'Teklifin yeni revizyonu hazırlandı.', life: 3000 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
    }
  }
}

const sipariseDonustur = async (teklif) => {
  if (confirm(`"${teklif.teklifNo}" numaralı teklif Satış Siparişine dönüştürülsün mü?`)) {
    try {
      const res = await teklifAPI.sipariseDonustur(teklif.id)
      toast.add({ severity: 'success', summary: 'Dönüştürüldü', detail: `Sipariş #${res.data?.siparisNo} olarak kaydedildi.`, life: 3500 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
    }
  }
}

const faturayaDonustur = async (teklif) => {
  if (confirm(`"${teklif.teklifNo}" numaralı teklif Satış Faturasına dönüştürülsün mü?`)) {
    try {
      const res = await teklifAPI.faturayaDonustur(teklif.id)
      toast.add({ severity: 'success', summary: 'Faturalaştı', detail: `Fatura #${res.data?.faturaNumarasi} olarak kesildi.`, life: 3500 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
    }
  }
}

const onizlemeAc = (teklif) => {
  seciliTeklif.value = teklif
  onizlemeDialog.value = true
}

const yazdirTeklif = () => {
  window.print()
}

const whatsAppPaylas = () => {
  if (!seciliTeklif.value) return
  const t = seciliTeklif.value
  const metin = `Sayın ${t.cariHesapAdi || 'Müşterimiz'},\n\nSizin için hazırladığımız ${t.teklifNo} numaralı Satış Teklifimizin toplam tutarı: ${formatCurrency(t.genelToplam)} ${t.paraBirimi || 'TRY'}'dir.\n\nİyi çalışmalar dileriz.`
  window.open(`https://wa.me/?text=${encodeURIComponent(metin)}`, '_blank')
}
</script>

<style scoped>
.sayfa-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.kart-kutu {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 16px;
}
.istatistik-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
.kart-ikon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.kart-toplam .kart-ikon { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.kart-bekleyen .kart-ikon { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.kart-onayli .kart-ikon { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.kart-donusen .kart-ikon { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }

.kart-icerik {
  display: flex;
  flex-direction: column;
}
.kart-etiket {
  font-size: 12px;
  color: var(--text-secondary);
}
.kart-deger {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.chip-btn {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: var(--bg-card);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}
.chip-btn.aktif {
  background: var(--primary-color, #3b82f6);
  color: #fff;
  border-color: var(--primary-color, #3b82f6);
}

.badge-rev {
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
}
.badge-kalem {
  background: rgba(0,0,0,0.05);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
}

.kalem-tablosu {
  border-collapse: collapse;
}
.kalem-tablosu th {
  background: rgba(0,0,0,0.03);
  padding: 8px;
  font-size: 12px;
  text-align: left;
  border-bottom: 2px solid var(--border);
}
.kalem-tablosu td {
  padding: 6px 8px;
  border-bottom: 1px solid var(--border);
}

.teklif-mektup-kagit {
  background: #ffffff;
  color: #1f2937;
  padding: 24px;
  border-radius: 8px;
}
.mektup-tablo th {
  font-size: 11px;
}
.mektup-tablo td {
  padding: 8px;
}

@media print {
  body * {
    visibility: hidden;
  }
  #teklif-mektubu-alani,
  #teklif-mektubu-alani * {
    visibility: visible;
  }
  #teklif-mektubu-alani {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    margin: 0;
    padding: 10mm;
  }
}
</style>
