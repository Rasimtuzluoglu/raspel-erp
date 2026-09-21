<template>
  <div class="saha-portali-sayfasi">
    <!-- Header Banner -->
    <div class="saha-header-card">
      <div class="saha-header-content">
        <div class="user-info">
          <div class="user-avatar">
            <i class="pi pi-user" />
          </div>
          <div>
            <h2 class="user-name">
              {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username || t('sahaPortali.sahaPersoneli') }}
            </h2>
            <span class="user-badge">
              <i class="pi pi-compass" /> {{ t('sahaPortali.portalBaslik') }}
            </span>
          </div>
        </div>
        <div class="header-actions">
          <Button
            :label="t('sahaPortali.hizliSiparis')"
            icon="pi pi-plus"
            class="p-button-warning p-button-sm font-semibold"
            @click="yeniSiparisModal = true"
          />
          <Button
            icon="pi pi-refresh"
            :aria-label="$t('common.refresh')"
            class="p-button-rounded p-button-text text-white p-button-sm"
            :loading="yukleniyor"
            @click="tumunuYukle"
          />
        </div>
      </div>
    </div>

    <!-- PrimeVue TabView Sekmeleri -->
    <TabView>
      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-shopping-bag" />
            {{ t('sahaPortali.siparisTeslimat') }}
            <span
              v-if="bekleyenSiparisSayisi > 0"
              class="tab-badge"
            >{{ bekleyenSiparisSayisi }}</span>
          </span>
        </template>

        <SahaSiparislerPanel
          :siparisler="siparisler"
          @durum-sec="durumSecModalAc"
          @imza-ac="imzaModalAc"
          @whatsapp="whatsappSiparisPaylas"
        />
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-map-marker" />
            {{ t('sahaPortali.ziyaretKaydi') }}
          </span>
        </template>

        <div class="fade-in-section">
          <div class="form-container-card">
            <div class="form-header">
              <h3><i class="pi pi-map-marker text-red-500 mr-2" />{{ t('sahaPortali.ziyaretBaslik') }}</h3>
              <p>{{ t('sahaPortali.ziyaretAciklama') }}</p>
            </div>

            <div class="form-body">
              <div class="form-field">
                <label>{{ t('sahaPortali.ziyaretMusteriZorunlu') }}</label>
                <Dropdown
                  v-model="ziyaretForm.cariHesapId"
                  :options="cariHesaplar"
                  option-label="ad"
                  option-value="id"
                  :placeholder="t('sahaPortali.musteriSecin')"
                  filter
                  class="w-full"
                />
              </div>
              <div class="form-field">
                <label>{{ t('sahaPortali.ziyaretAmaci') }}</label>
                <Dropdown
                  v-model="ziyaretForm.amac"
                  :options="[t('sahaPortali.amacSatisTanitim'), t('sahaPortali.amacSiparisTeklif'), t('sahaPortali.amacTahsilat'), t('sahaPortali.amacRutin'), t('sahaPortali.amacDestek')]"
                  class="w-full"
                />
              </div>
              <div class="form-field">
                <label>{{ t('sahaPortali.gorusmeNotlariZorunlu') }}</label>
                <Textarea
                  v-model="ziyaretForm.notlar"
                  rows="4"
                  :placeholder="t('sahaPortali.notlarPlaceholder')"
                  class="w-full"
                />
              </div>
              <div class="gps-location-row flex items-center justify-between p-3 bg-secondary dark:bg-gray-800 rounded-lg mb-3">
                <div class="flex items-center gap-2">
                  <i class="pi pi-map-marker text-red-500 text-lg" />
                  <span class="text-sm text-secondary dark:text-gray-200">
                    {{ ziyaretKonum || t('sahaPortali.konumAlinmadi') }}
                  </span>
                </div>
                <Button
                  :label="t('sahaPortali.konumAl')"
                  icon="pi pi-compass"
                  class="p-button-outlined p-button-sm p-button-secondary"
                  :loading="konumAliniyor"
                  @click="gpsKonumAl"
                />
              </div>
              <div class="form-field">
                <label>{{ t('sahaPortali.ziyaretFotografiOpsiyonel') }}</label>
                <label class="foto-sec-etiket">
                  <i class="pi pi-camera" /> {{ ziyaretFoto ? ziyaretFoto.name : t('sahaPortali.fotografSec') }}
                  <input
                    type="file"
                    accept="image/*"
                    hidden
                    @change="(e) => (ziyaretFoto = e.target.files?.[0] || null)"
                  >
                </label>
              </div>
              <Button
                :label="t('sahaPortali.ziyaretIlet')"
                icon="pi pi-send"
                class="p-button-primary w-full mt-2 font-bold"
                :loading="ziyaretKaydediliyor"
                @click="ziyaretKaydet"
              />
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-wallet" />
            {{ t('sahaPortali.masrafAvans') }}
          </span>
        </template>

        <div class="fade-in-section">
          <div class="section-title-row">
            <h3><i class="pi pi-receipt text-primary mr-2" />{{ t('sahaPortali.masrafAvansTaleplerim') }}</h3>
            <Button
              :label="t('sahaPortali.yeniMasrafAvans')"
              icon="pi pi-plus"
              class="p-button-primary p-button-sm"
              @click="yeniMasrafModal = true"
            />
          </div>

          <div
            v-if="masraflar && masraflar.length > 0"
            class="expense-list"
          >
            <div
              v-for="m in masraflar"
              :key="m.id"
              class="expense-card"
            >
              <div class="expense-left">
                <div class="flex items-center gap-2 mb-1">
                  <span class="expense-type-badge">{{ m.tur === 'AVANS' ? t('sahaPortali.avansTalebi') : m.kategori }}</span>
                  <Tag
                    :value="m.durum"
                    :severity="talepDurumSeverity(m.durum)"
                    rounded
                  />
                </div>
                <p class="expense-desc">
                  {{ m.aciklama || '-' }}
                </p>
                <small class="expense-date">{{ formatTarih(m.tarih) }}</small>
              </div>
              <div class="expense-right">
                <span class="expense-amount">{{ formatPara(m.tutar) }}</span>
              </div>
            </div>
          </div>

          <div
            v-else
            class="empty-box"
          >
            <i class="pi pi-wallet empty-icon" />
            <p>{{ t('sahaPortali.masrafBos') }}</p>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-calendar" />
            {{ t('sahaPortali.izinTalebi') }}
          </span>
        </template>

        <div class="fade-in-section">
          <div class="section-title-row">
            <h3><i class="pi pi-calendar-plus text-primary mr-2" />{{ t('sahaPortali.izinTaleplerim') }}</h3>
            <Button
              :label="t('sahaPortali.yeniIzinTalebi')"
              icon="pi pi-plus"
              class="p-button-primary p-button-sm"
              @click="yeniIzinModal = true"
            />
          </div>

          <div
            v-if="izinler && izinler.length > 0"
            class="expense-list"
          >
            <div
              v-for="i in izinler"
              :key="i.id"
              class="expense-card"
            >
              <div class="expense-left">
                <div class="flex items-center gap-2 mb-1">
                  <strong class="text-base text-primary dark:text-gray-100">{{ i.izinTuru }}</strong>
                  <Tag
                    :value="i.durum"
                    :severity="talepDurumSeverity(i.durum)"
                    rounded
                  />
                </div>
                <p class="expense-desc">
                  <i class="pi pi-calendar mr-1" />
                  {{ formatTarih(i.baslangic) }} → {{ formatTarih(i.bitis) }}
                  <span class="font-bold text-primary ml-1">({{ i.gunSayisi }} {{ t('sahaPortali.gun') }})</span>
                </p>
                <small
                  v-if="i.aciklama"
                  class="expense-date"
                >{{ i.aciklama }}</small>
              </div>
            </div>
          </div>

          <div
            v-else
            class="empty-box"
          >
            <i class="pi pi-calendar empty-icon" />
            <p>{{ t('sahaPortali.izinBos') }}</p>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-money-bill" />
            {{ t('sahaPortali.tahsilat') }}
          </span>
        </template>
        <div class="fade-in-section">
          <div class="form-container-card">
            <div class="form-header">
              <h3><i class="pi pi-money-bill text-primary mr-2" />{{ t('sahaPortali.tahsilatBaslik') }}</h3>
              <p>{{ t('sahaPortali.tahsilatAciklama') }}</p>
            </div>
            <div class="form-body">
              <div class="form-field">
                <label>{{ t('sahaPortali.musteriZorunlu') }}</label>
                <Dropdown
                  v-model="tahsilatForm.cariHesapId"
                  :options="cariHesaplar"
                  option-label="ad"
                  option-value="id"
                  :placeholder="t('sahaPortali.musteriSecin')"
                  filter
                  class="w-full"
                />
              </div>
              <div class="form-row-2">
                <div class="form-field">
                  <label>{{ t('sahaPortali.tutarZorunlu') }}</label>
                  <input
                    v-model.number="tahsilatForm.tutar"
                    type="number"
                    min="1"
                    step="0.01"
                    class="p-inputtext w-full"
                    placeholder="0.00"
                  >
                </div>
                <div class="form-field">
                  <label>{{ t('sahaPortali.odemeYontemi') }}</label>
                  <Dropdown
                    v-model="tahsilatForm.odemeYontemi"
                    :options="odemeYontemleri"
                    option-label="label"
                    option-value="value"
                    class="w-full"
                  />
                </div>
              </div>
              <div class="form-field">
                <label>{{ t('common.description') }}</label>
                <Textarea
                  v-model="tahsilatForm.aciklama"
                  rows="2"
                  class="w-full"
                />
              </div>
              <Button
                :label="t('sahaPortali.tahsilatiKaydet')"
                icon="pi pi-check"
                class="p-button-success w-full font-bold"
                :loading="tahsilatGonderiliyor"
                @click="tahsilatKaydet"
              />
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-check-square" />
            {{ t('sahaPortali.gorevNot') }}
          </span>
        </template>
        <div class="fade-in-section gorev-not-grid">
          <div class="form-container-card">
            <div class="form-header">
              <h3><i class="pi pi-check-square text-primary mr-2" />{{ t('sahaPortali.gorevEkle') }}</h3>
            </div>
            <div class="form-body">
              <div class="form-field">
                <label>{{ t('sahaPortali.gorevBasligi') }}</label>
                <InputText
                  v-model="gorevForm.baslik"
                  class="w-full"
                />
              </div>
              <div class="form-row-2">
                <div class="form-field">
                  <label>{{ t('sahaPortali.bitisTarihi') }}</label>
                  <input
                    v-model="gorevForm.bitisTarihi"
                    type="date"
                    class="p-inputtext w-full"
                  >
                </div>
                <div class="form-field">
                  <label>{{ t('sahaPortali.oncelik') }}</label>
                  <Dropdown
                    v-model="gorevForm.oncelik"
                    :options="oncelikSecenekleri"
                    option-label="label"
                    option-value="value"
                    class="w-full"
                  />
                </div>
              </div>
              <div class="form-field">
                <label>{{ t('common.description') }}</label>
                <Textarea
                  v-model="gorevForm.aciklama"
                  rows="2"
                  class="w-full"
                />
              </div>
              <Button
                :label="t('sahaPortali.gorevKaydet')"
                icon="pi pi-plus"
                class="p-button-primary w-full"
                :loading="gorevGonderiliyor"
                @click="gorevKaydet"
              />
            </div>
          </div>

          <div class="form-container-card">
            <div class="form-header">
              <h3><i class="pi pi-pen-to-square text-primary mr-2" />{{ t('sahaPortali.hizliNot') }}</h3>
            </div>
            <div class="form-body">
              <div class="form-field">
                <label>{{ t('sahaPortali.notBasligi') }}</label>
                <InputText
                  v-model="notForm.baslik"
                  class="w-full"
                />
              </div>
              <div class="form-field">
                <label>{{ t('common.description') }}</label>
                <Textarea
                  v-model="notForm.icerik"
                  rows="3"
                  class="w-full"
                />
              </div>
              <Button
                :label="t('sahaPortali.notKaydet')"
                icon="pi pi-save"
                class="p-button-primary w-full"
                :loading="notGonderiliyor"
                @click="notKaydet"
              />
            </div>
          </div>

          <div class="form-container-card gorev-liste">
            <div class="form-header">
              <h3><i class="pi pi-list text-primary mr-2" />{{ t('sahaPortali.gorevlerim') }}</h3>
            </div>
            <div v-if="gorevler.length">
              <div
                v-for="g in gorevler"
                :key="g.id"
                class="gorev-satir"
              >
                <Checkbox
                  :model-value="g.durum === 'TAMAMLANDI'"
                  :binary="true"
                  @update:model-value="gorevTamamla(g)"
                />
                <span :class="{ tamam: g.durum === 'TAMAMLANDI' }">{{ g.baslik }}</span>
                <small v-if="g.bitisTarihi">{{ formatTarih(g.bitisTarihi) }}</small>
              </div>
            </div>
            <div
              v-else
              class="empty-box"
            >
              <p>{{ t('sahaPortali.gorevYok') }}</p>
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-chart-bar" />
            {{ t('sahaPortali.performans') }}
          </span>
        </template>
        <div class="fade-in-section">
          <div class="performans-grid">
            <div class="kpi-kart">
              <span class="kpi-deger">{{ bekleyenSiparisSayisi }}</span>
              <span class="kpi-etiket">{{ t('sahaPortali.bekleyenSiparis') }}</span>
            </div>
            <div class="kpi-kart">
              <span class="kpi-deger">{{ teslimEdilenSiparisSayisi }}</span>
              <span class="kpi-etiket">{{ t('sahaPortali.teslimEdilenSiparis') }}</span>
            </div>
            <div class="kpi-kart">
              <span class="kpi-deger">{{ ziyaretSayisi }}</span>
              <span class="kpi-etiket">{{ t('sahaPortali.ziyaretSayisi') }}</span>
            </div>
            <div class="kpi-kart">
              <span class="kpi-deger">{{ formatPara(tahsilatToplam) }}</span>
              <span class="kpi-etiket">{{ t('sahaPortali.tahsilatToplam') }}</span>
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <!-- MODAL: HIZLI SİPARİŞ AL -->
    <Dialog
      v-model:visible="yeniSiparisModal"
      :modal="true"
      :header="t('sahaPortali.sahadaHizliSiparis')"
      :style="{ width: '90%', maxWidth: '520px' }"
    >
      <div class="modal-form-content">
        <div class="form-field">
          <label>{{ t('sahaPortali.musteriZorunlu') }}</label>
          <Dropdown
            v-model="yeniSiparisForm.cariHesapId"
            :options="cariHesaplar"
            option-label="ad"
            option-value="id"
            :placeholder="t('sahaPortali.musteriSecin')"
            filter
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('sahaPortali.barkodIleBul') }}</label>
          <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><i class="pi pi-qrcode" /></span>
            <InputText
              v-model="barkodArama"
              :placeholder="t('sahaPortali.barkodPlaceholder')"
              @keyup.enter="barkodlaUrunBul"
            />
            <Button
              icon="pi pi-search"
              :aria-label="$t('common.searchAction')"
              class="p-button-outlined"
              @click="barkodlaUrunBul"
            />
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('sahaPortali.urunStokZorunlu') }}</label>
          <Dropdown
            v-model="yeniSiparisForm.stokId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            :placeholder="t('sahaPortali.urunSecin')"
            filter
            class="w-full"
            @change="hizliSiparisStokSecildi"
          />
          <small
            v-if="seciliStokBilgi"
            class="stok-raf-bilgi"
          >
            <i class="pi pi-box" /> {{ t('sahaPortali.stokKodu') }}: {{ seciliStokBilgi.stokKodu || '-' }} ·
            {{ t('sahaPortali.rafNo') }}: {{ seciliStokBilgi.rafNo || '-' }} ·
            {{ t('sahaPortali.stokMiktar') }}: {{ seciliStokBilgi.miktar ?? '-' }}
          </small>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>{{ t('sahaPortali.miktar') }}</label>
            <input
              v-model.number="yeniSiparisForm.miktar"
              type="number"
              min="1"
              class="p-inputtext w-full"
            >
          </div>
          <div class="form-field">
            <label>{{ t('sahaPortali.birimFiyat') }}</label>
            <input
              v-model.number="yeniSiparisForm.birimFiyat"
              type="number"
              min="0"
              step="0.01"
              class="p-inputtext w-full"
            >
          </div>
        </div>
        <Button
          :label="t('sahaPortali.kalemEkle')"
          icon="pi pi-plus"
          class="p-button-outlined w-full"
          :disabled="!yeniSiparisForm.stokId"
          @click="kalemEkle"
        />
        <div
          v-if="yeniSiparisForm.kalemler.length"
          class="sepet-liste"
        >
          <div
            v-for="(k, i) in yeniSiparisForm.kalemler"
            :key="i"
            class="sepet-satir"
          >
            <span class="sepet-ad">{{ k.ad }} × {{ k.miktar }}</span>
            <span class="sepet-tutar">{{ formatPara(k.tutar) }}</span>
            <button
              type="button"
              class="sepet-sil"
              :aria-label="$t('common.delete')"
              @click="kalemSil(i)"
            >
              <i class="pi pi-trash" />
            </button>
          </div>
          <div class="sepet-toplam">
            {{ t('sahaPortali.sepetToplam') }}: {{ formatPara(sepetToplam) }}
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('sahaPortali.teslimatAdresi') }}</label>
          <InputText
            v-model="yeniSiparisForm.adres"
            :placeholder="t('sahaPortali.teslimAdresiPlaceholder')"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="yeniSiparisModal = false"
        />
        <Button
          :label="t('sahaPortali.siparisiGonder')"
          icon="pi pi-check"
          class="p-button-success font-bold"
          :loading="siparisKaydediliyor"
          @click="hizliSiparisKaydet"
        />
      </template>
    </Dialog>

    <!-- MODAL: DİJİTAL İMZA & TESLİMAT -->
    <Dialog
      v-model:visible="imzaModal"
      :modal="true"
      :header="t('sahaPortali.teslimatImzasi')"
      :style="{ width: '90%', maxWidth: '460px' }"
    >
      <div class="modal-form-content">
        <div class="form-field">
          <label>{{ t('sahaPortali.teslimAlanZorunlu') }}</label>
          <InputText
            v-model="imzaForm.teslimAlan"
            :placeholder="t('sahaPortali.teslimAlanPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('sahaPortali.teslimatNotu') }}</label>
          <InputText
            v-model="imzaForm.notlar"
            :placeholder="t('sahaPortali.teslimatNotuPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <div class="flex justify-between items-center mb-1">
            <label>{{ t('sahaPortali.dijitalImza') }}</label>
          </div>
          <ImzaPad
            ref="imzaPadRef"
            :etiket="t('sahaPortali.dijitalImzaEtiket')"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="imzaModal = false"
        />
        <Button
          :label="t('sahaPortali.teslimatiOnayla')"
          icon="pi pi-check"
          class="p-button-success font-bold"
          :loading="teslimEdiliyor"
          @click="teslimatOnayla"
        />
      </template>
    </Dialog>

    <!-- MODAL: YENİ İZİN TALEBİ -->
    <Dialog
      v-model:visible="yeniIzinModal"
      :modal="true"
      :header="t('sahaPortali.yeniIzinTalebiAc')"
      :style="{ width: '90%', maxWidth: '420px' }"
    >
      <div class="modal-form-content">
        <div class="form-field">
          <label>{{ t('sahaPortali.izinTuru') }}</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="[t('sahaPortali.izinYillik'), t('sahaPortali.izinMazeret'), t('sahaPortali.izinSaglik'), t('sahaPortali.izinEvlilik'), t('sahaPortali.izinUcretsiz')]"
            class="w-full"
          />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>{{ t('sahaPortali.baslangic') }}</label>
            <InputText
              v-model="izinForm.baslangic"
              type="date"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label>{{ t('sahaPortali.bitis') }}</label>
            <InputText
              v-model="izinForm.bitis"
              type="date"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="izinForm.aciklama"
            rows="2"
            :placeholder="t('sahaPortali.izinGerekcesi')"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="yeniIzinModal = false"
        />
        <Button
          :label="t('sahaPortali.talebiGonder')"
          icon="pi pi-send"
          class="p-button-primary font-bold"
          :loading="izinGonderiliyor"
          @click="izinTalepGonder"
        />
      </template>
    </Dialog>

    <!-- MODAL: YENİ MASRAF / AVANS -->
    <Dialog
      v-model:visible="yeniMasrafModal"
      :modal="true"
      :header="t('sahaPortali.yeniMasrafAvansTalebi')"
      :style="{ width: '90%', maxWidth: '420px' }"
    >
      <div class="modal-form-content">
        <div class="tab-pill-group">
          <button
            type="button"
            :class="['pill-btn', { active: masrafForm.tur === 'MASRAF' }]"
            @click="masrafForm.tur = 'MASRAF'"
          >
            {{ t('sahaPortali.harcamaMasraf') }}
          </button>
          <button
            type="button"
            :class="['pill-btn', { active: masrafForm.tur === 'AVANS' }]"
            @click="masrafForm.tur = 'AVANS'"
          >
            {{ t('sahaPortali.avansTalebi') }}
          </button>
        </div>

        <div
          v-if="masrafForm.tur === 'MASRAF'"
          class="form-field"
        >
          <label>{{ t('sahaPortali.harcamaKategorisi') }}</label>
          <Dropdown
            v-model="masrafForm.kategori"
            :options="['YAKIT', 'YEMEK', 'KONAKLAMA', 'ULASIM', 'MALZEME', 'DIGER']"
            class="w-full"
          />
        </div>

        <div class="form-field">
          <label>{{ t('sahaPortali.tutarZorunlu') }}</label>
          <input
            v-model.number="masrafForm.tutar"
            type="number"
            min="1"
            step="0.01"
            class="p-inputtext w-full"
            placeholder="0.00"
          >
        </div>

        <div class="form-field">
          <label>{{ t('sahaPortali.aciklamaZorunlu') }}</label>
          <InputText
            v-model="masrafForm.aciklama"
            :placeholder="t('sahaPortali.masrafAciklamaPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('sahaPortali.masrafFisiOpsiyonel') }}</label>
          <label class="foto-sec-etiket">
            <i class="pi pi-camera" /> {{ masrafFoto ? masrafFoto.name : t('sahaPortali.fotografSec') }}
            <input
              type="file"
              accept="image/*"
              hidden
              @change="(e) => (masrafFoto = e.target.files?.[0] || null)"
            >
          </label>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="yeniMasrafModal = false"
        />
        <Button
          :label="t('sahaPortali.talebiGonder')"
          icon="pi pi-send"
          class="p-button-primary font-bold"
          :loading="masrafGonderiliyor"
          @click="masrafTalepGonder"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="durumSecModal"
      :modal="true"
      :header="t('sahaPortali.durumGuncelle')"
      :style="{ width: '90%', maxWidth: '400px' }"
    >
      <div class="modal-form-content">
        <div class="form-group">
          <label>{{ t('common.status') }}</label>
          <Dropdown
            v-model="seciliYeniDurum"
            :options="durumSecenekleri"
            class="w-full"
            :placeholder="t('sahaPortali.durumSecin')"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="durumSecModal = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="durumKaydediliyor"
          @click="durumKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useAuthStore } from '../stores/authStore.js'
import { siparisAPI, personelIzinAPI, personelMasrafTalepAPI, cariHesapAPI, stokAPI, notAPI, belgeAPI, teslimatAPI, tahsilatAPI, ajandaAPI } from '../api/index.js'
import { useToast } from 'primevue/usetoast'
import SahaSiparislerPanel from '../components/SahaSiparislerPanel.vue'
import ImzaPad from '../components/ImzaPad.vue'
import { useI18n } from 'vue-i18n'
import { formatTarih } from '../utils/format.js'

const { t } = useI18n()
const authStore = useAuthStore()
const toast = useToast()

const yukleniyor = ref(false)

const siparisler = ref([])
const izinler = ref([])
const masraflar = ref([])
const cariHesaplar = ref([])
const stoklar = ref([])

// Modallar
const imzaModal = ref(false)
const yeniIzinModal = ref(false)
const yeniMasrafModal = ref(false)
const yeniSiparisModal = ref(false)
const seciliSiparis = ref(null)

const teslimEdiliyor = ref(false)
const izinGonderiliyor = ref(false)
const masrafGonderiliyor = ref(false)
const ziyaretKaydediliyor = ref(false)
const siparisKaydediliyor = ref(false)

// Formlar
const imzaForm = ref({ teslimAlan: '', notlar: '' })
const izinForm = ref({
  izinTuru: t('sahaPortali.izinYillik'),
  baslangic: new Date().toISOString().substring(0, 10),
  bitis: new Date().toISOString().substring(0, 10),
  aciklama: ''
})
const masrafForm = ref({
  tur: 'MASRAF',
  kategori: 'YAKIT',
  tutar: null,
  aciklama: ''
})
const ziyaretForm = ref({
  cariHesapId: null,
  amac: t('sahaPortali.amacSatisTanitim'),
  notlar: ''
})
const barkodArama = ref('')
const ziyaretKonum = ref('')
const konumAliniyor = ref(false)

const yeniSiparisForm = ref({
  cariHesapId: null,
  stokId: null,
  miktar: 1,
  birimFiyat: 0,
  adres: '',
  kalemler: []
})

// Saha modülleri: tahsilat, görev/not, fotoğraflar
const tahsilatForm = ref({ cariHesapId: null, tutar: null, odemeYontemi: 'NAKIT', aciklama: '' })
const tahsilatGonderiliyor = ref(false)
const tahsilatToplam = ref(0)
const odemeYontemleri = computed(() => [
  { label: t('sahaPortali.nakit'), value: 'NAKIT' },
  { label: t('sahaPortali.havale'), value: 'HAVALE' },
  { label: t('sahaPortali.kart'), value: 'KART' }
])

const gorevForm = ref({ baslik: '', bitisTarihi: '', oncelik: 'ORTA', aciklama: '' })
const gorevler = ref([])
const gorevGonderiliyor = ref(false)
const oncelikSecenekleri = computed(() => [
  { label: t('sahaPortali.oncelikDusuk'), value: 'DUSUK' },
  { label: t('sahaPortali.oncelikOrta'), value: 'ORTA' },
  { label: t('sahaPortali.oncelikYuksek'), value: 'YUKSEK' }
])
const notForm = ref({ baslik: '', icerik: '' })
const notGonderiliyor = ref(false)
const ziyaretFoto = ref(null)
const masrafFoto = ref(null)
const notlar = ref([])

const seciliStokBilgi = computed(() => stoklar.value.find((s) => s.id === yeniSiparisForm.value.stokId) || null)
const sepetToplam = computed(() =>
  yeniSiparisForm.value.kalemler.reduce((t, k) => t + (Number(k.tutar) || 0), 0)
)
const teslimEdilenSiparisSayisi = computed(() =>
  siparisler.value.filter((s) => s?.durum === 'TESLIM_EDILDI').length
)
const ziyaretSayisi = computed(() => notlar.value.filter((n) => n?.kategori === 'SAHA_ZIYARET').length)

// Format Helpers (Lokal ve Güvenli)
const formatPara = (v) => {
  if (v == null || isNaN(v)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
// Canvas
const imzaPadRef = ref(null)

onMounted(async () => {
  await tumunuYukle()
})

const tumunuYukle = async () => {
  yukleniyor.value = true
  try {
    const [sipRes, izinRes, masrafRes, cariRes, stokRes, notRes, gorevRes] = await Promise.allSettled([
      siparisAPI.getAll({ size: 50 }),
      personelIzinAPI.getAll(),
      personelMasrafTalepAPI.getKullaniciTalepleri(),
      cariHesapAPI.getAll({ size: 500 }),
      stokAPI.getAll({ size: 500 }),
      notAPI.getAll({ size: 200 }),
      ajandaAPI.gorevler()
    ])
    if (sipRes.status === 'fulfilled') siparisler.value = unwrapList(sipRes.value)
    if (izinRes.status === 'fulfilled') izinler.value = unwrapList(izinRes.value)
    if (masrafRes.status === 'fulfilled') masraflar.value = unwrapList(masrafRes.value)
    if (cariRes.status === 'fulfilled') cariHesaplar.value = unwrapList(cariRes.value)
    if (stokRes.status === 'fulfilled') stoklar.value = unwrapList(stokRes.value)
    if (notRes.status === 'fulfilled') notlar.value = unwrapList(notRes.value)
    if (gorevRes.status === 'fulfilled') gorevler.value = unwrapList(gorevRes.value)
  } finally {
    yukleniyor.value = false
  }
}

// Mutasyon sonrasi yalnizca etkilenen veri kumesini yeniler (7 ucu birden cekmek yerine).
const siparisleriYukle = async () => {
  try { siparisler.value = unwrapList(await siparisAPI.getAll({ size: 50 })) } catch { /* yoksay */ }
}
const izinleriYukle = async () => {
  try { izinler.value = unwrapList(await personelIzinAPI.getAll()) } catch { /* yoksay */ }
}
const masraflariYukle = async () => {
  try { masraflar.value = unwrapList(await personelMasrafTalepAPI.getKullaniciTalepleri()) } catch { /* yoksay */ }
}
const notlariYukle = async () => {
  try { notlar.value = unwrapList(await notAPI.getAll({ size: 200 })) } catch { /* yoksay */ }
}
const gorevleriYukle = async () => {
  try { gorevler.value = unwrapList(await ajandaAPI.gorevler()) } catch { /* yoksay */ }
}

const bekleyenSiparisSayisi = computed(() =>
  siparisler.value.filter(s => s?.durum !== 'TESLIM_EDILDI' && s?.durum !== 'IPTAL').length
)


const talepDurumSeverity = (durum) => {
  const map = { BEKLEMEDE: 'warning', ONAYLANDI: 'success', REDDEDILDI: 'danger' }
  return map[durum] || 'info'
}

const imzaModalAc = (siparis) => {
  seciliSiparis.value = siparis
  imzaForm.value = { teslimAlan: '', notlar: '' }
  imzaModal.value = true
  nextTick(() => imzaPadRef.value?.hazirla())
}

const teslimatOnayla = async () => {
  if (!imzaForm.value.teslimAlan?.trim()) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.teslimAlanGiriniz'), life: 3000 })
    return
  }
  if (!seciliSiparis.value?.id) return
  if (imzaPadRef.value?.bosMu?.() !== false) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.imzaZorunlu'), life: 3000 })
    return
  }
  teslimEdiliyor.value = true
  try {
    const blob = await imzaPadRef.value.toBlob()
    if (!blob) {
      toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.imzaZorunlu'), life: 3000 })
      return
    }
    const dosya = new File([blob], `imza-${seciliSiparis.value.id}.png`, { type: 'image/png' })
    await teslimatAPI.teslimEtSiparis(
      seciliSiparis.value.id,
      { teslimAlanAd: imzaForm.value.teslimAlan.trim(), teslimNotu: imzaForm.value.notlar },
      dosya
    )
    seciliSiparis.value.durum = 'TESLIM_EDILDI'
    toast.add({ severity: 'success', summary: t('sahaPortali.teslimEdildi'), detail: t('sahaPortali.siparisTeslimEdildi'), life: 3000 })
    imzaModal.value = false
    await siparisleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    teslimEdiliyor.value = false
  }
}

const durumSecModal = ref(false)
const seciliYeniDurum = ref('BEKLIYOR')

const durumSecModalAc = (siparis) => {
  seciliSiparis.value = siparis
  seciliYeniDurum.value = siparis.durum || 'BEKLIYOR'
  durumSecModal.value = true
}

const durumKaydediliyor = ref(false)
const durumSecenekleri = ['BEKLIYOR', 'HAZIRLANIYOR', 'YOLDA', 'TESLIM_EDILDI', 'IPTAL']

const durumKaydet = async () => {
  if (!seciliSiparis.value) return
  durumKaydediliyor.value = true
  try {
    await siparisAPI.durumGuncelle(seciliSiparis.value.id, seciliYeniDurum.value)
    seciliSiparis.value.durum = seciliYeniDurum.value
    durumSecModal.value = false
    toast.add({ severity: 'success', summary: t('sahaPortali.guncellendi'), detail: t('sahaPortali.siparisDurumGuncellendi'), life: 2500 })
    await siparisleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    durumKaydediliyor.value = false
  }
}

const izinTalepGonder = async () => {
  const personelId = authStore?.kullanici?.personelId
  if (!personelId) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.personelKaydiYok'), life: 3000 })
    return
  }
  izinGonderiliyor.value = true
  try {
    const bas = new Date(izinForm.value.baslangic)
    const bit = new Date(izinForm.value.bitis)
    const gunSayisi = Math.max(1, Math.round((bit - bas) / (1000 * 60 * 60 * 24)) + 1)
    await personelIzinAPI.create({
      personelId,
      izinTuru: izinForm.value.izinTuru,
      baslangic: izinForm.value.baslangic,
      bitis: izinForm.value.bitis,
      gunSayisi,
      aciklama: izinForm.value.aciklama
    })
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.izinIletildi'), life: 3000 })
    yeniIzinModal.value = false
    await izinleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    izinGonderiliyor.value = false
  }
}

const masrafTalepGonder = async () => {
  if (!masrafForm.value.tutar || masrafForm.value.tutar <= 0) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksik'), detail: t('sahaPortali.gecerliTutar'), life: 3000 })
    return
  }
  masrafGonderiliyor.value = true
  try {
    const olusan = await personelMasrafTalepAPI.create({
      tur: masrafForm.value.tur,
      kategori: masrafForm.value.kategori,
      tutar: masrafForm.value.tutar,
      aciklama: masrafForm.value.aciklama,
      tarih: new Date().toISOString().substring(0, 10)
    })
    if (masrafFoto.value && olusan?.data?.id) {
      try {
        await belgeAPI.yukle('MasrafTalep', olusan.data.id, masrafFoto.value)
      } catch {
        /* fiş yüklenemedi; talep yine de iletildi */
      }
    }
    masrafFoto.value = null
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.talepIletildi'), life: 3000 })
    yeniMasrafModal.value = false
    await masraflariYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err.message, life: 3000 })
  } finally {
    masrafGonderiliyor.value = false
  }
}

const gpsKonumAl = () => {
  if (!navigator.geolocation) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.desteklenmiyor'), detail: t('sahaPortali.konumDesteklenmiyor'), life: 3000 })
    return
  }
  konumAliniyor.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      konumAliniyor.value = false
      const lat = pos.coords.latitude.toFixed(5)
      const lng = pos.coords.longitude.toFixed(5)
      ziyaretKonum.value = t('sahaPortali.konum', { enlem: lat, boylam: lng })
      const konumNotu = t('sahaPortali.konumEtiketi', { enlem: lat, boylam: lng })
      if (ziyaretForm.value.notlar) {
        ziyaretForm.value.notlar += `\n[📍 ${konumNotu}]`
      } else {
        ziyaretForm.value.notlar = `[📍 ${konumNotu}] `
      }
      toast.add({ severity: 'success', summary: t('sahaPortali.konumAlindi'), detail: t('sahaPortali.konumEklendi'), life: 2500 })
    },
    (err) => {
      konumAliniyor.value = false
      toast.add({ severity: 'error', summary: t('sahaPortali.konumHatasi'), detail: t('sahaPortali.konumAlinamadiHata') + err.message, life: 3000 })
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

const barkodlaUrunBul = () => {
  if (!barkodArama.value) return
  const kod = barkodArama.value.trim().toLowerCase()
  const bulunan = stoklar.value.find(s =>
    (s.barkod && s.barkod.toLowerCase() === kod) ||
    (s.stokKodu && s.stokKodu.toLowerCase() === kod)
  )
  if (bulunan) {
    yeniSiparisForm.value.stokId = bulunan.id
    hizliSiparisStokSecildi()
    toast.add({ severity: 'success', summary: t('sahaPortali.urunBulundu'), detail: bulunan.ad, life: 2000 })
  } else {
    toast.add({ severity: 'warn', summary: t('sahaPortali.bulunamadi'), detail: t('sahaPortali.barkodBulunamadi'), life: 2500 })
  }
}

const whatsappSiparisPaylas = (s) => {
  const musteri = s.cariHesapAdi || s.musteriAdi || t('sahaPortali.musterimiz')
  const kod = s.siparisNo || s.id
  const tutar = formatPara(s.toplamTutar || s.genelToplam || 0)
  const mesaj = t('sahaPortali.whatsappMesaj', { musteri, kod, tutar })
  const url = `https://api.whatsapp.com/send?text=${encodeURIComponent(mesaj)}`
  window.open(url, '_blank')
}

const ziyaretKaydet = async () => {
  if (!ziyaretForm.value.cariHesapId || !ziyaretForm.value.notlar) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.musteriNotlarZorunlu'), life: 3000 })
    return
  }
  ziyaretKaydediliyor.value = true
  try {
    const cari = cariHesaplar.value.find(c => c?.id === ziyaretForm.value.cariHesapId)
    await notAPI.create({
      baslik: t('sahaPortali.sahaZiyareti', { musteri: cari?.ad || t('sahaPortali.musteri'), amac: ziyaretForm.value.amac }),
      icerik: ziyaretForm.value.notlar,
      kategori: 'SAHA_ZIYARET'
    })
    if (ziyaretFoto.value) {
      try {
        await belgeAPI.yukle('CariHesap', ziyaretForm.value.cariHesapId, ziyaretFoto.value)
      } catch {
        /* fotoğraf yüklenemedi; ziyaret yine de kaydedildi */
      }
    }
    ziyaretFoto.value = null
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.ziyaretKaydedildi'), life: 3000 })
    ziyaretForm.value.notlar = ''
    ziyaretForm.value.cariHesapId = null
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err.message, life: 3000 })
  } finally {
    ziyaretKaydediliyor.value = false
  }
}

const kalemEkle = () => {
  const s = seciliStokBilgi.value
  if (!s) return
  const miktar = Number(yeniSiparisForm.value.miktar) || 1
  const fiyat = Number(yeniSiparisForm.value.birimFiyat) || 0
  yeniSiparisForm.value.kalemler.push({
    stokId: s.id,
    ad: s.ad,
    miktar,
    birimFiyat: fiyat,
    kdvOrani: s.kdvOrani ?? 20,
    tutar: miktar * fiyat
  })
  yeniSiparisForm.value.stokId = null
  yeniSiparisForm.value.miktar = 1
  yeniSiparisForm.value.birimFiyat = 0
}

const kalemSil = (i) => {
  yeniSiparisForm.value.kalemler.splice(i, 1)
}

const tahsilatKaydet = async () => {
  if (!tahsilatForm.value.cariHesapId || !tahsilatForm.value.tutar || tahsilatForm.value.tutar <= 0) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.cariTutarZorunlu'), life: 3000 })
    return
  }
  tahsilatGonderiliyor.value = true
  try {
    await tahsilatAPI.gir({
      cariId: tahsilatForm.value.cariHesapId,
      tutar: tahsilatForm.value.tutar,
      odemeYontemi: tahsilatForm.value.odemeYontemi,
      aciklama: tahsilatForm.value.aciklama || t('sahaPortali.sahaTahsilati'),
      hareketTarihi: new Date().toISOString().substring(0, 10)
    })
    tahsilatToplam.value += Number(tahsilatForm.value.tutar) || 0
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.tahsilatKaydedildi'), life: 3000 })
    tahsilatForm.value = { cariHesapId: null, tutar: null, odemeYontemi: 'NAKIT', aciklama: '' }
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    tahsilatGonderiliyor.value = false
  }
}

const gorevKaydet = async () => {
  if (!gorevForm.value.baslik?.trim()) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.gorevBasligiZorunlu'), life: 3000 })
    return
  }
  gorevGonderiliyor.value = true
  try {
    await ajandaAPI.gorevOlustur({
      baslik: gorevForm.value.baslik.trim(),
      bitisTarihi: gorevForm.value.bitisTarihi || null,
      oncelik: gorevForm.value.oncelik,
      aciklama: gorevForm.value.aciklama
    })
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.gorevKaydedildi'), life: 3000 })
    gorevForm.value = { baslik: '', bitisTarihi: '', oncelik: 'ORTA', aciklama: '' }
    await gorevleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    gorevGonderiliyor.value = false
  }
}

const gorevTamamla = async (g) => {
  if (!g?.id) return
  try {
    if (g.durum === 'TAMAMLANDI') {
      await ajandaAPI.gorevGuncelle(g.id, { ...g, durum: 'BEKLIYOR' })
      g.durum = 'BEKLIYOR'
    } else {
      await ajandaAPI.gorevTamamla(g.id)
      g.durum = 'TAMAMLANDI'
    }
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  }
}

const notKaydet = async () => {
  if (!notForm.value.baslik?.trim() || !notForm.value.icerik?.trim()) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.notZorunlu'), life: 3000 })
    return
  }
  notGonderiliyor.value = true
  try {
    await notAPI.create({ baslik: notForm.value.baslik.trim(), icerik: notForm.value.icerik.trim(), kategori: 'SAHA_NOT' })
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.notKaydedildi'), life: 3000 })
    notForm.value = { baslik: '', icerik: '' }
    await notlariYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    notGonderiliyor.value = false
  }
}

const hizliSiparisStokSecildi = () => {
  const stokId = yeniSiparisForm.value?.stokId
  if (!stokId) return
  const s = stoklar.value.find(item => item?.id === stokId)
  if (s) {
    yeniSiparisForm.value.birimFiyat = s.fiyat || s.satisFiyati || 0
  }
}

const hizliSiparisKaydet = async () => {
  const form = yeniSiparisForm.value
  // Sepete eklenmemiş ama seçili tek ürün varsa otomatik kalem olarak al.
  if (form.stokId) kalemEkle()
  if (!form.cariHesapId || !form.kalemler.length) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.musteriUrunZorunlu'), life: 3000 })
    return
  }
  siparisKaydediliyor.value = true
  try {
    await siparisAPI.create({
      cariHesapId: form.cariHesapId,
      tarih: new Date().toISOString().substring(0, 10),
      durum: 'BEKLIYOR',
      aciklama: t('sahaPortali.sahaSiparisi'),
      teslimatAdresi: form.adres || '',
      kalemler: form.kalemler.map((k) => ({
        stokId: k.stokId,
        miktar: k.miktar,
        birimFiyat: k.birimFiyat,
        kdvOrani: k.kdvOrani,
        tutar: k.tutar
      }))
    })
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.siparisGonderildi'), life: 3000 })
    yeniSiparisModal.value = false
    yeniSiparisForm.value = { cariHesapId: null, stokId: null, miktar: 1, birimFiyat: 0, adres: '', kalemler: [] }
    await siparisleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err.message, life: 3000 })
  } finally {
    siparisKaydediliyor.value = false
  }
}
</script>

<style scoped>
.saha-portali-sayfasi {
  padding-bottom: 40px;
}

.saha-header-card {
  background: linear-gradient(135deg, #1e40af 0%, var(--accent) 100%);
  color: white;
  padding: 1.25rem 1.5rem;
  border-radius: 1rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 8px 20px -4px rgba(30, 64, 175, 0.3);
}

.saha-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.user-avatar {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  backdrop-filter: blur(4px);
}

.user-name {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0 0 0.15rem 0;
}

.user-badge {
  font-size: 0.75rem;
  color: #dbeafe;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tab-badge {
  background: #ef4444;
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 9999px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-title-row h3 {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
}

.count-pill {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--bg-muted, rgba(0, 0, 0, 0.05));
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.form-container-card {
  max-width: 600px;
  margin: 0 auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.5rem;
}

.form-header h3 {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
}

.form-header p {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 1.25rem;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: var(--text-secondary);
}

.form-row-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.empty-box {
  text-align: center;
  padding: 3.5rem 1rem;
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: 1rem;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 0.75rem;
  opacity: 0.4;
}

.expense-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.expense-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 0.875rem;
  padding: 1rem 1.25rem;
}

.expense-type-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--accent-soft);
  color: var(--accent);
}

.expense-desc {
  font-size: 0.85rem;
  margin: 0.25rem 0;
  color: var(--text-primary);
}

.expense-date {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.expense-amount {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--primary-color, var(--accent));
}

.tab-pill-group {
  display: flex;
  border-radius: 0.625rem;
  border: 1px solid var(--border);
  overflow: hidden;
  margin-bottom: 0.75rem;
}

.pill-btn {
  flex: 1;
  padding: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  border: none;
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
}

.pill-btn.active {
  background: var(--primary-color, var(--accent));
  color: white;
}

.modal-form-content {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding-top: 0.5rem;
}

.fade-in-section {
  animation: fadeIn 0.25s ease-in-out;
}

.stok-raf-bilgi {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}
.sepet-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--bg-muted, rgba(0, 0, 0, 0.04));
  border-radius: 10px;
  padding: 8px 10px;
}
.sepet-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}
.sepet-ad {
  flex: 1;
  min-width: 0;
}
.sepet-tutar {
  font-weight: 700;
}
.sepet-sil {
  border: none;
  background: transparent;
  color: var(--danger, #ef4444);
  cursor: pointer;
}
.sepet-toplam {
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px solid var(--border);
  font-size: 12.5px;
  font-weight: 700;
  text-align: right;
}
.foto-sec-etiket {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px dashed var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.foto-sec-etiket:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.gorev-not-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(320px, 100%), 1fr));
  gap: 1rem;
  align-items: start;
}
.gorev-liste .gorev-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--border);
  font-size: 0.85rem;
}
.gorev-liste .gorev-satir:last-child {
  border-bottom: none;
}
.gorev-liste .gorev-satir span {
  flex: 1;
  min-width: 0;
}
.gorev-liste .gorev-satir .tamam {
  text-decoration: line-through;
  color: var(--text-muted);
}
.gorev-liste .gorev-satir small {
  color: var(--text-muted);
  white-space: nowrap;
}
.performans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  gap: 1rem;
}
.kpi-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kpi-deger {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--primary-color, var(--accent));
}
.kpi-etiket {
  font-size: 0.8rem;
  color: var(--text-secondary);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
