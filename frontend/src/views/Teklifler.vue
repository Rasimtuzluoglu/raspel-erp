<template>
  <div class="teklifler-sayfasi">
    <!-- Üst Başlık & İstatistikler -->
    <div class="sayfa-header mb-4">
      <div class="baslik-kutu">
        <h1 class="page-title">
          <i class="pi pi-file-edit text-primary mr-2" />
          {{ t('teklifler.title') }}
        </h1>
        <p class="text-muted">
          {{ t('teklifler.subtitle') }}
        </p>
      </div>
      <div class="aksiyon-kutu">
        <Button
          :label="t('teklifler.yeniTeklif')"
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
          <span class="kart-etiket">{{ t('teklifler.toplamTeklif') }}</span>
          <span class="kart-deger">{{ toplamTeklifSayisi }}</span>
        </div>
      </div>
      <div class="istatistik-kart kart-bekleyen">
        <div class="kart-ikon">
          <i class="pi pi-send" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">{{ t('teklifler.gonderilenBekleyen') }}</span>
          <span class="kart-deger">{{ gonderilenTeklifSayisi }}</span>
        </div>
      </div>
      <div class="istatistik-kart kart-onayli">
        <div class="kart-ikon">
          <i class="pi pi-check-circle" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">{{ t('teklifler.onaylanan') }}</span>
          <span class="kart-deger">{{ onaylananTeklifSayisi }}</span>
        </div>
      </div>
      <div class="istatistik-kart kart-donusen">
        <div class="kart-ikon">
          <i class="pi pi-sync" />
        </div>
        <div class="kart-icerik">
          <span class="kart-etiket">{{ t('teklifler.donusenHacim') }}</span>
          <span class="kart-deger">{{ formatCurrency(donusenHacim) }}</span>
        </div>
      </div>
    </div>

    <!-- Teklifler Listesi Tablosu -->
    <div class="kart-kutu">
      <DataTable
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
          <div class="tablo-toolbar">
            <div class="durum-filtre-chips flex gap-2">
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'HEPSI' }]"
                @click="durumFiltrele('HEPSI')"
              >
                {{ t('teklifler.tumu') }} ({{ teklifler ? teklifler.length : 0 }})
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'TASLAK' }]"
                @click="durumFiltrele('TASLAK')"
              >
                {{ t('teklifler.durumTaslak') }}
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'GONDERILDI' }]"
                @click="durumFiltrele('GONDERILDI')"
              >
                {{ t('teklifler.durumGonderildi') }}
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'ONAYLANDI' }]"
                @click="durumFiltrele('ONAYLANDI')"
              >
                {{ t('teklifler.durumOnaylandi') }}
              </button>
              <button
                type="button"
                :class="['chip-btn', { aktif: seciliDurumFiltre === 'SIPARISE_DONUSTU' }]"
                @click="durumFiltrele('SIPARISE_DONUSTU')"
              >
                {{ t('teklifler.durumSipariseDonustu') }}
              </button>
            </div>
            <span class="p-input-icon-left">
              <i class="pi pi-search" />
              <InputText
                v-model="aramaMetni"
                :placeholder="t('teklifler.aramaPlaceholder')"
                class="p-inputtext-sm"
              />
            </span>
          </div>
        </template>

        <Column
          field="teklifNo"
          :header="t('teklifler.teklifNo')"
          sortable
        >
          <template #body="{ data }">
            <div class="flex items-center gap-2">
              <span class="font-bold text-primary">{{ data.teklifNo }}</span>
              <span
                v-if="data.revizyonNo > 0"
                class="badge-rev"
              >{{ t('teklifler.rev') }}{{ data.revizyonNo }}</span>
            </div>
          </template>
        </Column>

        <Column
          field="tarih"
          :header="t('common.date')"
          sortable
        >
          <template #body="{ data }">
            {{ formatDate(data.tarih) }}
          </template>
        </Column>

        <Column
          field="gecerlilikTarihi"
          :header="t('teklifler.gecerlilik')"
          sortable
        >
          <template #body="{ data }">
            <div v-if="data.gecerlilikTarihi">
              <span>{{ formatDate(data.gecerlilikTarihi) }}</span>
              <small
                v-if="isGecmis(data.gecerlilikTarihi) && data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                class="text-red-500 block"
              >{{ t('teklifler.suresiDoldu') }}</small>
            </div>
            <span
              v-else
              class="text-muted"
            >-</span>
          </template>
        </Column>

        <Column
          field="cariHesapAdi"
          :header="t('teklifler.musteriCari')"
          sortable
        >
          <template #body="{ data }">
            <div class="font-medium">
              {{ data.cariHesapAdi || t('teklifler.genelMusteri') }}
            </div>
            <small
              v-if="data.cariVergiNo"
              class="text-muted"
            >{{ t('teklifler.vknTc') }} {{ data.cariVergiNo }}</small>
          </template>
        </Column>

        <Column
          field="kalemler"
          :header="t('teklifler.kalem')"
        >
          <template #body="{ data }">
            <span class="badge-kalem">{{ t('teklifler.nKalem', { n: data.kalemler?.length || 0 }) }}</span>
          </template>
        </Column>

        <Column
          field="genelToplam"
          :header="t('teklifler.genelToplam')"
          sortable
        >
          <template #body="{ data }">
            <span class="font-bold text-base text-primary dark:text-gray-100">
              {{ formatCurrency(data.genelToplam) }} {{ data.paraBirimi || 'TRY' }}
            </span>
          </template>
        </Column>

        <Column
          field="durum"
          :header="t('common.status')"
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
          :header="t('common.actions')"
          class="text-right"
          style="min-width: 220px;"
        >
          <template #body="{ data }">
            <div class="flex justify-end gap-1">
              <!-- Önizle & Mektup / Proforma PDF -->
              <Button
                icon="pi pi-print"
                class="p-button-text p-button-sm p-button-secondary"
                :title="t('teklifler.onizleYazdir')"
                @click="onizlemeAc(data)"
              />

              <!-- Revizyon Oluştur -->
              <Button
                icon="pi pi-copy"
                class="p-button-text p-button-sm p-button-info"
                :title="t('teklifler.yeniRevizyon')"
                @click="revizyonOlustur(data)"
              />

              <!-- Siparişe Dönüştür -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-shopping-cart"
                class="p-button-text p-button-sm p-button-success"
                :title="t('teklifler.sipariseDonustur')"
                @click="sipariseDonustur(data)"
              />

              <!-- Faturaya Dönüştür -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-file"
                class="p-button-text p-button-sm p-button-warning"
                :title="t('teklifler.faturayaDonustur')"
                @click="faturayaDonustur(data)"
              />

              <!-- Düzenle -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-pencil"
                class="p-button-text p-button-sm"
                :title="t('common.edit')"
                @click="duzenle(data)"
              />

              <!-- Sil -->
              <Button
                v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
                icon="pi pi-trash"
                class="p-button-text p-button-sm p-button-danger"
                :title="t('common.delete')"
                @click="silOnay(data)"
              />
            </div>
          </template>
        </Column>

        <template #empty>
          <div class="text-center py-6 text-muted">
            <i class="pi pi-inbox text-4xl mb-2 block text-gray-400" />
            {{ t('teklifler.empty') }}
          </div>
        </template>
      </DataTable>
    </div>

    <!-- TEKLİF OLUŞTURMA & DÜZENLEME DIALOG -->
    <Dialog
      v-model:visible="formDialog"
      :modal="true"
      :header="duzenlemeModu ? t('teklifler.duzenle') : t('teklifler.yeniBaslik')"
      class="teklif-form-dialog"
      :style="{ width: '88vw', maxWidth: '1200px' }"
    >
      <div class="form-layout-container flex flex-col gap-6 pt-2">
        <!-- ÜST BÖLÜM: Genel Bilgiler & Şartlar -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- Sol Kolon: Temel Bilgiler -->
          <div class="p-4 border rounded-xl bg-secondary/50 dark:bg-gray-800/50">
            <h3 class="text-sm font-bold text-secondary dark:text-gray-300 mb-4 flex items-center gap-2">
              <i class="pi pi-info-circle text-primary" /> {{ t('teklifler.temelBilgiler') }}
            </h3>
            <div class="flex flex-col gap-4">
              <div class="flex flex-col gap-1">
                <label class="text-xs font-semibold text-secondary">{{ t('teklifler.musteriCariHesap') }} <span class="text-red-500">*</span></label>
                <Dropdown
                  v-model="form.cariHesapId"
                  :options="cariHesaplar"
                  option-label="ad"
                  option-value="id"
                  :placeholder="t('teklifler.musteriSecin')"
                  filter
                  class="w-full p-inputtext-sm"
                />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                  <label class="text-xs font-semibold text-secondary">{{ t('teklifler.teklifTarihi') }} <span class="text-red-500">*</span></label>
                  <InputText
                    v-model="form.tarih"
                    type="date"
                    class="p-inputtext-sm"
                  />
                </div>
                <div class="flex flex-col gap-1">
                  <label class="text-xs font-semibold text-secondary">{{ t('teklifler.gecerlilikTarihi') }}</label>
                  <InputText
                    v-model="form.gecerlilikTarihi"
                    type="date"
                    class="p-inputtext-sm"
                  />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                  <label class="text-xs font-semibold text-secondary">{{ t('common.status') }}</label>
                  <Dropdown
                    v-model="form.durum"
                    :options="durumSecenekleri"
                    option-label="label"
                    option-value="value"
                    class="p-inputtext-sm"
                  />
                </div>
                <div class="flex flex-col gap-1">
                  <label class="text-xs font-semibold text-secondary">{{ t('teklifler.paraBirimi') }}</label>
                  <Dropdown
                    v-model="form.paraBirimi"
                    :options="['TRY', 'USD', 'EUR', 'GBP']"
                    class="p-inputtext-sm"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Sağ Kolon: Şartlar ve Notlar -->
          <div class="p-4 border rounded-xl bg-secondary/50 dark:bg-gray-800/50">
            <h3 class="text-sm font-bold text-secondary dark:text-gray-300 mb-4 flex items-center gap-2">
              <i class="pi pi-file text-primary" /> {{ t('teklifler.kosullarNotlar') }}
            </h3>
            <div class="flex flex-col gap-4">
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                  <label class="text-xs font-semibold text-secondary">{{ t('teklifler.teslimatSarti') }}</label>
                  <InputText
                    v-model="form.teslimatSarti"
                    :placeholder="t('teklifler.teslimatSartiPlaceholder')"
                    class="p-inputtext-sm"
                  />
                </div>
                <div class="flex flex-col gap-1">
                  <label class="text-xs font-semibold text-secondary">{{ t('teklifler.odemeSarti') }}</label>
                  <InputText
                    v-model="form.odemeSarti"
                    :placeholder="t('teklifler.odemeSartiPlaceholder')"
                    class="p-inputtext-sm"
                  />
                </div>
              </div>
              <div class="flex flex-col gap-1 h-full">
                <label class="text-xs font-semibold text-secondary">{{ t('teklifler.garantiNotlar') }}</label>
                <Textarea
                  v-model="form.notlar"
                  rows="3"
                  :placeholder="t('teklifler.notlarPlaceholder')"
                  class="w-full flex-grow text-sm"
                  style="resize: none;"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- ORTA BÖLÜM: Teklif Kalemleri (Ürünler) -->
        <div class="p-4 border rounded-xl border-blue-100 dark:border-blue-900 bg-blue-50/20 dark:bg-blue-900/10">
          <div class="flex justify-between items-center mb-4 pb-2 border-b border-blue-100 dark:border-blue-800">
            <h3 class="text-sm font-bold text-accent dark:text-blue-300 flex items-center gap-2">
              <i class="pi pi-list" /> {{ t('teklifler.kalemler') }}
            </h3>
            <Button
              :label="t('teklifler.yeniUrunEkle')"
              icon="pi pi-plus"
              class="p-button-sm p-button-primary"
              @click="kalemEkle"
            />
          </div>

          <div class="flex flex-col gap-3">
            <div 
              v-for="(k, idx) in form.kalemler" 
              :key="idx"
              class="p-3 bg-white dark:bg-gray-900 border rounded-lg shadow-sm flex flex-col md:flex-row items-center gap-3 transition hover:border-blue-300 relative"
            >
              <!-- Kalem Sil Butonu (Mobilde üstte, Desktobta sağda) -->
              <button 
                type="button"
                class="absolute -top-2 -right-2 bg-red-100 text-red-600 rounded-full w-6 h-6 flex items-center justify-center hover:bg-red-500 hover:text-white transition shadow-sm"
                :title="t('teklifler.kalemiSil')"
                @click="kalemSil(idx)"
              >
                <i class="pi pi-times text-xs" />
              </button>

              <div class="flex-grow grid grid-cols-1 md:grid-cols-12 gap-3 w-full">
                <!-- Ürün Seçimi -->
                <div class="md:col-span-3">
                  <label class="text-[10px] uppercase font-bold text-muted mb-1 block">{{ t('teklifler.urunStok') }}</label>
                  <Dropdown
                    v-model="k.stokId"
                    :options="stoklar"
                    option-label="ad"
                    option-value="id"
                    :placeholder="t('teklifler.seciniz')"
                    filter
                    class="w-full p-inputtext-sm"
                    @change="stokSecildi(k)"
                  />
                </div>
                
                <!-- Açıklama -->
                <div class="md:col-span-3">
                  <label class="text-[10px] uppercase font-bold text-muted mb-1 block">{{ t('common.description') }}</label>
                  <InputText
                    v-model="k.aciklama"
                    :placeholder="t('teklifler.detay')"
                    class="w-full p-inputtext-sm"
                  />
                </div>

                <!-- Miktar & Birim -->
                <div class="md:col-span-2 flex gap-2">
                  <div class="w-1/2">
                    <label class="text-[10px] uppercase font-bold text-muted mb-1 block">{{ t('teklifler.miktar') }}</label>
                    <input
                      v-model.number="k.miktar"
                      type="number"
                      min="1"
                      class="p-inputtext p-inputtext-sm w-full"
                      @input="kalemHesapla(k)"
                    >
                  </div>
                  <div class="w-1/2">
                    <label class="text-[10px] uppercase font-bold text-muted mb-1 block">{{ t('teklifler.birim') }}</label>
                    <Dropdown
                      v-model="k.birim"
                      :options="['Adet', 'Kg', 'Metre', 'Paket', 'Koli', 'Saat', 'Ay']"
                      class="w-full p-inputtext-sm"
                    />
                  </div>
                </div>

                <!-- Fiyat, İskonto, KDV -->
                <div class="md:col-span-4 flex gap-2">
                  <div class="w-2/5">
                    <label class="text-[10px] uppercase font-bold text-muted mb-1 block">{{ t('teklifler.birimFiyat') }}</label>
                    <input
                      v-model.number="k.birimFiyat"
                      type="number"
                      min="0"
                      step="0.01"
                      class="p-inputtext p-inputtext-sm w-full text-right"
                      @input="kalemHesapla(k)"
                    >
                  </div>
                  <div class="w-1/5">
                    <label class="text-[10px] uppercase font-bold text-muted mb-1 block text-center">{{ t('teklifler.isk') }}</label>
                    <input
                      v-model.number="k.iskontoOrani"
                      type="number"
                      min="0"
                      max="100"
                      class="p-inputtext p-inputtext-sm w-full text-center"
                      @input="kalemHesapla(k)"
                    >
                  </div>
                  <div class="w-1/5">
                    <label class="text-[10px] uppercase font-bold text-muted mb-1 block text-center">{{ t('teklifler.kdv') }}</label>
                    <Dropdown
                      v-model.number="k.kdvOrani"
                      :options="[0, 1, 10, 20]"
                      class="w-full p-inputtext-sm text-center"
                      @change="kalemHesapla(k)"
                    />
                  </div>
                  <div class="w-1/5 flex flex-col justify-end">
                    <label class="text-[10px] uppercase font-bold text-muted mb-1 block text-right">{{ t('teklifler.tutar') }}</label>
                    <div class="font-bold text-sm text-right text-primary dark:text-gray-200 mt-1 whitespace-nowrap">
                      {{ formatCurrency(k.tutar) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-if="!form.kalemler || form.kalemler.length === 0"
              class="p-6 text-center text-gray-400 border-2 border-dashed rounded-lg"
            >
              <i class="pi pi-shopping-cart text-3xl mb-2" /><br>
              {{ t('teklifler.kalemYok') }}
            </div>
          </div>
        </div>

        <!-- ALT BÖLÜM: Fiyat Özeti -->
        <div class="flex justify-end">
          <div class="w-full md:w-1/3 bg-secondary dark:bg-gray-800 rounded-xl p-4 border shadow-sm">
            <h4 class="text-xs uppercase font-bold text-muted mb-3 border-b pb-2">
              {{ t('teklifler.hesapOzeti') }}
            </h4>
            
            <div class="flex justify-between items-center py-1.5 text-sm">
              <span class="text-secondary dark:text-gray-400">{{ t('teklifler.araToplam') }}</span>
              <span class="font-semibold">{{ formatCurrency(hesaplananAraToplam) }}</span>
            </div>
            
            <div class="flex justify-between items-center py-1.5 text-sm group">
              <span class="text-secondary dark:text-gray-400 flex items-center gap-1">
                {{ t('teklifler.genelIskonto') }}
              </span>
              <input
                v-model.number="form.iskontoOrani"
                type="number"
                min="0"
                max="100"
                class="p-inputtext p-inputtext-sm w-20 text-right bg-white dark:bg-gray-900 border-gray-300 group-hover:border-blue-400 transition"
                :title="t('teklifler.genelIskontoTitle')"
              >
            </div>
            
            <div class="flex justify-between items-center py-1.5 text-sm">
              <span class="text-secondary dark:text-gray-400">{{ t('teklifler.hesaplananKdv') }}</span>
              <span class="font-semibold">{{ formatCurrency(hesaplananKdv) }}</span>
            </div>
            
            <div class="flex justify-between items-center py-3 mt-2 border-t border-gray-200 dark:border-gray-700">
              <span class="text-lg font-bold text-primary dark:text-gray-200">{{ t('teklifler.genelToplam') }}</span>
              <div class="text-right">
                <span class="text-xl font-black text-primary">{{ formatCurrency(hesaplananGenelToplam) }}</span>
                <span class="text-sm font-bold text-muted ml-1">{{ form.paraBirimi || 'TRY' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="formDialog = false"
        />
        <Button
          :label="duzenlemeModu ? t('teklifler.degisiklikleriKaydet') : t('teklifler.kaydet')"
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
      :header="t('teklifler.mektupBaslik')"
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
            <h3 class="text-lg font-bold text-primary tracking-wider">
              {{ t('teklifler.satisTeklifi') }}
            </h3>
            <div class="text-xs text-muted mt-1">
              <strong>{{ t('teklifler.teklifNoLabel') }}</strong> {{ seciliTeklif?.teklifNo }}
            </div>
            <div
              v-if="seciliTeklif?.revizyonNo > 0"
              class="text-xs text-muted"
            >
              <strong>{{ t('teklifler.revizyon') }}</strong> {{ t('teklifler.rev') }}{{ seciliTeklif?.revizyonNo }}
            </div>
            <div class="text-xs text-muted">
              <strong>{{ t('teklifler.tarihLabel') }}</strong> {{ formatDate(seciliTeklif?.tarih) }}
            </div>
            <div
              v-if="seciliTeklif?.gecerlilikTarihi"
              class="text-xs text-muted"
            >
              <strong>{{ t('teklifler.gecerlilikLabel') }}</strong> {{ formatDate(seciliTeklif?.gecerlilikTarihi) }}
            </div>
          </div>
        </div>

        <div class="musteri-kutusu bg-secondary p-3 rounded mb-4 border">
          <span class="text-xs font-bold text-secondary block mb-1">{{ t('teklifler.sayin') }}</span>
          <div class="font-bold text-primary">
            {{ seciliTeklif?.cariHesapAdi || t('teklifler.musteri') }}
          </div>
          <div class="text-xs text-secondary">
            {{ seciliTeklif?.cariAdres || t('teklifler.adresGirilmedi') }}
          </div>
          <div class="text-xs text-secondary">
            {{ t('teklifler.vknTc') }} {{ seciliTeklif?.cariVergiNo || '-' }} | Tel: {{ seciliTeklif?.cariTelefon || '-' }}
          </div>
        </div>

        <table class="w-full text-xs mb-4 mektup-tablo">
          <thead>
            <tr class="bg-gray-100 border-b border-t">
              <th class="p-2 text-left">
                #
              </th>
              <th class="p-2 text-left">
                {{ t('teklifler.urunHizmet') }}
              </th>
              <th class="p-2 text-center">
                {{ t('teklifler.miktar') }}
              </th>
              <th class="p-2 text-right">
                {{ t('teklifler.birimFiyat') }}
              </th>
              <th class="p-2 text-center">
                {{ t('teklifler.isk') }}
              </th>
              <th class="p-2 text-center">
                {{ t('teklifler.kdv') }}
              </th>
              <th class="p-2 text-right">
                {{ t('teklifler.tutar') }}
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
              {{ t('teklifler.kosullarSartlar') }}
            </h4>
            <ul class="list-disc pl-4 space-y-1 text-secondary">
              <li v-if="seciliTeklif?.teslimatSarti">
                <strong>{{ t('teklifler.teslimatLabel') }}</strong> {{ seciliTeklif.teslimatSarti }}
              </li>
              <li v-if="seciliTeklif?.odemeSarti">
                <strong>{{ t('teklifler.odemeLabel') }}</strong> {{ seciliTeklif.odemeSarti }}
              </li>
              <li v-if="seciliTeklif?.garantiSarti">
                <strong>{{ t('teklifler.garantiLabel') }}</strong> {{ seciliTeklif.garantiSarti }}
              </li>
              <li v-if="seciliTeklif?.notlar">
                <strong>{{ t('teklifler.notlarLabel') }}</strong> {{ seciliTeklif.notlar }}
              </li>
            </ul>
          </div>
          <div class="toplamlar-alani w-4/12 text-xs bg-secondary p-3 rounded border">
            <div class="flex justify-between py-1">
              <span>{{ t('teklifler.araToplam') }}</span>
              <span class="font-semibold">{{ formatCurrency(seciliTeklif?.araToplam) }}</span>
            </div>
            <div
              v-if="seciliTeklif?.iskontoTutari > 0"
              class="flex justify-between py-1 text-red-600"
            >
              <span>{{ t('teklifler.iskonto', { n: seciliTeklif?.iskontoOrani }) }}</span>
              <span>-{{ formatCurrency(seciliTeklif?.iskontoTutari) }}</span>
            </div>
            <div class="flex justify-between py-1">
              <span>{{ t('teklifler.kdvToplami') }}</span>
              <span class="font-semibold">{{ formatCurrency(seciliTeklif?.kdv) }}</span>
            </div>
            <div class="flex justify-between py-2 border-t mt-1 font-bold text-sm text-primary">
              <span>{{ t('teklifler.genelToplam') }}</span>
              <span>{{ formatCurrency(seciliTeklif?.genelToplam) }} {{ seciliTeklif?.paraBirimi }}</span>
            </div>
          </div>
        </div>

        <div class="imza-kutulari grid grid-cols-2 gap-8 pt-4 border-t text-center text-xs">
          <div class="border p-4 rounded min-h-24 flex flex-col justify-between">
            <span class="font-bold text-secondary">{{ t('teklifler.hazirlayan') }}</span>
            <span class="text-gray-400">{{ t('teklifler.imzaKase') }}</span>
          </div>
          <div class="border p-4 rounded min-h-24 flex flex-col justify-between">
            <span class="font-bold text-secondary">{{ t('teklifler.onaylayan') }}</span>
            <span class="text-gray-400">{{ t('teklifler.onayTarihiImza') }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          :label="t('teklifler.whatsapp')"
          icon="pi pi-whatsapp"
          class="p-button-success p-button-outlined"
          @click="whatsAppPaylas"
        />
        <Button
          :label="t('teklifler.yazdirPdf')"
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
import { useI18n } from 'vue-i18n'

const toast = useToast()
const { t } = useI18n()

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

const durumSecenekleri = computed(() => [
  { label: t('teklifler.durumTaslak'), value: 'TASLAK' },
  { label: t('teklifler.durumGonderildi'), value: 'GONDERILDI' },
  { label: t('teklifler.durumOnaylandi'), value: 'ONAYLANDI' },
  { label: t('teklifler.durumReddedildi'), value: 'REDDEDILDI' },
  { label: t('teklifler.durumSipariseDonustu'), value: 'SIPARISE_DONUSTU' },
  { label: t('teklifler.durumFaturalasti'), value: 'FATURALASTI' }
])

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
    toast.add({ severity: 'error', summary: t('teklifler.hata'), detail: t('teklifler.tekliflerYuklenemedi') + err.message, life: 3000 })
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
    TASLAK: t('teklifler.durumTaslak'),
    GONDERILDI: t('teklifler.durumGonderildi'),
    ONAYLANDI: t('teklifler.durumOnaylandi'),
    REDDEDILDI: t('teklifler.durumReddedildi'),
    SIPARISE_DONUSTU: t('teklifler.durumSipariseDonustu'),
    FATURALASTI: t('teklifler.durumFaturalasti')
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
    toast.add({ severity: 'warn', summary: t('teklifler.eksikBilgi'), detail: t('teklifler.musteriSecinUyari'), life: 3000 })
    return
  }
  if (!form.value.kalemler || form.value.kalemler.length === 0 || !form.value.kalemler[0].aciklama) {
    toast.add({ severity: 'warn', summary: t('teklifler.eksikBilgi'), detail: t('teklifler.kalemAciklamaUyari'), life: 3000 })
    return
  }

  kaydediliyor.value = true
  try {
    if (duzenlemeModu.value) {
      await teklifAPI.update(form.value.id, form.value)
      toast.add({ severity: 'success', summary: t('teklifler.guncellendi'), detail: t('teklifler.teklifGuncellendi'), life: 3000 })
    } else {
      await teklifAPI.create(form.value)
      toast.add({ severity: 'success', summary: t('teklifler.olusturuldu'), detail: t('teklifler.teklifOlusturuldu'), life: 3000 })
    }
    formDialog.value = false
    await teklifleriGetir()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('teklifler.hata'), detail: t('teklifler.kaydedilemedi') + err.message, life: 3000 })
  } finally {
    kaydediliyor.value = false
  }
}

const silOnay = async (teklif) => {
  if (confirm(t('teklifler.silOnay', { n: teklif.teklifNo }))) {
    try {
      await teklifAPI.delete(teklif.id)
      toast.add({ severity: 'success', summary: t('teklifler.silindi'), detail: t('teklifler.teklifSilindi'), life: 3000 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: t('teklifler.hata'), detail: err.message, life: 3000 })
    }
  }
}

const revizyonOlustur = async (teklif) => {
  if (confirm(t('teklifler.revizyonOnay', { n: teklif.teklifNo, r: teklif.revizyonNo + 1 }))) {
    try {
      await teklifAPI.revizyonOlustur(teklif.id)
      toast.add({ severity: 'success', summary: t('teklifler.revizyonOlusturuldu'), detail: t('teklifler.revizyonHazirlandi'), life: 3000 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: t('teklifler.hata'), detail: err.message, life: 3000 })
    }
  }
}

const sipariseDonustur = async (teklif) => {
  if (confirm(t('teklifler.siparisOnay', { n: teklif.teklifNo }))) {
    try {
      const res = await teklifAPI.sipariseDonustur(teklif.id)
      toast.add({ severity: 'success', summary: t('teklifler.donusturuldu'), detail: t('teklifler.siparisKaydedildi', { n: res.data?.siparisNo }), life: 3500 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: t('teklifler.hata'), detail: err.message, life: 3000 })
    }
  }
}

const faturayaDonustur = async (teklif) => {
  if (confirm(t('teklifler.faturaOnay', { n: teklif.teklifNo }))) {
    try {
      const res = await teklifAPI.faturayaDonustur(teklif.id)
      toast.add({ severity: 'success', summary: t('teklifler.faturalasti'), detail: t('teklifler.faturaKesildi', { n: res.data?.faturaNumarasi }), life: 3500 })
      await teklifleriGetir()
    } catch (err) {
      toast.add({ severity: 'error', summary: t('teklifler.hata'), detail: err.message, life: 3000 })
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
