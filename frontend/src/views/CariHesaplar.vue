<template>
  <div class="cari-hesaplar-container">
    <h1 class="page-title">
      {{ t('cariHesaplar.title') }}
    </h1>

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
      </template>
      <template #end>
        <TabloAyarlari
          tablo-key="cari"
          :kolonlar="kolonlar"
          @update:kolonlar="kolonGuncelle"
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
        <Button
          :label="t('cariHesaplar.riskliCariler')"
          icon="pi pi-exclamation-triangle"
          class="p-button-sm p-button-outlined p-button-warning"
          style="margin-right: 8px"
          @click="riskliCarilerAc"
        />
        <Button
          :label="t('cariHesaplar.topluGuncelle')"
          icon="pi pi-pencil"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
          :disabled="selectedCariHesaplar.length === 0"
          @click="topluGuncelleAc"
        />
        <!-- REDTEAM/Faz3.3: `p-input-icon-left` PrimeVue 4'te kaldirildi. -->
        <IconField>
          <InputIcon class="pi pi-search" />
          <InputText
            ref="aramaGirdiRef"
            v-model="aramaMetni"
            :placeholder="t('cariHesaplar.aramaPlaceholder')"
            @input="ara"
          />
        </IconField>
      </template>
    </Toolbar>

    <div
      v-if="loading"
      class="loading-iskelet"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="6"
        height="44px"
      />
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
        <span>{{ t('cariHesaplar.bizeAlacakli') }}</span>
        <strong class="positive">{{ formatCurrency(cariOzet.alacakli ?? 0) }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>{{ t('cariHesaplar.bizeBorclu') }}</span>
        <strong class="negative">{{ formatCurrency(cariOzet.borclu ?? 0) }}</strong>
      </div>
    </div>

    <div
      v-if="!loading"
      class="cari-filtreler"
    >
      <Dropdown
        v-model="filtreTur"
        :options="cariTurSecenekleri"
        option-label="label"
        option-value="value"
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
      <InputText
        v-model="filtreEtiket"
        :placeholder="t('cariHesaplar.etiketFiltre')"
        class="filtre-select"
        @keyup.enter="filtreEtiketAra"
        @blur="filtreEtiketAra"
      />
      <Button
        v-if="filtreTur || filtreBakiye || filtreEtiket"
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
      <AppDataTable
        v-model:selection="selectedCariHesaplar"
        :value="cariHesapStore?.cariHesaplar || []"
        data-key="id"
        striped-rows
        :size="tabloYogunluk === 'compact' ? 'small' : 'normal'"
        :lazy="true"
        :total-records="cariHesapStore.toplamKayit"
        :rows="cariSayfaBoyutu"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        :current-page-report-template="'{first} - {last} ({totalRecords} ' + $t('common.recordsWord') + ')'"
        gorunum-anahtari="cari-hesaplar"
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
          style="width: 240px"
        >
          <template #body="s">
            <div class="cari-ad-hucre">
              <img
                v-if="s.data.fotoThumbUrl || s.data.fotoUrl"
                :src="s.data.fotoThumbUrl || s.data.fotoUrl"
                class="cari-thumb"
                alt=""
                loading="lazy"
                decoding="async"
              >
              <span
                v-else
                class="cari-thumb-yok"
              ><i class="pi pi-user" /></span>
              <span>{{ s.data.ad }}</span>
            </div>
          </template>
        </Column>
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
          field="temsilciAd"
          :header="t('cariHesaplar.satisTemsilcisi')"
          style="width: 140px"
        >
          <template #body="slotProps">
            <!-- Temsilci performans raporu bu alana göre gruplar; atanmamış
                 cariler raporda tek satırda toplanıyor. -->
            <span v-if="slotProps.data.temsilciAd">{{ slotProps.data.temsilciAd }}</span>
            <span
              v-else
              class="gizli-veri"
            >-</span>
          </template>
        </Column>
        <Column
          v-if="kolonlar[8].visible"
          field="bakiye"
          :header="t('cariHesaplar.bakiye')"
          class="sayisal"
          style="width: 140px"
        >
          <template #body="slotProps">
            <!-- Bakiye işareti kuralı: negatif = cari bize borçlu (ALACAK).
                 Önceden `>= 0` kontrolü borçlu müşteriyi kırmızı "alacak"
                 rozetiyle gösteriyordu. -->
            <span
              class="bakiye-rozet gizli-veri"
              :class="borcluMu(slotProps.data.bakiye) ? 'borc' : 'alacak'"
              :title="borcluMu(slotProps.data.bakiye) ? t('cariHesaplar.bizeBorclu') : t('cariHesaplar.bizeAlacakli')"
            >
              <i :class="borcluMu(slotProps.data.bakiye) ? 'pi pi-arrow-down' : 'pi pi-arrow-up'" />
              {{ formatCurrency(Math.abs(slotProps.data.bakiye || 0)) }}
            </span>
          </template>
        </Column>
        <Column
          v-if="kolonlar[9].visible"
          field="etiketler"
          :header="t('cariHesaplar.etiketler')"
          style="width: 160px"
        >
          <template #body="slotProps">
            <span
              v-if="slotProps.data.etiketler"
              class="etiket-rozet"
            >{{ slotProps.data.etiketler }}</span>
            <span
              v-else
              class="gizli-veri"
            >-</span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 150px"
        >
          <template #body="slotProps">
            <div class="satir-islemler">
              <Button
                icon="pi pi-file-plus"
                class="p-button-rounded p-button-success p-button-sm"
                :title="t('cariHesaplar.yeniFatura')"
                @click="yeniFatura(slotProps.data)"
              />
              <Button
                icon="pi pi-pencil"
                class="p-button-rounded p-button-warning p-button-sm"
                :title="t('common.edit')"
                @click="editCariHesap(slotProps.data)"
              />
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="cariEylemleri(slotProps.data)"
              />
            </div>
          </template>
        </Column>
        <template #batch-actions>
          <Button
            :label="t('cariHesaplar.topluEposta')"
            icon="pi pi-envelope"
            class="p-button-sm p-button-info"
            @click="topluEmailDialog = true"
          />
          <!--
              REDTEAM/Faz3.2: `v-permission="'CARI_DELETE'"` burada
              GEREKSIZDI ve yanlisti: backend `CariHesapController:160`
              toplu silmeyi `hasRole('ADMIN')` ile koruyor (tekil silme ise
              `CARI_DELETE` kabul ediyor). `v-if="isAdmin"` zaten vardi ve
              dogru kontrol bu; iki kontrol yan yana birakildiginda biri
              sessizce etkisiz kaliyordu. Yanlis olan silindi.
            -->
          <Button
            v-if="isAdmin"
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
        </template>
        <template #empty>
          <EmptyState
            v-if="cariHesapStore?.cariHesaplar?.length === 0"
            :message="t('cariHesaplar.empty')"
            :sub-message="t('cariHesaplar.emptyHint')"
            icon="pi pi-users"
            :action-label="t('cariHesaplar.emptyAction')"
            action-icon="pi pi-plus"
            @action="openDialog"
          />
        </template>
      </AppDataTable>
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
          <div class="form-group">
            <label>{{ t('cariHesaplar.fotograf') }}</label>
            <div class="foto-satir">
              <img
                v-if="form.fotoUrl || form.fotoThumbUrl"
                :src="form.fotoThumbUrl || form.fotoUrl"
                class="foto-onizle"
                alt="foto"
                loading="lazy"
                decoding="async"
              >
              <input
                ref="fotoInput"
                type="file"
                accept="image/*"
                hidden
                @change="fotoSec"
              >
              <Button
                :label="t('cariHesaplar.fotografYukle')"
                icon="pi pi-image"
                class="p-button-outlined"
                @click="$refs.fotoInput.click()"
              />
              <Button
                v-if="form.fotoUrl || form.fotoThumbUrl"
                :label="t('stoklar.kaldir')"
                icon="pi pi-times"
                class="p-button-text p-button-danger"
                @click="form.fotoUrl = ''; form.fotoThumbUrl = ''"
              />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="tur">{{ t('cariHesaplar.cariTuru') }}</label>
              <Dropdown
                v-model="form.tur"
                :options="cariTurSecenekleri"
                option-label="label"
                option-value="value"
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
            <div class="form-group">
              <label for="paraBirimi">{{ t('cariHesaplar.paraBirimi') }}</label>
              <Dropdown
                id="paraBirimi"
                v-model="form.paraBirimi"
                :options="paraBirimiSecenekleri"
                option-label="label"
                option-value="value"
                class="w-full"
              />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.ekBilgiler') }}
          </div>
          <!-- Satis temsilcisi: temsilci performans raporu bu alana gore gruplar.
               Once yazma yolu yoktu, alan her zaman null kaldi ve rapor tek
               satirda "temsilci atanmamis" donuyordu. -->
          <div class="form-group">
            <label for="temsilciId">{{ t('cariHesaplar.satisTemsilcisi') }}</label>
            <Dropdown
              id="temsilciId"
              v-model="form.temsilciId"
              :options="temsilciSecenekleri"
              option-label="ad"
              option-value="id"
              :placeholder="t('cariHesaplar.temsilciSeciniz')"
              class="w-full"
              show-clear
              filter
              filter-by="ad,email"
              :loading="temsilcilerYukleniyor"
            />
          </div>
          <div class="form-group">
            <label for="etiketler">{{ t('cariHesaplar.etiketler') }}</label>
            <InputText
              id="etiketler"
              v-model="form.etiketler"
              :placeholder="t('cariHesaplar.etiketlerPlaceholder')"
              class="w-full"
            />
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
          <div class="ozet-satir">
            <span class="ozet-etiket">{{ t('cariHesaplar.toplamBorclandirma') }}</span>
            <span class="negative">{{ formatCurrency(toplamBorclandirma) }}</span>
          </div>
          <div class="ozet-satir ozet-bakiye">
            <span class="ozet-etiket">{{ t('cariHesaplar.guncelBakiye') }}</span>
            <strong :class="guncelBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(guncelBakiye) }}</strong>
          </div>
        </div>
      </div>

      <div class="ekstre-arac">
        <DatePicker
          v-model="ekstreBaslangic"
          :placeholder="t('cariHesaplar.ekstreBaslangic')"
          date-format="dd.mm.yy"
          show-icon
          class="ekstre-tarih"
        />
        <DatePicker
          v-model="ekstreBitis"
          :placeholder="t('cariHesaplar.ekstreBitis')"
          date-format="dd.mm.yy"
          show-icon
          class="ekstre-tarih"
        />
        <Button
          :label="t('cariHesaplar.ekstreYazdir')"
          icon="pi pi-print"
          class="p-button-sm p-button-outlined"
          @click="cariEkstreYazdir"
        />
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
        <AppDataTable
          :value="cariHareketlerBakiye"
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
              <span :class="['badge', hareketBadgeSinifi(slotProps.data.tur)]">
                {{ hareketTuruEtiketi(slotProps.data.tur) }}
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
                {{ slotProps.data.tur === 'TAHSILAT' ? '+' : '-' }}{{ formatCurrency(slotProps.data.tutar) }}
              </span>
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            field="bakiye"
            :header="t('cariHesaplar.bakiye')"
            style="width: 130px"
          >
            <template #body="slotProps">
              <span :class="slotProps.data.bakiye >= 0 ? 'positive' : 'negative'">
                {{ formatCurrency(slotProps.data.bakiye) }}
              </span>
            </template>
          </Column>
          <template #empty>
            <EmptyState
              v-if="cariHareketler && cariHareketler.length === 0"
              :message="t('cariHesaplar.hareketYok')"
              icon="pi pi-list"
            />
          </template>
        </AppDataTable>

        <TabView
          class="cari-sekmeler"
          @update:active-index="cariSekmeDegisti"
        >
          <TabPanel :header="t('cariHesaplar.gecmisFaturalarTab')">
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
              <AppDataTable
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
              </AppDataTable>
            </div>
          </TabPanel>

          <TabPanel :header="t('cariHesaplar.ozelFiyatlar')">
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
                  :aria-label="$t('common.delete')"
                  class="p-button-rounded p-button-text p-button-danger p-button-sm"
                  @click="cariOzelFiyatSil(f)"
                />
              </div>
              <div class="ozel-fiyat-ekle">
                <!-- Sunucu aramali stok secici. Sayfa acilisinda 1000 kayit
                     cekiliyordu, ama bu dropdown yalnizca "Ozel Fiyatlar"
                     sekmesine girildiginde gorunuyor; 1000. stoktan sonrasi
                     zaten secilemiyordu. -->
                <Dropdown
                  v-model="ozelFiyatStok"
                  :options="stokOnerileri"
                  option-label="ad"
                  option-value="id"
                  :placeholder="t('cariHesaplar.urunSec')"
                  filter
                  filter-by="ad,stokKodu,barkod"
                  class="ozel-fiyat-stok-select"
                  @filter="stokAra"
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
          </TabPanel>

          <TabPanel :header="t('cariHesaplar.gecmisteAldigiUrunler')">
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
                    :aria-label="t('common.close')"
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
          </TabPanel>

          <TabPanel :header="t('cariHesaplar.gorusmeNotlari')">
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
                  :aria-label="$t('common.add')"
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
                  <small>{{ formatTarihSaat(n.olusturmaTarihi, '') }}</small>
                </div>
                <Button
                  icon="pi pi-trash"
                  :aria-label="$t('common.delete')"
                  class="p-button-rounded p-button-text p-button-danger p-button-sm"
                  @click="cariNotSil(n)"
                />
              </div>
            </div>
          </TabPanel>
        </TabView>
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

    <BorclandirmaGirDialog
      v-model:visible="borclandirmaDialog"
      :cari="borclandirmaHedefCari"
      @kaydedildi="borclandirmaSonrasiYenile"
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

    <CariFaturaDetayDialog
      v-model:visible="faturaDetayDialog"
      :fatura="seciliFatura"
    />

    <Message
      v-if="cariHesapStore.error"
      severity="error"
      :text="cariHesapStore.error"
    />

    <CariKart360Dialog
      v-model:visible="kartDialogAcik"
      :cari-id="kartCariId"
    />

    <Dialog
      v-model:visible="acilisDialogVisible"
      :header="t('cariHesaplar.acilisFisi')"
      modal
      :draggable="false"
      :style="{ width: '440px', maxWidth: '95vw' }"
    >
      <p class="toplu-secim-bilgi">
        {{ acilisCari?.ad }}
      </p>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.acilisTutar') }}</label>
        <InputNumber
          v-model="acilisForm.tutar"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
          :placeholder="t('cariHesaplar.acilisTutarYardim')"
        />
      </div>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.acilisTarih') }}</label>
        <DatePicker
          v-model="acilisForm.tarih"
          date-format="dd.mm.yy"
          show-icon
          class="w-full"
        />
      </div>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.acilisAciklama') }}</label>
        <InputText
          v-model="acilisForm.aciklama"
          class="w-full"
        />
      </div>
      <small>{{ t('cariHesaplar.acilisYardim') }}</small>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="acilisDialogVisible = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="acilisKaydediliyor"
          @click="acilisKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="adresDialogVisible"
      :header="t('cariHesaplar.adreslerBaslik')"
      modal
      :draggable="false"
      :style="{ width: '760px', maxWidth: '95vw' }"
    >
      <p class="toplu-secim-bilgi">
        {{ adresCari?.ad }}
      </p>
      <DataTable
        :value="adresler"
        data-key="id"
        striped-rows
        responsive-layout="scroll"
        :paginator="adresler.length > 5"
        :rows="5"
      >
        <Column
          field="baslik"
          :header="t('cariHesaplar.adresBaslik')"
        />
        <Column
          field="adres"
          :header="t('cariHesaplar.adres')"
        />
        <Column
          field="il"
          :header="t('cariHesaplar.il')"
        />
        <Column :header="t('cariHesaplar.adresVarsayilan')">
          <template #body="{ data }">
            <i
              v-if="data.varsayilan"
              class="pi pi-check"
            />
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 80px"
        >
          <template #body="{ data }">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="adresSil(data)"
            />
          </template>
        </Column>
        <template #empty>
          <p>{{ t('cariHesaplar.adresYok') }}</p>
        </template>
      </DataTable>

      <Divider />

      <div class="adres-form">
        <InputText
          v-model="adresForm.baslik"
          :placeholder="t('cariHesaplar.adresBaslik')"
        />
        <InputText
          v-model="adresForm.adres"
          :placeholder="t('cariHesaplar.adres')"
          class="genis"
        />
        <InputText
          v-model="adresForm.il"
          :placeholder="t('cariHesaplar.il')"
        />
        <InputText
          v-model="adresForm.ilce"
          :placeholder="t('cariHesaplar.ilce')"
        />
        <InputText
          v-model="adresForm.yetkiliKisi"
          :placeholder="t('cariHesaplar.yetkili')"
        />
        <InputText
          v-model="adresForm.telefon"
          :placeholder="t('cariHesaplar.telefon')"
        />
        <div class="varsayilan-alan">
          <Checkbox
            v-model="adresForm.varsayilan"
            :binary="true"
            input-id="adresVarsayilan"
          />
          <label for="adresVarsayilan">{{ t('cariHesaplar.adresVarsayilan') }}</label>
        </div>
        <Button
          :label="t('common.add')"
          icon="pi pi-plus"
          :loading="adresKaydediliyor"
          @click="adresEkleKaydet"
        />
      </div>
    </Dialog>

    <Dialog
      v-model:visible="topluDialogVisible"
      :header="t('cariHesaplar.topluGuncelleBaslik')"
      modal
      :draggable="false"
      :style="{ width: '460px', maxWidth: '95vw' }"
    >
      <p class="toplu-secim-bilgi">
        {{ t('cariHesaplar.topluSecilen', { sayi: selectedCariHesaplar.length }) }}
      </p>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.tur') }}</label>
        <Dropdown
          v-model="topluForm.tur"
          :options="cariTurSecenekleri"
          option-label="label"
          option-value="value"
          show-clear
          :placeholder="t('cariHesaplar.degisiklikYok')"
          class="w-full"
        />
      </div>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.krediLimiti') }}</label>
        <InputNumber
          v-model="topluForm.krediLimiti"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          :placeholder="t('cariHesaplar.degisiklikYok')"
          class="w-full"
        />
      </div>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.vadeGun') }}</label>
        <InputNumber
          v-model="topluForm.odemeVadesi"
          :min="0"
          :placeholder="t('cariHesaplar.degisiklikYok')"
          class="w-full"
        />
      </div>
      <div class="form-alani">
        <label>{{ t('cariHesaplar.durum') }}</label>
        <Dropdown
          v-model="topluForm.aktif"
          :options="aktifSecenekleri"
          option-label="label"
          option-value="value"
          show-clear
          :placeholder="t('cariHesaplar.degisiklikYok')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="topluDialogVisible = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="topluKaydediliyor"
          @click="topluGuncelleKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="riskDialogVisible"
      :header="t('cariHesaplar.riskliCarilerBaslik')"
      modal
      :draggable="false"
      :style="{ width: '760px', maxWidth: '95vw' }"
    >
      <div
        v-if="riskYukleniyor"
        class="loading-iskelet"
      >
        <SkeletonLoader
          :count="4"
          height="40px"
        />
      </div>
      <DataTable
        v-else
        :value="riskliCariler"
        data-key="cariId"
        striped-rows
        responsive-layout="scroll"
        :paginator="riskliCariler.length > 10"
        :rows="10"
      >
        <Column
          field="cariAd"
          :header="t('cariHesaplar.ad')"
        />
        <Column
          :header="t('cariHesaplar.borc')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.borc) }}
          </template>
        </Column>
        <Column
          :header="t('cariHesaplar.krediLimiti')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.krediLimiti) }}
          </template>
        </Column>
        <Column
          :header="t('cariHesaplar.asimTutari')"
        >
          <template #body="{ data }">
            <span class="negative">{{ formatCurrency(data.asimTutari) }}</span>
          </template>
        </Column>
        <Column
          :header="t('cariHesaplar.riskOrani')"
        >
          <template #body="{ data }">
            %{{ data.riskOrani }}
          </template>
        </Column>
        <template #empty>
          <p>{{ t('cariHesaplar.riskliYok') }}</p>
        </template>
      </DataTable>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { useRouter } from 'vue-router'
import { excelAPI, hareketAPI, notAPI, faturaAPI, stokAPI, cariHesapAPI, uploadAPI, kullaniciAPI, raporAPI } from '../api/index.js'
import { resimDogrula } from '../utils/dosyaDogrula.js'
import { resimSikistir } from '../utils/resimSikistir.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import TabloAyarlari from '../components/TabloAyarlari.vue'
import EmptyState from '../components/EmptyState.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import TahsilatGirDialog from '../components/TahsilatGirDialog.vue'
import BorclandirmaGirDialog from '../components/BorclandirmaGirDialog.vue'
import CariKart360Dialog from '../components/CariKart360Dialog.vue'
import CariFaturaDetayDialog from '../components/CariFaturaDetayDialog.vue'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import { formatCurrency } from '../utils/format.js'
import { escapeHtml } from '../utils/escapeHtml.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const cariHesapStore = useCariHesapStore()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore?.kullanici?.role === 'ADMIN')
const router = useRouter()
const { kopyala } = usePanoyaKopyala()
const { t } = useI18n()

const tabloYogunluk = ref('comfortable')
const kolonGorunurluk = ref({})
const varsayilanKolonlar = computed(() => [
  { field: 'id', header: 'ID' },
  { field: 'ad', header: t('cariHesaplar.ad') },
  { field: 'tur', header: t('cariHesaplar.tur') },
  { field: 'yetkiliKisi', header: t('cariHesaplar.yetkili') },
  { field: 'telefon', header: t('cariHesaplar.telefon') },
  { field: 'krediLimiti', header: t('cariHesaplar.krediLimiti') },
  { field: 'odemeVadesi', header: t('cariHesaplar.vadeGun') },
  { field: 'temsilciAd', header: t('cariHesaplar.satisTemsilcisi') },
  { field: 'bakiye', header: t('cariHesaplar.bakiye') },
  { field: 'etiketler', header: t('cariHesaplar.etiketler') }
])
const kolonlar = computed(() =>
  varsayilanKolonlar.value.map((k) => ({ ...k, visible: kolonGorunurluk.value[k.field] !== false }))
)
const kolonGuncelle = (yeni) => {
  const map = {}
  ;(yeni || []).forEach((k) => {
    map[k.field] = k.visible !== false
  })
  kolonGorunurluk.value = map
}

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => {
    showDialog.value = false
  },
  kaydet: () => saveCariHesap(),
  // Ctrl+K: arama kutusuna odaklan. Önceden placeholder'da "Ctrl+F" yazıyordu
  // ama hiçbir kısayol bağlı değildi (tarayıcının kendi bul işlevi açılıyordu).
  ara: () => {
    const el = aramaGirdiRef.value?.$el || aramaGirdiRef.value
    el?.focus?.()
    el?.select?.()
  }
})

const showDialog = ref(false)
const showHareketlerDialog = ref(false)
const kartDialogAcik = ref(false)
const kartCariId = ref(null)
const riskDialogVisible = ref(false)
const riskliCariler = ref([])
const riskYukleniyor = ref(false)
const topluDialogVisible = ref(false)
const topluKaydediliyor = ref(false)
const topluForm = ref({ tur: null, krediLimiti: null, odemeVadesi: null, aktif: null })
const adresDialogVisible = ref(false)
const adresCari = ref(null)
const adresler = ref([])
const adresKaydediliyor = ref(false)
const adresForm = ref({ baslik: '', adres: '', il: '', ilce: '', yetkiliKisi: '', telefon: '', varsayilan: false })
const acilisDialogVisible = ref(false)
const acilisCari = ref(null)
const acilisKaydediliyor = ref(false)
const acilisForm = ref({ tutar: null, tarih: null, aciklama: '' })
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const cariHareketler = ref([])
const cariHareketlerYukleniyor = ref(false)
// Faz 2.2: tarih aralıklı ekstre (boşsa tüm hareketler yazdırılır).
const ekstreBaslangic = ref(null)
const ekstreBitis = ref(null)
const selectedCariHesaplar = ref([])
const selectedCariHesap = ref(null)
const aramaMetni = ref('')
const aramaGirdiRef = ref(null)
let aramaZamanlayici = null
onUnmounted(() => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
})

const filtreTur = ref(null)
const filtreBakiye = ref(null)
const filtreEtiket = ref('')
// Değerler backend'in beklediği ASCII sabitler; etiketler yerelleştirilir.
const cariTurSecenekleri = computed(() => [
  { label: t('cariTur.musteri'), value: 'Musteri' },
  { label: t('cariTur.tedarikci'), value: 'Tedarikci' },
  { label: t('cariTur.herIkisi'), value: 'Her Ikisi' }
])
const bakiyeFiltreleri = computed(() => [
  { label: t('cariHesaplar.bizeAlacakli'), value: 'alacak' },
  { label: t('cariHesaplar.bizeBorclu'), value: 'borc' }
])
const aktifSecenekleri = computed(() => [
  { label: t('common.active'), value: true },
  { label: t('common.passive'), value: false }
])
// Faz 2.9: cari çalışma para birimi.
const paraBirimiSecenekleri = [
  { label: 'TRY (₺)', value: 'TRY' },
  { label: 'USD ($)', value: 'USD' },
  { label: 'EUR (€)', value: 'EUR' },
  { label: 'GBP (£)', value: 'GBP' }
]

/**
 * Bakiye işareti kuralı (backend ile aynı): negatif = cari bize borçlu.
 * @param {number|null} bakiye
 * @returns {boolean} true → bize borçlu (borç)
 */
const borcluMu = (bakiye) => Number(bakiye || 0) < 0

const cariOzet = ref({})

const cariOzetYukle = async () => {
  try {
    const r = await cariHesapAPI.ozet()
    cariOzet.value = r.data || {}
  } catch {
    cariOzet.value = {}
  }
}

// Faz 2.1: kredi limitini aşan cariler (risk listesi).
const riskliCarilerAc = async () => {
  riskDialogVisible.value = true
  if (riskliCariler.value.length) return
  riskYukleniyor.value = true
  try {
    const r = await cariHesapAPI.riskliCariler()
    riskliCariler.value = unwrapList(r.data) || []
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    riskYukleniyor.value = false
  }
}

// Faz 2.3: seçili carilerde toplu alan güncelleme.
const topluGuncelleAc = () => {
  topluForm.value = { tur: null, krediLimiti: null, odemeVadesi: null, aktif: null }
  topluDialogVisible.value = true
}

const topluGuncelleKaydet = async () => {
  const payload = { idler: selectedCariHesaplar.value.map((c) => c.id) }
  const f = topluForm.value
  if (f.tur != null) payload.tur = f.tur
  if (f.krediLimiti != null) payload.krediLimiti = f.krediLimiti
  if (f.odemeVadesi != null) payload.odemeVadesi = f.odemeVadesi
  if (f.aktif != null) payload.aktif = f.aktif
  if (Object.keys(payload).length <= 1) {
    toastBildirim.uyari(t('cariHesaplar.topluAlanSec'))
    return
  }
  topluKaydediliyor.value = true
  try {
    await cariHesapAPI.topluGuncelle(payload)
    toastBildirim.basarili(t('cariHesaplar.topluGuncellendi'))
    topluDialogVisible.value = false
    selectedCariHesaplar.value = []
    loadCariHesaplar(cariSayfa.value, cariSayfaBoyutu.value)
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    topluKaydediliyor.value = false
  }
}

// Faz 2.5: cari çoklu adres yönetimi.
const bosAdresForm = () => ({ baslik: '', adres: '', il: '', ilce: '', yetkiliKisi: '', telefon: '', varsayilan: false })

const adresleriYukle = async () => {
  if (!adresCari.value) return
  try {
    const r = await cariHesapAPI.adresler(adresCari.value.id)
    adresler.value = unwrapList(r.data) || []
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  }
}

const adreslerAc = async (c) => {
  adresCari.value = c
  adresForm.value = bosAdresForm()
  adresDialogVisible.value = true
  await adresleriYukle()
}

const adresEkleKaydet = async () => {
  // Cift gonderim engeli: ayni adres iki kez eklenmesin.
  if (adresKaydediliyor.value) return
  if (!adresForm.value.adres || !adresForm.value.adres.trim()) {
    toastBildirim.uyari(t('cariHesaplar.adresZorunlu'))
    return
  }
  adresKaydediliyor.value = true
  try {
    await cariHesapAPI.adresEkle(adresCari.value.id, adresForm.value)
    toastBildirim.basarili(t('cariHesaplar.adresEklendi'))
    adresForm.value = bosAdresForm()
    await adresleriYukle()
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    adresKaydediliyor.value = false
  }
}

const adresSil = async (a) => {
  try {
    await cariHesapAPI.adresSil(adresCari.value.id, a.id)
    toastBildirim.basarili(t('cariHesaplar.adresSilindi'))
    await adresleriYukle()
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  }
}

// Faz 2.8: açılış fişi / devir kaydı.
const acilisAc = (c) => {
  acilisCari.value = c
  acilisForm.value = { tutar: null, tarih: new Date(), aciklama: '' }
  acilisDialogVisible.value = true
}

const acilisKaydet = async () => {
  // Cift gonderim engeli: ayni acilis hareketi iki kez yazilmasin.
  if (acilisKaydediliyor.value) return
  if (!acilisForm.value.tutar) {
    toastBildirim.uyari(t('cariHesaplar.acilisTutarZorunlu'))
    return
  }
  acilisKaydediliyor.value = true
  try {
    await hareketAPI.acilis({
      cariHesapId: acilisCari.value.id,
      tutar: acilisForm.value.tutar,
      tarih: acilisForm.value.tarih ? tarihIso(acilisForm.value.tarih) : null,
      aciklama: acilisForm.value.aciklama
    })
    toastBildirim.basarili(t('cariHesaplar.acilisKaydedildi'))
    acilisDialogVisible.value = false
    loadCariHesaplar(cariSayfa.value, cariSayfaBoyutu.value)
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    acilisKaydediliyor.value = false
  }
}

const filtreleriTemizle = () => {
  filtreTur.value = null
  filtreBakiye.value = null
  filtreEtiket.value = ''
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

const filtreDegisti = () => {
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

// Faz 2.4: etiket filtresi (Enter/odak kaybında uygula).
const filtreEtiketAra = () => {
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

const toplamTahsilat = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'TAHSILAT').reduce((s, h) => s + (h.tutar || 0), 0)
)
const toplamOdeme = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'ODEME').reduce((s, h) => s + (h.tutar || 0), 0)
)
const toplamBorclandirma = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'BORC').reduce((s, h) => s + (h.tutar || 0), 0)
)
// Hareket tablosu `cari.hareket` kayitlarini gosterir; SATIS/ALIS faturalari
// burada satir acmaz (FaturaService cari bakiyeyi dogrudan gunceller). Bu yuzden
// hareketlerden hesaplanan bakiye, kaydin gercek bakiyesiyle UYUSMAZ: 50.000 TL
// acik faturali bir cari listede -50.000 gosterirken burada 0 cikardi.
// KAYIT BAKIYESI (negatif = cari bize borcu) esas alinir; hareketlerin saf
// etkisi ekstrede gorunur, kaydi yalnizca baslangic bakiyesi olarak telafi eder.
const netHareketEtkisi = computed(() =>
  toplamTahsilat.value - toplamOdeme.value - toplamBorclandirma.value
)

const guncelBakiye = computed(() => {
  const kayit = selectedCariHesap.value?.bakiye
  return kayit != null ? kayit : netHareketEtkisi.value
})

// Hareketlerden onceki (acilis) bakiye: en son hareketin yuruyen bakiyesi
// kaydin gercek bakiyesine tam otursun diye fark kadar eklenir.
const hareketBaslangicBakiyesi = computed(() => {
  const kayit = selectedCariHesap.value?.bakiye
  if (kayit == null) return 0
  return kayit - netHareketEtkisi.value
})

const hareketDelta = (h) => (h.tur === 'TAHSILAT' ? h.tutar || 0 : -(h.tutar || 0))

// Ekstre: tarihe gore artan sirali, yuruyen bakiye kolonlu hareket listesi.
const cariHareketlerBakiye = computed(() => {
  const sirali = [...cariHareketler.value].sort(
    (a, b) => new Date(a.hareketTarihi || 0) - new Date(b.hareketTarihi || 0)
  )
  let bakiye = hareketBaslangicBakiyesi.value
  return sirali.map((h) => {
    bakiye += hareketDelta(h)
    return { ...h, bakiye }
  })
})

// Faz 2.2: Date nesnesini YYYY-MM-DD'ye çevirir.
const tarihIso = (d) => {
  const x = new Date(d)
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`
}

const cariEkstreYazdir = async () => {
  const cari = selectedCariHesap.value
  if (!cari) return
  const aralikli = !!(ekstreBaslangic.value && ekstreBitis.value)
  let satirlar = ''
  let basBakiye = null
  let sonBakiye = guncelBakiye.value
  try {
    if (aralikli) {
      // Faz 2.2: sunucudan tarih aralıklı, devir/yürüyen bakiyeli ekstre.
      const r = await raporAPI.cariEkstre({
        cariHesapId: cari.id,
        baslangic: tarihIso(ekstreBaslangic.value),
        bitis: tarihIso(ekstreBitis.value)
      })
      const e = r.data || {}
      basBakiye = e.donemBasBakiye
      sonBakiye = e.donemSonBakiye
      satirlar = (e.hareketler || [])
        .map(
          (h) => `<tr>
            <td>${formatDate(h.tarih)}</td>
            <td>${escapeHtml(h.tur || '')}</td>
            <td class="sag">${h.borc ? formatCurrency(h.borc) : ''}</td>
            <td class="sag">${h.alacak ? formatCurrency(h.alacak) : ''}</td>
            <td class="sag ${(h.yuruyenBakiye || 0) >= 0 ? 'poz' : 'neg'}">${h.yuruyenBakiye != null ? formatCurrency(h.yuruyenBakiye) : ''}</td>
            <td>${escapeHtml(h.aciklama || '')}</td>
          </tr>`
        )
        .join('')
    } else {
      satirlar = cariHareketlerBakiye.value
        .map(
          (h) => `<tr>
            <td>${formatDate(h.hareketTarihi)}</td>
            <td>${escapeHtml(hareketTuruEtiketi(h.tur))}</td>
            <td class="sag">${h.tur === 'TAHSILAT' ? '' : formatCurrency(h.tutar)}</td>
            <td class="sag">${h.tur === 'TAHSILAT' ? formatCurrency(h.tutar) : ''}</td>
            <td class="sag ${h.bakiye >= 0 ? 'poz' : 'neg'}">${formatCurrency(h.bakiye)}</td>
            <td>${escapeHtml(h.aciklama || '')}</td>
          </tr>`
        )
        .join('')
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('common.error'))
    return
  }
  const aralikMetni = aralikli
    ? `${formatDate(tarihIso(ekstreBaslangic.value))} - ${formatDate(tarihIso(ekstreBitis.value))}`
    : ''
  const basSatiri =
    basBakiye != null
      ? `<div class="ozet">${escapeHtml(t('cariHesaplar.ekstreDevir'))}: ${formatCurrency(basBakiye)}</div>`
      : ''
  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
    <title>${escapeHtml(t('cariHesaplar.ekstreBaslik'))}</title>
    <style>
      body{font-family:'Segoe UI',Arial,sans-serif;color:#111;margin:24px;font-size:12px;}
      h1{font-size:16px;margin:0 0 4px;} h2{font-size:14px;margin:0 0 12px;color:#444;}
      table{width:100%;border-collapse:collapse;margin-top:12px;}
      th,td{border-bottom:1px solid #ddd;padding:6px 8px;text-align:left;}
      th{background:#f3f4f6;} .sag{text-align:right;} .poz{color:#15803d;} .neg{color:#b91c1c;}
      .ozet{margin-top:12px;text-align:right;font-size:13px;}
      @media print{.no-print{display:none;}}
    </style></head><body>
      <div class="no-print" style="text-align:right;margin-bottom:8px;">
        <button onclick="window.print()">${escapeHtml(t('common.print'))}</button>
      </div>
      <h1>${escapeHtml(t('cariHesaplar.ekstreBaslik'))}</h1>
      <h2>${escapeHtml(cari.ad || '')}${aralikMetni ? ' — ' + escapeHtml(aralikMetni) : ''}</h2>
      <table><thead><tr>
        <th>${escapeHtml(t('common.date'))}</th>
        <th>${escapeHtml(t('cariHesaplar.tur'))}</th>
        <th class="sag">${escapeHtml(t('cariHesaplar.borc'))}</th>
        <th class="sag">${escapeHtml(t('common.amount'))}</th>
        <th class="sag">${escapeHtml(t('cariHesaplar.bakiye'))}</th>
        <th>${escapeHtml(t('common.description'))}</th>
      </tr></thead><tbody>${satirlar}</tbody></table>
      ${basSatiri}
      <div class="ozet"><strong>${escapeHtml(t('cariHesaplar.guncelBakiye'))}: ${formatCurrency(sonBakiye)}</strong></div>
    </body></html>`
  const pencere = fisPenceresiAcVeYazdir(html)
  if (!pencere) toastBildirim.hata(t('common.popupEngellendi'))
}

const hareketTuruEtiketi = (tur) =>
  ({ TAHSILAT: t('cariHesaplar.tahsilat'), ODEME: t('cariHesaplar.odeme'), BORC: t('cariHesaplar.borclandirma') })[tur] || tur
const hareketBadgeSinifi = (tur) =>
  ({ TAHSILAT: 'tahsilat', ODEME: 'odeme', BORC: 'borclandirma' })[tur] || 'odeme'

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
  temsilciId: null,
  notlar: '',
  etiketler: '',
  fotoUrl: '',
  fotoThumbUrl: '',
  aktif: true,
  paraBirimi: 'TRY'
})

const fotoInput = ref(null)

const fotoSec = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  const hata = resimDogrula(file)
  if (hata) { toastBildirim.hata(t(hata.key, hata.params)); e.target.value = ''; return }
  try {
    const kucuk = await resimSikistir(file)
    const r = await uploadAPI.foto(kucuk)
    form.value.fotoUrl = r.data?.url || ''
    form.value.fotoThumbUrl = r.data?.thumbUrl || ''
    toastBildirim.basarili(t('cariHesaplar.fotografYuklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.error || err?.response?.data?.message || t('cariHesaplar.fotografYuklenemedi'))
  } finally {
    e.target.value = ''
  }
}

const { temizle: formTemizle } = useFormKorumasi(form)

onMounted(async () => {
  await loadCariHesaplar()
  cariOzetYukle()
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
    if (filtreEtiket.value && filtreEtiket.value.trim().length >= 2) params.etiket = filtreEtiket.value.trim()
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
  temsilcileriYukle()
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
    temsilciId: null,
    notlar: '',
    etiketler: '',
    aktif: true,
    paraBirimi: 'TRY'
  }
  submitted.value = false
  formTemizle()
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
  submitted.value = false
}

const cariEylemleri = (c) => {
  const eylemler = [
    { etiket: t('cariHesaplar.tahsilat'), ikon: 'pi pi-money-bill', islem: () => tahsilatAc(c) },
    { etiket: t('cariHesaplar.borclandirma'), ikon: 'pi pi-plus-circle', islem: () => borclandirmaAc(c) },
    { etiket: t('cariHesaplar.acilisFisi'), ikon: 'pi pi-flag', islem: () => acilisAc(c) },
    { etiket: t('cariHesaplar.hareketlerDetay'), ikon: 'pi pi-list', islem: () => viewHareketler(c) },
    { etiket: t('cariHesaplar.adresler'), ikon: 'pi pi-map-marker', islem: () => adreslerAc(c) },
    { etiket: t('cariKart.baslik'), ikon: 'pi pi-id-card', islem: () => kartAc(c) }
  ]
  // Cari silme yalnizca ADMIN; islem kaydi olan cari backend'de engellenir.
  if (isAdmin.value) {
    eylemler.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => confirmDelete(c.id) })
  }
  return eylemler
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
    // temsilciAd denormalize alan; raporlar bu alana göre gruplar, bu yüzden
    // seçim değiştiğinde isimle birlikte gönderilir.
    temsilciId: cariHesap.temsilciId ?? null,
    temsilciAd: cariHesap.temsilciAd || null,
    notlar: cariHesap.notlar || '',
    etiketler: cariHesap.etiketler || '',
    paraBirimi: cariHesap.paraBirimi || 'TRY',
    fotoUrl: cariHesap.fotoUrl || '',
    fotoThumbUrl: cariHesap.fotoThumbUrl || '',
    aktif: cariHesap.aktif !== false
  }
  submitted.value = false
  formTemizle()
  showDialog.value = true
}

// Satis temsilcisi secenekleri. Kullanici listesi cari ekraninda zaten
// yuklenmiyordu; dropdown ilk acilista bos kalsin diye forma girildiginde bir
// kez cekilir. Raporlar bu alana gore grupladigi icin secim zorunlu degil
// ama atanmayan cariler raporda ayri satirda gosterilir.
const temsilciSecenekleri = ref([])
const temsilcilerYukleniyor = ref(false)
let temsilcilerYuklendi = false

const temsilcileriYukle = async () => {
  if (temsilcilerYuklendi || temsilcilerYukleniyor.value) return
  temsilcilerYukleniyor.value = true
  try {
    const r = await kullaniciAPI.getAll({ size: 500 })
    const liste = unwrapList(r)
    // API sirket filtresini uyguluyor olsa da liste kendi sirketine gore
    // daraltilir: yanlis sirketten temsilci secilmemeli.
    const sirketId = authStore?.sirketId
    temsilciSecenekleri.value = sirketId ? liste.filter((k) => k.sirketId === sirketId) : liste
    temsilcilerYuklendi = true
  } catch {
    temsilciSecenekleri.value = []
  } finally {
    temsilcilerYukleniyor.value = false
  }
}

const saveCariHesap = async () => {
  submitted.value = true
  if (!form.value.ad.trim()) {
    toastBildirim.uyari(t('cariHesaplar.adBosOlamaz'))
    return
  }

  // Temsilcinin adi denormalize olarak saklanir (temsilci performans raporu
  // JOIN yapmadan bu alana gore gruplar). Dropdown yalnizca id dondurdugu icin
  // ad burada cozulur; temsilci temizlendiyse alanlar da temizlenir.
  const seciliTemsilci = temsilciSecenekleri.value.find((k) => k.id === form.value.temsilciId)
  const temsilciAd = form.value.temsilciId ? seciliTemsilci?.ad || null : null
  const payload = { ...form.value, temsilciAd }

  saving.value = true
  try {
    if (editingId.value) {
      await cariHesapStore.updateCariHesap(editingId.value, payload)
      toastBildirim.basarili(t('cariHesaplar.guncellendi'))
    } else {
      await cariHesapStore.addCariHesap(payload)
      toastBildirim.basarili(t('cariHesaplar.olusturuldu'))
    }
    formTemizle()
    closeDialog()
    // Liste ve KPI yeniden sorgulanır. `createCrudStore.add/update` yeni kaydı
    // listenin SONUNA ekliyor ve toplam kaydı güncellemiyordu; özellikle
    // sayfalı/aramalı listede kayıt hiç görünmüyor ya da yanlış sayfada kalıyordu.
    await Promise.allSettled([loadCariHesaplar(), cariOzetYukle()])
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: t('cariHesaplar.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: () => deleteCariHesap(id),
    reject: () => {}
  })
}

const deleteCariHesap = async (id) => {
  try {
    await cariHesapStore.deleteCariHesap(id)
    toastBildirim.basarili(t('cariHesaplar.silindi'))
    // Silinen kayıt liste ve KPI'dan da düşmeli.
    await Promise.allSettled([loadCariHesaplar(), cariOzetYukle()])
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.silmeHata'))
  }
}

const batchSil = () => {
  if (selectedCariHesaplar.value.length === 0) return
  confirm.require({
    message: t('cariHesaplar.topluSilOnayMesaj', { n: selectedCariHesaplar.value.length }),
    header: t('common.topluSilmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        const ids = selectedCariHesaplar.value.map((c) => c.id).filter((v) => v != null)
        const r = await cariHesapAPI.topluSil(ids)
        const silinen = r.data?.silinen?.length || 0
        const atlanan = r.data?.atlanan || []
        if (silinen > 0) toastBildirim.basarili(t('cariHesaplar.topluSilindiSayi', { n: silinen }))
        atlanan.forEach((a) => toastBildirim.hata(a.neden))
      } catch (error) {
        toastBildirim.hata(error?.response?.data?.message || t('cariHesaplar.silmeHata'))
      } finally {
        selectedCariHesaplar.value = []
        await loadCariHesaplar()
      }
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
    const faturalar = unwrapList(r)
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

// Tahsilat/borçlandırma sonrası yenileme.
// ÖNCE getAllCariHesaplar() çağrılıyordu; bu parametresiz (size=50) tüm şirket
// sayfasını getirip listeyi eziyor, dolayısıyla kullanıcının arama/tür/bakiye
// filtresi ve bulunduğu sayfa sessizce kayboluyordu. KPI kartları (ozet) de
// filtreyi yok sayar; o da yenilenmeli.
const islemSonrasiYenile = async () => {
  try {
    await loadCariHesaplar()
    await cariOzetYukle()
  } catch {
    /* yenileme hatasi global olarak bildirilir */
  }
}

const tahsilatSonrasiYenile = async () => {
  tahsilatHedefCari.value = null
  await islemSonrasiYenile()
}

const borclandirmaDialog = ref(false)
const borclandirmaHedefCari = ref(null)

const borclandirmaAc = (cariHesap) => {
  borclandirmaHedefCari.value = cariHesap
  borclandirmaDialog.value = true
}

const borclandirmaSonrasiYenile = async () => {
  borclandirmaHedefCari.value = null
  await islemSonrasiYenile()
  if (showHareketlerDialog.value && selectedCariHesap.value) {
    await viewHareketler(selectedCariHesap.value)
  }
}

const kartAc = (cariHesap) => {
  kartCariId.value = cariHesap.id
  kartDialogAcik.value = true
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
// Özel fiyat ekleme sekmesindeki stok seçici sunucu aramalı. Sayfa açılışında
// 1000 kayıt çekiliyordu ve dropdown yalnızca bu sekme açıldığında göründüğü
// için hem gereksiz ağ trafiği hem de 1000. stoktan sonrasının seçilememesi
// anlamına geliyordu.
const stokOnerileri = ref([])
let stokAramaZamanlayici = null
let stokAramaSeq = 0

const stokAra = (event) => {
  const q = (event?.filter ?? event?.query ?? '').toString().trim()
  const benimSeq = ++stokAramaSeq
  if (stokAramaZamanlayici) clearTimeout(stokAramaZamanlayici)
  stokAramaZamanlayici = setTimeout(async () => {
    try {
      const r = await stokAPI.ara(q)
      if (benimSeq !== stokAramaSeq) return
      stokOnerileri.value = unwrapList(r)
    } catch {
      if (benimSeq === stokAramaSeq) stokOnerileri.value = []
    }
  }, 250)
}

// Sekme ilk açıldığında boş öneri listesiyle karşılaşılmasın.
const ozelFiyatSekmesiAcildi = async () => {
  if (stokOnerileri.value.length) return
  try {
    const r = await stokAPI.ara('')
    stokOnerileri.value = unwrapList(r)
  } catch {
    stokOnerileri.value = []
  }
}

// Hareket dialog'undaki sekmeler: 0 = Geçmiş Faturalar, 1 = Özel Fiyatlar,
// 2 = Geçmişte Aldığı Ürünler, 3 = Görüşme Notları.
const cariSekmeDegisti = (idx) => {
  if (idx === 1) ozelFiyatSekmesiAcildi()
}

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

const cariOzelFiyatSil = (f) => {
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await cariHesapAPI.fiyatSil(f.id)
        cariOzelFiyatlar.value = cariOzelFiyatlar.value.filter((x) => x.id !== f.id)
        toastBildirim.basarili(t('cariHesaplar.ozelFiyatSilindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('cariHesaplar.silinemedi'))
      }
    }
  })
}

const cariFaturalar = ref([])

const cariFaturalariYukle = async (cariId) => {
  try {
    const r = await faturaAPI.cariFaturalari(cariId, { size: 100 })
    cariFaturalar.value = unwrapList(r)
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

const cariNotSil = (n) => {
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await notAPI.delete(n.id)
        await cariNotlariYukle(selectedCariHesap.value.id)
      } catch (err) {
        toastBildirim.hata(t('cariHesaplar.notSilinemedi'))
      }
    }
  })
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


import { formatTarih as formatDate, formatTarihSaat } from '../utils/format.js'
</script>

<style scoped>
.cari-ad-hucre {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cari-thumb {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
  flex: 0 0 auto;
}
.cari-thumb-yok {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px dashed var(--border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
  flex: 0 0 auto;
}
.foto-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.foto-onizle {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--border);
}
.fatura-link {
  color: var(--accent);
  cursor: pointer;
  font-weight: 600;
  text-decoration: none;
}
.fatura-link:hover {
  text-decoration: underline;
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
  background: var(--success-soft);
  color: var(--success);
}
.bakiye-rozet.borc {
  background: var(--danger-soft);
  color: var(--danger);
}
.cari-filtreler {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.cari-istatistik {
  display: flex;
  flex-wrap: wrap;
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
  color: var(--text-muted);
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
  background: var(--warning-soft);
  color: var(--warning);
}
.not-onem-rozet.kritik {
  background: var(--danger-soft);
  color: var(--danger);
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
}
.satir-islemler {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
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
  color: var(--accent);
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
  color: var(--danger);
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
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 3px var(--accent-soft-strong) !important;
}
.form-group :deep(.p-inputtext.p-invalid),
.form-group :deep(.p-textarea.p-invalid) {
  border-color: var(--danger) !important;
}

.error {
  display: block;
  color: var(--danger);
  font-size: 11px;
  margin-top: 4px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.positive {
  color: var(--success);
  font-weight: bold;
}

.negative {
  color: var(--danger);
  font-weight: bold;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.badge.tahsilat {
  background-color: var(--success-soft);
  color: var(--success);
}

.badge.odeme {
  background-color: var(--danger-soft);
  color: var(--danger);
}

.badge.borclandirma {
  background-color: var(--warning-soft, rgba(245, 158, 11, 0.15));
  color: var(--warning, #f59e0b);
}

.hareket-info {
  background: var(--bg-muted);
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
  color: var(--success);
  font-size: 11px;
  margin-top: 2px;
}
.iban-gecersiz {
  display: block;
  color: var(--danger);
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
