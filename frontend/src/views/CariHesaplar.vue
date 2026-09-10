<template>
  <div class="cari-hesaplar-container">
    <h1>{{ t('cariHesaplar.title') }}</h1>

    <IlkZiyaretIpuclari
      anahtar="cari-hesaplar"
      :baslik="t('cariHesaplar.ipucuBaslik')"
      :metin="t('cariHesaplar.ipucuMetin')"
    />

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('cariHesaplar.yeniCariHesap')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
        <div
          v-if="selectedCariHesaplar && selectedCariHesaplar.length > 0"
          class="batch-actions"
        >
          <span class="batch-count">{{ t('cariHesaplar.nSecili', { n: selectedCariHesaplar ? selectedCariHesaplar.length : 0 }) }}</span>
          <Button
            :label="t('cariHesaplar.topluEposta')"
            icon="pi pi-envelope"
            class="p-button-sm p-button-info"
            @click="topluEmailDialog = true"
          />
          <Button
            :label="t('cariHesaplar.topluSil')"
            icon="pi pi-trash"
            class="p-button-sm p-button-danger"
            @click="batchSil"
          />
          <Button
            :label="t('cariHesaplar.csvAktar')"
            icon="pi pi-download"
            class="p-button-sm p-button-outlined"
            @click="batchCsvExport"
          />
        </div>
      </template>
      <template #end>
        <TabloAyarlari
          tablo-key="cari"
          :kolonlar="kolonlar"
          @update:yogunluk="tabloYogunluk = $event"
        />
        <Button
          :label="t('cariHesaplar.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          style="margin-right: 4px"
          @click="excelIndir"
        />
        <Button
          :label="t('cariHesaplar.csv')"
          icon="pi pi-download"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
          @click="csvExport"
        />
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="aramaMetni"
            :placeholder="t('cariHesaplar.aramaPlaceholder')"
            @input="ara"
          />
        </span>
      </template>
    </Toolbar>

    <div
      v-if="loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}</p>
    </div>

    <div
      v-if="!loading"
      class="cari-istatistik"
    >
      <div class="istatistik-kutu">
        <span>{{ t('cariHesaplar.toplamCari') }}</span>
        <strong>{{ cariOzet.toplamKayit ?? cariHesapStore.toplamKayit }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>{{ t('cariHesaplar.alacakli') }}</span>
        <strong class="positive">{{ formatCurrency(cariOzet.alacakli ?? 0) }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>{{ t('cariHesaplar.borclu') }}</span>
        <strong class="negative">{{ formatCurrency(cariOzet.borclu ?? 0) }}</strong>
      </div>
    </div>

    <div
      v-if="!loading"
      class="cari-filtreler"
    >
      <Dropdown
        v-model="filtreTur"
        :options="['Musteri', 'Tedarikci', 'Her Ikisi']"
        :placeholder="t('cariHesaplar.tur')"
        class="filtre-select"
        show-clear
        @change="filtreDegisti"
      />
      <Dropdown
        v-model="filtreBakiye"
        :options="bakiyeFiltreleri"
        option-label="label"
        option-value="value"
        :placeholder="t('cariHesaplar.bakiye')"
        class="filtre-select"
        show-clear
        @change="filtreDegisti"
      />
      <Button
        v-if="filtreTur || filtreBakiye"
        :label="t('cariHesaplar.temizle')"
        icon="pi pi-times"
        size="small"
        class="p-button-outlined"
        @click="filtreleriTemizle"
      />
    </div>

    <div
      v-if="!loading"
      class="table-container"
    >
      <DataTable
        v-model:selection="selectedCariHesaplar"
        :value="cariHesapStore?.cariHesaplar || []"
        selection-mode="multiple"
        data-key="id"
        responsive-layout="scroll"
        striped-rows
        :size="tabloYogunluk === 'compact' ? 'small' : 'normal'"
        :lazy="true"
        :total-records="cariHesapStore.toplamKayit"
        :rows="cariSayfaBoyutu"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
        :virtual-scroll="cariHesapStore.toplamKayit > 100 && !aramaMetni"
        :virtual-scroll-options="{
          itemSize: tabloYogunluk === 'compact' ? 38 : 46,
          scrollHeight: '600px',
          showLoader: true
        }"
        @page="cariSayfaDegisti"
      >
        <Column
          selection-mode="multiple"
          header-style="width: 3rem"
        />
        <Column
          v-if="kolonlar[0].visible"
          field="id"
          header="ID"
          style="width: 60px"
        />
        <Column
          v-if="kolonlar[1].visible"
          field="ad"
          :header="t('cariHesaplar.ad')"
          sortable
          style="width: 200px"
        />
        <Column
          v-if="kolonlar[2].visible"
          field="tur"
          :header="t('cariHesaplar.tur')"
          style="width: 100px"
        >
          <template #body="s">
            <Tag
              :value="s.data.tur || '-'"
              :severity="s.data.tur === 'Musteri' ? 'info' : s.data.tur === 'Tedarikci' ? 'warn' : 'secondary'"
            />
          </template>
        </Column>
        <Column
          v-if="kolonlar[3].visible"
          field="yetkiliKisi"
          :header="t('cariHesaplar.yetkili')"
          style="width: 130px"
        />
        <Column
          v-if="kolonlar[4].visible"
          field="telefon"
          :header="t('cariHesaplar.telefon')"
          style="width: 130px"
        >
          <template #body="s">
            <span
              v-if="s.data.telefon"
              class="kopyalanabilir"
              @click="kopyala(s.data.telefon, t('cariHesaplar.telefonKopyalandi'))"
            >
              {{ s.data.telefon }} <i class="pi pi-copy kopyala-ikon" />
            </span>
            <span v-else>-</span>
          </template>
        </Column>
        <Column
          v-if="kolonlar[5].visible"
          field="krediLimiti"
          :header="t('cariHesaplar.krediLimiti')"
          style="width: 120px"
        >
          <template #body="s">
            {{ s.data.krediLimiti ? formatCurrency(s.data.krediLimiti) : '-' }}
          </template>
        </Column>
        <Column
          v-if="kolonlar[6].visible"
          field="odemeVadesi"
          :header="t('cariHesaplar.vadeGun')"
          style="width: 80px"
        />
        <Column
          v-if="kolonlar[7].visible"
          field="bakiye"
          :header="t('cariHesaplar.bakiye')"
          style="width: 140px"
        >
          <template #body="slotProps">
            <span
              class="bakiye-rozet"
              :class="slotProps.data.bakiye >= 0 ? 'alacak' : 'borc'"
            >
              <i :class="slotProps.data.bakiye >= 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ formatCurrency(slotProps.data.bakiye) }}
            </span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 250px"
        >
          <template #body="slotProps">
            <Button
              icon="pi pi-file-plus"
              class="p-button-rounded p-button-success p-button-sm"
              :title="t('cariHesaplar.yeniFatura')"
              @click="yeniFatura(slotProps.data)"
            />
            <Button
              icon="pi pi-money-bill"
              class="p-button-rounded p-button-info p-button-sm"
              :title="t('cariHesaplar.tahsilat')"
              @click="tahsilatAc(slotProps.data)"
            />
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-warning p-button-sm"
              :title="t('common.edit')"
              @click="editCariHesap(slotProps.data)"
            />
            <Button
              icon="pi pi-list"
              class="p-button-rounded p-button-secondary p-button-sm"
              :title="t('cariHesaplar.hareketlerDetay')"
              @click="viewHareketler(slotProps.data)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              :title="t('common.delete')"
              @click="confirmDelete(slotProps.data.id)"
            />
          </template>
        </Column>
      </DataTable>

      <EmptyState
        v-if="cariHesapStore?.cariHesaplar?.length === 0"
        :message="t('cariHesaplar.empty')"
        :sub-message="t('cariHesaplar.emptyHint')"
        icon="pi pi-users"
        :action-label="t('cariHesaplar.emptyAction')"
        action-icon="pi pi-plus"
        @action="openDialog"
      />
    </div>

    <!-- Cari Hesap Dialog -->
    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('cariHesaplar.duzenle') : t('cariHesaplar.yeniCariHesap')"
      :modal="true"
      style="width: 650px"
      :draggable="false"
    >
      <div class="dialog-form">
        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.genelBilgiler') }}
          </div>
          <div class="form-group">
            <label for="ad">{{ t('cariHesaplar.cariAdi') }} <span class="required">*</span></label>
            <InputText
              id="ad"
              v-model="form.ad"
              :placeholder="t('cariHesaplar.adPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="tur">{{ t('cariHesaplar.cariTuru') }}</label>
              <Dropdown
                v-model="form.tur"
                :options="['Musteri', 'Tedarikci', 'Her Ikisi']"
                :placeholder="t('faturalar.seciniz')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="vergiNumarasi">{{ t('cariHesaplar.vergiNoTc') }}</label>
              <InputText
                id="vergiNumarasi"
                v-model="form.vergiNumarasi"
                :placeholder="t('cariHesaplar.vergiNoPlaceholder')"
                class="w-full"
              />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="telefon">{{ t('cariHesaplar.telefon') }}</label>
              <InputText
                id="telefon"
                v-model="form.telefon"
                placeholder="05XX XXX XX XX"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="email">{{ t('cariHesaplar.eposta') }}</label>
              <InputText
                id="email"
                v-model="form.email"
                placeholder="ornek@firma.com"
                class="w-full"
              />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="vergiDairesi">{{ t('cariHesaplar.vergiDairesi') }}</label>
              <InputText
                id="vergiDairesi"
                v-model="form.vergiDairesi"
                :placeholder="t('cariHesaplar.vergiDairesiPlaceholder')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="iban">{{ t('cariHesaplar.iban') }}</label>
              <InputText
                id="iban"
                v-model="form.iban"
                placeholder="TR..."
                class="w-full"
              />
              <small
                v-if="ibanGecerli === true"
                class="iban-gecerli"
              >&#x2713; {{ t('cariHesaplar.ibanGecerli') }}</small>
              <small
                v-if="ibanGecerli === false"
                class="iban-gecersiz"
              >&#x2717; {{ t('cariHesaplar.ibanGecersiz') }}</small>
              <small class="iban-yardim">{{ t('cariHesaplar.ibanYardim') }}</small>
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.adresBilgileri') }}
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="il">{{ t('cariHesaplar.il') }}</label>
              <InputText
                id="il"
                v-model="form.il"
                :placeholder="t('cariHesaplar.il')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="ilce">{{ t('cariHesaplar.ilce') }}</label>
              <InputText
                id="ilce"
                v-model="form.ilce"
                :placeholder="t('cariHesaplar.ilce')"
                class="w-full"
              />
            </div>
          </div>
          <div class="form-group">
            <label for="adres">{{ t('cariHesaplar.adres') }}</label>
            <Textarea
              id="adres"
              v-model="form.adres"
              :placeholder="t('cariHesaplar.adresPlaceholder')"
              rows="2"
              class="w-full"
            />
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.yetkiliKisi') }}
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="yetkiliKisi">{{ t('cariHesaplar.adSoyad') }}</label>
              <InputText
                id="yetkiliKisi"
                v-model="form.yetkiliKisi"
                :placeholder="t('cariHesaplar.adSoyadPlaceholder')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="yetkiliTelefon">{{ t('cariHesaplar.telefon') }}</label>
              <InputText
                id="yetkiliTelefon"
                v-model="form.yetkiliTelefon"
                placeholder="0XXX XXX XX XX"
                class="w-full"
              />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.krediVade') }}
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="krediLimiti">{{ t('cariHesaplar.krediLimitiTL') }}</label>
              <InputNumber
                id="krediLimiti"
                v-model="form.krediLimiti"
                :min="0"
                :min-fraction-digits="2"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="odemeVadesi">{{ t('cariHesaplar.odemeVadesiGun') }}</label>
              <InputNumber
                id="odemeVadesi"
                v-model="form.odemeVadesi"
                :min="0"
                :min-fraction-digits="0"
                class="w-full"
              />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.ekBilgiler') }}
          </div>
          <div class="form-group">
            <label for="notlar">{{ t('cariHesaplar.notlar') }}</label>
            <Textarea
              id="notlar"
              v-model="form.notlar"
              :placeholder="t('cariHesaplar.notlarPlaceholder')"
              rows="3"
              class="w-full"
            />
          </div>
          <div
            v-if="editingId"
            class="form-group"
          >
            <label>{{ t('cariHesaplar.aktif') }}</label>
            <InputSwitch v-model="form.aktif" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <Button
            :label="t('common.cancel')"
            icon="pi pi-times"
            class="p-button-text"
            @click="closeDialog"
          />
          <Button
            :label="editingId ? t('cariHesaplar.guncelle') : t('common.save')"
            icon="pi pi-check"
            :loading="saving"
            @click="saveCariHesap"
          />
        </div>
      </template>
    </Dialog>

    <!-- Hareketler Dialog -->
    <Dialog
      v-model:visible="showHareketlerDialog"
      :header="t('cariHesaplar.hareketlerBaslik')"
      :modal="true"
      style="width: 800px"
    >
      <div class="hareket-info">
        <h3>{{ selectedCariHesap?.ad }}</h3>
        <div class="bakiye-ozet">
          <div class="ozet-satir">
            <span class="ozet-etiket">{{ t('cariHesaplar.toplamTahsilat') }}</span>
            <span class="positive">{{ formatCurrency(toplamTahsilat) }}</span>
          </div>
          <div class="ozet-satir">
            <span class="ozet-etiket">{{ t('cariHesaplar.toplamOdeme') }}</span>
            <span class="negative">{{ formatCurrency(toplamOdeme) }}</span>
          </div>
          <div class="ozet-satir ozet-bakiye">
            <span class="ozet-etiket">{{ t('cariHesaplar.guncelBakiye') }}</span>
            <strong :class="guncelBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(guncelBakiye) }}</strong>
          </div>
        </div>
      </div>

      <div
        v-if="cariHareketlerYukleniyor"
        class="loading"
      >
        <p><i class="pi pi-spin pi-spinner" /> {{ t('cariHesaplar.hareketlerYukleniyor') }}</p>
      </div>

      <div
        v-else
        class="table-container"
      >
        <DataTable
          v-if="cariHareketler && cariHareketler.length > 0"
          :value="cariHareketler"
          responsive-layout="scroll"
          striped-rows
          :rows="10"
          :paginator="true"
          size="small"
        >
          <Column
            field="hareketTarihi"
            :header="t('common.date')"
            style="width: 120px"
          >
            <template #body="slotProps">
              {{ formatDate(slotProps.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            field="tur"
            :header="t('cariHesaplar.tur')"
            style="width: 100px"
          >
            <template #body="slotProps">
              <span :class="['badge', slotProps.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
                {{ slotProps.data.tur === 'TAHSILAT' ? t('cariHesaplar.tahsilat') : t('cariHesaplar.odeme') }}
              </span>
            </template>
          </Column>
          <Column
            field="tutar"
            :header="t('common.amount')"
            style="width: 120px"
          >
            <template #body="slotProps">
              <span :class="slotProps.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
                {{ formatCurrency(slotProps.data.tutar) }}
              </span>
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
        </DataTable>

        <EmptyState
          v-if="cariHareketler && cariHareketler.length === 0"
          :message="t('cariHesaplar.hareketYok')"
          icon="pi pi-list"
        />

        <div
          v-if="cariFaturalar && cariFaturalar.length > 0"
          class="cari-faturalar"
        >
          <div class="cari-not-baslik">
            <h4>
              <i
                class="pi pi-file"
                style="margin-right: 6px"
              />{{ t('cariHesaplar.gecmisFaturalar', { n: cariFaturalar.length }) }}
            </h4>
          </div>
          <DataTable
            :value="cariFaturalar"
            size="small"
            striped-rows
            :rows="5"
            :paginator="cariFaturalar.length > 5"
          >
            <Column :header="t('cariHesaplar.faturaNo')">
              <template #body="{ data }">
                <a
                  class="fatura-link"
                  @click="faturaDetayAc(data)"
                >{{ data.faturaNumarasi }}</a>
              </template>
            </Column>
            <Column :header="t('common.date')">
              <template #body="{ data }">
                {{ formatDate(data.tarih) }}
              </template>
            </Column>
            <Column :header="t('cariHesaplar.tur')">
              <template #body="{ data }">
                {{ data.tur === 'SATIS' ? t('cariHesaplar.satis') : t('cariHesaplar.alis') }}
              </template>
            </Column>
            <Column :header="t('common.amount')">
              <template #body="{ data }">
                <span class="positive">{{ formatCurrency(data.genelToplam) }}</span>
              </template>
            </Column>
            <Column :header="t('common.status')">
              <template #body="{ data }">
                <span :class="['badge', data.odemeDurumu === 'ODENDI' ? 'tahsilat' : 'odeme']">
                  {{ data.odemeDurumu === 'ODENDI' ? t('cariHesaplar.odendi') : data.odemeDurumu === 'KISMI_ODENDI' ? t('cariHesaplar.kismi') : t('cariHesaplar.odenmedi') }}
                </span>
              </template>
            </Column>
          </DataTable>
        </div>

        <div class="cari-ozel-fiyatlar">
          <div class="cari-not-baslik">
            <h4>
              <i
                class="pi pi-tag"
                style="margin-right: 6px"
              />{{ t('cariHesaplar.ozelFiyatlar') }}
            </h4>
          </div>
          <div
            v-for="f in cariOzelFiyatlar"
            :key="f.id"
            class="ozel-fiyat-satir"
          >
            <span class="ozel-fiyat-urun">{{ f.stokAd }}</span>
            <span class="ozel-fiyat-tutar">{{ formatCurrency(f.fiyat) }}</span>
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-text p-button-danger p-button-sm"
              @click="cariOzelFiyatSil(f)"
            />
          </div>
          <div class="ozel-fiyat-ekle">
            <Dropdown
              v-model="ozelFiyatStok"
              :options="stokSecenekleri"
              option-label="ad"
              option-value="id"
              :placeholder="t('cariHesaplar.urunSec')"
              filter
              class="ozel-fiyat-stok-select"
            />
            <InputNumber
              v-model="ozelFiyatTutar"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              :min-fraction-digits="2"
              :placeholder="t('cariHesaplar.fiyat')"
              class="ozel-fiyat-tutar-input"
            />
            <Button
              icon="pi pi-plus"
              :label="t('cariHesaplar.ekle')"
              size="small"
              @click="cariOzelFiyatEkle"
            />
          </div>
        </div>

        <div
          v-if="sonUrunler && sonUrunler.length > 0"
          class="cari-urunler"
        >
          <div class="cari-not-baslik">
            <h4>
              <i
                class="pi pi-box"
                style="margin-right: 6px"
              />{{ t('cariHesaplar.gecmisteAldigiUrunler') }}
            </h4>
          </div>
          <div class="cari-urunler-grid">
            <button
              v-for="u in sonUrunler"
              :key="u.stokId"
              type="button"
              class="cari-urun-chip"
              @click="urunFiyatGecmisiGoster(u)"
            >
              <span class="cari-urun-ad">{{ u.stokAd }}</span>
              <span class="cari-urun-bilgi">{{ u.sonAlisTarihi }} · {{ t('cariHesaplar.nAdet', { n: u.adet }) }}</span>
              <span class="cari-urun-fiyat">{{ formatCurrency(u.sonBirimFiyat) }}</span>
            </button>
          </div>
          <div
            v-if="seciliUrunFiyatGecmisi"
            class="cari-urun-fiyat-gecmisi"
          >
            <div class="fg-baslik">
              <span><i class="pi pi-chart-line" /> {{ seciliUrunFiyatGecmisi.urunAd }} — {{ t('cariHesaplar.fiyatGecmisi') }}</span>
              <button
                type="button"
                class="fg-kapat"
                @click="seciliUrunFiyatGecmisi = null"
              >
                <i class="pi pi-times" />
              </button>
            </div>
            <div
              v-for="(k, i) in seciliUrunFiyatGecmisi.gecmis"
              :key="i"
              class="fg-satir"
            >
              <span class="fg-tarih">{{ formatDate(k.tarih) }}</span>
              <span class="fg-fatura">{{ k.faturaNumarasi }}</span>
              <span class="fg-adet">{{ t('cariHesaplar.nAdet', { n: k.adet }) }}</span>
              <span class="fg-fiyat">{{ formatCurrency(k.birimFiyat) }}</span>
            </div>
          </div>
        </div>

        <div class="cari-notlar">
          <div class="cari-not-baslik">
            <h4>
              <i
                class="pi pi-pen-to-square"
                style="margin-right: 6px"
              />{{ t('cariHesaplar.gorusmeNotlari') }}
            </h4>
          </div>
          <div class="cari-not-ekle">
            <Dropdown
              v-model="yeniCariNotOnem"
              :options="notOnemSecenekleri"
              option-label="label"
              option-value="value"
              class="not-onem-select"
            />
            <InputText
              v-model="yeniCariNot"
              :placeholder="t('cariHesaplar.yeniNotPlaceholder')"
              @keyup.enter="cariNotEkle"
            />
            <Button
              icon="pi pi-plus"
              class="p-button-sm"
              @click="cariNotEkle"
            />
          </div>
          <div
            v-if="(!cariNotlar || !cariNotlar.length)"
            class="cari-not-bos"
          >
            {{ t('cariHesaplar.notYok') }}
          </div>
          <div
            v-for="n in cariNotlar"
            :key="n.id"
            class="cari-not-satir"
          >
            <div class="cari-not-icerik">
              <div class="cari-not-baslik-satir">
                <strong>{{ n.baslik }}</strong>
                <span
                  v-if="n.onemDerecesi && n.onemDerecesi !== 'NORMAL'"
                  class="not-onem-rozet"
                  :class="n.onemDerecesi.toLowerCase()"
                >
                  {{ n.onemDerecesi === 'YUKSEK' ? t('cariHesaplar.yuksek') : t('cariHesaplar.kritik') }}
                </span>
              </div>
              <p>{{ n.icerik }}</p>
              <small>{{ n.olusturmaTarihi ? new Date(n.olusturmaTarihi).toLocaleString('tr-TR') : '' }}</small>
            </div>
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-text p-button-danger p-button-sm"
              @click="cariNotSil(n)"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          :label="t('stoklar.kapat')"
          icon="pi pi-times"
          @click="showHareketlerDialog = false"
        />
      </template>
    </Dialog>

    <TahsilatGirDialog
      v-model:visible="tahsilatDialog"
      :cari="tahsilatHedefCari"
      :cariler="[]"
      @kaydedildi="tahsilatSonrasiYenile"
    />

    <Dialog
      v-model:visible="topluEmailDialog"
      :header="t('cariHesaplar.topluEposta')"
      :modal="true"
      style="width: 520px"
    >
      <p class="toplu-email-aciklama">
        {{ t('cariHesaplar.topluEpostaAciklama', { n: selectedCariHesaplar ? selectedCariHesaplar.length : 0 }) }}
      </p>
      <div class="form-group">
        <label>{{ t('cariHesaplar.konu') }}</label>
        <InputText
          v-model="topluEmailKonu"
          class="w-full"
          :placeholder="t('cariHesaplar.epostaKonusuPlaceholder')"
        />
      </div>
      <div class="form-group">
        <label>{{ t('cariHesaplar.mesaj') }}</label>
        <Textarea
          v-model="topluEmailMesaj"
          rows="5"
          class="w-full"
          :placeholder="t('cariHesaplar.epostaIcerigiPlaceholder')"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="topluEmailDialog = false"
        />
        <Button
          :label="t('cariHesaplar.epostaIstemisiniAc')"
          icon="pi pi-envelope"
          @click="topluEmailGonder"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="faturaDetayDialog"
      :header="t('cariHesaplar.faturaDetayi')"
      :modal="true"
      style="width: 620px"
    >
      <div
        v-if="seciliFatura"
        class="fatura-detay"
      >
        <div class="fatura-detay-ust">
          <div>
            <strong>{{ seciliFatura.faturaNumarasi }}</strong>
            <span class="fatura-detay-tarih">{{ formatDate(seciliFatura.tarih) }}</span>
          </div>
          <Tag
            :value="seciliFatura.tur === 'SATIS' ? t('cariHesaplar.satis') : t('cariHesaplar.alis')"
            :severity="seciliFatura.tur === 'SATIS' ? 'success' : 'warning'"
          />
        </div>
        <div
          v-if="seciliFatura.cariHesapAd"
          class="fatura-detay-cari"
        >
          {{ t('cariHesaplar.cari') }}: {{ seciliFatura.cariHesapAd }}
        </div>
        <DataTable
          :value="seciliFatura.kalemler || []"
          size="small"
          striped-rows
        >
          <Column :header="t('common.description')">
            <template #body="{ data }">
              {{ data.aciklama }}
            </template>
          </Column>
          <Column :header="t('cariHesaplar.adet')">
            <template #body="{ data }">
              {{ data.adet }}
            </template>
          </Column>
          <Column :header="t('cariHesaplar.birimFiyat')">
            <template #body="{ data }">
              {{ formatCurrency(data.birimFiyat) }}
            </template>
          </Column>
          <Column :header="t('common.amount')">
            <template #body="{ data }">
              {{ formatCurrency(data.tutar) }}
            </template>
          </Column>
        </DataTable>
        <div class="fatura-detay-ozet">
          <div class="ozet-satir">
            <span>{{ t('cariHesaplar.araToplam') }}</span><span>{{ formatCurrency(seciliFatura.araToplam) }}</span>
          </div>
          <div class="ozet-satir">
            <span>{{ t('cariHesaplar.kdv') }}</span><span>{{ formatCurrency(seciliFatura.kdv) }}</span>
          </div>
          <div class="ozet-satir ozet-genel">
            <span>{{ t('cariHesaplar.genelToplam') }}</span><strong>{{ formatCurrency(seciliFatura.genelToplam) }}</strong>
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('stoklar.kapat')"
          icon="pi pi-times"
          class="p-button-text"
          @click="faturaDetayDialog = false"
        />
      </template>
    </Dialog>

    <Message
      v-if="cariHesapStore.error"
      severity="error"
      :text="cariHesapStore.error"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useRouter } from 'vue-router'
import { excelAPI, hareketAPI, notAPI, faturaAPI, stokAPI, cariHesapAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import TabloAyarlari from '../components/TabloAyarlari.vue'
import EmptyState from '../components/EmptyState.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import TahsilatGirDialog from '../components/TahsilatGirDialog.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const cariHesapStore = useCariHesapStore()
const router = useRouter()
const { kopyala } = usePanoyaKopyala()
const { t } = useI18n()

const tabloYogunluk = ref('comfortable')
const kolonlar = computed(() => [
  { field: 'id', header: 'ID', visible: true },
  { field: 'ad', header: t('cariHesaplar.ad'), visible: true },
  { field: 'tur', header: t('cariHesaplar.tur'), visible: true },
  { field: 'yetkiliKisi', header: t('cariHesaplar.yetkili'), visible: true },
  { field: 'telefon', header: t('cariHesaplar.telefon'), visible: true },
  { field: 'krediLimiti', header: t('cariHesaplar.krediLimiti'), visible: true },
  { field: 'odemeVadesi', header: t('cariHesaplar.vadeGun'), visible: true },
  { field: 'bakiye', header: t('cariHesaplar.bakiye'), visible: true }
])

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => {
    showDialog.value = false
  },
  kaydet: () => saveCariHesap()
})

const showDialog = ref(false)
const showHareketlerDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const cariHareketler = ref([])
const cariHareketlerYukleniyor = ref(false)
const selectedCariHesaplar = ref([])
const selectedCariHesap = ref(null)
const aramaMetni = ref('')
let aramaZamanlayici = null
onUnmounted(() => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
})

const filtreTur = ref(null)
const filtreBakiye = ref(null)
const bakiyeFiltreleri = computed(() => [
  { label: t('cariHesaplar.alacakli'), value: 'alacak' },
  { label: t('cariHesaplar.borclu'), value: 'borc' }
])

const cariOzet = ref({})

const cariOzetYukle = async () => {
  try {
    const r = await cariHesapAPI.ozet()
    cariOzet.value = r.data || {}
  } catch {
    cariOzet.value = {}
  }
}

const filtreleriTemizle = () => {
  filtreTur.value = null
  filtreBakiye.value = null
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

const filtreDegisti = () => {
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

const toplamTahsilat = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'TAHSILAT').reduce((s, h) => s + (h.tutar || 0), 0)
)
const toplamOdeme = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'ODEME').reduce((s, h) => s + (h.tutar || 0), 0)
)
const guncelBakiye = computed(() => toplamOdeme.value - toplamTahsilat.value)

const ibanGecerli = computed(() => {
  const val = (form.value.iban || '').replace(/\s/g, '').toUpperCase()
  if (!val || val.length < 5) return null
  if (!val.startsWith('TR') || val.length !== 26) return false
  const rearranged = val.slice(4) + val.slice(0, 4)
  const numeric = rearranged.replace(/[A-Z]/g, (c) => String(c.charCodeAt(0) - 55))
  try {
    return BigInt(numeric) % 97n === 1n
  } catch {
    return false
  }
})

const submitted = ref(false)
const form = ref({
  ad: '',
  tur: '',
  vergiNumarasi: '',
  vergiDairesi: '',
  telefon: '',
  email: '',
  iban: '',
  il: '',
  ilce: '',
  adres: '',
  yetkiliKisi: '',
  yetkiliTelefon: '',
  krediLimiti: null,
  odemeVadesi: 0,
  notlar: '',
  aktif: true
})

const { temizle: formTemizle } = useFormKorumasi(form)

onMounted(async () => {
  await loadCariHesaplar()
  cariOzetYukle()
  try {
    const r = await stokAPI.getAll({ size: 1000 })
    stokSecenekleri.value = r.data?.content || r.data || []
  } catch {
    stokSecenekleri.value = []
  }
})

const cariSayfa = ref(0)
const cariSayfaBoyutu = ref(10)

const loadCariHesaplar = async (sayfa = cariSayfa.value, boyut = cariSayfaBoyutu.value) => {
  loading.value = true
  try {
    const params = { page: sayfa, size: boyut }
    if (aramaMetni.value.trim()) params.q = aramaMetni.value.trim()
    if (filtreTur.value) params.tur = filtreTur.value
    if (filtreBakiye.value) params.bakiyeYonu = filtreBakiye.value
    await cariHesapStore.filtreliCari(params)
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.hataYukleme'))
  } finally {
    loading.value = false
  }
}

const cariSayfaDegisti = (e) => {
  cariSayfa.value = e.page
  cariSayfaBoyutu.value = e.rows
  loadCariHesaplar(e.page, e.rows)
}

const ara = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(async () => {
    cariSayfa.value = 0
    await loadCariHesaplar(0, cariSayfaBoyutu.value)
  }, 300)
}

const openDialog = () => {
  editingId.value = null
  form.value = {
    ad: '',
    tur: '',
    vergiNumarasi: '',
    vergiDairesi: '',
    telefon: '',
    email: '',
    iban: '',
    il: '',
    ilce: '',
    adres: '',
    yetkiliKisi: '',
    yetkiliTelefon: '',
    krediLimiti: null,
    odemeVadesi: 0,
    notlar: '',
    aktif: true
  }
  submitted.value = false
  formTemizle()
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
  submitted.value = false
}

const editCariHesap = (cariHesap) => {
  editingId.value = cariHesap.id
  form.value = {
    ad: cariHesap.ad,
    tur: cariHesap.tur || '',
    vergiNumarasi: cariHesap.vergiNumarasi || '',
    vergiDairesi: cariHesap.vergiDairesi || '',
    telefon: cariHesap.telefon || '',
    email: cariHesap.email || '',
    iban: cariHesap.iban || '',
    il: cariHesap.il || '',
    ilce: cariHesap.ilce || '',
    adres: cariHesap.adres || '',
    yetkiliKisi: cariHesap.yetkiliKisi || '',
    yetkiliTelefon: cariHesap.yetkiliTelefon || '',
    krediLimiti: cariHesap.krediLimiti || null,
    odemeVadesi: cariHesap.odemeVadesi ?? 0,
    notlar: cariHesap.notlar || '',
    aktif: cariHesap.aktif !== false
  }
  submitted.value = false
  formTemizle()
  showDialog.value = true
}

const saveCariHesap = async () => {
  submitted.value = true
  if (!form.value.ad.trim()) {
    toastBildirim.uyari(t('cariHesaplar.adBosOlamaz'))
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await cariHesapStore.updateCariHesap(editingId.value, form.value)
      toastBildirim.basarili(t('cariHesaplar.guncellendi'))
    } else {
      await cariHesapStore.addCariHesap(form.value)
      toastBildirim.basarili(t('cariHesaplar.olusturuldu'))
    }
    formTemizle()
    closeDialog()
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: t('cariHesaplar.silOnayMesaj'),
    header: t('kasa.onay'),
    icon: 'pi pi-exclamation-triangle',
    accept: () => deleteCariHesap(id),
    reject: () => {}
  })
}

const deleteCariHesap = async (id) => {
  try {
    await cariHesapStore.deleteCariHesap(id)
    toastBildirim.basarili(t('cariHesaplar.silindi'))
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.silmeHata'))
  }
}

const batchSil = () => {
  if (selectedCariHesaplar.value.length === 0) return
  confirm.require({
    message: t('cariHesaplar.topluSilOnayMesaj', { n: selectedCariHesaplar.value.length }),
    header: t('cariHesaplar.topluSilmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      const sonuclar = await Promise.allSettled(
        [...selectedCariHesaplar.value].map((c) => cariHesapStore.deleteCariHesap(c.id))
      )
      sonuclar.forEach((r) => {
        if (r.status === 'rejected') {
          toastBildirim.hata(
            r.reason?.response?.data?.message || r.reason?.message || t('cariHesaplar.silmeHata')
          )
        }
      })
      selectedCariHesaplar.value = []
      await loadCariHesaplar()
      toastBildirim.basarili(t('cariHesaplar.topluSilindi'))
    }
  })
}

const batchCsvExport = () => {
  const ids = selectedCariHesaplar.value.map((c) => c.id).join(',')
  window.open(`/api/cari-hesaplar/export/csv?ids=${ids}`, '_blank')
}

const yeniFatura = (cariHesap) => {
  router.push({ name: 'Faturalar', query: { cariId: cariHesap.id } })
}

const topluEmailDialog = ref(false)
const topluEmailKonu = ref('')
const topluEmailMesaj = ref('')

const topluEmailGonder = () => {
  const alicilar = (selectedCariHesaplar.value || [])
    .filter((c) => c.email)
    .map((c) => c.email)
  if (alicilar.length === 0) {
    toastBildirim.uyari(t('cariHesaplar.epostaAdresiYok'))
    return
  }
  const mailto = `mailto:?bcc=${alicilar.join(',')}&subject=${encodeURIComponent(topluEmailKonu.value || '')}&body=${encodeURIComponent(topluEmailMesaj.value || '')}`
  window.open(mailto, '_blank')
  topluEmailDialog.value = false
  toastBildirim.basarili(t('cariHesaplar.epostaIstemiAcildi', { n: alicilar.length }))
}

const tahsilatDialog = ref(false)
const tahsilatHedefCari = ref(null)

const tahsilatAc = async (cariHesap) => {
  tahsilatHedefCari.value = cariHesap
  tahsilatDialog.value = true
  try {
    const r = await faturaAPI.cariFaturalari(cariHesap.id, { size: 200 })
    const faturalar = r.data?.content || r.data || []
    tahsilatHedefCari.value = {
      ...cariHesap,
      acikFaturalar: faturalar
        .filter((f) => Number(f.kalanTutar) > 0)
        .map((f) => ({
          faturaId: f.id,
          faturaNumarasi: f.faturaNumarasi,
          vadeTarihi: f.vadeTarihi,
          kalanTutar: f.kalanTutar
        }))
    }
  } catch {
    tahsilatHedefCari.value = cariHesap
  }
}

const tahsilatSonrasiYenile = async () => {
  tahsilatHedefCari.value = null
  await cariHesapStore.getAllCariHesaplar()
}

const viewHareketler = async (cariHesap) => {  selectedCariHesap.value = cariHesap
  showHareketlerDialog.value = true
  cariHareketlerYukleniyor.value = true
  try {
    const res = await hareketAPI.getByCariHesap(cariHesap.id)
    cariHareketler.value = res.data._embedded
      ? res.data._embedded.hareketler || res.data._embedded.hareketList || []
      : Array.isArray(res.data)
        ? res.data
        : res.data.content || []
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.hareketlerHata'))
    cariHareketler.value = []
  } finally {
    cariHareketlerYukleniyor.value = false
  }
  cariNotlariYukle(cariHesap.id)
  sonUrunleriYukle(cariHesap.id)
  cariFaturalariYukle(cariHesap.id)
  cariOzelFiyatlariYukle(cariHesap.id)
}

const cariOzelFiyatlar = ref([])
const ozelFiyatStok = ref(null)
const ozelFiyatTutar = ref(null)
const stokSecenekleri = ref([])

const cariOzelFiyatlariYukle = async (cariId) => {
  try {
    const r = await cariHesapAPI.getFiyatlar(cariId)
    cariOzelFiyatlar.value = r.data || []
  } catch {
    cariOzelFiyatlar.value = []
  }
}

const cariOzelFiyatEkle = async () => {
  if (!ozelFiyatStok.value || !ozelFiyatTutar.value) {
    toastBildirim.uyari(t('cariHesaplar.urunFiyatSecin'))
    return
  }
  try {
    await cariHesapAPI.fiyatKaydet(selectedCariHesap.value.id, {
      stokId: ozelFiyatStok.value,
      fiyat: ozelFiyatTutar.value
    })
    ozelFiyatStok.value = null
    ozelFiyatTutar.value = null
    cariOzelFiyatlariYukle(selectedCariHesap.value.id)
    toastBildirim.basarili(t('cariHesaplar.ozelFiyatEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('cariHesaplar.fiyatEklenemedi'))
  }
}

const cariOzelFiyatSil = async (f) => {
  try {
    await cariHesapAPI.fiyatSil(f.id)
    cariOzelFiyatlar.value = cariOzelFiyatlar.value.filter((x) => x.id !== f.id)
    toastBildirim.basarili(t('cariHesaplar.ozelFiyatSilindi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('cariHesaplar.silinemedi'))
  }
}

const cariFaturalar = ref([])

const cariFaturalariYukle = async (cariId) => {
  try {
    const r = await faturaAPI.cariFaturalari(cariId, { size: 100 })
    cariFaturalar.value = r.data?.content || r.data || []
  } catch {
    cariFaturalar.value = []
  }
}

const faturaDetayDialog = ref(false)
const seciliFatura = ref(null)

const faturaDetayAc = async (fatura) => {
  try {
    const r = await faturaAPI.getById(fatura.id)
    seciliFatura.value = r.data
    faturaDetayDialog.value = true
  } catch {
    seciliFatura.value = fatura
    faturaDetayDialog.value = true
  }
}

const sonUrunler = ref([])

const sonUrunleriYukle = async (cariId) => {
  try {
    const r = await faturaAPI.cariSonUrunler(cariId, 20)
    sonUrunler.value = r.data || []
  } catch {
    sonUrunler.value = []
  }
}

const seciliUrunFiyatGecmisi = ref(null)

const urunFiyatGecmisiGoster = async (urun) => {
  if (!selectedCariHesap.value || !urun.stokId) return
  try {
    const r = await faturaAPI.cariUrunFiyatGecmisi(selectedCariHesap.value.id, urun.stokId)
    seciliUrunFiyatGecmisi.value = { ...r.data, urunAd: urun.stokAd }
  } catch {
    seciliUrunFiyatGecmisi.value = null
  }
}

const cariNotlar = ref([])
const yeniCariNot = ref('')
const yeniCariNotOnem = ref('NORMAL')
const notOnemSecenekleri = computed(() => [
  { label: t('cariHesaplar.normal'), value: 'NORMAL' },
  { label: t('cariHesaplar.yuksek'), value: 'YUKSEK' },
  { label: t('cariHesaplar.kritik'), value: 'KRITIK' }
])

const cariNotlariYukle = async (cariId) => {
  try {
    const r = await notAPI.cariNotlari(cariId)
    cariNotlar.value = r.data || []
  } catch {
    cariNotlar.value = []
  }
}

const cariNotEkle = async () => {
  const metin = yeniCariNot.value.trim()
  if (!metin || !selectedCariHesap.value) return
  try {
    await notAPI.create({
      baslik: yeniCariNotOnem.value === 'YUKSEK' ? t('cariHesaplar.onemliNot') : t('cariHesaplar.gorusme'),
      icerik: metin,
      cariHesapId: selectedCariHesap.value.id,
      onemDerecesi: yeniCariNotOnem.value
    })
    yeniCariNot.value = ''
    yeniCariNotOnem.value = 'NORMAL'
    await cariNotlariYukle(selectedCariHesap.value.id)
  } catch (err) {
    toastBildirim.hata(t('cariHesaplar.notEklenemedi'))
  }
}

const cariNotSil = async (n) => {
  try {
    await notAPI.delete(n.id)
    await cariNotlariYukle(selectedCariHesap.value.id)
  } catch (err) {
    toastBildirim.hata(t('cariHesaplar.notSilinemedi'))
  }
}

const csvExport = () => {
  window.open('/api/cari-hesaplar/export/csv', '_blank')
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.cariHesaplar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'CariHesaplar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}


import { formatTarih as formatDate } from '../utils/format.js'
</script>

<style scoped>
.fatura-link {
  color: var(--accent);
  cursor: pointer;
  font-weight: 600;
  text-decoration: none;
}
.fatura-link:hover {
  text-decoration: underline;
}
.fatura-detay-ust {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.fatura-detay-tarih {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}
.fatura-detay-cari {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}
.fatura-detay-ozet {
  margin-top: 12px;
  padding: 10px 14px;
  border-top: 1px solid var(--border);
}
.fatura-detay-ozet .ozet-satir {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}
.fatura-detay-ozet .ozet-genel {
  font-weight: 700;
  font-size: 15px;
}
.bakiye-rozet {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
.bakiye-rozet.alacak {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
}
.bakiye-rozet.borc {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
}
.cari-filtreler {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.cari-istatistik {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}
.istatistik-kutu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
}
.istatistik-kutu span {
  font-size: 12px;
  color: var(--text-muted);
}
.istatistik-kutu strong {
  font-size: 18px;
  color: var(--text-primary);
}
.filtre-select {
  width: 180px;
}
.tahsilat-cari {
  margin-bottom: 14px;
  font-size: 14px;
}
.toplu-email-aciklama {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 14px;
}
.cari-ozel-fiyatlar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.ozel-fiyat-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
}
.ozel-fiyat-urun {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
}
.ozel-fiyat-tutar {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}
.ozel-fiyat-ekle {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  align-items: center;
}
.ozel-fiyat-stok-select {
  flex: 1;
}
.ozel-fiyat-tutar-input {
  width: 140px;
}
.cari-faturalar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.cari-urunler {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.cari-urunler-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cari-urun-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.cari-urun-chip:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.cari-urun-ad {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.cari-urun-bilgi {
  font-size: 11px;
  color: var(--text-muted);
}
.cari-urun-fiyat {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}
.cari-urun-fiyat-gecmisi {
  margin-top: 12px;
  padding: 12px;
  background: rgba(139, 92, 246, 0.06);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 8px;
}
.fg-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #a78bfa;
  margin-bottom: 8px;
}
.fg-kapat {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
}
.fg-satir {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 3px 0;
}
.fg-tarih {
  width: 80px;
  flex-shrink: 0;
}
.fg-fatura {
  flex: 1;
}
.fg-adet {
  width: 60px;
  text-align: right;
}
.fg-fiyat {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 80px;
  text-align: right;
}
.cari-notlar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.cari-not-baslik h4 {
  margin: 0 0 10px;
  font-size: 14px;
}
.cari-not-ekle {
  display: flex;
  gap: 8px;
  align-items: center;
}
.not-onem-select {
  width: 110px;
  flex-shrink: 0;
}
.cari-not-baslik-satir {
  display: flex;
  align-items: center;
  gap: 8px;
}
.not-onem-rozet {
  padding: 1px 8px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 700;
}
.not-onem-rozet.yuksek {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}
.not-onem-rozet.kritik {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.cari-not-ekle .p-inputtext {
  flex: 1;
}
.cari-not-bos {
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 0;
}
.cari-not-satir {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.cari-not-icerik strong {
  font-size: 13px;
}
.cari-not-icerik p {
  margin: 2px 0;
  font-size: 13px;
}
.cari-not-icerik small {
  color: var(--text-muted);
  font-size: 11px;
}

.cari-hesaplar-container {
  padding: 0;
  max-width: 100%;
}

h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

h3 {
  color: var(--text-primary);
  margin: 0 0 10px 0;
}

.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}

.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  overflow-x: auto;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
}

.dialog-form {
  padding: 0;
}
.form-section {
  margin-bottom: 20px;
}
.form-section:last-child {
  margin-bottom: 0;
}
.form-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #60a5fa;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 700;
  color: var(--text-primary) !important;
  font-size: 13px;
}
.form-group .required {
  color: #f87171;
}

.form-group :deep(.p-inputtext),
.form-group :deep(.p-textarea) {
  width: 100%;
  background: var(--bg-primary) !important;
  border: 1px solid var(--border) !important;
  border-radius: 8px;
  padding: 10px 12px !important;
  color: var(--text-primary) !important;
  font-size: 14px;
}
.form-group :deep(.p-inputtext:enabled:focus),
.form-group :deep(.p-textarea:enabled:focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
}
.form-group :deep(.p-inputtext.p-invalid),
.form-group :deep(.p-textarea.p-invalid) {
  border-color: #f87171 !important;
}

.error {
  display: block;
  color: #f87171;
  font-size: 11px;
  margin-top: 4px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.positive {
  color: #4caf50;
  font-weight: bold;
}

.negative {
  color: #f44336;
  font-weight: bold;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.badge.tahsilat {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.badge.odeme {
  background-color: #ffebee;
  color: #c62828;
}

.hareket-info {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.hareket-info p {
  margin: 5px 0 0 0;
}

.bakiye-ozet {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.ozet-satir {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ozet-etiket {
  font-size: 12px;
  color: var(--text-secondary);
}

.ozet-bakiye {
  margin-left: auto;
}

.w-full {
  width: 100% !important;
}

.batch-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid var(--border);
}
.batch-count {
  font-size: 12px;
  color: #60a5fa;
  font-weight: 600;
}
.kopyalanabilir {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.kopyalanabilir:hover {
  color: var(--accent);
}
.kopyala-ikon {
  font-size: 11px;
  opacity: 0.5;
}
.kopyalanabilir:hover .kopyala-ikon {
  opacity: 1;
}
.iban-gecerli {
  display: block;
  color: #4caf50;
  font-size: 11px;
  margin-top: 2px;
}
.iban-gecersiz {
  display: block;
  color: #f44336;
  font-size: 11px;
  margin-top: 2px;
}
.iban-yardim {
  display: block;
  color: var(--text-secondary);
  font-size: 11px;
  margin-top: 2px;
}
</style>
