<template>
  <div class="irsaliye-sayfasi">
    <PageHeader
      :title="t('irsaliyeler.title')"
      :description="t('irsaliyeler.altBaslik')"
      icon="pi pi-truck"
    >
      <template #actions>
        <Button
          :label="t('irsaliyeler.excel')"
          icon="pi pi-file-excel"
          outlined
          :loading="listeExcelYukleniyor"
          @click="listeExcelIndir"
        />
        <Button
          :label="t('irsaliyeler.yeniIrsaliye')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </template>
    </PageHeader>

    <div class="kpi-satiri">
      <template v-if="ozetIskelet">
        <div
          v-for="i in 4"
          :key="i"
          class="kpi-iskelet"
        >
          <SkeletonLoader height="86px" />
        </div>
      </template>
      <template v-else>
        <KpiKart
          :baslik="t('irsaliyeler.kpiHazirlanan')"
          :deger="ozet.hazirlanan"
          ikon="pi pi-box"
          renk="#f59e0b"
          :para-birimi="false"
          class="kpi-tiklanabilir"
          :class="{ 'kpi-secili': aktifSekme === 1 }"
          role="button"
          tabindex="0"
          :aria-pressed="aktifSekme === 1"
          :title="t('irsaliyeler.kpiFiltreIpucu')"
          @click="sekmeSec(1)"
          @keydown.enter="sekmeSec(1)"
          @keydown.space.prevent="sekmeSec(1)"
        />
        <KpiKart
          :baslik="t('irsaliyeler.kpiSevkEdilen')"
          :deger="ozet.sevkEdilen"
          ikon="pi pi-send"
          renk="#3b82f6"
          :para-birimi="false"
          class="kpi-tiklanabilir"
          :class="{ 'kpi-secili': aktifSekme === 2 }"
          role="button"
          tabindex="0"
          :aria-pressed="aktifSekme === 2"
          :title="t('irsaliyeler.kpiFiltreIpucu')"
          @click="sekmeSec(2)"
          @keydown.enter="sekmeSec(2)"
          @keydown.space.prevent="sekmeSec(2)"
        />
        <KpiKart
          :baslik="t('irsaliyeler.kpiTeslimEdilen')"
          :deger="ozet.teslimEdilenBuAy"
          ikon="pi pi-check-circle"
          renk="#10b981"
          :para-birimi="false"
          class="kpi-tiklanabilir"
          :class="{ 'kpi-secili': aktifSekme === 3 }"
          role="button"
          tabindex="0"
          :aria-pressed="aktifSekme === 3"
          :title="t('irsaliyeler.kpiFiltreIpucu')"
          @click="sekmeSec(3)"
          @keydown.enter="sekmeSec(3)"
          @keydown.space.prevent="sekmeSec(3)"
        />
        <KpiKart
          :baslik="t('irsaliyeler.kpiFaturasiz')"
          :deger="ozet.faturasiz"
          ikon="pi pi-file-excel"
          renk="#ef4444"
          :para-birimi="false"
          class="kpi-tiklanabilir"
          :class="{ 'kpi-secili': faturasiz }"
          role="button"
          tabindex="0"
          :aria-pressed="faturasiz"
          :title="t('irsaliyeler.kpiFiltreIpucu')"
          @click="faturasizSec(true)"
          @keydown.enter="faturasizSec(true)"
          @keydown.space.prevent="faturasizSec(true)"
        />
      </template>
    </div>

    <TabView v-model:active-index="aktifSekme">
      <TabPanel
        v-for="sekme in sekmeler"
        :key="sekme.index"
        :header="sekme.label"
      >
        <div
          v-if="aktifSekme === sekme.index"
          class="sekme-icerik"
        >
          <div class="irsaliye-arac">
            <IconField class="filtre-arama">
              <InputIcon class="pi pi-search" />
              <InputText
                v-model="q"
                :placeholder="t('irsaliyeler.aramaPlaceholder')"
                :aria-label="t('irsaliyeler.aramaPlaceholder')"
                @input="aramaDegisti"
              />
            </IconField>
            <Dropdown
              v-model="cariId"
              :options="cariler"
              option-label="ad"
              option-value="id"
              filter
              show-clear
              class="arac-secim"
              :placeholder="t('irsaliyeler.cariHesap')"
              :aria-label="t('irsaliyeler.cariHesap')"
              @change="filtreDegisti"
            />
            <Dropdown
              v-model="tur"
              :options="turSecenekleri"
              option-label="label"
              option-value="value"
              show-clear
              class="arac-secim"
              :placeholder="t('irsaliyeler.tur')"
              :aria-label="t('irsaliyeler.tur')"
              @change="filtreDegisti"
            />
            <Dropdown
              v-model="durum"
              :options="durumSecenekleri"
              option-label="label"
              option-value="value"
              show-clear
              class="arac-secim"
              :placeholder="t('common.status')"
              :aria-label="t('common.status')"
              @change="durumDegisti"
            />
            <DatePicker
              v-model="baslangic"
              date-format="dd.mm.yy"
              show-clear
              class="arac-tarih"
              :placeholder="t('irsaliyeler.baslangic')"
              :aria-label="t('irsaliyeler.baslangic')"
              @date-select="filtreDegisti"
              @clear-click="filtreDegisti"
            />
            <DatePicker
              v-model="bitis"
              date-format="dd.mm.yy"
              show-clear
              class="arac-tarih"
              :placeholder="t('irsaliyeler.bitis')"
              :aria-label="t('irsaliyeler.bitis')"
              @date-select="filtreDegisti"
              @clear-click="filtreDegisti"
            />
            <button
              type="button"
              class="faturasiz-cip"
              :aria-pressed="faturasiz"
              :title="t('irsaliyeler.faturasizFiltre')"
              @click="faturasizSec(!faturasiz)"
            >
              <i class="pi pi-file-excel" />
              {{ t('irsaliyeler.faturasizFiltre') }}
            </button>
            <span class="arac-bosluk" />
            <Button
              v-if="filtreVar"
              :label="t('irsaliyeler.filtreTemizle')"
              icon="pi pi-filter-slash"
              class="p-button-text p-button-sm"
              @click="filtreTemizle"
            />
            <Button
              icon="pi pi-refresh"
              :aria-label="t('common.refresh')"
              class="p-button-text p-button-sm"
              :loading="yukleniyor"
              @click="yenidenYukle"
            />
          </div>

          <p
            class="sr-only"
            aria-live="polite"
          >
            {{ listeDuyuru }}
          </p>

          <SkeletonLoader
            v-if="listeIskelet"
            class="tablo-iskelet"
            :count="6"
            height="44px"
          />

          <AppDataTable
            v-else
            :value="list"
            lazy
            data-key="id"
            selection-mode="multiple"
            :selection="secili"
            :total-records="toplam"
            :rows="boyut"
            :loading="yukleniyor"
            striped-rows
            class="irsaliye-masa"
            gorunum-anahtari="irsaliye-listesi"
            :empty-message="filtreVar ? t('irsaliyeler.filtreYok') : t('irsaliyeler.empty')"
            @page="sayfaDegisti"
            @sort="siralaDegisti"
            @update:selection="secimDegisti"
          >
            <template #batch-actions>
              <Button
                :label="t('irsaliyeler.topluFatura')"
                icon="pi pi-file"
                size="small"
                :disabled="!topluUygun"
                @click="topluFaturaAc"
              />
              <span
                v-if="farkliCari"
                class="toplu-uyari"
                role="alert"
              >
                <i class="pi pi-exclamation-triangle" />
                {{ t('irsaliyeler.topluFarkliCari') }}
              </span>
            </template>

            <template #empty>
              <EmptyState
                v-if="!yukleniyor"
                :message="filtreVar ? t('irsaliyeler.filtreYok') : t('irsaliyeler.empty')"
                :sub-message="filtreVar ? null : t('irsaliyeler.emptyHint')"
                icon="pi pi-truck"
                :action-label="t('irsaliyeler.yeniIrsaliye')"
                action-icon="pi pi-plus"
                @action="dialogAc()"
              />
            </template>

            <Column
              selection-mode="multiple"
              style="width: 3rem"
            />
            <Column
              field="irsaliyeNo"
              :header="t('irsaliyeler.irsaliyeNo')"
              sortable
            />
            <Column
              field="tarih"
              :header="t('common.date')"
              sortable
              style="width: 110px"
            >
              <template #body="{ data }">
                {{ tarih(data.tarih) }}
              </template>
            </Column>
            <Column
              field="cariHesapAdi"
              :header="t('irsaliyeler.cariHesap')"
            />
            <Column
              field="tur"
              :header="t('irsaliyeler.tur')"
              style="width: 100px"
            >
              <template #body="{ data }">
                <Tag
                  :value="data.tur === 'ALIS' ? t('irsaliyeler.turAlis') : t('irsaliyeler.turSatis')"
                  :severity="data.tur === 'ALIS' ? 'warn' : 'info'"
                />
              </template>
            </Column>
            <Column
              field="durum"
              :header="t('common.status')"
              sortable
              style="width: 130px"
            >
              <template #body="{ data }">
                <Tag
                  :value="durumAdi(data.durum)"
                  :severity="durumSeverity(data.durum)"
                />
              </template>
            </Column>
            <Column
              :header="t('irsaliyeler.plakaSofor')"
              style="width: 160px"
            >
              <template #body="{ data }">
                <div class="plaka-sofor">
                  <strong>{{ data.plaka || '-' }}</strong>
                  <small>{{ data.soforAd || '-' }}</small>
                </div>
              </template>
            </Column>
            <Column
              :header="t('irsaliyeler.fatura')"
              style="width: 130px"
            >
              <template #body="{ data }">
                <router-link
                  v-if="data.faturaId"
                  class="fatura-baglanti"
                  :to="`/faturalar/${data.faturaId}`"
                >
                  {{ data.faturaNumarasi || `#${data.faturaId}` }}
                </router-link>
                <span
                  v-else
                  class="muted"
                >{{ t('irsaliyeler.faturaYok') }}</span>
              </template>
            </Column>
            <Column
              :header="t('irsaliyeler.islem')"
              style="width: 70px"
            >
              <template #body="{ data }">
                <SatirEylemleri
                  :baslik="data.irsaliyeNo"
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="satirEylemleri(data)"
                />
              </template>
            </Column>
          </AppDataTable>

          <!-- Mobil kart görünümü -->
          <div class="irsaliye-mobil">
            <SkeletonLoader
              v-if="yukleniyor && !list.length"
              :count="4"
              height="104px"
            />
            <EmptyState
              v-else-if="!list.length"
              icon="pi pi-truck"
              :message="filtreVar ? t('irsaliyeler.filtreYok') : t('irsaliyeler.empty')"
            />
            <div
              v-for="kayit in list"
              :key="kayit.id"
              class="irs-mobil-kart"
              role="button"
              tabindex="0"
              @click="detayAc(kayit)"
              @keydown.enter="detayAc(kayit)"
            >
              <div class="im-ust">
                <strong>{{ kayit.irsaliyeNo }}</strong>
                <div class="im-rozetler">
                  <Tag
                    :value="durumAdi(kayit.durum)"
                    :severity="durumSeverity(kayit.durum)"
                  />
                  <SatirEylemleri
                    :baslik="kayit.irsaliyeNo"
                    :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                    :items="satirEylemleri(kayit)"
                    @click.stop
                  />
                </div>
              </div>
              <div class="im-satir">
                <span>{{ kayit.cariHesapAdi || '-' }}</span>
                <span>{{ tarih(kayit.tarih) }}</span>
              </div>
              <div class="im-satir">
                <span>
                  <i class="pi pi-car" />
                  {{ kayit.plaka || '-' }} · {{ kayit.soforAd || '-' }}
                </span>
                <router-link
                  v-if="kayit.faturaId"
                  class="fatura-baglanti"
                  :to="`/faturalar/${kayit.faturaId}`"
                  @click.stop
                >
                  {{ kayit.faturaNumarasi || `#${kayit.faturaId}` }}
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <IrsaliyeDetayDrawer
      v-model:visible="detayDrawer"
      :kayit="detayKayit"
      :gecmis="detayKayit?.gecmis || []"
      :gecmis-yukleniyor="detayYukleniyor"
      @duzenle="drawerDuzenle"
      @hazirla="hazirlaAc"
      @sevk="drawerSevk"
      @teslim="drawerTeslim"
      @sevki-geri-al="sevkiGeriAlAc"
      @iptal="iptalAc"
      @faturaya-donustur="faturayaDonusturAc"
    />

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '640px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <h4>{{ t('irsaliyeler.temelBilgiler') }}</h4>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.irsaliyeNoZorunlu') }}</label>
            <InputText
              v-model="form.irsaliyeNo"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('common.date') }}</label>
            <DatePicker
              v-model="form.tarih"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.cariHesapZorunlu') }}</label>
            <Dropdown
              v-model="form.cariHesapId"
              :options="cariler"
              option-label="ad"
              option-value="id"
              filter
              :placeholder="t('irsaliyeler.cariSecin')"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.tur') }}</label>
            <Dropdown
              v-model="form.tur"
              :options="turSecenekleri"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.depo') }}</label>
            <Dropdown
              v-model="form.depoId"
              :options="depolar"
              option-label="ad"
              option-value="id"
              filter
              show-clear
              :placeholder="t('irsaliyeler.depoSecin')"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.siparisOpsiyonel') }}</label>
            <Dropdown
              v-model="form.siparisId"
              :options="siparisler"
              option-label="siparisNo"
              option-value="id"
              filter
              show-clear
              :placeholder="t('irsaliyeler.siparisBagla')"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>

        <h4>{{ t('irsaliyeler.sevkAlanlari') }}</h4>
        <small class="muted">{{ t('irsaliyeler.sevkAlanlariIpucu') }}</small>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.plaka') }}</label>
            <InputText
              v-model="form.plaka"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.sofor') }}</label>
            <Dropdown
              v-model="form.soforId"
              :options="soforler"
              option-label="ad"
              option-value="id"
              filter
              show-clear
              :placeholder="t('irsaliyeler.soforSecin')"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.tasiyiciFirma') }}</label>
            <InputText
              v-model="form.tasiyiciFirma"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.teslimatAdresi') }}</label>
            <InputText
              v-model="form.teslimatAdresi"
              class="w-full"
              :placeholder="t('irsaliyeler.teslimatAdresiIpucu')"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.koliAdedi') }}</label>
            <InputNumber
              v-model="form.koliAdedi"
              :min="0"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.desi') }}</label>
            <InputNumber
              v-model="form.desi"
              :min="0"
              :min-fraction-digits="0"
              :max-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.brutKg') }}</label>
            <InputNumber
              v-model="form.brutKg"
              :min="0"
              :min-fraction-digits="0"
              :max-fraction-digits="2"
              class="w-full"
            />
          </div>
        </div>

        <div class="kalem-baslik">
          <h4>{{ t('irsaliyeler.kalemler') }}</h4>
          <Button
            icon="pi pi-camera"
            :label="t('irsaliyeler.barkod')"
            class="p-button-sm p-button-outlined"
            @click="barkodAcik = true"
          />
        </div>
        <div
          v-for="(k, i) in form.kalemler"
          :key="i"
          class="kalem-satir"
        >
          <Dropdown
            v-model="k.stokId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            filter
            class="w-full"
            :placeholder="t('irsaliyeler.urun')"
          />
          <InputNumber
            v-model="k.miktar"
            :min="0"
            class="kalem-miktar"
            :placeholder="t('irsaliyeler.miktar')"
          />
          <InputText
            v-model="k.birim"
            class="kalem-birim"
            :placeholder="t('irsaliyeler.birim')"
          />
          <Button
            icon="pi pi-trash"
            :aria-label="t('common.delete')"
            class="p-button-text p-button-danger p-button-sm"
            @click="form.kalemler.splice(i, 1)"
          />
        </div>
        <Button
          :label="t('irsaliyeler.kalemEkle')"
          icon="pi pi-plus"
          class="p-button-sm p-button-text"
          @click="kalemEkle"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="sevkDialog"
      :header="t('irsaliyeler.sevkBaslik')"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p
        v-if="sevkHedef"
        class="dialog-ozet"
      >
        {{ sevkHedef.irsaliyeNo }} · {{ sevkHedef.cariHesapAdi }}
      </p>
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.plaka') }}</label>
            <InputText
              v-model="sevkForm.plaka"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.sofor') }} *</label>
            <Dropdown
              v-model="sevkForm.soforId"
              :options="soforler"
              option-label="ad"
              option-value="id"
              filter
              class="w-full"
              :placeholder="t('irsaliyeler.soforSecin')"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('irsaliyeler.tasiyiciFirma') }}</label>
          <InputText
            v-model="sevkForm.tasiyiciFirma"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('irsaliyeler.teslimatAdresi') }}</label>
          <Textarea
            v-model="sevkForm.teslimatAdresi"
            rows="2"
            class="w-full"
            :placeholder="t('irsaliyeler.teslimatAdresiIpucu')"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('irsaliyeler.koliAdedi') }}</label>
            <InputNumber
              v-model="sevkForm.koliAdedi"
              :min="0"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.desi') }}</label>
            <InputNumber
              v-model="sevkForm.desi"
              :min="0"
              :min-fraction-digits="0"
              :max-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('irsaliyeler.brutKg') }}</label>
            <InputNumber
              v-model="sevkForm.brutKg"
              :min="0"
              :min-fraction-digits="0"
              :max-fraction-digits="2"
              class="w-full"
            />
          </div>
        </div>
        <small
          v-if="sevkHata"
          class="alan-hatasi"
          role="alert"
        >{{ sevkHata }}</small>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="sevkDialog = false"
        />
        <Button
          :label="t('irsaliyeler.sevkEt')"
          icon="pi pi-send"
          :loading="islemde"
          @click="sevkKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="teslimDialog"
      :header="t('irsaliyeler.teslimBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p
        v-if="teslimHedef"
        class="dialog-ozet"
      >
        {{ teslimHedef.irsaliyeNo }} · {{ teslimHedef.cariHesapAdi }}
      </p>
      <div class="form-grid">
        <div class="field">
          <label>{{ t('irsaliyeler.teslimAlan') }} *</label>
          <InputText
            v-model="teslimForm.teslimAlan"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('irsaliyeler.teslimTarihi') }}</label>
          <DatePicker
            v-model="teslimForm.teslimTarihi"
            show-time
            hour-format="24"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <small
          v-if="teslimHata"
          class="alan-hatasi"
          role="alert"
        >{{ teslimHata }}</small>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="teslimDialog = false"
        />
        <Button
          :label="t('irsaliyeler.teslimEt')"
          icon="pi pi-check"
          severity="success"
          :loading="islemde"
          @click="teslimKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="iptalDialog"
      :header="t('irsaliyeler.iptalBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p
        v-if="iptalHedef"
        class="dialog-ozet"
      >
        {{ iptalHedef.irsaliyeNo }} · {{ iptalHedef.cariHesapAdi }}
      </p>
      <p class="iptal-uyari">
        <i class="pi pi-exclamation-triangle" /> {{ t('irsaliyeler.iptalUyari') }}
      </p>
      <Textarea
        v-model="iptalAciklama"
        rows="3"
        class="w-full"
        :placeholder="t('irsaliyeler.iptalAciklamaPlaceholder')"
      />
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="iptalDialog = false"
        />
        <Button
          :label="t('irsaliyeler.iptalEt')"
          icon="pi pi-times"
          severity="danger"
          :loading="islemde"
          @click="iptalKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="sonucDialog"
      :header="t('irsaliyeler.topluSonucBaslik')"
      modal
      :style="{ width: '560px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p class="dialog-ozet">
        {{ t('irsaliyeler.topluSonucOzet', { basarili: sonuc.basarili?.length || 0, atlanan: sonuc.atlanan?.length || 0 }) }}
      </p>
      <h4>{{ t('irsaliyeler.topluBasarili') }}</h4>
      <ul
        v-if="sonuc.basarili?.length"
        class="sonuc-liste"
      >
        <li
          v-for="(b, i) in sonuc.basarili"
          :key="`b-${i}`"
        >
          <span>{{ b.irsaliyeNo || `#${b.irsaliyeId}` }}</span>
          <span class="sonuc-etiket">{{ b.faturaNo || `#${b.faturaId}` }}</span>
        </li>
      </ul>
      <p
        v-else
        class="muted"
      >
        {{ t('common.noData') }}
      </p>
      <h4>{{ t('irsaliyeler.topluAtlanan') }}</h4>
      <ul
        v-if="sonuc.atlanan?.length"
        class="sonuc-liste"
      >
        <li
          v-for="(a, i) in sonuc.atlanan"
          :key="`a-${i}`"
        >
          <span>{{ a.irsaliyeNo || `#${a.irsaliyeId}` }}</span>
          <span class="sonuc-etiket">{{ a.sebep || '-' }}</span>
        </li>
      </ul>
      <p
        v-else
        class="muted"
      >
        {{ t('common.noData') }}
      </p>
      <template #footer>
        <Button
          :label="t('common.close')"
          icon="pi pi-check"
          @click="sonucDialog = false"
        />
      </template>
    </Dialog>

    <BarcodeScannerModal
      v-model:visible="barkodAcik"
      @scan="barkodOkundu"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import {
  irsaliyeAPI,
  cariHesapAPI,
  stokAPI,
  siparisAPI,
  depoAPI,
  siparisTakipAPI,
  pdfAPI,
  excelAPI
} from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import IrsaliyeDetayDrawer from '../components/IrsaliyeDetayDrawer.vue'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'
import { formatDate as tarih, getLocalDateString } from '../utils/format.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

// ---- Hafıza ----
const TAB_ANAHTAR = 'raspel_irsaliye_tab'
const FILTRE_ANAHTAR = 'raspel_irsaliye_filtre'
const gecerliSekmeler = [0, 1, 2, 3, 4]

const kayitliFiltre = (() => {
  try {
    return JSON.parse(localStorage.getItem(FILTRE_ANAHTAR) || '{}') || {}
  } catch {
    return {}
  }
})()

const kayitliSekme = (() => {
  const ham = localStorage.getItem(TAB_ANAHTAR)
  if (ham == null) return 1
  const deger = Number(ham)
  return gecerliSekmeler.includes(deger) ? deger : 1
})()

const aktifSekme = ref(kayitliFiltre.faturasiz ? 0 : kayitliSekme)
const durum = ref(sekmeDurum(aktifSekme.value))

function sekmeDurum(index) {
  return [null, 'HAZIRLANDI', 'SEVK_EDILDI', 'TESLIM_EDILDI', null][index] || null
}

// ---- Filtreler ----
const q = ref(kayitliFiltre.q || '')
const cariId = ref(kayitliFiltre.cariId || null)
const tur = ref(kayitliFiltre.tur || null)
const baslangic = ref(tarihNesnesi(kayitliFiltre.baslangic, null))
const bitis = ref(tarihNesnesi(kayitliFiltre.bitis, null))
const faturasiz = ref(!!kayitliFiltre.faturasiz)
if (faturasiz.value) durum.value = null

const sayfa = ref(0)
const boyut = ref(25)
const sirala = ref('tarih,desc')

// ---- Liste / özet ----
const list = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)
const ozet = ref({})
const ozetYuklendi = ref(false)
const listeExcelYukleniyor = ref(false)
let aramaZaman = null

// ---- Referanslar ----
const cariler = ref([])
const stoklar = ref([])
const siparisler = ref([])
const depolar = ref([])
const soforler = ref([])

// ---- Dialoglar ----
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const barkodAcik = ref(false)
const form = ref(bosForm())

const sevkDialog = ref(false)
const sevkHedef = ref(null)
const sevkForm = ref(bosSevkForm())
const sevkHata = ref('')

const teslimDialog = ref(false)
const teslimHedef = ref(null)
const teslimForm = ref({ teslimAlan: '', teslimTarihi: new Date() })
const teslimHata = ref('')

const iptalDialog = ref(false)
const iptalHedef = ref(null)
const iptalAciklama = ref('')

const detayDrawer = ref(false)
const detayKayit = ref(null)
const detayYukleniyor = ref(false)

const secili = ref([])
const sonucDialog = ref(false)
const sonuc = ref({ basarili: [], atlanan: [] })
const islemde = ref(false)

const sekmeler = computed(() => [
  { index: 0, label: t('irsaliyeler.tumu') },
  { index: 1, label: t('irsaliyeler.tabHazirlanan') },
  { index: 2, label: t('irsaliyeler.tabSevkEdilen') },
  { index: 3, label: t('irsaliyeler.tabTeslimEdilen') },
  { index: 4, label: t('irsaliyeler.tabFaturalanan') }
])

const turSecenekleri = computed(() => [
  { value: 'SATIS', label: t('irsaliyeler.turSatis') },
  { value: 'ALIS', label: t('irsaliyeler.turAlis') }
])

const durumSecenekleri = computed(() => [
  { value: 'TASLAK', label: t('irsaliyeler.durumTaslak') },
  { value: 'HAZIRLANDI', label: t('irsaliyeler.durumHazirlandi') },
  { value: 'SEVK_EDILDI', label: t('irsaliyeler.durumSevkEdildi') },
  { value: 'TESLIM_EDILDI', label: t('irsaliyeler.durumTeslimEdildi') },
  { value: 'IPTAL', label: t('irsaliyeler.durumIptal') }
])

const filtreVar = computed(
  () =>
    !!q.value?.trim() ||
    !!cariId.value ||
    !!tur.value ||
    !!durum.value ||
    !!baslangic.value ||
    !!bitis.value ||
    faturasiz.value
)

const ozetIskelet = computed(() => !ozetYuklendi.value)
const listeIskelet = computed(() => yukleniyor.value && !list.value.length)
const listeDuyuru = computed(() =>
  yukleniyor.value
    ? ''
    : t('irsaliyeler.kayitDuyuru', { gorunen: list.value.length, toplam: toplam.value })
)

const farkliCari = computed(() => {
  const kimlikler = new Set(secili.value.map((s) => s.cariHesapId).filter((x) => x != null))
  return kimlikler.size > 1
})
const topluUygun = computed(() => secili.value.length > 0 && !farkliCari.value)

const dialogHeader = computed(() =>
  duzenleme.value ? t('irsaliyeler.irsaliyeDuzenle') : t('irsaliyeler.yeniIrsaliye')
)

// ---- Yardımcılar ----
function tarihNesnesi(value, varsayilan = new Date()) {
  if (!value) return varsayilan
  if (value instanceof Date) return value
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split('-').map(Number)
    return new Date(y, m - 1, d)
  }
  const d = new Date(value)
  return isNaN(d.getTime()) ? varsayilan : d
}

function yerelDateTimeString(value) {
  const d = value ? new Date(value) : new Date()
  if (isNaN(d.getTime())) return null
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function bosForm() {
  return {
    id: null,
    irsaliyeNo: '',
    tarih: new Date(),
    cariHesapId: null,
    tur: 'SATIS',
    depoId: null,
    aciklama: '',
    siparisId: null,
    plaka: '',
    soforId: null,
    tasiyiciFirma: '',
    teslimatAdresi: '',
    koliAdedi: null,
    desi: null,
    brutKg: null,
    kalemler: []
  }
}

function bosSevkForm() {
  return {
    plaka: '',
    soforId: null,
    tasiyiciFirma: '',
    teslimatAdresi: '',
    koliAdedi: null,
    desi: null,
    brutKg: null
  }
}

const kalemEkle = () => {
  form.value.kalemler.push({ stokId: null, miktar: 1, aciklama: '', birim: 'Adet' })
}

const durumAdi = (d) =>
  ({
    TASLAK: t('irsaliyeler.durumTaslak'),
    HAZIRLANDI: t('irsaliyeler.durumHazirlandi'),
    SEVK_EDILDI: t('irsaliyeler.durumSevkEdildi'),
    KESILDI: t('irsaliyeler.durumSevkEdildi'),
    TESLIM_EDILDI: t('irsaliyeler.durumTeslimEdildi'),
    IPTAL: t('irsaliyeler.durumIptal')
  })[d] || d || '-'

const durumSeverity = (d) =>
  ({
    TASLAK: 'secondary',
    HAZIRLANDI: 'warn',
    SEVK_EDILDI: 'info',
    KESILDI: 'info',
    TESLIM_EDILDI: 'success',
    IPTAL: 'danger'
  })[d] || 'secondary'

const cariAdresi = (id) => cariler.value.find((c) => c.id === id)?.adres || ''

// ---- Veri yükleme ----
const filtreParams = () => {
  const params = {}
  if (q.value?.trim()) params.q = q.value.trim()
  if (durum.value) params.durum = durum.value
  if (tur.value) params.tur = tur.value
  if (cariId.value) params.cariId = cariId.value
  if (baslangic.value) params.baslangic = getLocalDateString(baslangic.value)
  if (bitis.value) params.bitis = getLocalDateString(bitis.value)
  if (faturasiz.value) params.faturali = false
  else if (aktifSekme.value === 4) params.faturali = true
  return params
}

const listeYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await irsaliyeAPI.filtreli({
      ...filtreParams(),
      page: sayfa.value,
      size: boyut.value,
      sort: sirala.value
    })
    list.value = r.data?.content || []
    toplam.value = r.data?.totalElements ?? list.value.length
  } catch (err) {
    list.value = []
    toplam.value = 0
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const ozetYukle = async () => {
  try {
    const r = await irsaliyeAPI.ozet()
    ozet.value = r.data || {}
  } catch {
    ozet.value = {}
  } finally {
    ozetYuklendi.value = true
  }
}

const yenidenYukle = () => {
  listeYukle()
  ozetYukle()
  if (detayDrawer.value && detayKayit.value) detayYenile()
}

const referanslariYukle = async () => {
  const [c, s, sp, d, sf] = await Promise.allSettled([
    cariHesapAPI.getAll(),
    stokAPI.getAll({ size: 500 }),
    siparisAPI.getAll({ size: 500 }),
    depoAPI.getAll(),
    siparisTakipAPI.soforler()
  ])
  const al = (r) => (r.status === 'fulfilled' ? unwrapList(r.value) : [])
  cariler.value = al(c)
  stoklar.value = al(s)
  siparisler.value = al(sp)
  depolar.value = al(d)
  soforler.value = al(sf).map((x) =>
    typeof x === 'string' ? { id: x, ad: x } : { ...x, ad: x.ad || x.soforAd || x.adSoyad || `#${x.id}` }
  )
}

// ---- Sekme / filtre ----
const sekmeSec = (index) => {
  if (!gecerliSekmeler.includes(index) || aktifSekme.value === index) return
  aktifSekme.value = index
}

const faturasizSec = (deger) => {
  faturasiz.value = !!deger
  durum.value = null
  if (aktifSekme.value !== 0) {
    aktifSekme.value = 0
  } else {
    sayfa.value = 0
    listeYukle()
  }
}

const durumDegisti = () => {
  faturasiz.value = false
  const eslesen = { HAZIRLANDI: 1, SEVK_EDILDI: 2, TESLIM_EDILDI: 3 }[durum.value] ?? 0
  if (aktifSekme.value !== eslesen) {
    aktifSekme.value = eslesen
  } else {
    sayfa.value = 0
    listeYukle()
  }
}

const filtreDegisti = () => {
  sayfa.value = 0
  listeYukle()
}

const aramaDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(filtreDegisti, 300)
}

const filtreTemizle = () => {
  q.value = ''
  cariId.value = null
  tur.value = null
  durum.value = null
  baslangic.value = null
  bitis.value = null
  faturasiz.value = false
  sayfa.value = 0
  if (aktifSekme.value !== 0) aktifSekme.value = 0
  else listeYukle()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  boyut.value = e.rows
  listeYukle()
}

const siralaDegisti = (e) => {
  if (!e.sortField || !e.sortOrder) {
    sirala.value = 'tarih,desc'
  } else {
    sirala.value = `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
  }
  sayfa.value = 0
  listeYukle()
}

const secimDegisti = (val) => {
  secili.value = Array.isArray(val) ? val : []
}

// ---- Excel / PDF ----
const indir = (data, dosyaAdi) => {
  const blob = new Blob([data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = dosyaAdi
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 60000)
}

const listeExcelIndir = async () => {
  listeExcelYukleniyor.value = true
  try {
    const res = await excelAPI.irsaliyeler(filtreParams())
    indir(res.data, 'irsaliyeler.xlsx')
    toastBildirim.basarili(t('irsaliyeler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('irsaliyeler.excelHata'))
  } finally {
    listeExcelYukleniyor.value = false
  }
}

const pdfIndir = async (kayit) => {
  try {
    const res = await pdfAPI.irsaliye(kayit.id)
    indir(res.data, `irsaliye_${kayit.irsaliyeNo || kayit.id}.pdf`)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.pdfIndirilemedi'))
  }
}

// ---- Detay drawer ----
const detayAc = async (kayit) => {
  detayKayit.value = kayit
  detayDrawer.value = true
  detayYenile()
}

const detayYenile = async () => {
  if (!detayKayit.value) return
  detayYukleniyor.value = true
  try {
    const r = await irsaliyeAPI.getById(detayKayit.value.id)
    if (r?.data) detayKayit.value = r.data
  } catch {
    /* liste verisiyle devam et */
  } finally {
    detayYukleniyor.value = false
  }
}

const drawerDuzenle = (kayit) => {
  detayDrawer.value = false
  dialogAc(kayit)
}

const drawerSevk = (kayit) => sevkAc(kayit)
const drawerTeslim = (kayit) => teslimAc(kayit)

// ---- Yeni / düzenle ----
const dialogAc = async (kayit) => {
  duzenleme.value = !!kayit
  if (!kayit) {
    form.value = bosForm()
    form.value.irsaliyeNo = 'IRS-' + Date.now()
    dialog.value = true
    return
  }
  let detay = kayit
  try {
    const r = await irsaliyeAPI.getById(kayit.id)
    if (r?.data) detay = r.data
  } catch {
    /* liste verisiyle devam et */
  }
  form.value = {
    ...bosForm(),
    ...detay,
    tarih: tarihNesnesi(detay.tarih),
    kalemler: (detay.kalemler || []).map((k) => ({
      stokId: k.stokId,
      miktar: Number(k.miktar) || 0,
      aciklama: k.aciklama || '',
      birim: k.birim || 'Adet'
    }))
  }
  dialog.value = true
}

const formPayload = () => ({
  irsaliyeNo: form.value.irsaliyeNo,
  tarih: getLocalDateString(form.value.tarih),
  cariHesapId: form.value.cariHesapId,
  tur: form.value.tur,
  depoId: form.value.depoId,
  aciklama: form.value.aciklama,
  siparisId: form.value.siparisId,
  plaka: form.value.plaka,
  soforId: form.value.soforId,
  tasiyiciFirma: form.value.tasiyiciFirma,
  teslimatAdresi: form.value.teslimatAdresi,
  koliAdedi: form.value.koliAdedi,
  desi: form.value.desi,
  brutKg: form.value.brutKg,
  kalemler: form.value.kalemler.filter((k) => k.stokId && Number(k.miktar) > 0)
})

const kaydet = async () => {
  if (kaydediliyor.value) return
  if (!form.value.irsaliyeNo?.trim()) {
    toastBildirim.uyari(t('irsaliyeler.irsaliyeNoGerekli'))
    return
  }
  if (!form.value.cariHesapId) {
    toastBildirim.uyari(t('irsaliyeler.cariSecin'))
    return
  }
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await irsaliyeAPI.update(form.value.id, formPayload())
      toastBildirim.basarili(t('irsaliyeler.guncellendi'))
    } else {
      await irsaliyeAPI.create(formPayload())
      toastBildirim.basarili(t('irsaliyeler.olusturuldu'))
    }
    dialog.value = false
    yenidenYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.hataKaydet'))
  } finally {
    kaydediliyor.value = false
  }
}

const barkodOkundu = (kod) => {
  const stok = stoklar.value.find((s) => s.barkod === kod || s.stokKodu === kod)
  if (!stok) {
    toastBildirim.uyari(t('irsaliyeler.barkodBulunamadi') + ' ' + kod)
    return
  }
  form.value.kalemler.push({
    stokId: stok.id,
    miktar: 1,
    aciklama: stok.ad,
    birim: stok.birim || 'Adet'
  })
  toastBildirim.basarili(t('irsaliyeler.eklendi', { ad: stok.ad }))
}

// ---- Geçiş aksiyonları ----
const hataMesaji = (err, varsayilan) =>
  err?.response?.data?.message || err?.message || varsayilan

const hazirlaAc = (kayit) => {
  confirm.require({
    message: t('irsaliyeler.hazirlaOnay', { no: kayit.irsaliyeNo }),
    header: t('irsaliyeler.hazirla'),
    icon: 'pi pi-box',
    acceptLabel: t('irsaliyeler.hazirla'),
    rejectLabel: t('common.vazgec'),
    accept: () => hazirlaKaydet(kayit)
  })
}

const hazirlaKaydet = async (kayit) => {
  islemde.value = true
  try {
    await irsaliyeAPI.hazirla(kayit.id)
    toastBildirim.basarili(t('irsaliyeler.hazirlandi'))
    yenidenYukle()
  } catch (err) {
    toastBildirim.hata(hataMesaji(err, t('irsaliyeler.hataHazirla')))
  } finally {
    islemde.value = false
  }
}

const sevkAc = (kayit) => {
  sevkHedef.value = kayit
  sevkHata.value = ''
  sevkForm.value = {
    plaka: kayit.plaka || '',
    soforId: kayit.soforId || null,
    tasiyiciFirma: kayit.tasiyiciFirma || '',
    teslimatAdresi: kayit.teslimatAdresi || cariAdresi(kayit.cariHesapId),
    koliAdedi: kayit.koliAdedi ?? null,
    desi: kayit.desi ?? null,
    brutKg: kayit.brutKg ?? null
  }
  sevkDialog.value = true
}

const sevkKaydet = async () => {
  if (!sevkHedef.value) return
  if (!sevkForm.value.soforId) {
    sevkHata.value = t('irsaliyeler.soforZorunlu')
    return
  }
  sevkHata.value = ''
  islemde.value = true
  try {
    await irsaliyeAPI.sevk(sevkHedef.value.id, {
      plaka: sevkForm.value.plaka || null,
      soforId: sevkForm.value.soforId,
      teslimatAdresi: sevkForm.value.teslimatAdresi || null,
      tasiyiciFirma: sevkForm.value.tasiyiciFirma || null,
      koliAdedi: sevkForm.value.koliAdedi,
      desi: sevkForm.value.desi,
      brutKg: sevkForm.value.brutKg
    })
    toastBildirim.basarili(t('irsaliyeler.sevkEdildi'))
    sevkDialog.value = false
    yenidenYukle()
  } catch (err) {
    const mesaj = hataMesaji(err, t('irsaliyeler.hataSevk'))
    sevkHata.value = mesaj
    toastBildirim.hata(mesaj)
  } finally {
    islemde.value = false
  }
}

const teslimAc = (kayit) => {
  teslimHedef.value = kayit
  teslimHata.value = ''
  teslimForm.value = { teslimAlan: kayit.teslimAlan || '', teslimTarihi: new Date() }
  teslimDialog.value = true
}

const teslimKaydet = async () => {
  if (!teslimHedef.value) return
  if (!teslimForm.value.teslimAlan?.trim()) {
    teslimHata.value = t('irsaliyeler.teslimAlanZorunlu')
    return
  }
  teslimHata.value = ''
  islemde.value = true
  try {
    await irsaliyeAPI.teslim(teslimHedef.value.id, {
      teslimAlan: teslimForm.value.teslimAlan.trim(),
      teslimTarihi: yerelDateTimeString(teslimForm.value.teslimTarihi)
    })
    toastBildirim.basarili(t('irsaliyeler.teslimEdildi'))
    teslimDialog.value = false
    yenidenYukle()
  } catch (err) {
    const mesaj = hataMesaji(err, t('irsaliyeler.hataTeslim'))
    teslimHata.value = mesaj
    toastBildirim.hata(mesaj)
  } finally {
    islemde.value = false
  }
}

const sevkiGeriAlAc = (kayit) => {
  confirm.require({
    message: t('irsaliyeler.sevkiGeriAlOnay', { no: kayit.irsaliyeNo }),
    header: t('irsaliyeler.sevkiGeriAl'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('irsaliyeler.sevkiGeriAl'),
    rejectLabel: t('common.vazgec'),
    accept: () => sevkiGeriAlKaydet(kayit)
  })
}

const sevkiGeriAlKaydet = async (kayit) => {
  islemde.value = true
  try {
    await irsaliyeAPI.sevkiGeriAl(kayit.id)
    toastBildirim.basarili(t('irsaliyeler.sevkiGeriAlindi'))
    yenidenYukle()
  } catch (err) {
    toastBildirim.hata(hataMesaji(err, t('irsaliyeler.hataSevkiGeriAl')))
  } finally {
    islemde.value = false
  }
}

const iptalAc = (kayit) => {
  iptalHedef.value = kayit
  iptalAciklama.value = ''
  iptalDialog.value = true
}

const iptalKaydet = async () => {
  if (!iptalHedef.value) return
  islemde.value = true
  try {
    await irsaliyeAPI.iptal(iptalHedef.value.id, iptalAciklama.value || null)
    toastBildirim.basarili(t('irsaliyeler.iptalEdildi'))
    iptalDialog.value = false
    yenidenYukle()
  } catch (err) {
    toastBildirim.hata(hataMesaji(err, t('irsaliyeler.hataIptal')))
  } finally {
    islemde.value = false
  }
}

const faturayaDonusturAc = (kayit) => {
  confirm.require({
    message: t('irsaliyeler.faturayaDonusturOnay', { no: kayit.irsaliyeNo }),
    header: t('irsaliyeler.faturayaDonustur'),
    icon: 'pi pi-file',
    acceptLabel: t('common.onayla'),
    rejectLabel: t('common.vazgec'),
    accept: () => faturayaDonusturKaydet(kayit)
  })
}

const faturayaDonusturKaydet = async (kayit) => {
  islemde.value = true
  try {
    await irsaliyeAPI.faturayaDonustur(kayit.id)
    toastBildirim.basarili(t('irsaliyeler.faturayaDonusturuldu'))
    yenidenYukle()
  } catch (err) {
    toastBildirim.hata(hataMesaji(err, t('irsaliyeler.hataDurum')))
  } finally {
    islemde.value = false
  }
}

const sil = (kayit) => {
  confirm.require({
    message: t('irsaliyeler.silOnay', { no: kayit.irsaliyeNo }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await irsaliyeAPI.delete(kayit.id)
        toastBildirim.basarili(t('irsaliyeler.silindi'))
        secili.value = secili.value.filter((s) => s.id !== kayit.id)
        yenidenYukle()
      } catch (err) {
        toastBildirim.hata(hataMesaji(err, t('irsaliyeler.hataSil')))
      }
    }
  })
}

// ---- Toplu fatura ----
const topluFaturaAc = () => {
  if (!topluUygun.value) return
  confirm.require({
    message: t('irsaliyeler.topluFaturaOnay', { n: secili.value.length }),
    header: t('irsaliyeler.topluFatura'),
    icon: 'pi pi-file',
    acceptLabel: t('common.onayla'),
    rejectLabel: t('common.vazgec'),
    accept: topluFaturaKaydet
  })
}

const topluFaturaKaydet = async () => {
  if (!topluUygun.value) return
  islemde.value = true
  try {
    const r = await irsaliyeAPI.topluFatura(secili.value.map((s) => s.id))
    sonuc.value = r?.data || { basarili: [], atlanan: [] }
    sonucDialog.value = true
    secili.value = []
    yenidenYukle()
  } catch (err) {
    toastBildirim.hata(hataMesaji(err, t('irsaliyeler.topluFaturaBasarisiz')))
  } finally {
    islemde.value = false
  }
}

// ---- Satır eylemleri ----
const satirEylemleri = (kayit) => {
  const items = [
    { etiket: t('irsaliyeler.detay'), ikon: 'pi pi-search', islem: () => detayAc(kayit) }
  ]
  if (['TASLAK', 'HAZIRLANDI'].includes(kayit.durum)) {
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(kayit) })
  }
  if (kayit.durum === 'TASLAK') {
    items.push({ etiket: t('irsaliyeler.hazirla'), ikon: 'pi pi-box', islem: () => hazirlaAc(kayit) })
  }
  if (kayit.durum === 'HAZIRLANDI') {
    items.push({ etiket: t('irsaliyeler.sevkEt'), ikon: 'pi pi-send', islem: () => sevkAc(kayit) })
  }
  if (kayit.durum === 'SEVK_EDILDI') {
    items.push({
      etiket: t('irsaliyeler.teslimEt'),
      ikon: 'pi pi-check',
      sinif: 'eylem-pozitif',
      islem: () => teslimAc(kayit)
    })
    items.push({
      etiket: t('irsaliyeler.sevkiGeriAl'),
      ikon: 'pi pi-undo',
      sinif: 'eylem-uyari',
      islem: () => sevkiGeriAlAc(kayit)
    })
  }
  if (kayit.durum !== 'IPTAL' && !kayit.faturaId) {
    items.push({
      etiket: t('irsaliyeler.faturayaDonustur'),
      ikon: 'pi pi-file',
      sinif: 'eylem-uyari',
      islem: () => faturayaDonusturAc(kayit)
    })
  }
  items.push({ ayrac: true })
  items.push({ etiket: t('irsaliyeler.pdf'), ikon: 'pi pi-file-pdf', islem: () => pdfIndir(kayit) })
  if (kayit.durum !== 'IPTAL') {
    items.push({
      etiket: t('irsaliyeler.iptalEt'),
      ikon: 'pi pi-times',
      islem: () => iptalAc(kayit)
    })
  }
  items.push({
    etiket: t('common.delete'),
    ikon: 'pi pi-trash',
    sinif: 'eylem-sil',
    islem: () => sil(kayit)
  })
  return items
}

// ---- İzleme / başlangıç ----
watch([q, cariId, tur, baslangic, bitis, faturasiz], () => {
  try {
    localStorage.setItem(
      FILTRE_ANAHTAR,
      JSON.stringify({
        q: q.value || '',
        cariId: cariId.value || null,
        tur: tur.value || null,
        baslangic: baslangic.value ? getLocalDateString(baslangic.value) : null,
        bitis: bitis.value ? getLocalDateString(bitis.value) : null,
        faturasiz: !!faturasiz.value
      })
    )
  } catch {
    /* depolama kapalı olabilir */
  }
})

watch(aktifSekme, (index) => {
  try {
    localStorage.setItem(TAB_ANAHTAR, String(index))
  } catch {
    /* depolama kapalı olabilir */
  }
  // Faturasız filtresi yalnızca "Tümü" sekmesinde geçerlidir; başka bir
  // sekmeye geçildiğinde durum filtresi sekmeyi izler.
  if (index !== 0) faturasiz.value = false
  durum.value = index === 0 ? (faturasiz.value ? null : durum.value) : sekmeDurum(index)
  sayfa.value = 0
  listeYukle()
})

onMounted(async () => {
  await referanslariYukle()
  ozetYukle()
  listeYukle()
})
</script>

<style scoped>
.irsaliye-sayfasi {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
.kpi-iskelet {
  min-width: 0;
}
.kpi-tiklanabilir {
  cursor: pointer;
}
.kpi-tiklanabilir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kpi-secili {
  border-color: var(--accent-border, var(--accent));
  box-shadow: 0 0 0 1px var(--accent-border, var(--accent)) inset;
}
.tablo-iskelet {
  padding: 4px 0;
}
.irsaliye-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.irsaliye-arac .arac-secim {
  min-width: 130px;
}
.irsaliye-arac .arac-tarih {
  min-width: 140px;
  max-width: 160px;
}
.filtre-arama {
  min-width: 220px;
  max-width: 320px;
  flex: 1 1 220px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.faturasiz-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.faturasiz-cip[aria-pressed='true'] {
  background: var(--accent-soft);
  border-color: var(--accent-border, var(--accent));
  color: var(--accent);
}
.faturasiz-cip:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.toplu-uyari {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: var(--danger, #ef4444);
  max-width: 320px;
}
.plaka-sofor {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.plaka-sofor strong {
  font-size: 12.5px;
}
.plaka-sofor small {
  font-size: 11.5px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fatura-baglanti {
  color: var(--accent);
  font-weight: 600;
  text-decoration: none;
}
.fatura-baglanti:hover {
  text-decoration: underline;
}
.muted {
  color: var(--text-muted);
  font-size: 12px;
}
.irsaliye-mobil {
  display: none;
}
.irs-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  margin-bottom: 8px;
  cursor: pointer;
}
.irs-mobil-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.im-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.im-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.im-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.im-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.field-row .field {
  flex: 1 1 180px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
h4 {
  margin: 0;
  font-size: 15px;
}
.kalem-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
}
.kalem-miktar {
  width: 100px;
  flex-shrink: 0;
}
.kalem-birim {
  width: 100px;
  flex-shrink: 0;
}
.dialog-ozet {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
}
.iptal-uyari {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  margin: 0 0 12px;
  border-radius: 10px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger, #ef4444);
  font-size: 12.5px;
}
.alan-hatasi {
  color: var(--danger, #ef4444);
  font-size: 12px;
  font-weight: 600;
}
.sonuc-liste {
  list-style: none;
  margin: 0 0 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sonuc-liste li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  font-size: 12.5px;
}
.sonuc-etiket {
  color: var(--text-muted);
  font-size: 11.5px;
}
@media (max-width: 768px) {
  .irsaliye-masa {
    display: none;
  }
  .irsaliye-mobil {
    display: block;
  }
  .irsaliye-arac .arac-secim,
  .irsaliye-arac .arac-tarih {
    flex: 1 1 130px;
    max-width: none;
  }
  .filtre-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .faturasiz-cip {
    flex: 1 1 100%;
    justify-content: center;
  }
  .kalem-satir {
    flex-wrap: wrap;
  }
  .kalem-miktar,
  .kalem-birim {
    flex: 1 1 90px;
    width: auto;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kpi-tiklanabilir,
  .irs-mobil-kart {
    transition: none;
  }
}
</style>
