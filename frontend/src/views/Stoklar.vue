<template>
  <div class="stoklar-container">
    <PageHeader :title="t('stoklar.title')" />
    <IlkZiyaretIpuclari
      anahtar="stoklar"
      :baslik="t('stoklar.ipucuBaslik')"
      :metin="t('stoklar.ipucuMetin')"
    />
    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('stoklar.yeniUrun')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
        <Button
          :label="t('stoklar.topluFiyatGuncelle')"
          icon="pi pi-dollar"
          class="p-button-help"
          style="margin-left: 8px"
          @click="batchFiyatDialog = true"
        />
        <Button
          :label="t('stoklar.degerleme')"
          icon="pi pi-chart-line"
          class="p-button-outlined"
          style="margin-left: 8px"
          @click="degerlemeAc"
        />
        <Button
          :label="t('stoklar.siparisOnerisi')"
          icon="pi pi-shopping-cart"
          class="p-button-outlined"
          style="margin-left: 8px"
          @click="oneriAc"
        />
        <div
          v-if="seciliStoklar && seciliStoklar.length > 0"
          class="batch-actions"
        >
          <span class="batch-count">{{ seciliStoklar ? seciliStoklar.length : 0 }} {{ t('stoklar.secili') }}</span>
          <Button
            v-permission="'STOK_DELETE'"
            :label="t('stoklar.topluSil')"
            icon="pi pi-trash"
            class="p-button-sm p-button-danger"
            @click="batchSil"
          />
          <Button
            :label="t('stoklar.csvAktar')"
            icon="pi pi-download"
            class="p-button-sm p-button-outlined"
            @click="batchCsvExport"
          />
          <Button
            :label="t('stoklar.topluEtiket')"
            icon="pi pi-tags"
            class="p-button-sm p-button-outlined"
            @click="topluEtiketDialog = true"
          />
          <Button
            :label="t('stoklar.barkodUretToplu')"
            icon="pi pi-sparkles"
            class="p-button-sm p-button-outlined"
            @click="barkodUretToplu"
          />
        </div>
      </template>
      <template #end>
        <Button
          icon="pi pi-filter"
          class="p-button-text p-button-sm filtre-toggle"
          :class="{ 'filtre-aktif': aktifFiltreSayisi > 0 }"
          :title="t('stoklar.filtreler')"
          :aria-label="t('stoklar.filtreler')"
          @click="filtreAcik = !filtreAcik"
        >
          <span
            v-if="aktifFiltreSayisi > 0"
            class="filtre-rozet"
          >{{ aktifFiltreSayisi }}</span>
        </Button>
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
          @click="excelIndir"
        />
        <div class="toolbar-end">
          <Button
            v-if="gosterim === 'tablo'"
            :icon="'pi pi-sitemap'"
            class="p-button-text p-button-sm"
            :class="{ 'grupla-aktif': grupla }"
            :title="t('stoklar.grupla')"
            :aria-label="t('stoklar.grupla')"
            :severity="grupla ? 'primary' : undefined"
            @click="grupla = !grupla"
          />
          <Button
            :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
            class="p-button-text p-button-sm"
            :title="gosterim === 'tablo' ? t('stoklar.kartGorunumu') : t('stoklar.tabloGorunumu')"
            :aria-label="gosterim === 'tablo' ? t('stoklar.kartGorunumu') : t('stoklar.tabloGorunumu')"
            @click="gosterim = gosterim === 'tablo' ? 'kart' : 'tablo'"
          />
        </div>
      </template>
    </Toolbar>

    <!-- Stok grubu hızlı filtre çipleri: grubun ne işe yaradığını görünür kılar -->
    <div
      v-if="gosterim === 'tablo' && stokGruplari.length > 1"
      class="grup-cipler"
    >
      <span class="grup-cip-etiket">{{ t('stoklar.grup') }}:</span>
      <button
        type="button"
        class="grup-cip"
        :class="{ aktif: !filtreStokGrubu }"
        @click="grubaFiltrele('')"
      >
        {{ t('stoklar.tumGruplar') }}
      </button>
      <button
        v-for="g in stokGruplari"
        :key="g"
        type="button"
        class="grup-cip"
        :class="{ aktif: filtreStokGrubu === g }"
        @click="grubaFiltrele(g)"
      >
        {{ g }}
      </button>
    </div>

    <div
      class="filter-bar"
      :class="{ 'filtre-gizli': !filtreAcik }"
    >
      <!-- REDTEAM/Faz3.3: `p-input-icon-left` PrimeVue 4'te kaldirildi; asagidaki
           `.filter-bar > .p-input-icon-left` CSS kurali da bu yuzden oluydu. -->
      <IconField>
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="filtreArama"
          :placeholder="t('stoklar.filtreArama')"
          @input="filtreDegisti"
        />
      </IconField>
      <InputText
        v-model="filtreKategori"
        :placeholder="t('stoklar.filtreKategori')"
        class="filter-input"
        @input="filtreDegisti"
      />
      <InputText
        v-model="filtreMarka"
        :placeholder="t('stoklar.filtreMarka')"
        class="filter-input"
        @input="filtreDegisti"
      />
      <Dropdown
        v-model="filtreStokGrubu"
        :options="stokGruplari"
        :placeholder="t('stoklar.filtreStokGrubu')"
        class="filter-dropdown"
        filter
        show-clear
        @change="filtreDegisti"
      />
      <Dropdown
        v-model="filtreDepo"
        :options="depolar"
        option-label="ad"
        option-value="id"
        :placeholder="t('stoklar.filtreDepo')"
        :show-clear="true"
        class="filter-dropdown"
        @change="filtreDegisti"
      />
      <InputNumber
        v-model="filtreMinFiyat"
        :placeholder="t('stoklar.minFiyat')"
        class="filter-input-sm"
        @input="filtreDegisti"
      />
      <InputNumber
        v-model="filtreMaxFiyat"
        :placeholder="t('stoklar.maxFiyat')"
        class="filter-input-sm"
        @input="filtreDegisti"
      />
      <Button
        icon="pi pi-times"
        class="p-button-text p-button-sm"
        :title="t('stoklar.temizle')"
        @click="filtreTemizle"
      />
    </div>

    <div
      v-if="stokStore.loading"
      class="loading-iskelet"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="6"
        height="44px"
      />
    </div>

    <!-- Yukleme hatasi: "veri yok" ile karismamasi icin ayri hata durumu + tekrar dene. -->
    <div
      v-if="!stokStore.loading && stokStore.error"
      class="yukleme-hatasi"
      role="alert"
    >
      <i class="pi pi-exclamation-triangle" />
      <div class="yukleme-hatasi-metin">
        <strong>{{ t('common.yuklenemedi') }}</strong>
        <span>{{ stokStore.error }}</span>
      </div>
      <Button
        :label="t('common.tekrarDene')"
        icon="pi pi-refresh"
        class="p-button-sm p-button-outlined"
        @click="stoklariYukle"
      />
    </div>

    <template v-if="!stokStore.loading && !stokStore.error && gosterim === 'tablo'">
      <AppDataTable
        v-model:selection="seciliStoklar"
        v-model:expanded-rows="expandedRows"
        :value="stokStore.stoklar"
        :paginator="true"
        :rows="25"
        :rows-per-page-options="[15, 25, 50, 100]"
        :lazy="true"
        :total-records="stokStore.toplamKayit"
        v-bind="grupla ? { rowGroupMode: 'subheader', groupRowsBy: 'stokGrubu' } : {}"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        :current-page-report-template="'{totalRecords} ' + $t('common.recordsWord') + ' · {first}-{last}'"
        data-key="id"
        striped-rows
        sort-field="stokGrubu"
        :sort-order="1"
        class="p-datatable-sm"
        :global-filter-fields="['ad', 'stokKodu', 'birim']"
        gorunum-anahtari="stoklar"
        @page="stokSayfaDegisti"
        @row-toggle="fiyatlariYukle($event.data)"
        @row-click="stokSec($event.data)"
      >
        <template #groupheader="slotProps">
          <span class="grup-baslik">
            <i class="pi pi-sitemap" />
            {{ slotProps.data.stokGrubu || t('stoklar.grupsuz') }}
            <span class="grup-adet">{{ grupAdetleri[slotProps.data.stokGrubu || ''] ?? '' }}</span>
          </span>
        </template>
        <template #header>
          <div class="table-header">
            <span class="toplam-bilgi">{{ stokStore.toplamKayit }} {{ t('stoklar.urun') }}</span>
            <span
              v-if="kritikAdet > 0"
              class="kritik-bilgi"
            ><i class="pi pi-exclamation-triangle" /> {{ kritikAdet }} {{ t('stoklar.kritik') }}</span>
          </div>
        </template>
        <template #empty>
          <EmptyState
            :message="t('stoklar.empty')"
            :sub-message="t('stoklar.emptyHint')"
            icon="pi pi-box"
            :action-label="t('stoklar.emptyAction')"
            action-icon="pi pi-plus"
            @action="openDialog"
          />
        </template>
        <template #expansion="slotProps">
          <div class="sira-detay">
            <div class="sira-detay-bolum">
              <strong>{{ t('stoklar.fiyatOzeti') }}</strong>
              <div class="fiyat-ozet-grid">
                <span>{{ t('stoklar.alisFiyati') }}: <b>{{ formatCurrency(slotProps.data.fiyat) }}</b></span>
                <span>{{ t('stoklar.satisFiyati') }}: <b>{{ formatCurrency(slotProps.data.satisFiyati) }}</b></span>
                <span>
                  {{ t('stoklar.karMarji') }}:
                  <b :class="marjHesapla(slotProps.data) < 0 ? 'negatif' : 'pozitif'">
                    {{ marjHesapla(slotProps.data) == null ? '-' : '%' + marjHesapla(slotProps.data) }}
                  </b>
                </span>
                <span v-if="slotProps.data.tedarikciFiyat">
                  {{ t('stoklar.tedarikciFiyati') }}: <b>{{ formatCurrency(slotProps.data.tedarikciFiyat) }}</b>
                </span>
              </div>
            </div>
            <div class="sira-detay-bolum">
              <strong>{{ t('stoklar.depoDagilimi') }}</strong>
              <div
                v-if="depoDagilim[slotProps.data.id] && depoDagilim[slotProps.data.id].length"
                class="depo-dagilim-liste"
              >
                <span
                  v-for="d in depoDagilim[slotProps.data.id]"
                  :key="d.depoId"
                  class="depo-chip"
                >
                  {{ d.depoAdi || '-' }}: <b>{{ d.miktar }} {{ slotProps.data.birim || '' }}</b>
                </span>
              </div>
              <span
                v-else
                class="text-muted"
              >{{ t('stoklar.depoYok') }}</span>
            </div>
            <div class="sira-detay-bolum">
              <strong>{{ t('stoklar.fiyatlar') }}</strong>
              <div
                v-if="fiyatListeleri[slotProps.data.id] && fiyatListeleri[slotProps.data.id].length"
                class="depo-dagilim-liste"
              >
                <span
                  v-for="f in fiyatListeleri[slotProps.data.id]"
                  :key="f.id || f.ad"
                  class="depo-chip"
                >
                  {{ f.ad }}: <b>{{ formatCurrency(f.fiyat) }}</b>
                </span>
              </div>
              <span
                v-else
                class="text-muted"
              >{{ t('stoklar.ozelFiyatYok') }}</span>
            </div>
          </div>
        </template>
        <!-- Satira tiklamak detay dialogunu acar. Bileşenlerin (genişletme oku,
             secim kutusu) kendi tiklamaları bu akışı tetiklememeli; aksi
             halde "⋮" menusunu acmak istemek detayi da acardi. -->
        <Column
          expander
          style="width: 3rem"
          @click.stop
        />
        <Column
          selection-mode="multiple"
          header-style="width: 2.5rem"
          @click.stop
        />
        <Column
          :header="t('stoklar.colGorsel')"
          style="width: 64px"
        >
          <template #body="s">
            <img
              v-if="s.data.fotoThumbUrl || s.data.fotoUrl"
              :src="s.data.fotoThumbUrl || s.data.fotoUrl"
              class="satir-thumb"
              alt=""
              loading="lazy"
              decoding="async"
            >
            <span
              v-else
              class="satir-thumb-yok"
            ><i class="pi pi-image" /></span>
          </template>
        </Column>
        <Column
          field="stokKodu"
          :header="t('stoklar.colStokKodu')"
          sortable
          style="width: 120px"
        />
        <Column
          field="ad"
          :header="t('stoklar.colUrunAdi')"
          sortable
          style="min-width: 180px"
        />
        <Column
          v-if="!grupla"
          field="stokGrubu"
          :header="t('stoklar.stokGrubu')"
          sortable
          style="width: 130px"
        >
          <template #body="s">
            <span
              v-if="s.data.stokGrubu"
              class="grup-chip"
            >{{ s.data.stokGrubu }}</span>
            <span
              v-else
              class="text-muted"
            >-</span>
          </template>
        </Column>
        <Column
          field="birim"
          :header="t('stoklar.colBirim')"
          sortable
          style="width: 90px"
        />
        <Column
          field="miktar"
          :header="t('stoklar.colMiktar')"
          sortable
          class="sayisal"
          style="width: 110px"
        >
          <template #body="s">
            <span :class="s.data.minMiktar && s.data.miktar <= s.data.minMiktar ? 'kritik' : 'normal'">
              {{ s.data.miktar }} {{ s.data.birim || '' }}
            </span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.colDepo')"
          style="min-width: 150px"
        >
          <template #body="s">
            <div
              v-if="depoDagilim[s.data.id] && depoDagilim[s.data.id].length"
              class="depo-dagilim-mini"
            >
              <span
                v-for="d in depoDagilim[s.data.id]"
                :key="d.depoId"
                class="depo-chip-mini"
              >{{ d.depoAdi || '-' }}: {{ d.miktar }}</span>
            </div>
            <span
              v-else
              class="text-muted"
            >-</span>
          </template>
        </Column>
        <Column
          field="fiyat"
          :header="t('stoklar.alisFiyati')"
          sortable
          class="sayisal"
          style="width: 120px"
        >
          <template #body="s">
            <span class="gizli-veri">{{ formatCurrency(s.data.fiyat) }}</span>
          </template>
        </Column>
        <Column
          field="satisFiyati"
          :header="t('stoklar.satisFiyati')"
          sortable
          class="sayisal"
          style="width: 120px"
        >
          <template #body="s">
            <span class="gizli-veri">{{ s.data.satisFiyati ? formatCurrency(s.data.satisFiyati) : '-' }}</span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.karMarji')"
          class="sayisal"
          style="width: 90px"
        >
          <template #body="s">
            <span
              v-if="marjHesapla(s.data) != null"
              :class="marjHesapla(s.data) < 0 ? 'negatif' : 'pozitif'"
            >%{{ marjHesapla(s.data) }}</span>
            <span
              v-else
              class="text-muted"
            >-</span>
          </template>
        </Column>
        <Column
          field="tedarikciAd"
          :header="t('stoklar.colTedarikci')"
          sortable
          style="width: 150px"
        >
          <template #body="s">
            <span class="gizli-veri">
              <span v-if="s.data.tedarikciAd"><i
                class="pi pi-building"
                style="margin-right: 6px; color: #3b82f6"
              />{{ s.data.tedarikciAd }}</span>
              <span
                v-else
                class="text-muted"
              >-</span>
            </span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.colStokDegeri')"
          sortable
          style="width: 130px"
        >
          <template #body="s">
            <span class="gizli-veri">{{ formatCurrency((s.data.miktar || 0) * (s.data.fiyat || 0)) }}</span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.colKritik')"
          style="width: 80px"
        >
          <template #body="s">
            <i
              v-if="s.data.minMiktar && s.data.miktar <= s.data.minMiktar"
              class="pi pi-exclamation-triangle"
              style="color: #f87171; font-size: 16px"
            />
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 90px"
        >
          <template #body="s">
            <div class="satir-islemler">
              <Button
                icon="pi pi-pencil"
                class="p-button-rounded p-button-info p-button-sm"
                :title="t('common.edit')"
                @click.stop="editStok(s.data)"
              />
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="stokEylemleri(s.data)"
              />
            </div>
          </template>
        </Column>
      </AppDataTable>
    </template>

    <div
      v-if="!stokStore.loading && !stokStore.error && gosterim === 'kart'"
      class="stok-kartlar"
    >
      <StokKart
        v-for="s in stokStore.stoklar"
        :key="s.id"
        :stok="s"
        @sec="stokSec"
        @duzenle="editStok"
        @sil="confirmDel"
      />
      <Message
        v-if="!loading && stokStore.stoklar.length === 0"
        severity="info"
        :text="t('stoklar.eslesenYok')"
        class="full-width"
      />
    </div>

    <StokHareketBolum
      v-if="seciliStok"
      :stok="seciliStok"
      :hareketler="stokHareketler"
      @giris="openHareketDialog('GIRIS')"
      @cikis="openHareketDialog('CIKIS')"
      @kapat="seciliStok = null"
      @sil="delHareket"
    />

    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('stoklar.urunDuzenle') : t('stoklar.yeniUrunDialog')"
      :modal="true"
      style="width: 650px"
    >
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.temelBilgiler') }}
        </div>
        <div class="form-row">
          <div class="form-grup flex-2">
            <label>{{ t('stoklar.urunAdi') }}</label>
            <InputText
              v-model="form.ad"
              :placeholder="t('stoklar.urunAdiPlaceholder')"
              :class="{ 'p-invalid': formHatalar.ad }"
              class="w-full"
              @input="formHatalar.ad = ''"
            />
            <small
              v-if="formHatalar.ad"
              class="p-error"
            >{{ formHatalar.ad }}</small>
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.birim') }}</label>
            <Dropdown
              v-model="form.birim"
              :options="['Adet', 'Koli', 'Kg', 'Metre', 'Litre', 'Paket']"
              :placeholder="t('stoklar.seciniz')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.stokKodu') }}</label>
            <InputText
              v-model="form.stokKodu"
              :placeholder="t('stoklar.stokKoduPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.barkod') }}</label>
            <div class="barkod-alan">
              <InputText
                v-model="form.barkod"
                :placeholder="t('stoklar.barkodPlaceholder')"
                class="w-full"
              />
              <Button
                :label="t('stoklar.uret')"
                icon="pi pi-sparkles"
                class="p-button-sm p-button-outlined"
                :loading="barkodOneriYukleniyor"
                :title="t('stoklar.barkodUret')"
                @click="barkodOnerForma"
              />
            </div>
            <small class="alan-ipucu">{{ t('stoklar.barkodIpucu') }}</small>
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.marka') }}</label>
            <InputText
              v-model="form.marka"
              :placeholder="t('stoklar.markaPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.kategori') }}</label>
            <AutoComplete
              v-model="form.kategori"
              :suggestions="kategoriOnerileri"
              :placeholder="t('stoklar.kategoriPlaceholder')"
              class="w-full"
              :force-selection="false"
              dropdown
            />
            <small class="alan-ipucu">{{ t('stoklar.kategoriIpucu') }}</small>
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.stokGrubu') }}</label>
            <AutoComplete
              v-model="form.stokGrubu"
              :suggestions="stokGruplari"
              :placeholder="t('stoklar.stokGrubuPlaceholder')"
              class="w-full"
              :force-selection="false"
              dropdown
            />
            <small class="alan-ipucu">{{ t('stoklar.stokGrubuIpucu') }}</small>
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.rafNo') }}</label>
            <InputText
              v-model="form.rafNo"
              :placeholder="t('stoklar.rafNoPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.birim2') }}</label>
            <InputText
              v-model="form.birim2"
              :placeholder="t('stoklar.birim2Placeholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.cevrimKatsayisi') }}</label>
            <InputNumber
              v-model="form.cevrimKatsayisi"
              :min="0"
              :min-fraction-digits="4"
              class="w-full"
              placeholder="1.0000"
            />
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.fiyatStok') }}
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.alisFiyati') }}</label>
            <InputNumber
              v-model="form.fiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.satisFiyati') }}</label>
            <InputNumber
              v-model="form.satisFiyati"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.kdvOrani') }}</label>
            <InputNumber
              v-model="form.kdvOrani"
              :min="0"
              :max="100"
              class="w-full"
              placeholder="%"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.agirlik') }}</label>
            <InputNumber
              v-model="form.agirlik"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.mevcutMiktar') }}</label>
            <InputNumber
              v-model="form.miktar"
              :min="0"
              :min-fraction-digits="0"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.minStok') }}</label>
            <InputNumber
              v-model="form.minMiktar"
              :min="0"
              :min-fraction-digits="0"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.maliyetYontemi') }}</label>
            <Dropdown
              v-model="form.maliyetYontemi"
              :options="maliyetYontemiSecenekleri"
              option-label="label"
              option-value="value"
              :placeholder="t('stoklar.seciniz')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.varsayilanDepo') }}</label>
            <Dropdown
              v-model="form.varsayilanDepoId"
              :options="depolar"
              option-label="ad"
              option-value="id"
              show-clear
              :placeholder="t('stoklar.varsayilanDepoSecin')"
              class="w-full"
            />
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.tedarikciBilgileri') }}
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.tedarikci') }}</label>
            <Dropdown
              v-model="form.tedarikciId"
              :options="tedarikciOnerileri"
              option-label="ad"
              option-value="id"
              :placeholder="t('stoklar.tedarikciSecin')"
              class="w-full"
              show-clear
              filter
              filter-by="ad,vergiNumarasi,telefon"
              :loading="tedarikciOnerileriYukleniyor"
              @filter="tedarikciAra"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.tedarikciStokKodu') }}</label>
            <InputText
              v-model="form.tedarikciStokKodu"
              :placeholder="t('stoklar.tedarikciStokKoduPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.tedarikciFiyati') }}</label>
            <InputNumber
              v-model="form.tedarikciFiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label />
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.ekBilgiler') }}
        </div>
        <div class="form-grup">
          <label>{{ t('stoklar.urunFotografi') }}</label>
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
              :label="t('stoklar.fotografYukle')"
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
        <div class="form-grup">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>

        <div class="form-grup coklu-fiyat-bolumu">
          <div class="coklu-fiyat-baslik">
            <label>{{ t('stoklar.fiyatlar') }}</label>
            <span class="coklu-fiyat-ipucu">{{ t('stoklar.fiyatIpuclari') }}</span>
          </div>
          <div
            v-for="f in form.fiyatlar"
            :key="f.id || f.ad"
            class="coklu-fiyat-satir"
          >
            <InputText
              v-model="f.ad"
              :placeholder="t('stoklar.fiyatAdiPlaceholder')"
              class="fiyat-ad-input"
            />
            <InputNumber
              v-model="f.fiyat"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              :min-fraction-digits="2"
              class="fiyat-tutar-input"
            />
            <Button
              icon="pi pi-trash"
              :aria-label="$t('common.delete')"
              class="p-button-rounded p-button-text p-button-danger"
              @click="fiyatSil(f)"
            />
          </div>
          <Button
            :label="t('stoklar.fiyatEkle')"
            icon="pi pi-plus"
            size="small"
            class="p-button-outlined"
            @click="fiyatEkle"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showDialog = false"
        />
        <Button
          :label="editingId ? t('stoklar.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveStok"
        />
      </template>
    </Dialog>

    <StokHareketDialog
      v-model:visible="showHareketDialog"
      v-model:miktar="hareketForm.miktar"
      v-model:hareket-tarihi="hareketForm.hareketTarihi"
      v-model:cari-hesap-id="hareketForm.cariHesapId"
      v-model:depo-id="hareketForm.depoId"
      v-model:aciklama="hareketForm.aciklama"
      :baslik="hareketBaslik"
      :depolar="depolar"
      :loading="saving"
      @kaydet="saveHareket"
    />

    <StokTopluFiyatDialog
      v-model:visible="batchFiyatDialog"
      v-model:yon="batchFiyatForm.yon"
      v-model:oran="batchFiyatForm.oran"
      v-model:kategori="batchFiyatForm.kategori"
      v-model:stok-grubu="batchFiyatForm.stokGrubu"
      :gruplar="stokGruplari"
      :loading="batchLoading"
      @uygula="batchFiyatUygula"
    />

    <StokDetayDialog
      v-model:visible="showDetailDialog"
      :stok="detailStok"
      :hareketler="hareketler"
      :hareketler-yukleniyor="hareketlerYukleniyor"
    />

    <BarkodEtiketDialog
      v-model:visible="etiketDialog"
      :stok="etiketStok"
    />

    <Dialog
      v-model:visible="topluEtiketDialog"
      :header="t('stoklar.topluEtiketBaslik')"
      :modal="true"
      style="width: 420px"
    >
      <p style="font-size: 13px; color: var(--text-secondary); margin-top: 0">
        {{ t('stoklar.topluEtiketAciklama') }}
      </p>
      <div class="form-grup">
        <label>{{ t('stoklar.etiketTipi') }}</label>
        <SelectButton
          v-model="topluEtiketTip"
          :options="etiketTipSecenekleri"
          option-label="etiket"
          option-value="value"
          :allow-empty="false"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('stoklar.adetEtiket') }}</label>
        <InputNumber
          v-model="topluEtiketAdet"
          :min="1"
          :max="100"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="topluEtiketDialog = false"
        />
        <Button
          :label="t('stoklar.etiketIndir')"
          icon="pi pi-file-pdf"
          :loading="topluEtiketYukleniyor"
          @click="topluEtiketIndir"
        />
      </template>
    </Dialog>

    <StokRaporDialoglari
      v-model:degerleme-visible="degerlemeDialog"
      v-model:oneri-visible="oneriDialog"
      :degerleme-verisi="degerlemeVerisi"
      :oneri-verisi="oneriVerisi"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { useStokStore } from '../stores/stokStore.js'
import { useCariOnerileri } from '../composables/useCariOnerileri.js'
import { stokAPI, excelAPI, uploadAPI, depoAPI, raporAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import EmptyState from '../components/EmptyState.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import StokHareketDialog from '../components/StokHareketDialog.vue'
import StokTopluFiyatDialog from '../components/StokTopluFiyatDialog.vue'
import StokDetayDialog from '../components/StokDetayDialog.vue'
import StokKart from '../components/StokKart.vue'
import StokHareketBolum from '../components/StokHareketBolum.vue'
import BarkodEtiketDialog from '../components/BarkodEtiketDialog.vue'
import StokRaporDialoglari from '../components/StokRaporDialoglari.vue'
import { useKisayollar } from '../composables/useKisayollar.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import { useGeriAl } from '../composables/useGeriAl.js'
import { resimDogrula } from '../utils/dosyaDogrula.js'
import { resimSikistir } from '../utils/resimSikistir.js'
import { formatCurrency, getLocalDateString } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const stokStore = useStokStore()

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => {
    showDialog.value = false
  },
  kaydet: () => saveStok()
})

const filtreArama = ref('')
const filtreKategori = ref('')
const filtreMarka = ref('')
const filtreStokGrubu = ref('')
const filtreDepo = ref(null)
const filtreMinFiyat = ref(null)
const filtreMaxFiyat = ref(null)

const stokSayfa = ref(0)
const stokSayfaBoyutu = ref(25)
let stokAramaZaman = null

const seciliStok = ref(null)
const seciliStokId = ref(null)
const seciliStoklar = ref([])
const stokHareketler = ref([])
// Stok değerleme (ortalama + FIFO) ve sipariş önerisi raporları.
const degerlemeDialog = ref(false)
const degerlemeVerisi = ref(null)
const oneriDialog = ref(false)
const oneriVerisi = ref([])

const degerlemeAc = async () => {
  degerlemeDialog.value = true
  try {
    const r = await raporAPI.stokDegerleme()
    degerlemeVerisi.value = r.data || null
  } catch {
    degerlemeVerisi.value = null
  }
}

const oneriAc = async () => {
  oneriDialog.value = true
  try {
    const r = await raporAPI.siparisOnerisi()
    oneriVerisi.value = Array.isArray(r.data) ? r.data : []
  } catch {
    oneriVerisi.value = []
  }
}

const showDetailDialog = ref(false)
const detailStok = ref(null)
const hareketler = ref([])
const hareketlerYukleniyor = ref(false)
const saving = ref(false)
const gosterim = ref('tablo')

// Mobil/PWA: filtre paneli varsayilan kapali, masaustunde her zaman acik (CSS ile).
const filtreAcik = ref(true)
const aktifFiltreSayisi = computed(() => {
  let n = 0
  if (filtreArama.value) n++
  if (filtreKategori.value) n++
  if (filtreMarka.value) n++
  if (filtreStokGrubu.value) n++
  if (filtreDepo.value) n++
  if (filtreMinFiyat.value != null && filtreMinFiyat.value !== '') n++
  if (filtreMaxFiyat.value != null && filtreMaxFiyat.value !== '') n++
  return n
})

const etiketDialog = ref(false)
const etiketStok = ref(null)

const barkodEtiket = (stok) => {
  etiketStok.value = stok
  etiketDialog.value = true
}

// Otomatik barkod: barkodu bos urunlere sirket ici EAN-13 uretir/kaydeder.
const barkodUretTek = async (stok) => {
  try {
    const { data } = await stokAPI.barkodUret([stok.id])
    const adet = data?.uretildi || 0
    if (adet > 0) {
      toastBildirim.basarili(t('stoklar.barkodUretildi', { n: adet }))
      await stoklariYukle()
    } else {
      toastBildirim.uyari(t('stoklar.barkodZatenVar'))
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stoklar.islemBasarisiz'))
  }
}

const barkodUretToplu = async () => {
  const secili = seciliStoklar.value || []
  if (!secili.length) return
  try {
    const { data } = await stokAPI.barkodUret(secili.map((s) => s.id))
    const adet = data?.uretildi || 0
    if (adet > 0) {
      toastBildirim.basarili(t('stoklar.barkodUretildi', { n: adet }))
      await stoklariYukle()
      seciliStoklar.value = []
    } else {
      toastBildirim.uyari(t('stoklar.barkodZatenVar'))
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stoklar.islemBasarisiz'))
  }
}

// Faz 5: toplu etiket (seçili ürünler, adetli, tip seçimli)
const topluEtiketDialog = ref(false)
const topluEtiketTip = ref('BARKOD')
const topluEtiketAdet = ref(1)
const topluEtiketYukleniyor = ref(false)
const etiketTipSecenekleri = computed(() => [
  { etiket: t('stoklar.etiketBarkod'), value: 'BARKOD' },
  { etiket: t('stoklar.etiketQr'), value: 'QR' }
])

const topluEtiketIndir = async () => {
  const secili = seciliStoklar.value || []
  if (!secili.length) return
  topluEtiketYukleniyor.value = true
  try {
    const payload = {
      tip: topluEtiketTip.value,
      kalemler: secili.map((s) => ({ stokId: s.id, adet: topluEtiketAdet.value }))
    }
    const { data } = await stokAPI.topluEtiket(payload)
    const url = URL.createObjectURL(data)
    const a = document.createElement('a')
    a.href = url
    a.download = `etiketler-${Date.now()}.pdf`
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 60000)
    topluEtiketDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stoklar.islemBasarisiz'))
  } finally {
    topluEtiketYukleniyor.value = false
  }
}

const showDialog = ref(false)
const editingId = ref(null)
const maliyetYontemiSecenekleri = computed(() => [
  { label: t('stoklar.ortalamaMaliyet'), value: 'ORTALAMA' },
  { label: t('stoklar.fifo'), value: 'FIFO' },
  { label: t('stoklar.lifo'), value: 'LIFO' }
])
const form = ref({
  stokKodu: '',
  barkod: '',
  ad: '',
  birim: '',
  birim2: '',
  cevrimKatsayisi: null,
  marka: '',
  stokGrubu: '',
  kategori: '',
  rafNo: '',
  fiyat: 0,
  satisFiyati: null,
  kdvOrani: null,
  agirlik: null,
  miktar: 0,
  minMiktar: null,
  tedarikciId: null,
  tedarikciStokKodu: '',
  tedarikciFiyat: null,
  maliyetYontemi: 'ORTALAMA',
  varsayilanDepoId: null,
  aciklama: '',
  fotoUrl: '',
  fotoThumbUrl: '',
  fiyatlar: []
})

const formHatalar = ref({ ad: '' })

const { temizle: formTemizle } = useFormKorumasi(form)
const { silVeGeriAl } = useGeriAl()

const showHareketDialog = ref(false)
const hareketTur = ref('GIRIS')
const hareketForm = ref({ miktar: null, hareketTarihi: new Date(), cariHesapId: null, depoId: null, aciklama: '' })
const depolar = ref([])
const expandedRows = ref({})
const depoDagilim = ref({})
// Gruplama ve özel fiyat listesi görünümü
const grupla = ref(localStorage.getItem('raspel_stok_grupla') === 'true')
watch(grupla, (v) => {
  try { localStorage.setItem('raspel_stok_grupla', String(v)) } catch { /* yoksay */ }
})
const fiyatListeleri = ref({})

// Gruba göre gruplama / toplu fiyat hedefleri için kullanılan değerler.
// ÖNCE `stokStore.stoklar` üzerinden hesaplanıyordu; liste sunucu tarafında
// sayfalanıyor (25 satır), dolayısıyla çipler sayfa değişince kayboluyor,
// grup başlığındaki sayaç yanlış oluyor ve 7. sayfadaki gruba ulaşılamıyordu.
// Artık sunucudan TÜM katalog dağılımı gelir.
const gruplamaDagilimi = ref({ kategoriler: [], stokGruplari: [] })

const gruplamaDagilimiYukle = async () => {
  try {
    const r = await stokAPI.gruplamaDagilimi()
    gruplamaDagilimi.value = {
      kategoriler: r.data?.kategoriler || [],
      stokGruplari: r.data?.stokGruplari || []
    }
  } catch {
    /* dağılım alınamadı: çipler boş kalır, liste etkilenmez */
  }
}

const stokGruplari = computed(() =>
  (gruplamaDagilimi.value.stokGruplari || []).map((g) => g.deger).filter(Boolean)
)
const kategoriOnerileri = computed(() =>
  (gruplamaDagilimi.value.kategoriler || []).map((g) => g.deger).filter(Boolean)
)

const grupAdetleri = computed(() => {
  const harita = {}
  for (const g of gruplamaDagilimi.value.stokGruplari || []) {
    if (g?.deger) harita[g.deger] = g.adet
  }
  return harita
})

const grubaFiltrele = (g) => {
  filtreStokGrubu.value = g || ''
  filtreDegisti()
}

// Satış - alış üzerinden kâr marjı (%)
const marjHesapla = (s) => {
  const alis = Number(s?.fiyat || 0)
  const satis = Number(s?.satisFiyati || 0)
  if (!alis || !satis) return null
  return Math.round(((satis - alis) / alis) * 1000) / 10
}

const fiyatlariYukle = async (s) => {
  if (!s?.id || fiyatListeleri.value[s.id]) return
  try {
    const r = await stokAPI.getFiyatlar(s.id)
    fiyatListeleri.value = { ...fiyatListeleri.value, [s.id]: r.data || [] }
  } catch {
    fiyatListeleri.value = { ...fiyatListeleri.value, [s.id]: [] }
  }
}

const depoDagilimYukle = async () => {
  try {
    const r = await depoAPI.stokDagilimi()
    const harita = {}
    for (const d of (r.data || [])) {
      if (!harita[d.stokId]) harita[d.stokId] = []
      harita[d.stokId].push(d)
    }
    depoDagilim.value = harita
  } catch {
    // Dağılım opsiyonel bir görünümdür; hata listeyi engellemez.
  }
}

const hareketBaslik = computed(() => (hareketTur.value === 'GIRIS' ? t('stoklar.hareketGiris') : t('stoklar.hareketCikis')))

// Toplu fiyat diyaloğundaki üretim tipi seçenekleri de tüm katalogdan gelir.
// NOT: `stokGrubuOnerileri` kaldırıldı. Filtre artık Dropdown (yalnızca mevcut
// değerler) ve toplu fiyat diyaloğu da filtresiz `stokGruplari` listesini alır;
// önce ikisi aynı (filtreyle daraltılmış) listeyi paylaşıyordu.

// Not: `filtrelenmisStoklar` kaldırıldı. Liste sunucu tarafında sayfalanıyor ve
// filtreler `stoklariYukle()` ile sunucuya gönderiliyor; aynı filtreleri client'da
// tekrar uygulamak yalnızca o SAYFA satırlarını etkiliyordu (kategori/üretim tipi
// karşılaştırması ayrıca büyük/küçük harfe duyarlıydı). Boş sonuç kontrolü artık
// doğrudan gelen satır sayısına bakıyor.

const stoklariYukle = async () => {
  const params = { page: stokSayfa.value, size: stokSayfaBoyutu.value }
  if (filtreArama.value.trim()) params.q = filtreArama.value.trim()
  if (filtreKategori.value) params.kategori = filtreKategori.value
  if (filtreMarka.value) params.marka = filtreMarka.value
  if (filtreStokGrubu.value) params.stokGrubu = filtreStokGrubu.value
  if (filtreMinFiyat.value != null) params.minFiyat = filtreMinFiyat.value
  if (filtreMaxFiyat.value != null) params.maxFiyat = filtreMaxFiyat.value
  if (filtreDepo.value != null) params.depoId = filtreDepo.value
  await stokStore.filtreli(params)
}

const stokSayfaDegisti = (event) => {
  stokSayfa.value = event.page
  stokSayfaBoyutu.value = event.rows
  stoklariYukle()
}

// Filtre değişince debounce ile yeniden yükle
const filtreDegisti = () => {
  if (stokAramaZaman) clearTimeout(stokAramaZaman)
  stokAramaZaman = setTimeout(() => {
    stokSayfa.value = 0
    stoklariYukle()
  }, 300)
}

const kritikAdet = computed(() => stokStore.stoklar.filter((s) => s.minMiktar && s.miktar <= s.minMiktar).length)

// Tedarikci secici sunucu aramali ve yalnizca tedarikci/"her ikisi" turune
// filtreli. Once `getAllCariHesaplar()` ile ilk 50 kayit cekiliyordu; 50.
// kayittan sonraki bir tedarikci stoga atanamadan sessizce kayboluyordu.
// Backend `tur` filtresi "Her Ikisi" kayitlarini da dahil eder.
const {
  oneriler: tedarikciOnerileri,
  ara: tedarikciAra,
  hemenAra: tedarikciOnerileriYukle,
  yukleniyor: tedarikciOnerileriYukleniyor
} = useCariOnerileri({ ekParams: { tur: 'Tedarikci' } })

onMounted(async () => {
  // Bir yukleme hatasi digerini engellemesin (store'lar hata firlatir).
  await Promise.allSettled([
    stoklariYukle(),
    tedarikciOnerileriYukle(),
    depoAPI.getAll({ size: 500 }).then((r) => { depolar.value = unwrapList(r) }),
    depoDagilimYukle(),
    gruplamaDagilimiYukle()
  ])
})

const filtreTemizle = () => {
  filtreArama.value = ''
  filtreKategori.value = ''
  filtreMarka.value = ''
  filtreStokGrubu.value = ''
  filtreDepo.value = null
  filtreMinFiyat.value = null
  filtreMaxFiyat.value = null
  stokSayfa.value = 0
  stoklariYukle()
}

const stokSec = async (s) => {
  seciliStok.value = s
  seciliStokId.value = s.id
  detailStok.value = s
  showDetailDialog.value = true
  // Tek istek: hem yan panel (`StokHareketBolum`) hem detay dialogu aynı listeyi
  // gösteriyor. Önceden burada `getHareketler` çağrılıp ardından
  // `stokHareketleriYukle` ile aynı uç ikinci kez isteniyordu.
  await stokHareketleriYukle(s.id)
}

const openDialog = () => {
  editingId.value = null
  form.value = {
    stokKodu: '',
    barkod: '',
    ad: '',
    birim: '',
    birim2: '',
    cevrimKatsayisi: null,
    marka: '',
    stokGrubu: '',
    kategori: '',
    rafNo: '',
    fiyat: 0,
    satisFiyati: null,
    kdvOrani: null,
    agirlik: null,
    miktar: 0,
    minMiktar: null,
    tedarikciId: null,
    tedarikciStokKodu: '',
    tedarikciFiyat: null,
    maliyetYontemi: 'ORTALAMA',
    varsayilanDepoId: null,
    aciklama: '',
    fiyatlar: [
      { ad: 'Perakende', fiyat: 0 },
      { ad: 'Toptan', fiyat: 0 },
      { ad: 'Kurumsal', fiyat: 0 }
    ]
  }
  formTemizle()
  showDialog.value = true
}

const stokEylemleri = (s) => {
  const eylemler = [
    { etiket: t('stoklar.barkodEtiket'), ikon: 'pi pi-barcode', islem: () => barkodEtiket(s) }
  ]
  if (!s.barkod) {
    eylemler.push({ etiket: t('stoklar.barkodUret'), ikon: 'pi pi-sparkles', islem: () => barkodUretTek(s) })
  }
  eylemler.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => confirmDel(s.id) })
  return eylemler
}

const editStok = (s) => {
  editingId.value = s.id
  form.value = {
    stokKodu: s.stokKodu || '',
    barkod: s.barkod || '',
    ad: s.ad,
    birim: s.birim || '',
    birim2: s.birim2 || '',
    cevrimKatsayisi: s.cevrimKatsayisi || null,
    marka: s.marka || '',
    stokGrubu: s.stokGrubu || '',
    kategori: s.kategori || '',
    rafNo: s.rafNo || '',
    fiyat: s.fiyat,
    satisFiyati: s.satisFiyati,
    kdvOrani: s.kdvOrani,
    agirlik: s.agirlik,
    miktar: s.miktar,
    minMiktar: s.minMiktar,
    tedarikciId: s.tedarikciId || null,
    tedarikciStokKodu: s.tedarikciStokKodu || '',
    tedarikciFiyat: s.tedarikciFiyat || null,
    maliyetYontemi: s.maliyetYontemi || 'ORTALAMA',
    varsayilanDepoId: s.varsayilanDepoId || null,
    aciklama: s.aciklama || '',
    fotoUrl: s.fotoUrl || '',
    fotoThumbUrl: s.fotoThumbUrl || '',
    fiyatlar: (s.fiyatlar || []).map((f) => ({ ...f }))
  }
  formTemizle()
  showDialog.value = true
}

const fiyatEkle = () => {
  form.value.fiyatlar.push({ ad: '', fiyat: 0 })
}

const fiyatSil = async (f) => {
  if (f.id) {
    try {
      await stokAPI.fiyatSil(f.id)
      toastBildirim.basarili(t('stoklar.fiyatSilindi'))
    } catch (err) {
      toastBildirim.hata(err?.response?.data?.message || t('stoklar.fiyatSilinemedi'))
      return
    }
  }
  form.value.fiyatlar = form.value.fiyatlar.filter((x) => x !== f)
}

const barkodOneriYukleniyor = ref(false)

// Formda barkod alanini sunucunun onerdigi yeni EAN-13 ile doldurur.
const barkodOnerForma = async () => {
  barkodOneriYukleniyor.value = true
  try {
    const { data } = await stokAPI.barkodOnerisi()
    if (data?.barkod) form.value.barkod = data.barkod
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stoklar.islemBasarisiz'))
  } finally {
    barkodOneriYukleniyor.value = false
  }
}

const saveStok = async () => {
  formHatalar.value = { ad: '' }
  if (!form.value.ad.trim()) {
    formHatalar.value.ad = t('stoklar.urunAdiGiriniz')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await stokStore.updateStok(editingId.value, form.value)
      await fiyatlariKaydet(editingId.value)
      toastBildirim.basarili(t('stoklar.urunGuncellendi'))
    } else {
      const yeniStok = await stokStore.addStok(form.value)
      // Yeni stokun ID'siyle fiyatları kaydet
      if (yeniStok?.id) {
        await fiyatlariKaydet(yeniStok.id)
      }
      toastBildirim.basarili(t('stoklar.urunEklendi'))
    }
    formTemizle()
    showDialog.value = false
    await stoklariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const fiyatlariKaydet = async (stokId) => {
  for (const f of form.value.fiyatlar) {
    if (!f.ad || !f.ad.trim() || f.fiyat == null || f.fiyat <= 0) continue
    if (f.id) {
      await stokAPI.fiyatGuncelle(f.id, { ad: f.ad, fiyat: f.fiyat })
    } else {
      await stokAPI.fiyatEkle(stokId, { ad: f.ad, fiyat: f.fiyat })
    }
  }
}

const fotoSec = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  const hata = resimDogrula(file)
  if (hata) { toastBildirim.hata(t(hata.key, hata.params)); e.target.value = ''; return }
  try {
    // Yükleme öncesi tarayıcıda sıkıştır (telefon fotoğrafları 10MB'a kadar kabul edilir).
    const kucuk = await resimSikistir(file)
    const r = await uploadAPI.foto(kucuk)
    form.value.fotoUrl = r.data?.url || ''
    form.value.fotoThumbUrl = r.data?.thumbUrl || ''
    toastBildirim.basarili(t('stoklar.fotografYuklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.error || err?.response?.data?.message || t('stoklar.fotografYuklenemedi'))
  } finally {
    e.target.value = ''
  }
}

const confirmDel = (id) => {
  const silinecek = stokStore.stoklar.find((s) => s.id === id)
  confirm.require({
    message: t('stoklar.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await stokStore.deleteStok(id)
        if (seciliStokId.value === id) {
          seciliStok.value = null
          seciliStokId.value = null
          stokHareketler.value = []
        }
        toastBildirim.basarili(t('stoklar.urunSilindi'))
        if (silinecek)
          silVeGeriAl({ veri: silinecek, metin: `${silinecek.ad} silindi`, geriYukle: (v) => stokStore.addStok(v) })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.silmeBasarisiz'))
      }
    }
  })
}

const openHareketDialog = (tur) => {
  hareketTur.value = tur
  hareketForm.value = { miktar: null, hareketTarihi: new Date(), cariHesapId: null, depoId: null, aciklama: '' }
  showHareketDialog.value = true
}

const batchSil = () => {
  if (!seciliStoklar.value.length) return
  confirm.require({
    message: t('stoklar.topluSilOnayMesaj', { n: seciliStoklar.value.length }),
    header: t('common.topluSilmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      let basarili = 0,
        hatali = 0
      for (const s of [...seciliStoklar.value]) {
        try {
          await stokStore.deleteStok(s.id)
          basarili++
        } catch {
          hatali++
        }
      }
      seciliStoklar.value = []
      toast.add({
        severity: hatali ? 'warn' : 'success',
        summary: t('stoklar.tamamlandi'),
        detail: `${basarili} ${t('stoklar.silindi')}${hatali ? ', ' + hatali + ' ' + t('stoklar.hata') : ''}`,
        life: 5000
      })
    }
  })
}

const batchCsvExport = () => {
  if (!seciliStoklar.value.length) return
  const kolonlar = ['ad', 'stokKodu', 'barkod', 'birim', 'fiyat', 'miktar', 'minMiktar']
  const baslik = kolonlar.join(';')
  const satirlar = seciliStoklar.value.map((s) => kolonlar.map((k) => s[k] ?? '').join(';'))
  const csv = '\uFEFF' + [baslik, ...satirlar].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `stoklar-${getLocalDateString()}.csv`)
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

// Hareket sonrası tekrar yükleme: tablo sunucu sayfalı (`filtreli`), seçili
// stok da tekil. Önceden `getAll({ size: 1000 })` çağrılıyordu; `getAll` ve
// `filtreli` aynı store dizisini yazdığı için tablo 1000 satıra düşüyor, aktif
// kategori/marka/depo/arama filtreleri ve paginator kayboluyordu.
const hareketSonrasiYenile = async () => {
  const sr = await stokAPI.getById(seciliStokId.value)
  seciliStok.value = sr.data
  detailStok.value = sr.data
  await stokHareketleriYukle(seciliStokId.value)
  await stoklariYukle()
}

const saveHareket = async () => {
  if (!hareketForm.value.miktar || hareketForm.value.miktar <= 0) {
    toastBildirim.uyari(t('stoklar.gecerliMiktar'))
    return
  }
  saving.value = true
  try {
    await stokAPI.addHareket(seciliStokId.value, {
      tur: hareketTur.value,
      miktar: hareketForm.value.miktar,
      hareketTarihi: getLocalDateString(hareketForm.value.hareketTarihi),
      cariHesapId: hareketForm.value.cariHesapId,
      depoId: hareketForm.value.depoId,
      aciklama: hareketForm.value.aciklama
    })
    await hareketSonrasiYenile()
    depoDagilimYukle()
    showHareketDialog.value = false
    toastBildirim.basarili(t('stoklar.hareketEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const delHareket = (id) => {
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await stokAPI.deleteHareket(id)
        await hareketSonrasiYenile()
        toastBildirim.basarili(t('stoklar.hareketSilindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.silmeBasarisiz'))
      }
    }
  })
}

const batchFiyatDialog = ref(false)
const batchLoading = ref(false)
const batchFiyatForm = ref({ oran: 0, yon: 'ARTIR', kategori: '', stokGrubu: '' })

const batchFiyatUygula = async () => {
  if (!batchFiyatForm.value.oran || batchFiyatForm.value.oran <= 0) {
    toastBildirim.uyari(t('stoklar.gecerliOran'))
    return
  }
  batchLoading.value = true
  try {
    const r = await stokAPI.topluFiyatGuncelle({
      kategori: batchFiyatForm.value.kategori || null,
      stokGrubu: batchFiyatForm.value.stokGrubu || null,
      marka: null,
      yon: batchFiyatForm.value.yon,
      oran: batchFiyatForm.value.oran
    })
    const guncellenen = r.data?.etkilenenStokSayisi || r.data?.guncellenen || 0
    await stoklariYukle()
    batchFiyatDialog.value = false
    toastBildirim.basarili(t('stoklar.fiyatGuncellendi', { n: guncellenen }))
  } catch (e) {
    toastBildirim.hata(e.response?.data?.message || t('stoklar.topluFiyatBasarisiz'))
  } finally {
    batchLoading.value = false
  }
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.stoklar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Stoklar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}

// Hareket listesi hem yan panelde (`StokHareketBolum`) hem detay dialogunda
// (`StokDetayDialog`) gösteriliyor; ikisi de aynı veriyi okuyor.
const stokHareketleriYukle = async (stokId) => {
  hareketlerYukleniyor.value = true
  hareketler.value = []
  stokHareketler.value = []
  try {
    const r = await stokAPI.getHareketler(stokId)
    hareketler.value = r.data
    stokHareketler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.hareketYuklenemedi'))
  } finally {
    hareketlerYukleniyor.value = false
  }
}
</script>

<style scoped>
@import '../assets/stoklar.css';
</style>
