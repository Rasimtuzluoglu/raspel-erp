<template>
  <div class="uretim-sayfasi">
    <PageHeader
      :title="t('uretim.title')"
      :description="t('uretim.planlamaAciklama')"
      icon="pi pi-cog"
    >
      <template #actions>
        <Button
          :label="t('uretim.yenile')"
          icon="pi pi-refresh"
          outlined
          class="p-button-sm"
          :loading="yukleniyor"
          @click="yukle"
        />
        <Button
          :label="t('uretim.yeniEmir')"
          icon="pi pi-plus"
          class="p-button-sm"
          @click="emirDialogAc()"
        />
      </template>
    </PageHeader>

    <!-- KPI -->
    <div
      v-if="kpiIskelet"
      class="kpi-satiri"
      :aria-busy="true"
    >
      <div
        v-for="i in 6"
        :key="i"
        class="kpi-iskelet"
      >
        <SkeletonLoader height="86px" />
      </div>
    </div>
    <div
      v-else
      class="kpi-satiri"
    >
      <KpiKart
        :baslik="t('uretim.kpiTaslak')"
        :deger="ozet.taslak"
        ikon="pi pi-file-edit"
        renk="#64748b"
        :para-birimi="false"
        class="kpi-tiklanabilir"
        :class="{ 'kpi-aktif': aktifCip === 'TASLAK' }"
        role="button"
        tabindex="0"
        @click="kpiEmirFiltre('TASLAK')"
        @keydown.enter.prevent="kpiEmirFiltre('TASLAK')"
        @keydown.space.prevent="kpiEmirFiltre('TASLAK')"
      />
      <KpiKart
        :baslik="t('uretim.kpiUretimde')"
        :deger="ozet.uretimde"
        ikon="pi pi-cog"
        renk="#3b82f6"
        :para-birimi="false"
        class="kpi-tiklanabilir"
        :class="{ 'kpi-aktif': aktifCip === 'URETIMDE' }"
        role="button"
        tabindex="0"
        @click="kpiEmirFiltre('URETIMDE')"
        @keydown.enter.prevent="kpiEmirFiltre('URETIMDE')"
        @keydown.space.prevent="kpiEmirFiltre('URETIMDE')"
      />
      <KpiKart
        :baslik="t('uretim.kpiBuAy')"
        :deger="ozet.buAyTamamlanan"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
        class="kpi-tiklanabilir"
        :class="{ 'kpi-aktif': aktifCip === 'BUAY' }"
        role="button"
        tabindex="0"
        @click="kpiEmirFiltre('BUAY')"
        @keydown.enter.prevent="kpiEmirFiltre('BUAY')"
        @keydown.space.prevent="kpiEmirFiltre('BUAY')"
      />
      <KpiKart
        :baslik="t('uretim.kpiGeciken')"
        :deger="ozet.geciken"
        ikon="pi pi-clock"
        renk="#ef4444"
        :para-birimi="false"
        class="kpi-tiklanabilir"
        :class="{ 'kpi-aktif': aktifCip === 'GECIKEN' }"
        role="button"
        tabindex="0"
        @click="kpiEmirFiltre('GECIKEN')"
        @keydown.enter.prevent="kpiEmirFiltre('GECIKEN')"
        @keydown.space.prevent="kpiEmirFiltre('GECIKEN')"
      />
      <KpiKart
        :baslik="t('uretim.kpiFireOrani')"
        :deger="Number(ozet.fireOrani) || 0"
        ikon="pi pi-exclamation-triangle"
        renk="#f59e0b"
        :para-birimi="false"
      >
        <template #deger>
          {{ Number(ozet.fireOrani) || 0 }}%
        </template>
      </KpiKart>
      <KpiKart
        :baslik="t('uretim.kpiOrtSure')"
        :deger="Number(ozet.ortalamaSureSaat) || 0"
        ikon="pi pi-hourglass"
        renk="#8b5cf6"
        :para-birimi="false"
      >
        <template #deger>
          {{ Number(ozet.ortalamaSureSaat) || 0 }} sa
        </template>
      </KpiKart>
    </div>

    <TabView v-model:active-index="aktifSekme">
      <!-- EMİRLER -->
      <TabPanel :header="t('uretim.emirlerTab')">
        <div class="emir-arac">
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="emirQ"
              :aria-label="t('uretim.aramaPlaceholder')"
              :placeholder="t('uretim.aramaPlaceholder')"
              @input="emirFiltreDegisti"
            />
          </IconField>
          <Dropdown
            v-model="emirDurum"
            :options="durumSecenekleri"
            option-label="etiket"
            option-value="deger"
            show-clear
            class="filtre-select"
            :aria-label="t('common.status')"
            :placeholder="t('common.status')"
            @change="emirFiltreDegisti"
          />
          <Dropdown
            v-model="emirOncelik"
            :options="oncelikSecenekleri"
            option-label="etiket"
            option-value="deger"
            show-clear
            class="filtre-select"
            :aria-label="t('uretim.oncelik')"
            :placeholder="t('uretim.oncelik')"
            @change="emirFiltreDegisti"
          />
          <Dropdown
            v-model="emirHat"
            :options="hatlar"
            option-label="ad"
            option-value="id"
            show-clear
            class="filtre-select"
            :aria-label="t('uretim.hat')"
            :placeholder="t('uretim.hat')"
            @change="emirFiltreDegisti"
          />
          <DatePicker
            v-model="emirBaslangic"
            date-format="dd.mm.yy"
            show-icon
            :manual-input="false"
            class="filtre-tarih"
            :aria-label="t('uretim.baslangicTarihi')"
            :placeholder="t('uretim.baslangicTarihi')"
            @date-select="emirFiltreDegisti"
          />
          <DatePicker
            v-model="emirBitis"
            date-format="dd.mm.yy"
            show-icon
            :manual-input="false"
            class="filtre-tarih"
            :aria-label="t('uretim.bitisTarihi')"
            :placeholder="t('uretim.bitisTarihi')"
            @date-select="emirFiltreDegisti"
          />
          <Button
            v-if="emirFiltreVar"
            :label="t('denetim.temizle')"
            icon="pi pi-filter-slash"
            class="p-button-text p-button-sm"
            @click="emirFiltreTemizle"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('uretim.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="emirExcelYukleniyor"
            @click="emirExcelIndir"
          />
        </div>

        <div
          class="emir-cipler"
          role="group"
          :aria-label="t('uretim.hizliFiltre')"
        >
          <button
            v-for="c in emirCipleri"
            :key="c.deger"
            type="button"
            class="emir-cip"
            :class="{ aktif: aktifCip === c.deger }"
            :aria-pressed="aktifCip === c.deger"
            @click="emirCipUygula(c.deger)"
          >
            <i :class="c.ikon" /> {{ c.ad }}
          </button>
        </div>

        <p
          class="sr-only"
          aria-live="polite"
        >
          {{ emirDuyuru }}
        </p>

        <SkeletonLoader
          v-if="emirYukleniyor && !emirSayfa.length"
          class="tablo-iskelet"
          :count="6"
          height="44px"
        />

        <AppDataTable
          v-else
          :value="emirSayfa"
          lazy
          :total-records="emirToplam"
          :rows="emirSayfaBoyutu"
          class="emir-masa"
          striped-rows
          :loading="emirYukleniyor"
          :empty-message="t('uretim.emirYok')"
          :current-page-report-template="'{first} - {last} ({totalRecords} ' + $t('common.recordsWord') + ')'"
          @page="emirSayfaDegisti"
          @sort="emirSiralaDegisti"
        >
          <Column
            field="id"
            header="#"
            style="width: 70px"
            sortable
          />
          <Column
            field="urunAd"
            :header="t('uretim.mamul')"
            sortable
          />
          <Column
            field="hatAd"
            :header="t('uretim.hat')"
            style="width: 130px"
          >
            <template #body="{ data }">
              <Tag
                v-if="data.hatAd"
                :value="data.hatAd"
                severity="secondary"
              />
              <span
                v-else
                class="muted"
              >-</span>
            </template>
          </Column>
          <Column
            field="miktar"
            :header="t('uretim.miktar')"
            style="width: 110px"
            sortable
          />
          <Column
            field="durum"
            :header="t('common.status')"
            style="width: 130px"
            sortable
          >
            <template #body="{ data }">
              <Tag
                :value="durumAdi(data.durum)"
                :severity="durumSeverity(data.durum)"
              />
            </template>
          </Column>
          <Column
            field="oncelik"
            :header="t('uretim.oncelik')"
            style="width: 110px"
          >
            <template #body="{ data }">
              <Tag
                :value="oncelikAdi(data.oncelik)"
                :severity="oncelikSeverity(data.oncelik)"
              />
            </template>
          </Column>
          <Column
            :header="t('uretim.planlanan')"
            style="width: 190px"
          >
            <template #body="{ data }">
              <span
                class="planlanan-hucre"
                :class="{ gecikti: data.gecikti }"
              >
                <i
                  v-if="data.gecikti"
                  class="pi pi-exclamation-triangle"
                />
                {{ tarih(data.planlananBaslangic) }}
                <i class="pi pi-arrow-right" />
                {{ tarih(data.planlananBitis) }}
              </span>
            </template>
          </Column>
          <Column
            field="toplamMaliyet"
            :header="t('uretim.maliyet')"
            style="width: 130px"
            sortable
          >
            <template #body="{ data }">
              <span v-if="Number(data.toplamMaliyet) > 0">{{ para(data.toplamMaliyet) }}</span>
              <span
                v-else
                class="muted"
              >-</span>
            </template>
          </Column>
          <Column
            :header="t('uretim.islem')"
            style="width: 80px"
          >
            <template #body="{ data }">
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="emirAksiyonlari(data)"
              />
            </template>
          </Column>
          <template #empty>
            <EmptyState
              v-if="!emirYukleniyor"
              :message="emirFiltreVar ? t('uretim.emirFiltreYok') : t('uretim.emirYok')"
              icon="pi pi-cog"
              :action-label="t('uretim.yeniEmir')"
              action-icon="pi pi-plus"
              @action="emirDialogAc()"
            />
          </template>
        </AppDataTable>

        <!-- Mobil kart görünümü -->
        <div class="emir-mobil">
          <div
            v-if="emirYukleniyor"
            class="mobil-bos"
          >
            <SkeletonLoader
              :count="3"
              height="96px"
            />
          </div>
          <EmptyState
            v-else-if="!emirSayfa.length"
            :message="emirFiltreVar ? t('uretim.emirFiltreYok') : t('uretim.emirYok')"
            icon="pi pi-cog"
          />
          <div
            v-for="e in emirSayfa"
            :key="e.id"
            class="emir-mobil-kart"
            :class="{ tiklanabilir: true }"
            @click="detayAc(e)"
          >
            <div class="emk-ust">
              <strong>#{{ e.id }} · {{ e.urunAd }}</strong>
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="emirAksiyonlari(e)"
                @click.stop
              />
            </div>
            <div class="emk-taglar">
              <Tag
                :value="durumAdi(e.durum)"
                :severity="durumSeverity(e.durum)"
              />
              <Tag
                :value="oncelikAdi(e.oncelik)"
                :severity="oncelikSeverity(e.oncelik)"
              />
            </div>
            <div class="emk-satir">
              <span>{{ t('uretim.hat') }}: <strong>{{ e.hatAd || '-' }}</strong></span>
              <span>{{ t('uretim.miktar') }}: <strong>{{ e.miktar }}</strong></span>
              <span>{{ t('uretim.maliyet') }}: <strong>{{ Number(e.toplamMaliyet) > 0 ? para(e.toplamMaliyet) : '-' }}</strong></span>
            </div>
            <div
              class="emk-satir planlanan-hucre"
              :class="{ gecikti: e.gecikti }"
            >
              <i
                v-if="e.gecikti"
                class="pi pi-exclamation-triangle"
              />
              {{ tarih(e.planlananBaslangic) }} <i class="pi pi-arrow-right" /> {{ tarih(e.planlananBitis) }}
            </div>
          </div>
        </div>
      </TabPanel>

      <!-- REÇETELER -->
      <TabPanel :header="t('uretim.recetelerTab')">
        <div class="emir-arac">
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="receteQ"
              :aria-label="t('uretim.receteAra')"
              :placeholder="t('uretim.receteAra')"
            />
          </IconField>
          <Dropdown
            v-model="receteDurum"
            :options="receteDurumSecenekleri"
            option-label="etiket"
            option-value="deger"
            show-clear
            class="filtre-select"
            :aria-label="t('common.status')"
            :placeholder="t('common.status')"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('uretim.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="receteExcelYukleniyor"
            @click="receteExcelIndir"
          />
          <Button
            :label="t('uretim.yeniRecete')"
            icon="pi pi-plus"
            class="p-button-sm"
            @click="receteDialogAc(null)"
          />
        </div>
        <SkeletonLoader
          v-if="yukleniyor && !receteler.length"
          class="tablo-iskelet"
          :count="5"
          height="44px"
        />
        <AppDataTable
          v-else
          :value="filtreliReceteler"
          striped-rows
          arama-aktif
          :arama-placeholder="t('common.searchPlaceholder')"
          :loading="yukleniyor"
          :empty-message="t('uretim.receteYok')"
          gorunum-anahtari="uretim-receteler"
        >
          <Column
            field="ad"
            :header="t('uretim.recete')"
            sortable
          />
          <Column
            field="urunAd"
            :header="t('uretim.mamul')"
            sortable
          />
          <Column
            :header="t('uretim.kalem')"
            style="width: 100px"
          >
            <template #body="{ data }">
              {{ (data.kalemler || []).length }}
            </template>
          </Column>
          <Column
            :header="t('uretim.fireOrani')"
            style="width: 130px"
          >
            <template #body="{ data }">
              {{ Number(data.fireOrani) || 0 }}%
            </template>
          </Column>
          <Column
            :header="t('uretim.revizyon')"
            style="width: 100px"
          >
            <template #body="{ data }">
              v{{ data.revizyon || 1 }}
            </template>
          </Column>
          <Column
            :header="t('common.status')"
            style="width: 110px"
          >
            <template #body="{ data }">
              <Tag
                :value="data.aktif === false ? t('uretim.pasif') : t('uretim.aktif')"
                :severity="data.aktif === false ? 'secondary' : 'success'"
              />
            </template>
          </Column>
          <Column
            :header="t('uretim.islem')"
            style="width: 80px"
          >
            <template #body="{ data }">
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="receteAksiyonlari(data)"
              />
            </template>
          </Column>
          <template #empty>
            <EmptyState
              v-if="!yukleniyor && !receteler.length"
              :message="t('uretim.receteYok')"
              icon="pi pi-sitemap"
              :action-label="t('uretim.yeniRecete')"
              action-icon="pi pi-plus"
              @action="receteDialogAc(null)"
            />
          </template>
        </AppDataTable>
      </TabPanel>

      <!-- PLANLAMA -->
      <TabPanel :header="t('uretim.planlamaTab')">
        <div class="planlama-kart">
          <div class="planlama-form">
            <div class="field">
              <label>{{ t('uretim.mamul') }}</label>
              <Dropdown
                v-model="planForm.urunId"
                :options="stoklar"
                option-label="ad"
                option-value="id"
                filter
                class="w-full"
                :placeholder="t('uretim.urunSecin')"
              />
            </div>
            <div class="field">
              <label>{{ t('uretim.miktar') }}</label>
              <InputNumber
                v-model="planForm.miktar"
                :min="1"
                class="w-full"
              />
            </div>
            <div class="planlama-butonlar">
              <Button
                :label="t('uretim.hesapla')"
                icon="pi pi-calculator"
                :loading="ihtiyacYukleniyor"
                @click="ihtiyacHesapla"
              />
              <Button
                :label="t('uretim.yeniEmir')"
                icon="pi pi-plus"
                outlined
                @click="planlamadanEmirOlustur"
              />
            </div>
          </div>

          <div
            v-if="ihtiyac"
            class="ihtiyac-ozet"
          >
            <Tag
              :value="ihtiyac.yeterli ? t('uretim.yeterli') : t('uretim.yetersiz')"
              :severity="ihtiyac.yeterli ? 'success' : 'danger'"
            />
            <span class="ihtiyac-toplam">
              {{ t('uretim.toplamIhtiyacMaliyeti') }}:
              <strong>{{ para(ihtiyac.toplamMaliyet) }}</strong>
            </span>
          </div>

          <AppDataTable
            v-if="ihtiyac"
            :value="ihtiyac.kalemler || []"
            :paginator="false"
            striped-rows
            :empty-message="t('uretim.receteBulunamadi')"
          >
            <Column
              field="hammaddeAd"
              :header="t('uretim.hammadde')"
            />
            <Column
              field="gerekli"
              :header="t('uretim.gerekli')"
            />
            <Column
              field="mevcut"
              :header="t('uretim.mevcut')"
            />
            <Column
              field="eksik"
              :header="t('uretim.eksik')"
            >
              <template #body="{ data }">
                <span :class="{ 'eksik-vurgu': Number(data.eksik) > 0 }">
                  {{ data.eksik }}
                </span>
              </template>
            </Column>
            <Column
              field="birimFiyat"
              :header="t('uretim.birimFiyat')"
            >
              <template #body="{ data }">
                {{ para(data.birimFiyat) }}
              </template>
            </Column>
            <Column
              field="tutar"
              :header="t('uretim.tutar')"
            >
              <template #body="{ data }">
                {{ para(data.tutar) }}
              </template>
            </Column>
          </AppDataTable>

          <EmptyState
            v-else
            :message="t('uretim.ihtiyacIcinUrunSec')"
            icon="pi pi-calculator"
          />
        </div>
      </TabPanel>

      <!-- RAPOR -->
      <TabPanel :header="t('uretim.raporTab')">
        <div class="rapor-kart">
          <div class="rapor-ust">
            <h3>{{ t('uretim.durumDagilimi') }}</h3>
            <Button
              :label="t('uretim.excel')"
              icon="pi pi-file-excel"
              outlined
              size="small"
              :loading="emirExcelYukleniyor"
              @click="emirExcelIndir"
            />
          </div>
          <div class="dagilim">
            <div
              v-for="d in durumDagilimi"
              :key="d.durum"
              class="dagilim-satir"
            >
              <span class="dagilim-etiket">{{ durumAdi(d.durum) }}</span>
              <div class="dagilim-bar">
                <div
                  class="dagilim-dolu"
                  :style="{ width: `${d.yuzde}%`, background: d.renk }"
                />
              </div>
              <span class="dagilim-deger">{{ d.adet }}</span>
            </div>
          </div>

          <div class="rapor-metrics">
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.toplamEmir') }}</span>
              <span class="rapor-metric-deger">{{ toplamEmir }}</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.kpiTamamlandi') }}</span>
              <span class="rapor-metric-deger">{{ ozet.tamamlandi }}</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.kpiGeciken') }}</span>
              <span class="rapor-metric-deger">{{ ozet.geciken }}</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.kpiFireOrani') }}</span>
              <span class="rapor-metric-deger">{{ Number(ozet.fireOrani) || 0 }}%</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.kpiOrtSure') }}</span>
              <span class="rapor-metric-deger">{{ Number(ozet.ortalamaSureSaat) || 0 }}</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.toplamUretilen') }}</span>
              <span class="rapor-metric-deger">{{ formatMiktar(ozet.toplamUretilen) }}</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.toplamFire') }}</span>
              <span class="rapor-metric-deger">{{ formatMiktar(ozet.toplamFire) }}</span>
            </div>
            <div class="rapor-metric">
              <span class="rapor-metric-etiket">{{ t('uretim.toplamMaliyet') }}</span>
              <span class="rapor-metric-deger">{{ para(ozet.toplamMaliyet) }}</span>
            </div>
          </div>
        </div>
      </TabPanel>

      <!-- HATLAR -->
      <TabPanel :header="t('uretim.hatlarTab')">
        <div class="emir-arac">
          <div class="gorev-ozet-baslik hat-baslik">
            <i class="pi pi-sitemap" /> {{ t('uretim.hatlar') }} ({{ hatlar.length }})
          </div>
          <span class="arac-bosluk" />
          <Button
            :label="t('uretim.yeniHat')"
            icon="pi pi-plus"
            class="p-button-sm"
            @click="hatDialogAc(null)"
          />
        </div>

        <EmptyState
          v-if="!hatlar.length"
          icon="pi pi-sitemap"
          :message="t('uretim.hatYok')"
          :action-label="t('uretim.yeniHat')"
          action-icon="pi pi-plus"
          @action="hatDialogAc(null)"
        />

        <div
          v-else
          class="hat-kartlari"
        >
          <div
            v-for="h in hatlar"
            :key="h.id"
            class="hat-kart"
            :class="{ pasif: !h.aktif }"
          >
            <div class="hat-ust">
              <strong class="hat-ad">{{ h.ad }}</strong>
              <Tag
                :value="h.aktif ? t('uretim.aktif') : t('uretim.pasif')"
                :severity="h.aktif ? 'success' : 'secondary'"
              />
              <SatirEylemleri
                :baslik="h.ad"
                :gorunur="{ duzenle: true, sil: true }"
                @duzenle="hatDialogAc(h)"
                @sil="hatSil(h)"
              />
            </div>
            <p
              v-if="h.aciklama"
              class="hat-aciklama"
            >
              {{ h.aciklama }}
            </p>
            <div class="hat-doluluk-baslik">
              <span>{{ t('uretim.hatDoluluk') }}</span>
              <strong>{{ h.dolulukYuzde }}%</strong>
            </div>
            <div
              class="hat-doluluk"
              role="progressbar"
              :aria-valuenow="h.dolulukYuzde"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-label="`${h.ad} ${t('uretim.hatDoluluk')}`"
            >
              <div
                class="hat-doluluk-dolu"
                :class="{ dolu: h.dolulukYuzde >= 90 }"
                :style="{ width: `${h.dolulukYuzde}%` }"
              />
            </div>
            <div class="hat-metrikler">
              <span>
                <i class="pi pi-cog" />
                {{ h.aktifEmir }} / {{ h.kapasite }} {{ t('uretim.hatAktifEmir') }}
              </span>
              <span :class="{ 'metin-gecikmis': h.gecikenEmir > 0 }">
                <i class="pi pi-exclamation-triangle" />
                {{ h.gecikenEmir }} {{ t('uretim.hatGeciken') }}
              </span>
            </div>
          </div>
        </div>
      </TabPanel>

      <!-- ANALİZ -->
      <TabPanel :header="t('uretim.analizTab')">
        <div class="emir-arac">
          <DatePicker
            v-model="analizAralik"
            selection-mode="range"
            show-button-bar
            date-format="dd.mm.yy"
            class="analiz-tarih"
            :aria-label="t('uretim.analizDonem')"
            :placeholder="t('uretim.analizDonem')"
            @update:model-value="analizYukle"
          />
          <Button
            :label="t('uretim.analizUygula')"
            icon="pi pi-chart-line"
            size="small"
            :loading="analizYukleniyor"
            @click="analizYukle"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('uretim.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="emirExcelYukleniyor"
            @click="analizExcelIndir"
          />
        </div>

        <SkeletonLoader
          v-if="analizYukleniyor && !analiz"
          class="tablo-iskelet"
          :count="5"
          height="60px"
        />

        <template v-else-if="analiz">
          <div class="kpi-satiri analiz-kpi">
            <KpiKart
              :baslik="t('uretim.toplamUretilen')"
              :deger="Number(analiz.toplamUretilen) || 0"
              ikon="pi pi-box"
              renk="#3b82f6"
              :para-birimi="false"
            />
            <KpiKart
              :baslik="t('uretim.kpiFireOrani')"
              :deger="Number(analiz.fireOrani) || 0"
              ikon="pi pi-exclamation-triangle"
              renk="#f59e0b"
              :para-birimi="false"
            />
            <KpiKart
              :baslik="t('uretim.toplamMaliyet')"
              :deger="analiz.toplamMaliyet"
              ikon="pi pi-wallet"
              renk="#10b981"
            />
            <KpiKart
              :baslik="t('uretim.birimMaliyet')"
              :deger="analiz.birimMaliyet"
              ikon="pi pi-tag"
              renk="#8b5cf6"
            />
          </div>

          <div class="analiz-kart">
            <h3>{{ t('uretim.analizTrend') }}</h3>
            <p
              v-if="!analiz.trend || !analiz.trend.length"
              class="muted"
            >
              {{ t('uretim.analizYok') }}
            </p>
            <div
              v-else
              class="trend-liste"
            >
              <div
                v-for="tr in analiz.trend"
                :key="tr.donem"
                class="trend-satir"
              >
                <span class="trend-donem">{{ donemEtiket(tr.donem) }}</span>
                <div class="trend-bar">
                  <div
                    class="trend-dolu uretim"
                    :style="{ width: `${barYuzde(tr.uretilen)}%` }"
                  />
                  <div
                    class="trend-dolu fire"
                    :style="{ width: `${barYuzde(tr.fire)}%` }"
                  />
                </div>
                <span class="trend-deger">
                  {{ formatMiktar(tr.uretilen) }} /
                  <span class="metin-gecikmis">{{ formatMiktar(tr.fire) }}</span>
                </span>
                <span class="trend-maliyet">{{ para(tr.maliyet) }}</span>
              </div>
            </div>
          </div>

          <div class="analiz-kart">
            <h3>{{ t('uretim.analizUrunBazli') }}</h3>
            <AppDataTable
              :value="analiz.urunler || []"
              striped-rows
              gorunum-anahtari="uretim-analiz"
              :empty-message="t('uretim.analizYok')"
            >
              <Column
                field="urunAd"
                :header="t('uretim.mamul')"
                sortable
              />
              <Column
                field="emirSayisi"
                :header="t('uretim.emirSayisi')"
                style="width: 100px"
                sortable
              />
              <Column
                field="uretilen"
                :header="t('uretim.uretilen')"
                style="width: 130px"
                sortable
              >
                <template #body="{ data }">
                  {{ formatMiktar(data.uretilen) }}
                </template>
              </Column>
              <Column
                field="fire"
                :header="t('uretim.fire')"
                style="width: 100px"
              >
                <template #body="{ data }">
                  {{ formatMiktar(data.fire) }}
                </template>
              </Column>
              <Column
                field="fireOrani"
                :header="t('uretim.fireOrani')"
                style="width: 110px"
              >
                <template #body="{ data }">
                  {{ Number(data.fireOrani) || 0 }}%
                </template>
              </Column>
              <Column
                field="toplamMaliyet"
                :header="t('uretim.toplamMaliyet')"
                style="width: 140px"
                sortable
              >
                <template #body="{ data }">
                  {{ para(data.toplamMaliyet) }}
                </template>
              </Column>
              <Column
                field="birimMaliyet"
                :header="t('uretim.birimMaliyet')"
                style="width: 130px"
              >
                <template #body="{ data }">
                  {{ para(data.birimMaliyet) }}
                </template>
              </Column>
            </AppDataTable>
          </div>
        </template>

        <EmptyState
          v-else
          icon="pi pi-chart-line"
          :message="t('uretim.analizYok')"
        />
      </TabPanel>

      <!-- TAKVİM / GANTT -->
      <TabPanel :header="t('uretim.takvimTab')">
        <div class="emir-arac">
          <div class="ay-gezinme">
            <Button
              icon="pi pi-chevron-left"
              :aria-label="t('uretim.oncekiAy')"
              class="p-button-text p-button-sm"
              @click="takvimAyDegistir(-1)"
            />
            <span class="takvim-ay">{{ takvimEtiket }}</span>
            <Button
              icon="pi pi-chevron-right"
              :aria-label="t('uretim.sonrakiAy')"
              class="p-button-text p-button-sm"
              @click="takvimAyDegistir(1)"
            />
            <Button
              :label="t('uretim.bugun')"
              class="p-button-sm p-button-outlined"
              @click="takvimBugun"
            />
          </div>
          <span class="arac-bosluk" />
          <div
            class="takvim-lejant"
            role="group"
            :aria-label="t('uretim.takvimLejant')"
          >
            <span class="lejant"><i class="lejant-nokta taslak" />{{ t('uretim.taslak') }}</span>
            <span class="lejant"><i class="lejant-nokta uretimde" />{{ t('uretim.uretimde') }}</span>
            <span class="lejant"><i class="lejant-nokta tamamlandi" />{{ t('uretim.tamamlandi') }}</span>
          </div>
        </div>

        <SkeletonLoader
          v-if="takvimYukleniyor && !takvim.length"
          class="tablo-iskelet"
          :count="5"
          height="44px"
        />

        <EmptyState
          v-else-if="!takvimSatirlari.length"
          icon="pi pi-calendar"
          :message="t('uretim.takvimYok')"
        />

        <div
          v-else
          class="gantt"
          role="list"
          :aria-label="t('uretim.takvimTab')"
        >
          <div
            class="gantt-baslik"
            aria-hidden="true"
          >
            <span class="gantt-etiket-bos" />
            <div
              class="gantt-gunler"
              :style="{ gridTemplateColumns: `repeat(${takvimGunSayisi}, minmax(20px, 1fr))` }"
            >
              <span
                v-for="g in takvimGunleri"
                :key="g"
              >{{ g }}</span>
            </div>
          </div>
          <div
            v-for="satir in takvimSatirlari"
            :key="satir.emir.id"
            class="gantt-satir"
            role="listitem"
          >
            <button
              type="button"
              class="gantt-etiket"
              :title="`#${satir.emir.id} ${satir.emir.urunAd || ''}`"
              @click="detayAc(satir.emir)"
            >
              <strong>#{{ satir.emir.id }} · {{ satir.emir.urunAd }}</strong>
              <small>{{ satir.emir.hatAd || t('uretim.hatsiz') }} · {{ durumAdi(satir.emir.durum) }}</small>
            </button>
            <div
              class="gantt-izgara"
              :style="{ gridTemplateColumns: `repeat(${takvimGunSayisi}, minmax(20px, 1fr))` }"
            >
              <span
                v-for="g in takvimGunleri"
                :key="g"
                class="gantt-gun-cizgi"
              />
              <button
                type="button"
                class="gantt-cubuk"
                :class="durumSinif(satir.emir.durum)"
                :style="{ left: `${satir.bar.left}%`, width: `${satir.bar.width}%` }"
                :aria-label="ganttAria(satir)"
                :title="ganttAria(satir)"
                @click="detayAc(satir.emir)"
              >
                {{ satir.bar.gunSayisi }}g
              </button>
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <!-- Yeni Emir Dialog -->
    <Dialog
      v-model:visible="emirDialog"
      :header="t('uretim.yeniUretimEmri')"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form">
        <div class="field">
          <label>{{ t('uretim.urunZorunlu') }}</label>
          <Dropdown
            v-model="emirForm.urunId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            filter
            class="w-full"
            :placeholder="t('uretim.urunSecin')"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('uretim.miktarZorunlu') }}</label>
            <InputNumber
              v-model="emirForm.miktar"
              :min="1"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('uretim.oncelik') }}</label>
            <Dropdown
              v-model="emirForm.oncelik"
              :options="oncelikSecenekleri"
              option-label="etiket"
              option-value="deger"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('uretim.planlananBaslangic') }}</label>
            <DatePicker
              v-model="emirForm.planlananBaslangic"
              date-format="dd.mm.yy"
              class="w-full"
              show-icon
              :manual-input="false"
            />
          </div>
          <div class="field">
            <label>{{ t('uretim.planlananBitis') }}</label>
            <DatePicker
              v-model="emirForm.planlananBitis"
              date-format="dd.mm.yy"
              class="w-full"
              show-icon
              :manual-input="false"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('uretim.hat') }}</label>
          <Dropdown
            v-model="emirForm.hatId"
            :options="aktifHatSecenekleri"
            option-label="ad"
            option-value="id"
            show-clear
            class="w-full"
            :placeholder="t('uretim.hatSecin')"
          />
        </div>
        <div class="field">
          <label>{{ t('uretim.aciklama') }}</label>
          <Textarea
            v-model="emirForm.aciklama"
            rows="2"
            auto-resize
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="emirDialog = false"
        />
        <Button
          :label="t('uretim.olustur')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="emirKaydet"
        />
      </template>
    </Dialog>

    <!-- Tamamla Dialog -->
    <Dialog
      v-model:visible="tamamlaDialog"
      :header="t('uretim.tamamlaBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div
        v-if="seciliEmir"
        class="tamamla-ozet"
      >
        <strong>{{ seciliEmir.urunAd }}</strong>
        <span class="muted">{{ t('uretim.planlanan') }}: {{ seciliEmir.miktar }}</span>
      </div>
      <div class="form">
        <div class="field-row">
          <div class="field">
            <label>{{ t('uretim.uretilenMiktar') }}</label>
            <InputNumber
              v-model="tamamlaForm.uretilenMiktar"
              :min="0"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('uretim.fireMiktar') }}</label>
            <InputNumber
              v-model="tamamlaForm.fireMiktar"
              :min="0"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('uretim.iscilikMaliyetiGir') }}</label>
          <InputNumber
            v-model="tamamlaForm.iscilikMaliyeti"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('uretim.aciklama') }}</label>
          <Textarea
            v-model="tamamlaForm.aciklama"
            rows="2"
            auto-resize
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="tamamlaDialog = false"
        />
        <Button
          :label="t('uretim.tamamla')"
          icon="pi pi-check"
          class="p-button-success"
          :loading="kaydediliyor"
          @click="tamamlaKaydet"
        />
      </template>
    </Dialog>

    <!-- İptal Dialog -->
    <Dialog
      v-model:visible="iptalDialog"
      :header="t('uretim.iptalEt')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form">
        <p>{{ t('uretim.iptalOnay') }}</p>
        <div class="field">
          <label>{{ t('uretim.aciklama') }}</label>
          <Textarea
            v-model="iptalForm.aciklama"
            rows="2"
            auto-resize
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="iptalDialog = false"
        />
        <Button
          :label="t('uretim.iptalEt')"
          icon="pi pi-times"
          class="p-button-danger"
          :loading="kaydediliyor"
          @click="iptalKaydet"
        />
      </template>
    </Dialog>

    <!-- Detay / Geçmiş Drawer -->
    <!-- Hat Yönetimi Dialog -->
    <Dialog
      v-model:visible="hatDialog"
      :header="hatForm.id ? t('uretim.hatDuzenle') : t('uretim.yeniHat')"
      modal
      :style="{ width: '420px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form">
        <div class="field">
          <label>{{ t('uretim.hatAd') }}</label>
          <InputText
            v-model="hatForm.ad"
            class="w-full"
            :placeholder="t('uretim.hatAdPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t('uretim.hatKapasite') }}</label>
          <InputNumber
            v-model="hatForm.kapasite"
            :min="1"
            :max="9999"
            class="w-full"
          />
          <small class="muted">{{ t('uretim.hatKapasiteIpucu') }}</small>
        </div>
        <div class="field">
          <label>{{ t('uretim.aciklama') }}</label>
          <Textarea
            v-model="hatForm.aciklama"
            rows="2"
            auto-resize
            class="w-full"
          />
        </div>
        <div class="field field-inline">
          <label>{{ t('uretim.hatAktif') }}</label>
          <ToggleSwitch v-model="hatForm.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="hatDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="hatKaydet"
        />
      </template>
    </Dialog>

    <!-- Hat Ata Dialog -->
    <Dialog
      v-model:visible="hatAtaDialog"
      :header="t('uretim.hatAta')"
      modal
      :style="{ width: '380px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p
        v-if="seciliEmir"
        class="hat-ata-ozet"
      >
        #{{ seciliEmir.id }} · {{ seciliEmir.urunAd }}
      </p>
      <Dropdown
        v-model="hatAtaSecim"
        :options="aktifHatSecenekleri"
        option-label="ad"
        option-value="id"
        show-clear
        class="w-full"
        :placeholder="t('uretim.hatSecin')"
      />
      <small class="muted">{{ t('uretim.hatAtaIpucu') }}</small>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="hatAtaDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="hatAtaKaydet"
        />
      </template>
    </Dialog>

    <UretimEmriDetayDrawer
      v-model:visible="detayDrawer"
      :emir="seciliEmir"
      :gecmis="seciliGecmis"
      :gecmis-yukleniyor="detayYukleniyor"
      @baslat="drawerBaslat"
      @tamamla="drawerTamamla"
      @iptal="drawerIptal"
    />

    <!-- Reçete Dialog -->
    <Dialog
      v-model:visible="receteDialog"
      :header="receteForm.id ? t('uretim.duzenle') : t('uretim.yeniRecete')"
      modal
      :style="{ width: '720px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form">
        <div class="field-row">
          <div class="field">
            <label>{{ t('uretim.receteAdiZorunlu') }}</label>
            <InputText
              v-model="receteForm.ad"
              class="w-full"
              :placeholder="t('uretim.receteAdiPlaceholder')"
            />
          </div>
          <div class="field">
            <label>{{ t('uretim.mamulZorunlu') }}</label>
            <Dropdown
              v-model="receteForm.urunId"
              :options="stoklar"
              option-label="ad"
              option-value="id"
              filter
              class="w-full"
              :placeholder="t('uretim.mamulSecin')"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('uretim.fireOrani') }}</label>
            <InputNumber
              v-model="receteForm.fireOrani"
              :min="0"
              :max="100"
              class="w-full"
            />
          </div>
          <div class="field baz-field">
            <label>{{ t('uretim.bazMiktar') }}</label>
            <div class="baz-satir">
              <InputNumber
                v-model="receteForm.bazMiktar"
                :min="0.0001"
                :min-fraction-digits="0"
                :max-fraction-digits="4"
                class="baz-miktar"
              />
              <InputText
                v-model="receteForm.bazBirim"
                class="baz-birim"
                :placeholder="t('uretim.birim')"
              />
            </div>
          </div>
          <div class="field field-switch">
            <label>{{ t('common.status') }}</label>
            <div class="switch-satir">
              <InputSwitch v-model="receteForm.aktif" />
              <span>{{ receteForm.aktif ? t('uretim.aktif') : t('uretim.pasif') }}</span>
            </div>
          </div>
        </div>

        <div class="field">
          <div class="kalem-baslik-satir">
            <label>{{ t('uretim.hammaddeler') }}</label>
            <div class="olcek-girdi">
              <span class="olcek-etiket">{{ t('uretim.uretimMiktari') }}</span>
              <InputNumber
                v-model="onizlemeMiktar"
                :min="0"
                :max-fraction-digits="4"
                class="olcek-input"
              />
              <span class="olcek-birim">{{ receteForm.bazBirim }}</span>
            </div>
          </div>
          <div class="kalem-grid kalem-grid-baslik">
            <span>{{ t('uretim.hammadde') }}</span>
            <span>{{ t('uretim.miktar') }}</span>
            <span>{{ t('uretim.birim') }}</span>
            <span>{{ t('uretim.kalemFireOrani') }}</span>
            <span>{{ t('uretim.hesaplanan') }}</span>
            <span />
          </div>
          <div
            v-for="(k, i) in receteForm.kalemler"
            :key="i"
            class="kalem-grid"
          >
            <Dropdown
              v-model="k.hammaddeId"
              :options="stoklar"
              option-label="ad"
              option-value="id"
              filter
              class="kalem-hammadde"
              :placeholder="t('uretim.hammadde')"
            />
            <InputNumber
              v-model="k.miktar"
              :min="0"
              :max-fraction-digits="4"
              class="kalem-miktar"
              :placeholder="t('uretim.miktar')"
            />
            <InputText
              v-model="k.birim"
              class="kalem-birim"
              :placeholder="t('uretim.birim')"
            />
            <InputNumber
              v-model="k.fireOrani"
              :min="0"
              :max="100"
              class="kalem-fire"
              :placeholder="t('uretim.kalemFireOrani')"
            />
            <span class="kalem-hesaplanan">{{ formatMiktar(kalemHesaplanan(k)) }}</span>
            <Button
              icon="pi pi-trash"
              :aria-label="$t('common.delete')"
              class="p-button-text p-button-danger p-button-sm"
              @click="receteForm.kalemler.splice(i, 1)"
            />
          </div>
          <Button
            :label="t('uretim.kalemEkle')"
            icon="pi pi-plus"
            class="p-button-sm p-button-text"
            @click="receteKalemEkle"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="receteDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="receteKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { uretimAPI, stokAPI, excelAPI } from '../api/index.js'
import KpiKart from '../components/KpiKart.vue'
import UretimEmriDetayDrawer from '../components/UretimEmriDetayDrawer.vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { formatCurrency as para, formatDate as tarih } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t, locale } = useI18n()

const receteler = ref([])
const stoklar = ref([])
const ozet = ref({})
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const ihtiyacYukleniyor = ref(false)
const ozetYuklendi = ref(false)

// ---- Hafıza: son sekme + emir filtreleri ----
const SEKME_ANAHTAR = 'raspel_uretim_tab'
const FILTRE_ANAHTAR = 'raspel_uretim_emir_filtre'

const isoToDate = (s) => {
  if (!s) return null
  const [y, m, d] = String(s).slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d)
}

const kayitliFiltre = (() => {
  try {
    return JSON.parse(localStorage.getItem(FILTRE_ANAHTAR) || '{}') || {}
  } catch {
    return {}
  }
})()

// Emirler listesi (sunucu taraflı filtre + sayfalama).
const emirSayfa = ref([])
const emirToplam = ref(0)
const emirSayfaNo = ref(0)
const emirSayfaBoyutu = ref(25)
const emirSirala = ref('olusturmaTarihi,desc')
const emirYukleniyor = ref(false)
const emirExcelYukleniyor = ref(false)

const emirQ = ref(kayitliFiltre.q || '')
const emirDurum = ref(kayitliFiltre.durum || null)
const emirOncelik = ref(kayitliFiltre.oncelik || null)
const emirGeciken = ref(!!kayitliFiltre.geciken)
const emirBuAy = ref(!!kayitliFiltre.buAy)
const emirBaslangic = ref(isoToDate(kayitliFiltre.baslangic))
const emirBitis = ref(isoToDate(kayitliFiltre.bitis))
let emirAramaZaman = null

const receteDurum = ref(null)
const receteQ = ref('')
const receteExcelYukleniyor = ref(false)

const hatlar = ref([])
const emirHat = ref(kayitliFiltre.hatId || null)
const hatDialog = ref(false)
const hatForm = ref(bosHatForm())
const hatAtaDialog = ref(false)
const hatAtaSecim = ref(null)

const analiz = ref(null)
const analizYukleniyor = ref(false)
const analizYuklendi = ref(false)
const analizAralik = ref(varsayilanAnalizAralik())

const takvim = ref([])
const takvimYukleniyor = ref(false)
const takvimYuklendi = ref(false)
const takvimAy = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1))

function varsayilanAnalizAralik() {
  const bugun = new Date()
  return [new Date(bugun.getFullYear(), bugun.getMonth() - 11, 1), bugun]
}

const aktifSekme = ref(Number(localStorage.getItem(SEKME_ANAHTAR)) || 0)
watch(aktifSekme, (v) => {
  try {
    localStorage.setItem(SEKME_ANAHTAR, String(v))
  } catch {
    /* depolama kapalı olabilir */
  }
  if (v === 5 && !analizYuklendi.value) analizYukle()
  if (v === 6) takvimYukle()
})

const emirDialog = ref(false)
const tamamlaDialog = ref(false)
const iptalDialog = ref(false)
const detayDrawer = ref(false)
const receteDialog = ref(false)

const seciliEmir = ref(null)
const seciliGecmis = ref([])
const detayYukleniyor = ref(false)

const ihtiyac = ref(null)

const emirForm = ref(bosEmirForm())
const tamamlaForm = ref(bosTamamlaForm())
const iptalForm = ref({ aciklama: '' })
const receteForm = ref(bosReceteForm())
const planForm = ref({ urunId: null, miktar: null })

function bosEmirForm() {
  return { urunId: null, miktar: null, oncelik: 'NORMAL', planlananBaslangic: null, planlananBitis: null, hatId: null, aciklama: '' }
}
function bosHatForm() {
  return { id: null, ad: '', aciklama: '', kapasite: 5, aktif: true }
}
function bosTamamlaForm() {
  return { uretilenMiktar: null, fireMiktar: 0, iscilikMaliyeti: 0, aciklama: '' }
}
function bosReceteForm() {
  return { id: null, ad: '', urunId: null, fireOrani: 0, bazMiktar: 1, bazBirim: '', aktif: true, kalemler: [] }
}

const durumSecenekleri = computed(() => [
  { deger: 'TASLAK', etiket: t('uretim.taslak') },
  { deger: 'URETIMDE', etiket: t('uretim.uretimde') },
  { deger: 'TAMAMLANDI', etiket: t('uretim.tamamlandi') },
  { deger: 'IPTAL', etiket: t('uretim.iptal') }
])

const oncelikSecenekleri = computed(() => [
  { deger: 'DUSUK', etiket: t('uretim.oncelikDusuk') },
  { deger: 'NORMAL', etiket: t('uretim.oncelikNormal') },
  { deger: 'YUKSEK', etiket: t('uretim.oncelikYuksek') }
])

const receteDurumSecenekleri = computed(() => [
  { deger: 'AKTIF', etiket: t('uretim.aktif') },
  { deger: 'PASIF', etiket: t('uretim.pasif') }
])

const aktifHatSecenekleri = computed(() => hatlar.value.filter((h) => h.aktif !== false))

// Reçeteler küçük veri seti: istemci tarafı arama + aktiflik filtresi yeterli.
const receteAktifParam = () => {
  if (receteDurum.value === 'AKTIF') return true
  if (receteDurum.value === 'PASIF') return false
  return null
}

const receteParams = () => {
  const p = {}
  if (receteQ.value.trim()) p.q = receteQ.value.trim()
  const aktif = receteAktifParam()
  if (aktif !== null) p.aktif = aktif
  return p
}

const filtreliReceteler = computed(() => {
  const q = receteQ.value.trim().toLocaleLowerCase('tr')
  return receteler.value
    .filter((r) => {
      if (receteDurum.value === 'AKTIF') return r.aktif !== false
      if (receteDurum.value === 'PASIF') return r.aktif === false
      return true
    })
    .filter((r) => !q || `${r.ad || ''} ${r.urunAd || ''}`.toLocaleLowerCase('tr').includes(q))
})

const emirFiltreVar = computed(
  () =>
    !!emirQ.value?.trim() ||
    !!emirDurum.value ||
    !!emirOncelik.value ||
    !!emirHat.value ||
    emirGeciken.value ||
    emirBuAy.value ||
    !!emirBaslangic.value ||
    !!emirBitis.value
)

const kpiIskelet = computed(() => yukleniyor.value && !ozetYuklendi.value)

// Ekran okuyucular için liste sonucu duyurusu.
const emirDuyuru = computed(() =>
  emirYukleniyor.value
    ? ''
    : t('uretim.emirDuyuru', { gorunen: emirSayfa.value.length, toplam: emirToplam.value })
)

// Filtreler kalıcı: sayfa yenilense de son görünüm korunur.
watch(
  [emirQ, emirDurum, emirOncelik, emirHat, emirGeciken, emirBuAy, emirBaslangic, emirBitis],
  () => {
    try {
      localStorage.setItem(
        FILTRE_ANAHTAR,
        JSON.stringify({
          q: emirQ.value || '',
          durum: emirDurum.value || null,
          oncelik: emirOncelik.value || null,
          hatId: emirHat.value || null,
          geciken: emirGeciken.value,
          buAy: emirBuAy.value,
          baslangic: tarihIso(emirBaslangic.value),
          bitis: tarihIso(emirBitis.value)
        })
      )
    } catch {
      /* depolama kapalı olabilir */
    }
  }
)

// Hızlı filtre çipleri (sunucu taraflı).
const emirCipleri = computed(() => [
  { deger: 'TUMU', ad: t('uretim.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'GECIKEN', ad: t('uretim.cipGeciken'), ikon: 'pi pi-exclamation-triangle' },
  { deger: 'TASLAK', ad: t('uretim.taslak'), ikon: 'pi pi-file-edit' },
  { deger: 'URETIMDE', ad: t('uretim.uretimde'), ikon: 'pi pi-cog' },
  { deger: 'TAMAMLANDI', ad: t('uretim.tamamlandi'), ikon: 'pi pi-check-circle' }
])

// Aktif çip, mevcut filtrelerden türetilir (KPI tıklaması da çipi günceller).
const aktifCip = computed(() => {
  if (emirGeciken.value) return 'GECIKEN'
  if (emirBuAy.value) return 'BUAY'
  if (emirDurum.value && !emirOncelik.value && !emirQ.value?.trim() && !emirBaslangic.value && !emirBitis.value) {
    return emirDurum.value
  }
  return 'TUMU'
})

const toplamEmir = computed(
  () => (ozet.value.taslak || 0) + (ozet.value.uretimde || 0) + (ozet.value.tamamlandi || 0) + (ozet.value.iptal || 0)
)

const durumDagilimi = computed(() => {
  const toplam = toplamEmir.value || 1
  const tanimlar = [
    { durum: 'TASLAK', adet: ozet.value.taslak || 0, renk: '#64748b' },
    { durum: 'URETIMDE', adet: ozet.value.uretimde || 0, renk: '#3b82f6' },
    { durum: 'TAMAMLANDI', adet: ozet.value.tamamlandi || 0, renk: '#10b981' },
    { durum: 'IPTAL', adet: ozet.value.iptal || 0, renk: '#ef4444' }
  ]
  return tanimlar.map((d) => ({ ...d, yuzde: Math.round((d.adet / toplam) * 100) }))
})

const durumAdi = (d) =>
  ({
    TASLAK: t('uretim.taslak'),
    URETIMDE: t('uretim.uretimde'),
    TAMAMLANDI: t('uretim.tamamlandi'),
    IPTAL: t('uretim.iptal')
  })[d] || d

const durumSeverity = (d) =>
  ({ TASLAK: 'secondary', URETIMDE: 'info', TAMAMLANDI: 'success', IPTAL: 'danger' })[d] || 'secondary'

const oncelikAdi = (o) =>
  ({ DUSUK: t('uretim.oncelikDusuk'), NORMAL: t('uretim.oncelikNormal'), YUKSEK: t('uretim.oncelikYuksek') })[o] ||
  o ||
  '-'

const oncelikSeverity = (o) => ({ DUSUK: 'secondary', NORMAL: 'info', YUKSEK: 'warn' })[o] || 'secondary'

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [r, o, s, h] = await Promise.all([
      uretimAPI.receteler(),
      uretimAPI.ozet(),
      stokAPI.getAll({ size: 500 }),
      uretimAPI.hatlar()
    ])
    receteler.value = r.data || []
    ozet.value = o.data || {}
    ozetYuklendi.value = true
    stoklar.value = unwrapList(s)
    hatlar.value = h.data || []
  } catch {
    toastBildirim.hata(t('uretim.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
  await emirleriYukle()
}

// Liste ve Excel'in paylaştığı filtre parametreleri.
const emirParams = () => {
  const p = { page: emirSayfaNo.value, size: emirSayfaBoyutu.value, sort: emirSirala.value }
  if (emirQ.value?.trim()) p.q = emirQ.value.trim()
  if (emirDurum.value) p.durum = emirDurum.value
  if (emirOncelik.value) p.oncelik = emirOncelik.value
  if (emirHat.value) p.hatId = emirHat.value
  if (emirGeciken.value) p.geciken = true
  if (emirBuAy.value) p.buAy = true
  if (emirBaslangic.value) p.baslangic = tarihIso(emirBaslangic.value)
  if (emirBitis.value) p.bitis = tarihIso(emirBitis.value)
  return p
}

const emirleriYukle = async () => {
  emirYukleniyor.value = true
  try {
    const r = await uretimAPI.emirlerFiltreli(emirParams())
    emirSayfa.value = r.data?.content || []
    emirToplam.value = r.data?.totalElements ?? emirSayfa.value.length
  } catch {
    toastBildirim.hata(t('uretim.hataYukleme'))
  } finally {
    emirYukleniyor.value = false
  }
}

const emirFiltreDegisti = () => {
  if (emirAramaZaman) clearTimeout(emirAramaZaman)
  emirAramaZaman = setTimeout(() => {
    emirSayfaNo.value = 0
    emirleriYukle()
  }, 300)
}

const emirFiltreTemizle = () => {
  emirQ.value = ''
  emirDurum.value = null
  emirOncelik.value = null
  emirHat.value = null
  emirGeciken.value = false
  emirBuAy.value = false
  emirBaslangic.value = null
  emirBitis.value = null
  emirSayfaNo.value = 0
  emirleriYukle()
}

const emirCipUygula = (deger) => {
  emirQ.value = ''
  emirOncelik.value = null
  emirBaslangic.value = null
  emirBitis.value = null
  emirGeciken.value = deger === 'GECIKEN'
  emirBuAy.value = false
  emirDurum.value = ['TASLAK', 'URETIMDE', 'TAMAMLANDI', 'IPTAL'].includes(deger) ? deger : null
  emirSayfaNo.value = 0
  emirleriYukle()
}

// KPI kartına tıklama: ilgili filtreyi kurar ve Emirler sekmesine geçer.
const kpiEmirFiltre = (tur) => {
  emirQ.value = ''
  emirOncelik.value = null
  emirBaslangic.value = null
  emirBitis.value = null
  emirGeciken.value = tur === 'GECIKEN'
  emirBuAy.value = tur === 'BUAY'
  emirDurum.value = tur === 'TASLAK' || tur === 'URETIMDE' ? tur : tur === 'BUAY' ? 'TAMAMLANDI' : null
  emirSayfaNo.value = 0
  aktifSekme.value = 0
  emirleriYukle()
}

const emirSayfaDegisti = (e) => {
  emirSayfaNo.value = e.page
  emirSayfaBoyutu.value = e.rows
  emirleriYukle()
}

const emirSiralaDegisti = (e) => {
  const yon = e.sortOrder === 1 ? 'asc' : 'desc'
  emirSirala.value = `${e.sortField},${yon}`
  emirSayfaNo.value = 0
  emirleriYukle()
}

const emirExcelIndir = async () => {
  emirExcelYukleniyor.value = true
  try {
    const params = emirParams()
    delete params.page
    delete params.size
    delete params.sort
    const res = await excelAPI.uretimEmirleri(params)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `uretim-emirleri-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('uretim.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('uretim.excelHata'))
  } finally {
    emirExcelYukleniyor.value = false
  }
}

const emirAksiyonlari = (e) => {
  const items = []
  if (e.durum === 'TASLAK') {
    items.push({ etiket: t('uretim.baslat'), ikon: 'pi pi-play', islem: () => baslatOnay(e) })
  }
  if (e.durum === 'TASLAK' || e.durum === 'URETIMDE') {
    items.push({ etiket: t('uretim.tamamla'), ikon: 'pi pi-check', islem: () => tamamlaDialogAc(e) })
    items.push({
      etiket: t('uretim.satinalmaTalebi'),
      ikon: 'pi pi-shopping-cart',
      islem: () => satinalmaOnay(e)
    })
    items.push({ etiket: t('uretim.iptalEt'), ikon: 'pi pi-times', sinif: 'eylem-sil', islem: () => iptalDialogAc(e) })
    items.push({ etiket: t('uretim.hatAta'), ikon: 'pi pi-sitemap', islem: () => hatAtaDialogAc(e) })
  }
  items.push({ etiket: t('uretim.detay'), ikon: 'pi pi-search', islem: () => detayAc(e) })
  items.push({ etiket: t('uretim.gecmis'), ikon: 'pi pi-history', islem: () => detayAc(e) })
  return items
}

const receteAksiyonlari = (r) => [
  { etiket: t('uretim.duzenle'), ikon: 'pi pi-pencil', islem: () => receteDialogAc(r) },
  { etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => receteSil(r) }
]

const emirDialogAc = (onDoldur = null) => {
  emirForm.value = bosEmirForm()
  if (onDoldur) Object.assign(emirForm.value, onDoldur)
  emirDialog.value = true
}

const emirKaydet = async () => {
  if (!emirForm.value.urunId || !emirForm.value.miktar || emirForm.value.miktar <= 0) {
    toastBildirim.uyari(t('uretim.urunMiktarZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    await uretimAPI.emirOlustur({
      ...emirForm.value,
      planlananBaslangic: tarihIso(emirForm.value.planlananBaslangic),
      planlananBitis: tarihIso(emirForm.value.planlananBitis)
    })
    toastBildirim.basarili(t('uretim.emirOlusturuldu'))
    emirDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.emirOlusturulamadi'))
  } finally {
    kaydediliyor.value = false
  }
}

const baslatOnay = (e) => {
  confirm.require({
    message: t('uretim.baslatOnay'),
    header: t('uretim.title'),
    icon: 'pi pi-play',
    accept: async () => {
      try {
        await uretimAPI.emirBaslat(e.id)
        toastBildirim.basarili(t('uretim.baslatildi'))
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('uretim.baslatilamadi'))
      }
    }
  })
}

const tamamlaDialogAc = (e) => {
  seciliEmir.value = e
  tamamlaForm.value = bosTamamlaForm()
  tamamlaForm.value.uretilenMiktar = Number(e.miktar) || null
  tamamlaDialog.value = true
}

const tamamlaKaydet = async () => {
  if (!tamamlaForm.value.uretilenMiktar || tamamlaForm.value.uretilenMiktar <= 0) {
    toastBildirim.uyari(t('uretim.miktarSifirdanBuyuk'))
    return
  }
  if (Number(tamamlaForm.value.fireMiktar) < 0) {
    toastBildirim.uyari(t('uretim.fireNegatifOlamaz'))
    return
  }
  kaydediliyor.value = true
  try {
    await uretimAPI.emirTamamla(seciliEmir.value.id, tamamlaForm.value)
    toastBildirim.basarili(t('uretim.uretimTamamlandi'))
    tamamlaDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.uretimTamamlanamadi'))
  } finally {
    kaydediliyor.value = false
  }
}

const iptalDialogAc = (e) => {
  seciliEmir.value = e
  iptalForm.value = { aciklama: '' }
  iptalDialog.value = true
}

const iptalKaydet = async () => {
  kaydediliyor.value = true
  try {
    await uretimAPI.emirIptal(seciliEmir.value.id, iptalForm.value.aciklama)
    toastBildirim.basarili(t('uretim.iptalEdildi'))
    iptalDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.iptalEdilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}

const satinalmaOnay = (e) => {
  confirm.require({
    message: t('uretim.satinalmaTalebi') + '?',
    header: t('uretim.title'),
    icon: 'pi pi-shopping-cart',
    accept: async () => {
      try {
        await uretimAPI.satinalmaTalebi(e.id)
        toastBildirim.basarili(t('uretim.satinalmaOlusturuldu'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('uretim.satinalmaOlusturulamadi'))
      }
    }
  })
}

const detayAc = async (e) => {
  seciliEmir.value = e
  seciliGecmis.value = []
  detayDrawer.value = true
  detayYukleniyor.value = true
  try {
    const res = await uretimAPI.emirGecmis(e.id)
    seciliGecmis.value = res.data || []
  } catch {
    seciliGecmis.value = []
  } finally {
    detayYukleniyor.value = false
  }
}

// Drawer aksiyonları: drawer'ı kapatıp mevcut akışı başlatır.
const drawerBaslat = (e) => {
  detayDrawer.value = false
  baslatOnay(e)
}
const drawerTamamla = (e) => {
  detayDrawer.value = false
  tamamlaDialogAc(e)
}
const drawerIptal = (e) => {
  detayDrawer.value = false
  iptalDialogAc(e)
}

const receteDialogAc = (r) => {
  if (r) {
    receteForm.value = {
      id: r.id,
      ad: r.ad || '',
      urunId: r.urunId || null,
      fireOrani: Number(r.fireOrani) || 0,
      bazMiktar: Number(r.bazMiktar) || 1,
      bazBirim: r.bazBirim || '',
      aktif: r.aktif !== false,
      kalemler: (r.kalemler || []).map((k) => ({
        hammaddeId: k.hammaddeId,
        miktar: Number(k.miktar) || 0,
        birim: k.birim || '',
        fireOrani: Number(k.fireOrani) || 0
      }))
    }
    onizlemeMiktar.value = Number(r.bazMiktar) || 1
  } else {
    receteForm.value = bosReceteForm()
    onizlemeMiktar.value = 1
  }
  receteDialog.value = true
}

// Canlı ölçekleme önizlemesi: kalem miktarları `bazMiktar` içindir; istenen
// üretim miktarı için fire+fire oranı dahil hesaplanır ("1 litre -> N litre").
const onizlemeMiktar = ref(1)
const kalemHesaplanan = (k) => {
  const baz = Number(receteForm.value.bazMiktar) || 1
  const istenen = Number(onizlemeMiktar.value) || 0
  const katsayi = baz > 0 ? istenen / baz : 0
  const receteFire = Number(receteForm.value.fireOrani) || 0
  const toplam = katsayi * (1 + receteFire / 100)
  const kalemFire = Number(k.fireOrani) || 0
  return (Number(k.miktar) || 0) * toplam * (1 + kalemFire / 100)
}

const formatMiktar = (v) => {
  const n = Number(v) || 0
  return n.toLocaleString('tr-TR', { maximumFractionDigits: 4 })
}

const receteKalemEkle = () => {
  receteForm.value.kalemler.push({ hammaddeId: null, miktar: 1, birim: '', fireOrani: 0 })
}

const receteKaydet = async () => {
  if (!receteForm.value.ad?.trim() || !receteForm.value.urunId) {
    toastBildirim.uyari(t('uretim.receteZorunlu'))
    return
  }
  const govde = {
    ad: receteForm.value.ad,
    urunId: receteForm.value.urunId,
    fireOrani: receteForm.value.fireOrani,
    bazMiktar: receteForm.value.bazMiktar,
    bazBirim: receteForm.value.bazBirim || null,
    aktif: receteForm.value.aktif,
    kalemler: receteForm.value.kalemler.filter((k) => k.hammaddeId && k.miktar > 0)
  }
  kaydediliyor.value = true
  try {
    if (receteForm.value.id) {
      await uretimAPI.receteGuncelle(receteForm.value.id, govde)
    } else {
      await uretimAPI.receteOlustur(govde)
    }
    toastBildirim.basarili(t('uretim.receteKaydedildi'))
    receteDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.receteKaydedilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}

const receteSil = (r) => {
  confirm.require({
    message: t('uretim.receteSilOnay'),
    header: t('uretim.title'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await uretimAPI.receteSil(r.id)
        toastBildirim.basarili(t('uretim.receteSilindi'))
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('uretim.silinemedi'))
      }
    }
  })
}

const receteExcelIndir = async () => {  receteExcelYukleniyor.value = true
  try {
    const res = await excelAPI.uretimReceteler(receteParams())
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `uretim-receteler-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('uretim.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('uretim.excelHata'))
  } finally {
    receteExcelYukleniyor.value = false
  }
}

// ---- Üretim hatları (iş merkezleri) ----
const hatDialogAc = (h) => {
  hatForm.value = h
    ? {
        id: h.id,
        ad: h.ad || '',
        aciklama: h.aciklama || '',
        kapasite: h.kapasite || 5,
        aktif: h.aktif !== false
      }
    : bosHatForm()
  hatDialog.value = true
}

const hatKaydet = async () => {
  if (!hatForm.value.ad?.trim()) {
    toastBildirim.uyari(t('uretim.hatAdZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    const govde = {
      ad: hatForm.value.ad.trim(),
      aciklama: hatForm.value.aciklama,
      kapasite: hatForm.value.kapasite,
      aktif: hatForm.value.aktif
    }
    if (hatForm.value.id) await uretimAPI.hatGuncelle(hatForm.value.id, govde)
    else await uretimAPI.hatOlustur(govde)
    toastBildirim.basarili(t('uretim.hatKaydedildi'))
    hatDialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.hatKaydedilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}

const hatSil = (h) => {
  confirm.require({
    message: t('uretim.hatSilOnay', { ad: h.ad }),
    header: t('uretim.title'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await uretimAPI.hatSil(h.id)
        toastBildirim.basarili(t('uretim.hatSilindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('uretim.hatSilinemedi'))
      }
    }
  })
}

const hatAtaDialogAc = (e) => {
  seciliEmir.value = e
  hatAtaSecim.value = e.hatId || null
  hatAtaDialog.value = true
}

const hatAtaKaydet = async () => {
  kaydediliyor.value = true
  try {
    await uretimAPI.emirHatAta(seciliEmir.value.id, hatAtaSecim.value || null)
    toastBildirim.basarili(t('uretim.hatAtandi'))
    hatAtaDialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.hatAtanamadi'))
  } finally {
    kaydediliyor.value = false
  }
}

const analizMax = computed(() => {
  const degerler = (analiz.value?.trend || []).map((x) => Number(x.uretilen) || 0)
  return Math.max(1, ...degerler)
})
const barYuzde = (v) => Math.min(100, Math.round(((Number(v) || 0) / analizMax.value) * 100))
const donemEtiket = (donem) => {
  if (!donem) return '-'
  const [y, m] = String(donem).split('-').map(Number)
  if (!y || !m) return donem
  return new Date(y, m - 1, 1).toLocaleDateString(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    month: 'short',
    year: 'numeric'
  })
}

// ---- Takvim / Gantt ----
const tarihYerel = (v) => {
  if (!v) return null
  const [y, m, d] = String(v).slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d)
}
const takvimGunSayisi = computed(
  () => new Date(takvimAy.value.getFullYear(), takvimAy.value.getMonth() + 1, 0).getDate()
)
const takvimGunleri = computed(() => Array.from({ length: takvimGunSayisi.value }, (_, i) => i + 1))
const takvimEtiket = computed(() =>
  takvimAy.value.toLocaleDateString(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    month: 'long',
    year: 'numeric'
  })
)
const takvimAyBas = computed(() => new Date(takvimAy.value.getFullYear(), takvimAy.value.getMonth(), 1))
const takvimAyBit = computed(
  () => new Date(takvimAy.value.getFullYear(), takvimAy.value.getMonth(), takvimGunSayisi.value)
)
const cubukHesapla = (e) => {
  const bas0 = tarihYerel(e.planlananBaslangic) || tarihYerel(e.planlananBitis)
  const bit0 = tarihYerel(e.planlananBitis) || bas0
  if (!bas0 || !bit0) return null
  const bas = bas0 < takvimAyBas.value ? takvimAyBas.value : bas0
  const bit = bit0 > takvimAyBit.value ? takvimAyBit.value : bit0
  if (bas > bit) return null
  const gun = takvimGunSayisi.value
  return {
    left: ((bas.getDate() - 1) / gun) * 100,
    width: ((bit.getDate() - bas.getDate() + 1) / gun) * 100,
    gunSayisi: Math.round((bit - bas) / 86400000) + 1
  }
}
const takvimSatirlari = computed(() =>
  takvim.value
    .map((emir) => ({ emir, bar: cubukHesapla(emir) }))
    .filter((x) => x.bar)
)
const ganttAria = (satir) =>
  t('uretim.ganttAria', {
    id: satir.emir.id,
    urun: satir.emir.urunAd || '-',
    baslangic: tarih(satir.emir.planlananBaslangic) || '-',
    bitis: tarih(satir.emir.planlananBitis) || '-',
    durum: durumAdi(satir.emir.durum)
  })
const durumSinif = (d) =>
  ({ TASLAK: 'taslak', URETIMDE: 'uretimde', TAMAMLANDI: 'tamamlandi', IPTAL: 'iptal' })[d] || 'taslak'

const takvimYukle = async () => {
  takvimYukleniyor.value = true
  try {
    const r = await uretimAPI.takvim({
      baslangic: tarihIso(takvimAyBas.value),
      bitis: tarihIso(takvimAyBit.value)
    })
    takvim.value = r.data || []
    takvimYuklendi.value = true
  } catch (err) {
    takvim.value = []
    toastBildirim.hata(err?.response?.data?.message || t('uretim.takvimYuklenemedi'))
  } finally {
    takvimYukleniyor.value = false
  }
}

const takvimAyDegistir = (delta) => {
  takvimAy.value = new Date(takvimAy.value.getFullYear(), takvimAy.value.getMonth() + delta, 1)
  takvimYukle()
}

const takvimBugun = () => {
  const bugun = new Date()
  takvimAy.value = new Date(bugun.getFullYear(), bugun.getMonth(), 1)
  takvimYukle()
}

const analizYukle = async () => {
  analizYukleniyor.value = true
  try {
    const p = {}
    const [b, s] = analizAralik.value || []
    if (b) p.baslangic = tarihIso(b)
    if (s) p.bitis = tarihIso(s)
    const r = await uretimAPI.analiz(p)
    analiz.value = r.data
    analizYuklendi.value = true
  } catch (err) {
    analiz.value = null
    toastBildirim.hata(err?.response?.data?.message || t('uretim.analizYuklenemedi'))
  } finally {
    analizYukleniyor.value = false
  }
}

const analizExcelIndir = async () => {
  emirExcelYukleniyor.value = true
  try {
    const p = {}
    const [b, s] = analizAralik.value || []
    if (b) p.baslangic = tarihIso(b)
    if (s) p.bitis = tarihIso(s)
    const res = await excelAPI.uretimEmirleri(p)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `uretim-analiz-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('uretim.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('uretim.excelHata'))
  } finally {
    emirExcelYukleniyor.value = false
  }
}

const ihtiyacHesapla = async () => {
  if (!planForm.value.urunId || !planForm.value.miktar || planForm.value.miktar <= 0) {
    toastBildirim.uyari(t('uretim.ihtiyacIcinUrunSec'))
    return
  }
  ihtiyacYukleniyor.value = true
  try {
    const res = await uretimAPI.ihtiyac(planForm.value.urunId, planForm.value.miktar)
    ihtiyac.value = res.data
  } catch (err) {
    ihtiyac.value = null
    toastBildirim.hata(err?.response?.data?.message || t('uretim.receteBulunamadi'))
  } finally {
    ihtiyacYukleniyor.value = false
  }
}

const planlamadanEmirOlustur = () => {
  emirDialogAc({
    urunId: planForm.value.urunId,
    miktar: planForm.value.miktar,
    oncelik: 'NORMAL'
  })
}

const tarihIso = (d) => {
  if (!d) return null
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  const ay = String(date.getMonth() + 1).padStart(2, '0')
  const gun = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${ay}-${gun}`
}

onMounted(() => {
  yukle()
  if (aktifSekme.value === 5) analizYukle()
  if (aktifSekme.value === 6) takvimYukle()
})
</script>

<style scoped>
.uretim-sayfasi {
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

.tablo-iskelet {
  padding: 4px 0;
}
.kpi-iskelet {
  min-width: 0;
}

.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
}

.kpi-tiklanabilir {
  cursor: pointer;
}
.kpi-tiklanabilir.kpi-aktif {
  border-color: var(--accent-border, rgba(245, 158, 11, 0.55));
  box-shadow: 0 0 0 2px var(--accent-soft, rgba(245, 158, 11, 0.18));
}
.kpi-tiklanabilir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.emir-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filtre-arama {
  min-width: 220px;
  flex: 1 1 220px;
  max-width: 320px;
}
.filtre-select {
  min-width: 150px;
}
.filtre-tarih {
  min-width: 150px;
}
.arac-bosluk {
  flex: 1 1 auto;
}

.emir-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.emir-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.emir-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.emir-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.emir-cip i {
  font-size: 12px;
}

.emir-mobil {
  display: none;
}
.mobil-bos {
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 0;
}
.emir-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.emir-mobil-kart.tiklanabilir {
  cursor: pointer;
}
.emk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.emk-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.emk-taglar {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.emk-satir {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .emir-masa {
    display: none;
  }
  .emir-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
}

.sekme-arac {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.planlama-kart,
.rapor-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.planlama-form {
  display: grid;
  grid-template-columns: 2fr 1fr auto;
  gap: 12px;
  align-items: end;
}
.planlama-butonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ihtiyac-ozet {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.ihtiyac-toplam {
  font-size: 13px;
  color: var(--text-secondary);
}
.eksik-vurgu {
  color: #ef4444;
  font-weight: 700;
}

.rapor-kart h3,
.rapor-kart h4 {
  margin: 0;
  font-size: 15px;
}
.rapor-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.dagilim {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dagilim-satir {
  display: grid;
  grid-template-columns: 120px 1fr 48px;
  align-items: center;
  gap: 12px;
}
.dagilim-etiket {
  font-size: 13px;
  color: var(--text-secondary);
}
.dagilim-bar {
  height: 12px;
  border-radius: 999px;
  background: var(--border);
  overflow: hidden;
}
.dagilim-dolu {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}
.dagilim-deger {
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
.rapor-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.rapor-metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
}
.rapor-metric-etiket {
  font-size: 12px;
  color: var(--text-secondary);
}
.rapor-metric-deger {
  font-size: 18px;
  font-weight: 700;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
}
.field-row {
  display: flex;
  gap: 12px;
}
.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.field-switch {
  justify-content: flex-end;
}
.switch-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.w-full {
  width: 100%;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}

/* ---- Üretim hatları ---- */
.hat-baslik {
  margin: 0;
}
.hat-kartlari {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}
.hat-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.hat-kart:hover {
  border-color: var(--accent-border);
}
.hat-kart.pasif {
  opacity: 0.65;
}
.hat-ust {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hat-ad {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.hat-aciklama {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.hat-doluluk-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}
.hat-doluluk {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}
.hat-doluluk-dolu {
  height: 100%;
  border-radius: 999px;
  background: var(--accent);
  transition: width var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.hat-doluluk-dolu.dolu {
  background: var(--danger, #ef4444);
}
.hat-metrikler {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-secondary);
}
.hat-metrikler i {
  font-size: 11px;
  margin-right: 2px;
}
.hat-ata-ozet {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
}
.field-inline {
  flex-direction: row !important;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.metin-gecikmis {
  color: var(--danger, #ef4444);
  font-weight: 600;
}

/* ---- Takvim / Gantt ---- */
.takvim-ay {
  font-weight: 700;
  min-width: 130px;
  text-align: center;
  text-transform: capitalize;
}
.ay-gezinme {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.takvim-lejant {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-secondary);
}
.lejant {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.lejant-nokta {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  display: inline-block;
}
.lejant-nokta.taslak {
  background: #64748b;
}
.lejant-nokta.uretimde {
  background: #3b82f6;
}
.lejant-nokta.tamamlandi {
  background: #10b981;
}
.gantt {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  overflow: auto;
}
.gantt-baslik,
.gantt-satir {
  display: grid;
  grid-template-columns: 200px minmax(620px, 1fr);
  align-items: center;
  min-width: 820px;
}
.gantt-baslik {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border);
  font-size: 10px;
  color: var(--text-muted);
}
.gantt-gunler {
  display: grid;
}
.gantt-gunler span {
  text-align: center;
  padding: 4px 0;
}
.gantt-satir {
  border-bottom: 1px solid var(--border);
}
.gantt-satir:last-child {
  border-bottom: none;
}
.gantt-etiket {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  padding: 8px 10px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.gantt-etiket strong {
  font-size: 12px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gantt-etiket small {
  font-size: 10.5px;
  color: var(--text-muted);
}
.gantt-izgara {
  position: relative;
  display: grid;
  height: 40px;
}
.gantt-gun-cizgi {
  border-right: 1px dashed var(--border);
  height: 100%;
}
.gantt-cubuk {
  position: absolute;
  top: 8px;
  height: 24px;
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
  padding: 0 6px;
  overflow: hidden;
  white-space: nowrap;
  text-align: left;
}
.gantt-cubuk.taslak {
  background: #64748b;
}
.gantt-cubuk.uretimde {
  background: #3b82f6;
}
.gantt-cubuk.tamamlandi {
  background: #10b981;
}
.gantt-cubuk.iptal {
  background: #ef4444;
}
.gantt-cubuk:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ---- Analiz ---- */
.analiz-tarih {
  min-width: 240px;
}
.analiz-kpi {
  margin-top: 12px;
}
.analiz-kart {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  background: var(--bg-card);
  margin-top: 12px;
}
.analiz-kart h3 {
  margin: 0 0 10px;
  font-size: 14px;
}
.trend-liste {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.trend-satir {
  display: grid;
  grid-template-columns: 90px minmax(120px, 1fr) 150px 130px;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}
.trend-donem {
  color: var(--text-secondary);
  font-weight: 600;
}
.trend-bar {
  display: flex;
  height: 10px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
  gap: 2px;
}
.trend-dolu {
  height: 100%;
  border-radius: 999px;
}
.trend-dolu.uretim {
  background: var(--accent);
}
.trend-dolu.fire {
  background: #ef4444;
}
.trend-deger,
.trend-maliyet {
  color: var(--text-secondary);
  white-space: nowrap;
  text-align: right;
}
@media (max-width: 768px) {
  .trend-satir {
    grid-template-columns: 70px 1fr;
  }
  .trend-deger,
  .trend-maliyet {
    display: none;
  }
}

.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-hammadde {
  flex: 2;
  min-width: 0;
}
.kalem-miktar {
  width: 100px;
}
.kalem-birim {
  width: 90px;
}
.kalem-fire {
  width: 110px;
}

/* Reçete kalemleri: etiketli grid + canlı ölçek önizlemesi. */
.kalem-baslik-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.olcek-girdi {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.olcek-etiket {
  white-space: nowrap;
}
.olcek-input {
  width: 110px;
}
.olcek-birim {
  color: var(--text-muted);
  min-width: 20px;
}
.kalem-grid {
  display: grid;
  grid-template-columns: minmax(160px, 2fr) 96px 86px 100px 110px 40px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-grid-baslik {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.kalem-hesaplanan {
  font-weight: 600;
  color: var(--accent);
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.baz-satir {
  display: flex;
  gap: 8px;
}
.baz-miktar {
  width: 110px;
}
.baz-birim {
  flex: 1;
  min-width: 0;
}
@media (max-width: 768px) {
  .kalem-grid {
    grid-template-columns: minmax(120px, 1fr) 80px;
    grid-auto-rows: auto;
  }
  .kalem-grid-baslik {
    display: none;
  }
  .kalem-hesaplanan {
    grid-column: 2;
  }
}

.tamamla-ozet {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.planlanan-hucre {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.planlanan-hucre i {
  font-size: 10px;
}
.planlanan-hucre.gecikti {
  color: #ef4444;
  font-weight: 600;
}
.planlanan-hucre.gecikti i {
  font-size: 12px;
}

@media (max-width: 768px) {
  .planlama-form {
    grid-template-columns: 1fr;
  }
  .field-row {
    flex-direction: column;
  }
}
</style>
