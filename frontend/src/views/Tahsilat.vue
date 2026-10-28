<template>
  <div class="tahsilat-sayfasi">
    <PageHeader
      :title="t('tahsilat.title')"
      :description="t('tahsilat.aciklama')"
      icon="pi pi-money-bill"
    >
      <template #actions>
        <Button
          :label="t('tahsilat.excelYaslandirma')"
          icon="pi pi-file-excel"
          outlined
          :loading="excelYukleniyor"
          @click="excelYaslandirmaIndir"
        />
        <Button
          :label="t('tahsilat.tahsilatGir')"
          icon="pi pi-plus"
          severity="success"
          @click="tahsilatGir()"
        />
      </template>
    </PageHeader>

    <!-- KPI kartları: tıklanabilir, ilgili sekmeye/filtreye götürür -->
    <div class="kpi-satiri">
      <template v-if="ozetIskelet">
        <div
          v-for="i in 5"
          :key="i"
          class="kpi-iskelet"
        >
          <SkeletonLoader height="94px" />
        </div>
      </template>
      <template v-else>
        <button
          type="button"
          class="kpi-tikla"
          :aria-pressed="kpiAktif('toplam')"
          @click="kpiTikla('toplam')"
        >
          <KpiKart
            :baslik="t('tahsilat.toplamAlacak')"
            :deger="ozet?.toplamAlacak || 0"
            ikon="pi pi-wallet"
            renk="#3b82f6"
          >
            <template #alt>
              {{ t('tahsilat.cariSayisi', { n: (ozet?.cariler || []).length }) }}
            </template>
          </KpiKart>
        </button>
        <button
          type="button"
          class="kpi-tikla"
          :aria-pressed="kpiAktif('gecikmis')"
          @click="kpiTikla('gecikmis')"
        >
          <KpiKart
            :baslik="t('tahsilat.vadesiGecmis')"
            :deger="ozet?.vadesiGecmisToplam || 0"
            ikon="pi pi-exclamation-triangle"
            renk="#ef4444"
            trend-iyi-yonu="asagi"
          >
            <template #alt>
              {{ t('tahsilat.gecikmisCariAcikFatura') }}
            </template>
          </KpiKart>
        </button>
        <button
          type="button"
          class="kpi-tikla"
          :aria-pressed="kpiAktif('yaklasan')"
          @click="kpiTikla('yaklasan')"
        >
          <KpiKart
            :baslik="t('tahsilat.otuzGunIcinde')"
            :deger="ozet?.vadesiYaklasanToplam || 0"
            ikon="pi pi-calendar-clock"
            renk="#f59e0b"
          />
        </button>
        <button
          type="button"
          class="kpi-tikla"
          :aria-pressed="kpiAktif('buAy')"
          @click="kpiTikla('buAy')"
        >
          <KpiKart
            :baslik="t('tahsilat.kpiBuAyTahsilat')"
            :deger="buAyTahsilat"
            ikon="pi pi-chart-line"
            renk="#10b981"
          >
            <template #alt>
              {{ t('tahsilat.tahsilatSayisiAlt', { n: buAyPerformans?.tahsilatSayisi || 0 }) }}
            </template>
          </KpiKart>
        </button>
        <button
          type="button"
          class="kpi-tikla"
          :aria-pressed="kpiAktif('hedef')"
          @click="kpiTikla('hedef')"
        >
          <KpiKart
            :baslik="t('tahsilat.kpiHedefGerceklesme')"
            :deger="hedefGerceklesme || 0"
            ikon="pi pi-percentage"
            :renk="hedefGerceklesme != null && hedefGerceklesme >= 100 ? '#10b981' : '#8b5cf6'"
            :para-birimi="false"
          >
            <template #deger>
              {{ hedefGerceklesme != null ? hedefGerceklesme + '%' : '-' }}
            </template>
            <template #alt>
              {{ buAyPerformans?.hedef ? formatCurrency(buAyPerformans.hedef) : t('tahsilat.kpiHedefYok') }}
            </template>
          </KpiKart>
        </button>
      </template>
    </div>

    <p
      class="sr-only"
      aria-live="polite"
    >
      {{ canliDuyuru }}
    </p>

    <TabView v-model:active-index="aktifSekme">
      <!-- 1) YAŞLANDIRMA -->
      <TabPanel :header="t('tahsilat.tabYaslandirma')">
        <div class="tahsilat-arac">
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="q"
              :placeholder="t('tahsilat.aramaYer')"
              :aria-label="t('tahsilat.aramaYer')"
              @input="aramaDegisti"
            />
          </IconField>
          <Select
            v-model="aralik"
            :options="aralikSecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('tahsilat.aralikFiltre')"
            :aria-label="t('tahsilat.aralikFiltre')"
            @change="filtreDegisti"
          />
          <InputNumber
            v-model="minTutar"
            class="arac-secim min-tutar"
            :min="0"
            :placeholder="t('tahsilat.minTutar')"
            :aria-label="t('tahsilat.minTutar')"
            @input="aramaDegisti"
          />
          <button
            type="button"
            class="gecikmis-sec"
            :class="{ aktif: sadeceGecikmis }"
            :aria-pressed="sadeceGecikmis"
            @click="gecikmisToggle"
          >
            <i class="pi pi-exclamation-triangle" />
            {{ t('tahsilat.sadeceGecikmis') }}
          </button>
          <span class="arac-bosluk" />
          <Button
            v-if="filtreVar"
            :label="t('tahsilat.filtreTemizle')"
            icon="pi pi-filter-slash"
            text
            size="small"
            @click="filtreTemizle"
          />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            text
            size="small"
            :loading="yukleniyor"
            @click="yukle"
          />
        </div>

        <SkeletonLoader
          v-if="listeIskelet"
          class="tablo-iskelet"
          :count="6"
          height="46px"
        />

        <AppDataTable
          v-else
          v-model:selection="seciliCariler"
          v-model:expanded-rows="genisletilenler"
          :value="ozet?.cariler || []"
          data-key="cariId"
          selection-mode="multiple"
          striped-rows
          :paginator="false"
          class="yaslandirma-masa"
          :empty-message="t('tahsilat.acikAlacakYok')"
        >
          <template #batch-actions>
            <Button
              :label="t('tahsilat.topluEposta')"
              icon="pi pi-envelope"
              size="small"
              @click="topluEpostaAc"
            />
          </template>
          <Column
            selection-mode="multiple"
            header-style="width: 3rem"
          />
          <Column
            expander
            style="width: 3rem"
          />
          <Column
            field="cariAd"
            :header="t('tahsilat.cariHesap')"
            sortable
          >
            <template #body="{ data }">
              <button
                type="button"
                class="cari-link"
                :aria-label="t('tahsilat.detayAc')"
                @click="cariAc(data)"
              >
                <strong>{{ data.cariAd }}</strong>
                <span class="cari-alt">{{ t('tahsilat.acikFatura', { n: data.faturaSayisi }) }}</span>
              </button>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.toplamAlacak')"
            sortable
          >
            <template #body="{ data }">
              <strong class="gizli-veri">{{ formatCurrency(data.toplamAlacak) }}</strong>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.gecikmis')"
            sortable
          >
            <template #body="{ data }">
              <span
                class="gizli-veri"
                :class="data.gecikmisAlacak > 0 ? 'negatif' : 'muted'"
              >{{ formatCurrency(data.gecikmisAlacak) }}</span>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.yaslandirma')"
            sortable
          >
            <template #body="{ data }">
              <Tag
                :value="data.aralik"
                :severity="aralikSeverity(data.aralik)"
              />
            </template>
          </Column>
          <Column
            :header="t('tahsilat.islem')"
            style="width: 120px"
          >
            <template #body="{ data }">
              <div class="islem-grup">
                <Button
                  icon="pi pi-money-bill"
                  class="p-button-rounded p-button-text p-button-success"
                  :aria-label="t('tahsilat.tahsilatAl')"
                  :title="t('tahsilat.tahsilatAl')"
                  @click="tahsilatGir(data)"
                />
                <Button
                  icon="pi pi-search"
                  class="p-button-rounded p-button-text"
                  :aria-label="t('tahsilat.detayAc')"
                  :title="t('tahsilat.detayAc')"
                  @click="cariAc(data)"
                />
                <SatirEylemleri
                  :baslik="data.cariAd"
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="tahsilatEylemleri(data)"
                />
              </div>
            </template>
          </Column>

          <template #expansion="slotProps">
            <div class="fatura-liste">
              <div
                v-for="f in slotProps.data.faturalar"
                :key="f.faturaId"
                class="fatura-satir"
              >
                <div class="fatura-no">
                  <i class="pi pi-file" /> {{ f.faturaNumarasi }}
                </div>
                <div class="fatura-vade">
                  {{ t('tahsilat.vade') }}: {{ formatDate(f.vadeTarihi) }}
                  <span
                    v-if="f.gecikmeGunu > 0"
                    class="gecikme-pill"
                  >{{ t('tahsilat.gunGecikti', { n: f.gecikmeGunu }) }}</span>
                  <span
                    v-else
                    class="vade-pill"
                  >{{ t('tahsilat.gunKaldi', { n: Math.abs(f.gecikmeGunu) }) }}</span>
                </div>
                <div class="fatura-tutar gizli-veri">
                  {{ formatCurrency(f.kalanTutar) }}
                </div>
              </div>
              <div
                v-if="!slotProps.data.faturalar?.length"
                class="bos-satir"
              >
                {{ t('tahsilat.faturaDetayiYok') }}
              </div>
            </div>
          </template>

          <template #empty>
            <EmptyState
              v-if="!yukleniyor"
              icon="pi pi-check-circle"
              :message="filtreVar ? t('tahsilat.filtreYok') : t('tahsilat.acikAlacakYok')"
              :sub-message="filtreVar ? null : t('tahsilat.acikAlacakYokHint')"
              :action-label="filtreVar ? t('tahsilat.filtreTemizle') : null"
              action-icon="pi pi-filter-slash"
              @action="filtreTemizle"
            />
          </template>
        </AppDataTable>

        <!-- Mobil kart görünümü -->
        <div class="yaslandirma-mobil">
          <EmptyState
            v-if="!yukleniyor && !(ozet?.cariler || []).length"
            icon="pi pi-check-circle"
            :message="filtreVar ? t('tahsilat.filtreYok') : t('tahsilat.acikAlacakYok')"
          />
          <div
            v-for="c in ozet?.cariler || []"
            :key="c.cariId"
            class="cari-mobil-kart"
          >
            <div class="cm-ust">
              <strong>{{ c.cariAd }}</strong>
              <Tag
                :value="c.aralik"
                :severity="aralikSeverity(c.aralik)"
              />
            </div>
            <div class="cm-satir">
              <span>{{ t('tahsilat.toplamAlacak') }}</span>
              <strong class="gizli-veri">{{ formatCurrency(c.toplamAlacak) }}</strong>
            </div>
            <div class="cm-satir">
              <span>{{ t('tahsilat.gecikmis') }}</span>
              <strong
                class="gizli-veri"
                :class="c.gecikmisAlacak > 0 ? 'negatif' : 'muted'"
              >{{ formatCurrency(c.gecikmisAlacak) }}</strong>
            </div>
            <div class="cm-aksiyonlar">
              <Button
                :label="t('tahsilat.tahsilatAl')"
                icon="pi pi-money-bill"
                size="small"
                severity="success"
                @click="tahsilatGir(c)"
              />
              <Button
                icon="pi pi-search"
                size="small"
                text
                :aria-label="t('tahsilat.detayAc')"
                @click="cariAc(c)"
              />
            </div>
          </div>
        </div>
      </TabPanel>

      <!-- 2) TAHSİLAT GEÇMİŞİ -->
      <TabPanel :header="t('tahsilat.tabGecmis')">
        <div class="tahsilat-arac">
          <DatePicker
            :model-value="gecmisBaslangic"
            show-icon
            date-format="dd.mm.yy"
            class="arac-tarih"
            :placeholder="t('tahsilat.baslangic')"
            :aria-label="t('tahsilat.baslangic')"
            @update:model-value="gecmisTarihSec('baslangic', $event)"
          />
          <DatePicker
            :model-value="gecmisBitis"
            show-icon
            date-format="dd.mm.yy"
            class="arac-tarih"
            :placeholder="t('tahsilat.bitis')"
            :aria-label="t('tahsilat.bitis')"
            @update:model-value="gecmisTarihSec('bitis', $event)"
          />
          <Select
            v-model="gecmisCariId"
            :options="cariSecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            filter
            class="arac-secim"
            :placeholder="t('tahsilat.cariFiltre')"
            :aria-label="t('tahsilat.cariFiltre')"
            @change="gecmisFiltreDegisti"
          />
          <Select
            v-model="gecmisYontem"
            :options="yontemSecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('tahsilat.odemeYontemi')"
            :aria-label="t('tahsilat.odemeYontemi')"
            @change="gecmisFiltreDegisti"
          />
          <Select
            v-model="gecmisDurum"
            :options="durumSecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('tahsilat.durumFiltre')"
            :aria-label="t('tahsilat.durumFiltre')"
            @change="gecmisFiltreDegisti"
          />
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="gecmisQ"
              :placeholder="t('tahsilat.aramaYer')"
              :aria-label="t('tahsilat.aramaYer')"
              @input="gecmisAramaDegisti"
            />
          </IconField>
          <span class="arac-bosluk" />
          <Button
            v-if="gecmisFiltreVar"
            :label="t('tahsilat.filtreTemizle')"
            icon="pi pi-filter-slash"
            text
            size="small"
            @click="gecmisFiltreTemizle"
          />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            text
            size="small"
            :loading="gecmisYukleniyor"
            @click="gecmisYukle"
          />
          <Button
            :label="t('tahsilat.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="excelGecmisYukleniyor"
            @click="excelGecmisIndir"
          />
        </div>

        <SkeletonLoader
          v-if="gecmisIskelet"
          class="tablo-iskelet"
          :count="6"
          height="46px"
        />

        <AppDataTable
          v-else
          v-model:expanded-rows="gecmisGenisletilenler"
          :value="gecmis"
          lazy
          data-key="id"
          :total-records="gecmisToplam"
          :rows="gecmisBoyut"
          :loading="gecmisYukleniyor"
          striped-rows
          gorunum-anahtari="tahsilat-gecmis"
          :empty-message="t('tahsilat.gecmisYok')"
          @page="gecmisSayfaDegisti"
          @sort="gecmisSiralaDegisti"
        >
          <Column
            expander
            style="width: 3rem"
          />
          <Column
            field="tarih"
            :header="t('common.date')"
            sortable
            style="width: 120px"
          >
            <template #body="{ data }">
              {{ formatDate(data.tarih) }}
            </template>
          </Column>
          <Column
            field="cariAd"
            :header="t('tahsilat.cariHesap')"
            sortable
          />
          <Column
            field="tutar"
            :header="t('common.amount')"
            sortable
            style="width: 150px"
          >
            <template #body="{ data }">
              <strong
                class="gizli-veri"
                :class="data.durum === 'IPTAL' ? 'iptal-tutar' : 'pozitif'"
              >{{ formatCurrency(data.tutar) }}</strong>
            </template>
          </Column>
          <Column
            field="odemeYontemi"
            :header="t('tahsilat.odemeYontemi')"
            style="width: 130px"
          >
            <template #body="{ data }">
              <Tag
                v-if="data.odemeYontemi"
                :value="odemeYontemiLabel(data.odemeYontemi)"
                :severity="odemeYontemiSeverity(data.odemeYontemi)"
              />
              <span
                v-else
                class="muted"
              >-</span>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.kasaBanka')"
            style="width: 150px"
          >
            <template #body="{ data }">
              <span class="muted">{{ data.kasaAd || data.bankaAd || '-' }}</span>
            </template>
          </Column>
          <Column
            field="durum"
            :header="t('tahsilat.durumFiltre')"
            style="width: 110px"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum === 'IPTAL' ? t('tahsilat.durumIptal') : t('tahsilat.durumAktif')"
                :severity="data.durum === 'IPTAL' ? 'danger' : 'success'"
              />
            </template>
          </Column>
          <Column
            :header="t('tahsilat.satirSayisi')"
            style="width: 80px"
          >
            <template #body="{ data }">
              {{ data.satirSayisi ?? (data.dagilimlar || []).length }}
            </template>
          </Column>
          <Column
            :header="t('tahsilat.islem')"
            style="width: 110px"
          >
            <template #body="{ data }">
              <div class="islem-grup">
                <Button
                  v-if="data.durum === 'AKTIF' && iptalYetkili"
                  icon="pi pi-undo"
                  class="p-button-rounded p-button-text p-button-danger"
                  :aria-label="t('tahsilat.iptal')"
                  :title="t('tahsilat.iptal')"
                  @click="iptalAc(data)"
                />
                <span
                  v-if="data.durum === 'IPTAL' && data.iptalGerekce"
                  class="iptal-not"
                  :title="data.iptalGerekce"
                >{{ t('tahsilat.iptal') }}</span>
              </div>
            </template>
          </Column>

          <template #expansion="slotProps">
            <div class="dagilim-liste">
              <div
                v-if="slotProps.data.durum === 'IPTAL' && slotProps.data.iptalGerekce"
                class="iptal-bant"
              >
                <i class="pi pi-info-circle" />
                {{ t('tahsilat.iptalGerekce') }}: {{ slotProps.data.iptalGerekce }}
              </div>
              <div
                v-for="d in slotProps.data.dagilimlar || []"
                :key="d.hareketId"
                class="dagilim-satir"
              >
                <div class="dagilim-fatura">
                  <i class="pi pi-file" />
                  {{ d.faturaNumarasi || t('tahsilat.avans') }}
                </div>
                <div class="dagilim-aciklama">
                  {{ d.aciklama || '-' }}
                </div>
                <div class="dagilim-tutar gizli-veri">
                  {{ formatCurrency(d.tutar) }}
                </div>
                <Tag
                  v-if="d.iptal"
                  :value="t('tahsilat.durumIptal')"
                  severity="danger"
                />
              </div>
              <div
                v-if="!(slotProps.data.dagilimlar || []).length"
                class="bos-satir"
              >
                {{ t('tahsilat.dagilimYok') }}
              </div>
            </div>
          </template>

          <template #empty>
            <EmptyState
              v-if="!gecmisYukleniyor"
              icon="pi pi-history"
              :message="gecmisFiltreVar ? t('tahsilat.filtreYok') : t('tahsilat.gecmisYok')"
              :sub-message="gecmisFiltreVar ? null : t('tahsilat.gecmisYokHint')"
            />
          </template>
        </AppDataTable>
      </TabPanel>

      <!-- 3) HEDEF & PERFORMANS -->
      <TabPanel :header="t('tahsilat.tabPerformans')">
        <div class="tahsilat-arac">
          <Select
            v-model="perfYil"
            :options="yilSecenekleri"
            class="arac-secim"
            :aria-label="t('tahsilat.hedefYil')"
            @change="performansYukle"
          />
          <Select
            v-model="perfAy"
            :options="aySecenekleri"
            option-label="label"
            option-value="value"
            class="arac-secim"
            :aria-label="t('tahsilat.hedefAy')"
            @change="performansYukle"
          />
          <span class="arac-bosluk" />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            text
            size="small"
            :loading="performansYukleniyor"
            @click="performansYukle"
          />
          <Button
            v-if="hedefDuzenlenebilir"
            :label="t('tahsilat.hedefDuzenle')"
            icon="pi pi-pencil"
            size="small"
            @click="hedefDuzenle"
          />
        </div>

        <SkeletonLoader
          v-if="performansIskelet"
          :count="2"
          height="120px"
        />

        <template v-else>
          <div class="performans-grid">
            <div class="perf-kart">
              <span class="perf-etiket">{{ t('tahsilat.performansHedef') }}</span>
              <strong class="perf-deger gizli-veri">{{ formatCurrency(performans?.hedef) }}</strong>
              <span
                v-if="!performans?.hedef"
                class="perf-uyari"
              >{{ t('tahsilat.hedefYok') }}</span>
            </div>
            <div class="perf-kart">
              <span class="perf-etiket">{{ t('tahsilat.performansTahsilEdilen') }}</span>
              <strong class="perf-deger gizli-veri">{{ formatCurrency(performans?.tahsilEdilen) }}</strong>
              <span class="perf-alt">{{ t('tahsilat.tahsilatSayisiAlt', { n: performans?.tahsilatSayisi || 0 }) }}</span>
            </div>
            <div class="perf-kart">
              <span class="perf-etiket">{{ t('tahsilat.performansGerceklesme') }}</span>
              <strong class="perf-deger">{{ performans?.gerceklesmeYuzde != null ? performans.gerceklesmeYuzde + '%' : '-' }}</strong>
              <div
                v-if="performans?.gerceklesmeYuzde != null"
                class="gerceklesme-bar"
                role="progressbar"
                :aria-valuenow="Math.min(Number(performans.gerceklesmeYuzde), 100)"
                aria-valuemin="0"
                aria-valuemax="100"
              >
                <div
                  class="gerceklesme-dolu"
                  :class="{ asim: Number(performans.gerceklesmeYuzde) >= 100 }"
                  :style="{ width: Math.min(Number(performans.gerceklesmeYuzde), 100) + '%' }"
                />
              </div>
            </div>
            <div class="perf-kart">
              <span class="perf-etiket">{{ t('tahsilat.performansVadesiGecmis') }}</span>
              <strong class="perf-deger gizli-veri negatif">{{ formatCurrency(performans?.vadesiGecmisToplam) }}</strong>
            </div>
          </div>

          <div class="trend-kart">
            <div class="trend-baslik">
              <span><i class="pi pi-chart-bar" /> {{ t('tahsilat.trendBaslik') }}</span>
              <div class="trend-efsane">
                <span class="efsane tahsilat">{{ t('tahsilat.trendTahsilat') }}</span>
                <span class="efsane hedef">{{ t('tahsilat.trendHedef') }}</span>
              </div>
            </div>
            <div
              v-if="(performans?.trend || []).length"
              class="trend-grafik"
            >
              <div
                v-for="(nokta, i) in performans.trend"
                :key="i"
                class="trend-kolon"
              >
                <div class="trend-cubuklar">
                  <div
                    class="trend-cubuk tahsilat"
                    :style="{ height: cubukYuksekligi(nokta.tahsilat) }"
                    :title="`${t('tahsilat.trendTahsilat')}: ${formatCurrency(nokta.tahsilat)}`"
                  />
                  <div
                    class="trend-cubuk hedef"
                    :style="{ height: cubukYuksekligi(nokta.hedef) }"
                    :title="`${t('tahsilat.trendHedef')}: ${formatCurrency(nokta.hedef)}`"
                  />
                </div>
                <span class="trend-ay">{{ ayAdi(nokta.ay) }}</span>
              </div>
            </div>
            <p
              v-else
              class="muted"
            >
              {{ t('tahsilat.trendYok') }}
            </p>
          </div>
        </template>
      </TabPanel>
    </TabView>

    <!-- Tahsilat girişi (mevcut akış korunur) -->
    <TahsilatGirDialog
      v-model:visible="tahsilatDialogAcik"
      :cariler="ozet?.cariler || []"
      :baslangic-cari-id="seciliCariId"
      @kaydedildi="kaydetSonrasi"
    />

    <!-- Cari drawer -->
    <TahsilatCariDrawer
      v-model:visible="drawerAcik"
      :cari="drawerCari"
      :son-tahsilatlar="sonTahsilatlar"
      :yukleniyor="sonTahsilatYukleniyor"
      @tahsilat="drawerTahsilat"
      @whatsapp="whatsappAc"
      @eposta="hatirlat"
    />

    <!-- Toplu e-posta onay + sonuç -->
    <Dialog
      v-model:visible="topluDialog"
      :header="t('tahsilat.topluEpostaBaslik')"
      modal
      :style="{ width: '460px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <template v-if="!topluSonuc">
        <p class="dialog-metin">
          {{ t('tahsilat.topluEpostaOnay', { n: seciliEpostali.length }) }}
        </p>
        <p
          v-if="seciliEpostasiz.length"
          class="dialog-uyari"
        >
          <i class="pi pi-exclamation-triangle" />
          {{ t('tahsilat.topluEpostaAtlanacak', { n: seciliEpostasiz.length }) }}
        </p>
      </template>
      <template v-else>
        <div class="toplu-sonuc">
          <div class="sonuc-satir">
            <span>{{ t('tahsilat.topluEpostaGonderilen') }}</span>
            <strong class="pozitif">{{ topluSonuc.gonderilen ?? 0 }}</strong>
          </div>
          <div class="sonuc-satir">
            <span>{{ t('tahsilat.topluEpostaAtlanan') }}</span>
            <strong>{{ topluSonuc.atlanan ?? 0 }}</strong>
          </div>
          <div
            v-if="(topluSonuc.hatalar || []).length"
            class="sonuc-satir hatalar"
          >
            <span>{{ t('tahsilat.topluEpostaHatalar') }}</span>
            <strong class="negatif">{{ topluSonuc.hatalar.length }}</strong>
          </div>
          <ul
            v-if="(topluSonuc.hatalar || []).length"
            class="hata-liste"
          >
            <li
              v-for="(h, i) in topluSonuc.hatalar"
              :key="i"
            >
              {{ h }}
            </li>
          </ul>
        </div>
      </template>
      <template #footer>
        <Button
          :label="t('common.close')"
          text
          @click="topluDialog = false"
        />
        <Button
          v-if="!topluSonuc"
          :label="t('tahsilat.topluEpostaGonder')"
          icon="pi pi-send"
          :loading="topluGonderiliyor"
          :disabled="!seciliEpostali.length"
          @click="topluGonder"
        />
      </template>
    </Dialog>

    <!-- İptal gerekçesi -->
    <Dialog
      v-model:visible="iptalDialog"
      :header="t('tahsilat.iptalBaslik')"
      modal
      :style="{ width: '460px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p class="dialog-metin">
        {{ t('tahsilat.iptalAciklama') }}
      </p>
      <p
        v-if="iptalHedef"
        class="dialog-ozet"
      >
        {{ iptalHedef.cariAd }} · {{ formatDate(iptalHedef.tarih) }} · {{ formatCurrency(iptalHedef.tutar) }}
      </p>
      <FormField :label="t('tahsilat.iptalGerekce')">
        <Textarea
          v-model="iptalGerekce"
          rows="3"
          class="w-full"
          :placeholder="t('tahsilat.iptalGerekcePlaceholder')"
        />
      </FormField>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          text
          @click="iptalDialog = false"
        />
        <Button
          :label="t('tahsilat.iptal')"
          icon="pi pi-undo"
          severity="danger"
          :loading="iptalEdiliyor"
          @click="iptalKaydet"
        />
      </template>
    </Dialog>

    <!-- Hedef düzenleme -->
    <Dialog
      v-model:visible="hedefDialog"
      :header="t('tahsilat.hedefBaslik')"
      modal
      :style="{ width: '400px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="hedef-form">
        <p class="dialog-ozet">
          {{ ayAdi(perfAy) }} {{ perfYil }}
        </p>
        <FormField :label="t('tahsilat.hedefTutar')">
          <InputNumber
            v-model="hedefTutar"
            mode="currency"
            currency="TRY"
            :min="0"
            class="w-full"
          />
        </FormField>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          text
          @click="hedefDialog = false"
        />
        <Button
          :label="t('tahsilat.hedefKaydet')"
          icon="pi pi-check"
          :loading="hedefKaydediliyor"
          @click="hedefKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { tahsilatAPI, excelAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { formatCurrency, formatDate, getLocalDateString } from '../utils/format.js'
import KpiKart from '../components/KpiKart.vue'
import FormField from '../components/FormField.vue'
import TahsilatGirDialog from '../components/TahsilatGirDialog.vue'
import TahsilatCariDrawer from '../components/TahsilatCariDrawer.vue'

const { t, locale } = useI18n()
const route = useRoute()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()

// ---- Hafıza ----
const TAB_ANAHTAR = 'raspel_tahsilat_tab'
const FILTRE_ANAHTAR = 'raspel_tahsilat_filtre'
const kayitliFiltre = (() => {
  try {
    return JSON.parse(localStorage.getItem(FILTRE_ANAHTAR) || '{}') || {}
  } catch {
    return {}
  }
})()

const sekmeOku = () => {
  const n = Number(localStorage.getItem(TAB_ANAHTAR))
  return Number.isInteger(n) && n >= 0 && n <= 2 ? n : 0
}
const aktifSekme = ref(sekmeOku())

// ---- Yaşlandırma ----
const q = ref(kayitliFiltre.q || '')
const aralik = ref(kayitliFiltre.aralik || null)
const sadeceGecikmis = ref(!!kayitliFiltre.sadeceGecikmis)
const minTutar = ref(kayitliFiltre.minTutar ?? null)
const ozet = ref(null)
const yukleniyor = ref(false)
const ozetYuklendi = ref(false)
const seciliCariler = ref([])
const genisletilenler = ref([])
// Geçmiş sekmesindeki cari filtresi için filtresiz yüklemeden gelen liste.
const tumCariler = ref([])
const excelYukleniyor = ref(false)
let aramaZaman = null

// ---- Geçmiş ----
const gecmis = ref([])
const gecmisToplam = ref(0)
const gecmisYukleniyor = ref(false)
const gecmisYuklendi = ref(false)
const gecmisSayfa = ref(0)
const gecmisBoyut = ref(20)
const gecmisSirala = ref('tarih,desc')
const gecmisQ = ref(kayitliFiltre.gecmisQ || '')
const gecmisCariId = ref(kayitliFiltre.gecmisCariId || null)
const gecmisYontem = ref(kayitliFiltre.gecmisYontem || null)
const gecmisDurum = ref(kayitliFiltre.gecmisDurum || null)
const gecmisBaslangic = ref(kayitliFiltre.gecmisBaslangic ? new Date(kayitliFiltre.gecmisBaslangic) : null)
const gecmisBitis = ref(kayitliFiltre.gecmisBitis ? new Date(kayitliFiltre.gecmisBitis) : null)
const gecmisGenisletilenler = ref([])
const excelGecmisYukleniyor = ref(false)
let gecmisAramaZaman = null

// ---- Performans ----
const bugun = new Date()
const performans = ref(null)
const buAyPerformans = ref(null)
const performansYukleniyor = ref(false)
const performansYuklendi = ref(false)
const perfYil = ref(bugun.getFullYear())
const perfAy = ref(bugun.getMonth() + 1)
const hedefDialog = ref(false)
const hedefTutar = ref(0)
const hedefKaydediliyor = ref(false)

// ---- Diyaloglar ----
const tahsilatDialogAcik = ref(false)
const seciliCariId = ref(null)
const topluDialog = ref(false)
const topluGonderiliyor = ref(false)
const topluSonuc = ref(null)
const iptalDialog = ref(false)
const iptalHedef = ref(null)
const iptalGerekce = ref('')
const iptalEdiliyor = ref(false)

// ---- Drawer ----
const drawerAcik = ref(false)
const drawerCari = ref(null)
const sonTahsilatlar = ref([])
const sonTahsilatYukleniyor = ref(false)

// ---- Türetilen ----
const ozetIskelet = computed(() => !ozetYuklendi.value)
const listeIskelet = computed(() => yukleniyor.value && !(ozet.value?.cariler || []).length)
const gecmisIskelet = computed(() => gecmisYukleniyor.value && !gecmis.value.length)
const performansIskelet = computed(() => performansYukleniyor.value && !performans.value)
const filtreVar = computed(
  () => !!q.value?.trim() || !!aralik.value || sadeceGecikmis.value || minTutar.value != null
)
const gecmisFiltreVar = computed(
  () =>
    !!gecmisQ.value?.trim() ||
    gecmisCariId.value != null ||
    !!gecmisYontem.value ||
    !!gecmisDurum.value ||
    !!gecmisBaslangic.value ||
    !!gecmisBitis.value
)
const buAyTahsilat = computed(() => Number(buAyPerformans.value?.tahsilEdilen) || 0)
const hedefGerceklesme = computed(() => {
  const v = buAyPerformans.value?.gerceklesmeYuzde
  return v == null ? null : Number(v)
})
const iptalYetkili = computed(() => {
  const rol = authStore.kullanici?.role
  if (!rol) return true
  return rol === 'ADMIN' || rol === 'MUHASEBE' || authStore.hasPermission('FINANS_WRITE')
})
const hedefDuzenlenebilir = computed(() => iptalYetkili.value)

const aralikSecenekleri = computed(() => [
  { value: 'Vadesi Gelmemiş', label: t('tahsilat.aralikVadesiGelmemis') },
  { value: '0-30', label: t('tahsilat.aralik0_30') },
  { value: '31-60', label: t('tahsilat.aralik31_60') },
  { value: '61-90', label: t('tahsilat.aralik61_90') },
  { value: '90+', label: t('tahsilat.aralik90Plus') }
])

const yontemSecenekleri = computed(() => [
  { value: 'NAKIT', label: t('tahsilat.nakit') },
  { value: 'KART', label: t('tahsilat.kart') },
  { value: 'TAKSIT', label: t('tahsilat.taksit') },
  { value: 'HAVALE', label: t('tahsilat.havale') }
])

const durumSecenekleri = computed(() => [
  { value: 'AKTIF', label: t('tahsilat.durumAktif') },
  { value: 'IPTAL', label: t('tahsilat.durumIptal') }
])

const cariSecenekleri = computed(() => {
  const map = new Map()
  for (const c of [...tumCariler.value, ...(ozet.value?.cariler || [])]) {
    if (!map.has(c.cariId)) map.set(c.cariId, { label: c.cariAd, value: c.cariId })
  }
  return [...map.values()]
})

const yilSecenekleri = computed(() => {
  const yil = new Date().getFullYear()
  return Array.from({ length: 7 }, (_, i) => yil - 5 + i).reverse()
})

const aySecenekleri = computed(() =>
  Array.from({ length: 12 }, (_, i) => ({ label: ayAdi(i + 1), value: i + 1 }))
)

const seciliEpostali = computed(() => seciliCariler.value.filter((c) => !!c.email))
const seciliEpostasiz = computed(() => seciliCariler.value.filter((c) => !c.email))

const canliDuyuru = computed(() => {
  if (yukleniyor.value || gecmisYukleniyor.value) return ''
  if (aktifSekme.value === 1) {
    return t('tahsilat.gecmisKayitDuyuru', { gorunen: gecmis.value.length, toplam: gecmisToplam.value })
  }
  return t('tahsilat.kayitDuyuru', { gorunen: (ozet.value?.cariler || []).length })
})

// ---- Yardımcılar ----
const ayAdi = (ay) => {
  // Backend trend noktaları "YYYY-MM" döner; seçiciler ise 1-12 sayı kullanır.
  const n = typeof ay === 'string' && ay.includes('-') ? Number(ay.split('-')[1]) : Number(ay)
  if (!n || Number.isNaN(n)) return ''
  return new Date(2024, n - 1, 1).toLocaleDateString(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    month: 'short'
  })
}

const aralikSeverity = (a) => {
  if (!a) return 'info'
  if (a.startsWith('Vadesi')) return 'success'
  if (a.startsWith('61-90') || a.startsWith('90+')) return 'danger'
  return 'warning'
}

const odemeYontemiLabel = (y) =>
  ({
    NAKIT: t('tahsilat.nakit'),
    KART: t('tahsilat.kart'),
    TAKSIT: t('tahsilat.taksit'),
    HAVALE: t('tahsilat.havale')
  })[y] || y

const odemeYontemiSeverity = (y) =>
  ({ NAKIT: 'success', KART: 'info', TAKSIT: 'warn', HAVALE: 'secondary' })[y] || 'info'

const filtreParams = () => {
  const p = {}
  if (q.value?.trim()) p.q = q.value.trim()
  if (aralik.value) p.aralik = aralik.value
  if (sadeceGecikmis.value) p.sadeceGecikmis = true
  if (minTutar.value != null && minTutar.value !== '') p.minTutar = minTutar.value
  return p
}

const gecmisFiltreParams = () => {
  const p = {}
  if (gecmisQ.value?.trim()) p.q = gecmisQ.value.trim()
  if (gecmisCariId.value != null) p.cariId = gecmisCariId.value
  if (gecmisYontem.value) p.odemeYontemi = gecmisYontem.value
  if (gecmisDurum.value) p.durum = gecmisDurum.value
  if (gecmisBaslangic.value) p.baslangic = getLocalDateString(gecmisBaslangic.value)
  if (gecmisBitis.value) p.bitis = getLocalDateString(gecmisBitis.value)
  return p
}

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

// ---- Yükleme ----
const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await tahsilatAPI.filtreliOzet(filtreParams())
    ozet.value = r.data || {}
    if (!filtreVar.value) tumCariler.value = ozet.value.cariler || []
  } catch (err) {
    ozet.value = null
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.ozetYuklenemedi'))
  } finally {
    yukleniyor.value = false
    ozetYuklendi.value = true
  }
}

const gecmisYukle = async () => {
  gecmisYukleniyor.value = true
  try {
    const r = await tahsilatAPI.gecmisFiltreli({
      ...gecmisFiltreParams(),
      page: gecmisSayfa.value,
      size: gecmisBoyut.value,
      sort: gecmisSirala.value
    })
    gecmis.value = r.data?.content || []
    gecmisToplam.value = r.data?.totalElements ?? gecmis.value.length
    gecmisYuklendi.value = true
  } catch (err) {
    gecmis.value = []
    gecmisToplam.value = 0
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.gecmisYuklenemedi'))
  } finally {
    gecmisYukleniyor.value = false
  }
}

const performansYukle = async () => {
  performansYukleniyor.value = true
  try {
    const r = await tahsilatAPI.performans({ yil: perfYil.value, ay: perfAy.value })
    performans.value = r.data || null
    performansYuklendi.value = true
    if (
      Number(perfYil.value) === bugun.getFullYear() &&
      Number(perfAy.value) === bugun.getMonth() + 1
    ) {
      buAyPerformans.value = performans.value
    }
  } catch (err) {
    performans.value = null
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.performansYuklenemedi'))
  } finally {
    performansYukleniyor.value = false
  }
}

const yenile = () => {
  yukle()
  performansYukle()
  if (gecmisYuklendi.value) gecmisYukle()
}

// ---- Yaşlandırma olayları ----
const filtreDegisti = () => {
  seciliCariler.value = []
  genisletilenler.value = []
  yukle()
}

const aramaDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(filtreDegisti, 300)
}

const gecikmisToggle = () => {
  sadeceGecikmis.value = !sadeceGecikmis.value
  if (sadeceGecikmis.value) aralik.value = null
  filtreDegisti()
}

const filtreTemizle = () => {
  q.value = ''
  aralik.value = null
  sadeceGecikmis.value = false
  minTutar.value = null
  filtreDegisti()
}

// ---- Geçmiş olayları ----
const gecmisFiltreDegisti = () => {
  gecmisSayfa.value = 0
  gecmisYukle()
}

const gecmisTarihSec = (alan, deger) => {
  if (alan === 'baslangic') gecmisBaslangic.value = deger
  else gecmisBitis.value = deger
  gecmisFiltreDegisti()
}

const gecmisAramaDegisti = () => {
  if (gecmisAramaZaman) clearTimeout(gecmisAramaZaman)
  gecmisAramaZaman = setTimeout(gecmisFiltreDegisti, 300)
}

const gecmisFiltreTemizle = () => {
  gecmisQ.value = ''
  gecmisCariId.value = null
  gecmisYontem.value = null
  gecmisDurum.value = null
  gecmisBaslangic.value = null
  gecmisBitis.value = null
  gecmisSayfa.value = 0
  gecmisYukle()
}

const gecmisSayfaDegisti = (e) => {
  gecmisSayfa.value = e.page
  gecmisBoyut.value = e.rows
  gecmisYukle()
}

const gecmisSiralaDegisti = (e) => {
  gecmisSirala.value =
    e.sortField && e.sortOrder
      ? `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'tarih,desc'
  gecmisSayfa.value = 0
  gecmisYukle()
}

// ---- KPI ----
const kpiAktif = (anahtar) => {
  if (anahtar === 'gecikmis') return aktifSekme.value === 0 && sadeceGecikmis.value
  if (anahtar === 'yaklasan') return aktifSekme.value === 0 && aralik.value === '0-30'
  if (anahtar === 'buAy') return aktifSekme.value === 1
  if (anahtar === 'hedef') return aktifSekme.value === 2
  return (
    aktifSekme.value === 0 && !sadeceGecikmis.value && !aralik.value && !q.value && minTutar.value == null
  )
}

const kpiTikla = (anahtar) => {
  if (anahtar === 'hedef') {
    aktifSekme.value = 2
    return
  }
  if (anahtar === 'buAy') {
    const ilk = new Date(bugun.getFullYear(), bugun.getMonth(), 1)
    const son = new Date(bugun.getFullYear(), bugun.getMonth() + 1, 0)
    gecmisBaslangic.value = ilk
    gecmisBitis.value = son
    gecmisSayfa.value = 0
    const sekmeDegisecek = aktifSekme.value !== 1
    aktifSekme.value = 1
    // Sekme değişiyorsa ve geçmiş hiç yüklenmediyse watcher yükler; aksi halde
    // yeni tarih aralığıyla hemen yükle.
    if (!sekmeDegisecek || gecmisYuklendi.value) gecmisYukle()
    return
  }
  aktifSekme.value = 0
  if (anahtar === 'gecikmis') {
    sadeceGecikmis.value = true
    aralik.value = null
  } else if (anahtar === 'yaklasan') {
    aralik.value = '0-30'
    sadeceGecikmis.value = false
  } else {
    q.value = ''
    aralik.value = null
    sadeceGecikmis.value = false
    minTutar.value = null
  }
  filtreDegisti()
}

// ---- Cari drawer ----
const cariAc = async (cari) => {
  drawerCari.value = cari
  drawerAcik.value = true
  sonTahsilatlar.value = []
  sonTahsilatYukleniyor.value = true
  try {
    const r = await tahsilatAPI.gecmisFiltreli({ cariId: cari.cariId, page: 0, size: 5, sort: 'tarih,desc' })
    sonTahsilatlar.value = r.data?.content || []
  } catch {
    sonTahsilatlar.value = []
  } finally {
    sonTahsilatYukleniyor.value = false
  }
}

const drawerTahsilat = (cari) => {
  drawerAcik.value = false
  tahsilatGir(cari)
}

// ---- Tahsilat girişi ----
const tahsilatGir = (cari) => {
  seciliCariId.value = cari?.cariId || cari?.id || null
  tahsilatDialogAcik.value = true
}

const kaydetSonrasi = () => {
  yenile()
}

const tahsilatEylemleri = (d) => {
  const items = []
  if (d.email) {
    items.push({ etiket: t('tahsilat.epostaIleHatirlat'), ikon: 'pi pi-envelope', islem: () => hatirlat(d) })
  }
  if (d.telefon) {
    items.push({ etiket: t('tahsilat.whatsappAc'), ikon: 'pi pi-whatsapp', islem: () => whatsappAc(d) })
    items.push({ etiket: t('tahsilat.ara'), ikon: 'pi pi-phone', islem: () => ara(d) })
  }
  return items
}

const hatirlat = async (cari) => {
  try {
    const r = await tahsilatAPI.hatirlat(cari.cariId)
    toastBildirim.basarili(
      t('tahsilat.hatirlatmaGonderildi', { ad: cari.cariAd, n: r.data?.gonderilen || 0 })
    )
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.hatirlatmaGonderilemedi'))
  }
}

const telefonNormalle = (telefon) => {
  let no = String(telefon || '').replace(/\D/g, '')
  if (no.startsWith('0')) no = no.slice(1)
  if (no.length === 10) no = '90' + no
  return no
}

const whatsappAc = (cari) => {
  const no = telefonNormalle(cari.telefon)
  const mesaj = encodeURIComponent(
    t('tahsilat.whatsappMesaj', { ad: cari.cariAd, tutar: formatCurrency(cari.toplamAlacak) })
  )
  window.open(`https://wa.me/${no}?text=${mesaj}`, '_blank')
}

const ara = (cari) => {
  window.location.href = `tel:${cari.telefon}`
}

// ---- Toplu e-posta ----
const topluEpostaAc = () => {
  if (!seciliCariler.value.length) {
    toastBildirim.uyari(t('tahsilat.topluEpostaSecimYok'))
    return
  }
  topluSonuc.value = null
  topluDialog.value = true
}

const topluGonder = async () => {
  if (topluGonderiliyor.value) return
  topluGonderiliyor.value = true
  try {
    const r = await tahsilatAPI.topluHatirlat(seciliCariler.value.map((c) => c.cariId))
    topluSonuc.value = r.data || { gonderilen: 0, atlanan: 0, hatalar: [] }
    toastBildirim.basarili(
      t('tahsilat.topluEpostaBasarili', { gonderilen: topluSonuc.value.gonderilen ?? 0 })
    )
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.topluEpostaHata'))
  } finally {
    topluGonderiliyor.value = false
  }
}

// ---- İptal ----
const iptalAc = (islem) => {
  iptalHedef.value = islem
  iptalGerekce.value = ''
  iptalDialog.value = true
}

const iptalKaydet = async () => {
  if (iptalEdiliyor.value) return
  const gerekce = (iptalGerekce.value || '').trim()
  if (gerekce.length < 3) {
    toastBildirim.uyari(t('tahsilat.iptalGerekceKisa'))
    return
  }
  iptalEdiliyor.value = true
  try {
    await tahsilatAPI.iptal(iptalHedef.value.id, gerekce)
    toastBildirim.basarili(t('tahsilat.iptalBasarili'))
    iptalDialog.value = false
    yenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.iptalBasarisiz'))
  } finally {
    iptalEdiliyor.value = false
  }
}

// ---- Hedef ----
const hedefDuzenle = () => {
  hedefTutar.value = Number(performans.value?.hedef) || 0
  hedefDialog.value = true
}

const hedefKaydet = async () => {
  if (hedefKaydediliyor.value) return
  hedefKaydediliyor.value = true
  try {
    await tahsilatAPI.hedefKaydet({
      yil: Number(perfYil.value),
      ay: Number(perfAy.value),
      tutar: Number(hedefTutar.value) || 0
    })
    toastBildirim.basarili(t('tahsilat.hedefKaydedildi'))
    hedefDialog.value = false
    await performansYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.hedefKaydedilemedi'))
  } finally {
    hedefKaydediliyor.value = false
  }
}

const trendMaks = computed(() => {
  const degerler = (performans.value?.trend || []).flatMap((n) => [Number(n.tahsilat) || 0, Number(n.hedef) || 0])
  return Math.max(1, ...degerler)
})

const cubukYuksekligi = (deger) => `${Math.max(4, (Number(deger) || 0) / trendMaks.value * 100)}%`

// ---- Excel ----
const excelYaslandirmaIndir = async () => {
  excelYukleniyor.value = true
  try {
    const r = await excelAPI.tahsilatYaslandirma(filtreParams())
    indir(r.data, 'tahsilat-yaslandirma.xlsx')
    toastBildirim.basarili(t('tahsilat.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('tahsilat.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const excelGecmisIndir = async () => {
  excelGecmisYukleniyor.value = true
  try {
    const r = await excelAPI.tahsilatGecmis(gecmisFiltreParams())
    indir(r.data, 'tahsilat-gecmis.xlsx')
    toastBildirim.basarili(t('tahsilat.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('tahsilat.excelHata'))
  } finally {
    excelGecmisYukleniyor.value = false
  }
}

// ---- Kalıcılık ----
watch([q, aralik, sadeceGecikmis, minTutar, gecmisQ, gecmisCariId, gecmisYontem, gecmisDurum, gecmisBaslangic, gecmisBitis], () => {
  try {
    localStorage.setItem(
      FILTRE_ANAHTAR,
      JSON.stringify({
        q: q.value || '',
        aralik: aralik.value || null,
        sadeceGecikmis: sadeceGecikmis.value,
        minTutar: minTutar.value ?? null,
        gecmisQ: gecmisQ.value || '',
        gecmisCariId: gecmisCariId.value ?? null,
        gecmisYontem: gecmisYontem.value || null,
        gecmisDurum: gecmisDurum.value || null,
        gecmisBaslangic: gecmisBaslangic.value ? getLocalDateString(gecmisBaslangic.value) : null,
        gecmisBitis: gecmisBitis.value ? getLocalDateString(gecmisBitis.value) : null
      })
    )
  } catch {
    /* depolama kapalı olabilir */
  }
})

watch(aktifSekme, (v) => {
  try {
    localStorage.setItem(TAB_ANAHTAR, String(v))
  } catch {
    /* depolama kapalı olabilir */
  }
  if (v === 1 && !gecmisYuklendi.value) gecmisYukle()
  if (v === 2 && !performansYuklendi.value) performansYukle()
})

onMounted(async () => {
  await Promise.allSettled([yukle(), performansYukle()])
  if (aktifSekme.value === 1) gecmisYukle()
  // Faturalar/Satışlar satırından "Tahsilat Al" ile gelindiyse cari ön seçili açılır.
  const qCari = Number(route?.query?.cariId)
  if (qCari) tahsilatGir({ cariId: qCari })
})

onUnmounted(() => {
  if (aramaZaman) clearTimeout(aramaZaman)
  if (gecmisAramaZaman) clearTimeout(gecmisAramaZaman)
})
</script>

<style scoped>
.tahsilat-sayfasi {
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
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}
.kpi-tikla {
  display: block;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-lg, 14px);
  min-width: 0;
}
.kpi-tikla:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kpi-tikla[aria-pressed='true'] :deep(.kpi-kart) {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-soft);
}
.tahsilat-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.tahsilat-arac .arac-secim {
  min-width: 130px;
}
.tahsilat-arac .arac-tarih {
  min-width: 150px;
}
.min-tutar {
  min-width: 120px;
}
.filtre-arama {
  min-width: 220px;
  max-width: 320px;
  flex: 1 1 220px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.gecikmis-sec {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--dur-fast, 0.15s) ease, color var(--dur-fast, 0.15s) ease;
}
.gecikmis-sec.aktif {
  background: var(--danger-soft, rgba(239, 68, 68, 0.14));
  border-color: var(--danger, #ef4444);
  color: var(--danger, #ef4444);
}
.gecikmis-sec:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.tablo-iskelet {
  padding: 4px 0;
}
.cari-link {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  color: var(--text-primary);
  cursor: pointer;
}
.cari-link:hover strong {
  color: var(--accent);
  text-decoration: underline;
}
.cari-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
  border-radius: 4px;
}
.cari-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.islem-grup {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}
.negatif {
  color: var(--danger, #ef4444);
  font-weight: 700;
}
.pozitif {
  color: var(--success, #10b981);
}
.iptal-tutar {
  color: var(--text-muted);
  text-decoration: line-through;
}
.muted {
  color: var(--text-muted);
  font-size: 12px;
}
.fatura-liste,
.dagilim-liste {
  padding: 8px 16px;
  background: var(--bg-secondary, #f8fafc);
  border-radius: 8px;
}
.fatura-satir,
.dagilim-satir {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  flex-wrap: wrap;
}
.fatura-satir:last-child,
.dagilim-satir:last-child {
  border-bottom: none;
}
.fatura-no,
.dagilim-fatura {
  font-weight: 600;
  min-width: min(160px, 100%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.fatura-vade,
.dagilim-aciklama {
  flex: 1;
  color: var(--text-muted);
}
.fatura-tutar,
.dagilim-tutar {
  font-weight: 700;
  color: var(--text-primary);
}
.gecikme-pill {
  margin-left: 8px;
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.vade-pill {
  margin-left: 8px;
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.bos-satir {
  color: var(--text-muted);
  padding: 8px 0;
}
.iptal-bant {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: var(--danger-soft, rgba(239, 68, 68, 0.12));
  color: var(--danger, #ef4444);
  font-size: 12.5px;
}
.iptal-not {
  font-size: 11px;
  color: var(--danger, #ef4444);
  font-weight: 700;
}
.yaslandirma-mobil {
  display: none;
}
.cari-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  margin-bottom: 8px;
}
.cm-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.cm-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cm-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.cm-aksiyonlar {
  display: flex;
  gap: 6px;
  align-items: center;
}
.performans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.perf-kart {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}
.perf-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-muted);
}
.perf-deger {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.perf-alt {
  font-size: 12px;
  color: var(--text-secondary);
}
.perf-uyari {
  font-size: 11.5px;
  color: var(--warning, #f59e0b);
}
.gerceklesme-bar {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}
.gerceklesme-dolu {
  height: 100%;
  border-radius: 999px;
  background: var(--accent);
  transition: width var(--dur-base, 0.3s) var(--ease-standard, ease);
}
.gerceklesme-dolu.asim {
  background: var(--success, #10b981);
}
.trend-kart {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  padding: 16px 18px;
}
.trend-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}
.trend-baslik i {
  color: var(--accent);
}
.trend-efsane {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--text-secondary);
}
.efsane::before {
  content: '';
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 3px;
  margin-right: 5px;
}
.efsane.tahsilat::before {
  background: var(--success, #10b981);
}
.efsane.hedef::before {
  background: var(--accent);
}
.trend-grafik {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  height: 190px;
  padding-top: 8px;
}
.trend-kolon {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  height: 100%;
  justify-content: flex-end;
  min-width: 0;
}
.trend-cubuklar {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
}
.trend-cubuk {
  width: min(18px, 38%);
  border-radius: 5px 5px 2px 2px;
  min-height: 4px;
  transition: height var(--dur-base, 0.3s) var(--ease-standard, ease);
}
.trend-cubuk.tahsilat {
  background: var(--success, #10b981);
}
.trend-cubuk.hedef {
  background: var(--accent);
  opacity: 0.55;
}
.trend-ay {
  font-size: 11px;
  color: var(--text-muted);
}
.dialog-metin {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text-secondary);
}
.dialog-uyari {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 12.5px;
  color: var(--warning, #f59e0b);
}
.dialog-ozet {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.toplu-sonuc {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sonuc-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}
.hata-liste {
  margin: 4px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--danger, #ef4444);
}
.hedef-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.w-full {
  width: 100%;
}
@media (max-width: 768px) {
  .yaslandirma-masa {
    display: none;
  }
  .yaslandirma-mobil {
    display: block;
  }
  .tahsilat-arac .arac-secim,
  .tahsilat-arac .arac-tarih {
    flex: 1 1 130px;
  }
  .filtre-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .trend-grafik {
    height: 150px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .gecikmis-sec,
  .gerceklesme-dolu,
  .trend-cubuk {
    transition: none;
  }
}
</style>
