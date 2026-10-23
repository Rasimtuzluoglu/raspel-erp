<template>
  <div class="taksit-takvimi">
    <PageHeader
      :title="t('taksitTakvimi.baslik')"
      icon="pi pi-calendar"
      :subtitle="t('taksitTakvimi.altBaslik')"
    >
      <template #actions>
        <Button
          v-if="odeYetkisi"
          :label="t('taksitTakvimi.yeniPlan')"
          icon="pi pi-plus"
          @click="planDialogAc"
        />
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          :loading="yukleniyor"
          @click="yukle"
        />
      </template>
    </PageHeader>

    <div class="kpi-satiri">
      <KpiKart
        :baslik="t('taksitTakvimi.bekleyen')"
        :deger="ozet.bekleyenToplam"
        ikon="pi pi-hourglass"
        renk="#f59e0b"
      />
      <KpiKart
        :baslik="t('taksitTakvimi.gecikmis')"
        :deger="ozet.gecikmisToplam"
        ikon="pi pi-exclamation-triangle"
        renk="#ef4444"
      />
      <KpiKart
        :baslik="t('taksitTakvimi.buAy')"
        :deger="ozet.buAyToplam"
        ikon="pi pi-calendar"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('taksitTakvimi.kpiOdenen')"
        :deger="ozet.odenenBuYilToplam"
        ikon="pi pi-check-circle"
        renk="#10b981"
      />
    </div>

    <TabView v-model:active-index="aktifTab">
      <TabPanel :header="t('taksitTakvimi.tabTakvim')">
        <div class="takvim-kart">
          <div class="takvim-baslik">
            <Button
              icon="pi pi-chevron-left"
              text
              rounded
              :aria-label="t('taksitTakvimi.oncekiAy')"
              @click="ayDegistir(-1)"
            />
            <strong
              class="takvim-ay"
              aria-live="polite"
            >{{ ayAdi }}</strong>
            <Button
              icon="pi pi-chevron-right"
              text
              rounded
              :aria-label="t('taksitTakvimi.sonrakiAy')"
              @click="ayDegistir(1)"
            />
          </div>
          <div
            class="takvim-grid"
            role="grid"
            :aria-label="ayAdi"
          >
            <div
              v-for="g in gunBasliklari"
              :key="g"
              class="gun-baslik"
              role="columnheader"
            >
              {{ g }}
            </div>
            <div
              v-for="hucre in takvimHucreleri"
              :key="hucre.key"
              class="gun-hucre"
              :class="{ bugun: hucre.bugun, bos: !hucre.gun }"
              role="gridcell"
            >
              <template v-if="hucre.gun">
                <span class="gun-no">{{ hucre.gun }}</span>
                <button
                  v-for="k in hucre.kalemler"
                  :key="k.id"
                  type="button"
                  class="kalem"
                  :class="kalemSinif(k)"
                  :title="`${k.cariAd} - ${formatCurrency(k.tutar)}`"
                  @click="kalemDetay(k)"
                >
                  <span class="kalem-metin">{{ k.cariAd }} · {{ formatCurrency(k.tutar) }}</span>
                </button>
              </template>
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel :header="t('taksitTakvimi.tabTaksitler')">
        <div class="liste-kart">
          <div class="taksit-arac">
            <span class="arac-arama">
              <i class="pi pi-search" />
              <InputText
                v-model="arama"
                :placeholder="t('taksitTakvimi.aramaPlaceholder')"
                class="arama-input"
                @input="aramaGirdi"
              />
            </span>
            <Dropdown
              v-model="cariFiltre"
              :options="cariler"
              option-label="ad"
              option-value="id"
              :placeholder="t('taksitTakvimi.cari')"
              show-clear
              filter
              class="arac-secim"
              @change="filtreDegisti"
            />
            <Dropdown
              v-model="durumFiltre"
              :options="durumSecenekleri"
              option-label="label"
              option-value="value"
              class="arac-secim"
              @change="filtreDegisti"
            />
            <DatePicker
              v-model="tarihAraligi"
              selection-mode="range"
              :placeholder="t('taksitTakvimi.vade')"
              date-format="dd.mm.yy"
              show-button-bar
              class="arac-tarih"
              @update:model-value="filtreDegisti"
            />
            <span class="arac-bosluk" />
            <Button
              :label="t('taksitTakvimi.excel')"
              icon="pi pi-file-excel"
              outlined
              size="small"
              :loading="excelYukleniyor"
              @click="excelIndir"
            />
          </div>

          <div
            class="taksit-masa"
            :aria-busy="listeYukleniyor"
          >
            <AppDataTable
              :value="liste"
              lazy
              :total-records="toplam"
              :rows="sayfaBoyutu"
              striped-rows
              :loading="listeYukleniyor"
              :empty-message="filtreVar ? t('taksitTakvimi.filtreBos') : t('taksitTakvimi.kayitYok')"
              gorunum-anahtari="taksitler"
              @page="sayfaDegisti"
              @sort="siralaDegisti"
            >
              <Column
                field="vadeTarihi"
                :header="t('taksitTakvimi.vade')"
                sortable
              >
                <template #body="{ data }">
                  {{ formatDate(data.vadeTarihi) }}
                </template>
              </Column>
              <Column
                field="cariAd"
                :header="t('taksitTakvimi.cari')"
                sortable
              />
              <Column
                field="kurum"
                :header="t('taksitTakvimi.kurum')"
              >
                <template #body="{ data }">
                  {{ data.kurum || '-' }}
                </template>
              </Column>
              <Column :header="t('taksitTakvimi.taksitNo')">
                <template #body="{ data }">
                  {{ data.taksitNo }}/{{ data.taksitSayisi }}
                </template>
              </Column>
              <Column
                field="tutar"
                :header="t('taksitTakvimi.tutar')"
              >
                <template #body="{ data }">
                  {{ formatCurrency(data.tutar) }}
                </template>
              </Column>
              <Column :header="t('taksitTakvimi.durum')">
                <template #body="{ data }">
                  <Tag
                    :severity="durumSeverity(data)"
                    :value="durumEtiket(data)"
                  />
                </template>
              </Column>
              <Column
                :header="t('common.actions')"
                style="width: 72px"
              >
                <template #body="{ data }">
                  <SatirEylemleri
                    :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                    :items="kalemAksiyonlari(data)"
                    :baslik="data.cariAd"
                  />
                </template>
              </Column>
              <template #empty>
                <EmptyState
                  :message="filtreVar ? t('taksitTakvimi.filtreBos') : t('taksitTakvimi.kayitYok')"
                />
              </template>
            </AppDataTable>
          </div>

          <!-- Mobil kart görünümü -->
          <div
            class="taksit-mobil"
            :aria-busy="listeYukleniyor"
          >
            <span
              class="sr-only"
              role="status"
              aria-live="polite"
            >{{ t('taksitTakvimi.sonucSayisi', { sayi: toplam }) }}</span>
            <div
              v-if="listeYukleniyor && !liste.length"
              class="taksit-skeleton"
              role="status"
              :aria-label="t('common.loading')"
            >
              <SkeletonLoader
                :count="4"
                height="96px"
              />
            </div>
            <EmptyState
              v-else-if="!liste.length"
              :message="filtreVar ? t('taksitTakvimi.filtreBos') : t('taksitTakvimi.kayitYok')"
            />
            <article
              v-for="k in liste"
              :key="k.id"
              class="taksit-mobil-kart"
            >
              <div class="tmk-ust">
                <button
                  type="button"
                  class="tmk-ad"
                  :title="t('taksitTakvimi.detay')"
                  @click="kalemDetay(k)"
                >
                  <i class="pi pi-user" />
                  <span>{{ k.cariAd }}</span>
                </button>
                <SatirEylemleri
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="kalemAksiyonlari(k)"
                  :baslik="k.cariAd"
                />
              </div>
              <div class="tmk-satir">
                <Tag
                  :value="durumEtiket(k)"
                  :severity="durumSeverity(k)"
                />
                <strong class="tmk-tutar">{{ formatCurrency(k.tutar) }}</strong>
              </div>
              <div class="tmk-satir tmk-alt">
                <span><i class="pi pi-calendar" /> {{ formatDate(k.vadeTarihi) }}</span>
                <span>{{ k.taksitNo }}/{{ k.taksitSayisi }}</span>
                <span v-if="k.kurum">{{ k.kurum }}</span>
              </div>
            </article>
          </div>
        </div>
      </TabPanel>

      <TabPanel :header="t('taksitTakvimi.tabPlanlar')">
        <div class="liste-kart">
          <div class="taksit-arac">
            <span class="arac-arama">
              <i class="pi pi-search" />
              <InputText
                v-model="planArama"
                :placeholder="t('taksitTakvimi.planArama')"
                class="arama-input"
                @input="planAramaGirdi"
              />
            </span>
            <span class="arac-bosluk" />
          </div>

          <div
            class="plan-masa"
            :aria-busy="planYukleniyor"
          >
            <AppDataTable
              :value="planlar"
              striped-rows
              :loading="planYukleniyor"
              :paginator="false"
              :empty-message="planArama.trim() ? t('taksitTakvimi.filtreBos') : t('taksitTakvimi.planYok')"
              gorunum-anahtari="taksit-planlar"
            >
              <Column
                field="cariAd"
                :header="t('taksitTakvimi.cari')"
                sortable
              />
              <Column
                field="planNo"
                :header="t('taksitTakvimi.planNo')"
              >
                <template #body="{ data }">
                  <span class="detay-mono">{{ data.planNo }}</span>
                </template>
              </Column>
              <Column
                field="kurum"
                :header="t('taksitTakvimi.kurum')"
              >
                <template #body="{ data }">
                  {{ data.kurum || '-' }}
                </template>
              </Column>
              <Column :header="t('taksitTakvimi.taksitNo')">
                <template #body="{ data }">
                  <div class="plan-ilerleme">
                    <span>{{ data.odenenAdet }}/{{ data.taksitSayisi }}</span>
                    <span class="plan-bar">
                      <span
                        class="plan-bar-ic"
                        :style="{ width: `${planYuzde(data)}%` }"
                      />
                    </span>
                  </div>
                </template>
              </Column>
              <Column
                field="toplamTutar"
                :header="t('taksitTakvimi.toplamTutar')"
              >
                <template #body="{ data }">
                  {{ formatCurrency(data.toplamTutar) }}
                </template>
              </Column>
              <Column
                field="kalanTutar"
                :header="t('taksitTakvimi.kalan')"
              >
                <template #body="{ data }">
                  <strong>{{ formatCurrency(data.kalanTutar) }}</strong>
                </template>
              </Column>
              <Column
                field="sonVade"
                :header="t('taksitTakvimi.sonVade')"
              >
                <template #body="{ data }">
                  {{ formatDate(data.sonVade) }}
                </template>
              </Column>
              <Column
                :header="t('common.actions')"
                style="width: 72px"
              >
                <template #body="{ data }">
                  <SatirEylemleri
                    :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                    :items="planAksiyonlari(data)"
                    :baslik="data.cariAd"
                  />
                </template>
              </Column>
              <template #empty>
                <EmptyState
                  icon="pi pi-list"
                  :message="planArama.trim() ? t('taksitTakvimi.filtreBos') : t('taksitTakvimi.planYok')"
                />
              </template>
            </AppDataTable>
          </div>

          <!-- Mobil plan kartları -->
          <div
            class="plan-mobil"
            :aria-busy="planYukleniyor"
          >
            <span
              class="sr-only"
              role="status"
              aria-live="polite"
            >{{ t('taksitTakvimi.sonucSayisi', { sayi: planlar.length }) }}</span>
            <div
              v-if="planYukleniyor && !planlar.length"
              class="plan-skeleton"
              role="status"
              :aria-label="t('common.loading')"
            >
              <SkeletonLoader
                :count="3"
                height="104px"
              />
            </div>
            <EmptyState
              v-else-if="!planlar.length"
              icon="pi pi-list"
              :message="planArama.trim() ? t('taksitTakvimi.filtreBos') : t('taksitTakvimi.planYok')"
            />
            <article
              v-for="p in planlar"
              :key="p.planNo"
              class="plan-mobil-kart"
            >
              <div class="pmk-ust">
                <button
                  type="button"
                  class="pmk-ad"
                  :title="t('taksitTakvimi.detay')"
                  @click="planDetayAc(p)"
                >
                  <i class="pi pi-list" />
                  <span>{{ p.cariAd }}</span>
                </button>
                <SatirEylemleri
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="planAksiyonlari(p)"
                  :baslik="p.cariAd"
                />
              </div>
              <div class="pmk-satir">
                <span class="pmk-oran">{{ p.odenenAdet }}/{{ p.taksitSayisi }}</span>
                <span class="plan-bar pmk-bar">
                  <span
                    class="plan-bar-ic"
                    :style="{ width: `${planYuzde(p)}%` }"
                  />
                </span>
                <strong class="pmk-kalan">{{ formatCurrency(p.kalanTutar) }}</strong>
              </div>
              <div class="pmk-satir pmk-alt">
                <span>{{ formatCurrency(p.toplamTutar) }}</span>
                <span v-if="p.kurum">{{ p.kurum }}</span>
                <span>{{ formatDate(p.sonVade) }}</span>
              </div>
            </article>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="planDialog"
      :header="t('taksitTakvimi.planBaslik')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="plan-form">
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.cariSec') }}</label>
          <Select
            v-model="form.cariId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            filter
            :placeholder="t('taksitTakvimi.cariSec')"
            class="w-full"
          />
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.toplamTutar') }}</label>
          <InputNumber
            v-model="form.toplamTutar"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
          />
        </div>
        <div class="form-ikili">
          <div class="form-satir">
            <label>{{ t('taksitTakvimi.taksitSayisi') }}</label>
            <InputNumber
              v-model="form.taksitSayisi"
              :min="1"
              :max="60"
              show-buttons
              class="w-full"
            />
          </div>
          <div class="form-satir">
            <label>{{ t('taksitTakvimi.periyot') }}</label>
            <InputNumber
              v-model="form.periyotAy"
              :min="1"
              :max="12"
              show-buttons
              class="w-full"
            />
          </div>
        </div>
        <div
          v-if="form.toplamTutar > 0 && form.taksitSayisi > 0"
          class="plan-onizleme"
        >
          <span>{{ t('taksitTakvimi.taksitBasi') }}: <strong>{{ formatCurrency(taksitBasiTutar) }}</strong></span>
          <span>{{ t('taksitTakvimi.sonTaksit') }}: <strong>{{ formatCurrency(sonTaksitTutar) }}</strong></span>
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.baslangic') }}</label>
          <DatePicker
            v-model="form.baslangicTarihi"
            date-format="dd.mm.yy"
            show-icon
            class="w-full"
          />
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.kurum') }}</label>
          <InputText
            v-model="form.kurum"
            class="w-full"
          />
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.aciklama') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('taksitTakvimi.iptal')"
          severity="secondary"
          text
          @click="planDialog = false"
        />
        <Button
          :label="t('taksitTakvimi.kaydet')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="planKaydet"
        />
      </template>
    </Dialog>

    <!--
      REDTEAM/Faz1.2: Taksit "Ödendi" işaretleme penceresi.
      Eskiden tek tıkla `POST /taksitler/{id}/ode` çağrılıyordu; bu uç YALNIZCA
      `odemeDurumu='ODENDI'` yazıyor, hiçbir para hareketi üretmiyordu. Takvim
      "ödendi" derken cari defter "ödenmedi" diyordu.

      Doğru yol backend'de HAZIRDI: `POST /tahsilat` gövdesindeki `taksitId`
      alanı tahsilat kaydettikten sonra ilgili kalemi işaretliyor
      (TahsilatService:275-280). Bu yüzden burada kasa/banka seçilerek GERÇEK
      tahsilat hareketi oluşturuluyor ve taksit onunla bağlanıyor.
    -->
    <Dialog
      v-model:visible="odemeDialog"
      :header="t('taksitTakvimi.odemeBaslik')"
      :modal="true"
      :style="{ width: '440px' }"
      :breakpoints="{ '640px': '95vw' }"
    >
      <div
        v-if="odemeKalemi"
        class="odeme-ozet"
      >
        <div class="odeme-ozet-satir">
          <span>{{ t('taksitTakvimi.cari') }}</span>
          <strong>{{ odemeKalemi.cariAd || '-' }}</strong>
        </div>
        <div class="odeme-ozet-satir">
          <span>{{ t('taksitTakvimi.taksitNo') }}</span>
          <strong>
            {{ odemeKalemi.taksitNo }} / {{ odemeKalemi.taksitSayisi }}
          </strong>
        </div>
        <div class="odeme-ozet-satir">
          <span>{{ t('taksitTakvimi.vadeTarihi') }}</span>
          <strong>{{ formatDate(odemeKalemi.vadeTarihi) }}</strong>
        </div>
        <div class="odeme-ozet-satir toplam">
          <span>{{ t('taksitTakvimi.tutar') }}</span>
          <strong>{{ formatCurrency(odemeKalemi.tutar) }}</strong>
        </div>
      </div>

      <div class="odeme-form">
        <label :for="odemeYontemiId">{{ t('taksitTakvimi.odemeYontemi') }}</label>
        <SelectButton
          v-model="odemeForm.odemeYontemi"
          :options="odemeYontemleri"
          option-label="label"
          option-value="value"
          :allow-empty="false"
        />

        <label
          v-if="odemeForm.odemeYontemi === 'NAKIT'"
          :for="odemeKasaId"
        >
          {{ t('taksitTakvimi.kasa') }}
        </label>
        <Dropdown
          v-if="odemeForm.odemeYontemi === 'NAKIT'"
          :id="odemeKasaId"
          v-model="odemeForm.kasaId"
          :options="kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('taksitTakvimi.kasaSec')"
          :filter="true"
          class="w-full"
        />

        <label
          v-if="odemeForm.odemeYontemi !== 'NAKIT'"
          :for="odemeBankaId"
        >
          {{ t('taksitTakvimi.banka') }}
        </label>
        <Dropdown
          v-if="odemeForm.odemeYontemi !== 'NAKIT'"
          :id="odemeBankaId"
          v-model="odemeForm.bankaId"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('taksitTakvimi.bankaSec')"
          :filter="true"
          class="w-full"
        />

        <label :for="odemeTarihId">{{ t('taksitTakvimi.odemeTarihi') }}</label>
        <DatePicker
          :id="odemeTarihId"
          v-model="odemeForm.hareketTarihi"
          date-format="dd.mm.yy"
          :max-date="new Date()"
          show-icon
        />
      </div>

      <template #footer>
        <Button
          :label="t('taksitTakvimi.vazgec')"
          severity="secondary"
          text
          @click="odemeDialog = false"
        />
        <Button
          :label="t('taksitTakvimi.tahsilatKaydet')"
          icon="pi pi-check"
          :loading="odemeKaydediliyor"
          @click="tahsilatKaydet"
        />
      </template>
    </Dialog>

    <TaksitDetayDrawer
      v-model:visible="detayDrawer"
      :kalem="detayKalem"
      :ode-yetkisi="odeYetkisi"
      :sil-yetkisi="silYetkisi"
      @ode="detayOde"
      @sil="detaySil"
    />

    <TaksitPlanDrawer
      v-model:visible="planDrawer"
      :plan="seciliPlan"
      :kalemler="planKalemleri"
      :yukleniyor="planKalemYukleniyor"
      :ode-yetkisi="odeYetkisi"
      :sil-yetkisi="silYetkisi"
      @sil="planSilOnay"
      @ode="planDrawerOde"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { taksitAPI, cariHesapAPI, tahsilatAPI, kasaAPI, bankaAPI, excelAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import KpiKart from '../components/KpiKart.vue'
import TaksitDetayDrawer from '../components/TaksitDetayDrawer.vue'
import TaksitPlanDrawer from '../components/TaksitPlanDrawer.vue'
import { formatCurrency, formatTarih as formatDate, getLocalDateString } from '../utils/format.js'

// REDTEAM/Faz1.6: "YYYY-MM-DD" degerini YEREL olarak ayristirir.
// utils/format.js icindeki parseDate ayni isi yapar ancak disa aktarilmamis;
// burada tek kullanim icin yerel ve bagimsiz surum kullanilir (Saat dilimi
// hatasinin duzeltilmis hali).
const parseYerelTarih = (deger) => {
  if (!deger) return null
  if (typeof deger === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(deger)) {
    const [y, ay, g] = deger.split('-').map(Number)
    return new Date(y, ay - 1, g)
  }
  const d = new Date(deger)
  return isNaN(d.getTime()) ? null : d
}

const { t, locale } = useI18n()
const confirm = useConfirm()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
// REDTEAM/Faz1.6: "Ode" ve "Sil" backend'de FARKLI rollere acik:
  //   POST /taksitler/{id}/ode   -> ADMIN | MUHASEBE
  //   DELETE /taksitler/{id}     -> ADMIN
  // Tek bayrak (`yonetimYetkisi`) ikisini birlikte yonetti; MUHASEBE rolunde
  // "Sil" butonu gorunuyordu ama tiklaninca 403 aliyordu. Simdi ayridir.
  const odeYetkisi = computed(() => ['ADMIN', 'MUHASEBE'].includes(authStore?.kullanici?.role))
  const silYetkisi = computed(() => authStore?.kullanici?.role === 'ADMIN')

const bugun = new Date()
const seciliYil = ref(bugun.getFullYear())
const seciliAy = ref(bugun.getMonth() + 1)

const TAB_ANAHTAR = 'raspel_taksit_tab'
const aktifTab = ref(Number(localStorage.getItem(TAB_ANAHTAR)) || 0)
watch(aktifTab, (v) => localStorage.setItem(TAB_ANAHTAR, String(v)))

const kalemler = ref([])
const liste = ref([])
const ozet = ref({
  bekleyenToplam: 0,
  gecikmisToplam: 0,
  buAyToplam: 0,
  odenenBuYilToplam: 0,
  bekleyenAdet: 0,
  gecikmisAdet: 0,
  odenenBuYilAdet: 0
})
const cariler = ref([])
const yukleniyor = ref(false)
const listeYukleniyor = ref(false)
const excelYukleniyor = ref(false)
const kaydediliyor = ref(false)
const planDialog = ref(false)

const planlar = ref([])
const planArama = ref('')
const planYukleniyor = ref(false)
let planAramaZamanlayici = null
const detayDrawer = ref(false)
const detayKalem = ref(null)
const planDrawer = ref(false)
const seciliPlan = ref(null)
const planKalemleri = ref([])
const planKalemYukleniyor = ref(false)

const SAYFA_ANAHTAR = 'raspel_taksit_rows'
const arama = ref('')
const cariFiltre = ref(null)
const durumFiltre = ref('TUMU')
const tarihAraligi = ref(null)
const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_ANAHTAR)) || 25)
const sirala = ref('vadeTarihi,asc')
let aramaZamanlayici = null

const durumSecenekleri = computed(() => [
  { label: t('taksitTakvimi.durumTumu'), value: 'TUMU' },
  { label: t('taksitTakvimi.bekliyor'), value: 'BEKLEMEDE' },
  { label: t('taksitTakvimi.odendi'), value: 'ODENDI' }
])

const filtreVar = computed(
  () =>
    !!arama.value.trim() ||
    !!cariFiltre.value ||
    !!tarihAraligi.value?.[0] ||
    durumFiltre.value !== 'TUMU'
)

const form = ref({
  cariId: null,
  toplamTutar: null,
  taksitSayisi: 3,
  periyotAy: 1,
  baslangicTarihi: new Date(),
  kurum: '',
  aciklama: ''
})

const taksitBasiTutar = computed(() => {
  const toplam = Number(form.value.toplamTutar || 0)
  const adet = Number(form.value.taksitSayisi || 0)
  if (toplam <= 0 || adet <= 0) return 0
  return Math.floor((toplam / adet) * 100) / 100
})
const sonTaksitTutar = computed(() => {
  const toplam = Number(form.value.toplamTutar || 0)
  const adet = Number(form.value.taksitSayisi || 0)
  if (toplam <= 0 || adet <= 0) return 0
  return Math.round((toplam - taksitBasiTutar.value * (adet - 1)) * 100) / 100
})

const gunBasliklari = computed(() => {
  const isimler = [
    t('taksitTakvimi.pazartesi'), t('taksitTakvimi.sali'), t('taksitTakvimi.carsamba'),
    t('taksitTakvimi.persembe'), t('taksitTakvimi.cuma'), t('taksitTakvimi.cumartesi'), t('taksitTakvimi.pazar')
  ]
  return isimler
})

// ---------------------------------------------------------------------
// REDTEAM/Faz1.2: Taksit odemesi artik GERCEK tahsilat hareketi olusturur.
// ---------------------------------------------------------------------

const odemeDialog = ref(false)
const odemeKalemi = ref(null)
const odemeKaydediliyor = ref(false)
const kasalar = ref([])
const bankalar = ref([])
const kasaBankaYuklendi = ref(false)

const odemeYontemiId = 'taksit-odeme-yontemi'
const odemeKasaId = 'taksit-odeme-kasa'
const odemeBankaId = 'taksit-odeme-banka'
const odemeTarihId = 'taksit-odeme-tarih'

const odemeYontemleri = computed(() => [
  { label: t('hizliSatis.nakit'), value: 'NAKIT' },
  { label: t('hizliSatis.kart'), value: 'KART' },
  { label: t('hizliSatis.havale'), value: 'HAVALE' }
])

const odemeForm = ref({
  odemeYontemi: 'NAKIT',
  kasaId: null,
  bankaId: null,
  hareketTarihi: null
})

/** Kasa/banka listeleri yalnizca ilk kez odeme penceresi acildiginda cekilir. */
const kasaBankaYukle = async () => {
  if (kasaBankaYuklendi.value) return
  try {
    const [kR, bR] = await Promise.all([
      kasaAPI.getAll({ size: 200 }),
      bankaAPI.getAll({ size: 200 })
    ])
    kasalar.value = unwrapList(kR)
    bankalar.value = unwrapList(bR)
    kasaBankaYuklendi.value = true
    if (!odemeForm.value.kasaId && kasalar.value.length) {
      odemeForm.value.kasaId = kasalar.value[0].id
    }
    if (!odemeForm.value.bankaId && bankalar.value.length) {
      odemeForm.value.bankaId = bankalar.value[0].id
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
}

const ode = (k) => {
  if (k.odemeDurumu === 'ODENDI') return
  if (!odeYetkisi.value) return
  odemeKalemi.value = k
  odemeForm.value = {
    odemeYontemi: 'NAKIT',
    kasaId: null,
    bankaId: null,
    hareketTarihi: new Date()
  }
  odemeDialog.value = true
  kasaBankaYukle()
}

/**
 * Tahsilat kaydeder ve taksiti onunla bağlar.
 * Backend'de `POST /tahsilat` gövdesindeki `taksitId` alanı, tahsilat
 * yazıldıktan SONRA ilgili kalemi 'ODENDI' işaretler (TahsilatService:275-280).
 * Önceki `POST /taksitler/{id}/ode` ucu yalnizca durum yaziyor, para
 * hareketi üretmiyordu.
 */
const tahsilatKaydet = async () => {
  const k = odemeKalemi.value
  if (!k) return
  if (!k.cariId) {
    toastBildirim.hata(t('taksitTakvimi.cariBulunamadi'))
    return
  }
  if (odemeForm.value.odemeYontemi === 'NAKIT' && !odemeForm.value.kasaId) {
    toastBildirim.hata(t('taksitTakvimi.kasaZorunlu'))
    return
  }
  if (odemeForm.value.odemeYontemi !== 'NAKIT' && !odemeForm.value.bankaId) {
    toastBildirim.hata(t('taksitTakvimi.bankaZorunlu'))
    return
  }

  odemeKaydediliyor.value = true
  try {
    await tahsilatAPI.gir({
      cariId: k.cariId,
      tutar: k.tutar,
      odemeYontemi: odemeForm.value.odemeYontemi,
      kasaId: odemeForm.value.odemeYontemi === 'NAKIT' ? odemeForm.value.kasaId : null,
      bankaId: odemeForm.value.odemeYontemi !== 'NAKIT' ? odemeForm.value.bankaId : null,
      hareketTarihi: getLocalDateString(odemeForm.value.hareketTarihi) || null,
      taksitId: k.id,
      aciklama: `${t('taksitTakvimi.taksitOdemesi')} #${k.taksitNo}/${k.taksitSayisi}`
    })
    toastBildirim.basarili(t('taksitTakvimi.tahsilatKaydedildi'))
    odemeDialog.value = false
    odemeKalemi.value = null
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  } finally {
    odemeKaydediliyor.value = false
  }
}

const ayAdi = computed(() => {
  const d = new Date(seciliYil.value, seciliAy.value - 1, 1)
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    month: 'long', year: 'numeric'
  }).format(d)
})

const takvimHucreleri = computed(() => {
  const ilk = new Date(seciliYil.value, seciliAy.value - 1, 1)
  const gunSayisi = new Date(seciliYil.value, seciliAy.value, 0).getDate()
  let offset = ilk.getDay() - 1
  if (offset < 0) offset = 6

  const hucreler = []
  for (let i = 0; i < offset; i++) {
    hucreler.push({ key: `bos-${i}`, gun: null, kalemler: [] })
  }
  const simdi = new Date()
  for (let g = 1; g <= gunSayisi; g++) {
    const gunKalemleri = kalemler.value.filter((k) => {
      // REDTEAM/Faz1.6: `new Date(k.vadeTarihi).getDate()` saat dilimine
      // bagliydi. Backend 'YYYY-MM-DD' gonderiyor; bu, JS'te UTC gece yarisi
      // olarak ayristirilir ve getDate() YEREL saati dondurur. Negatif UTC
      // ofsetli bir tarayicida (orn. America/*) HER KALEM bir gun once
      // gorunurdu. Projenin kendi parseDate yardimcisi "YYYY-MM-DD"ni yerel
      // olarak ayristiriyor; dosya zaten format.js'ten formatTarih aliyor.
      const d = parseYerelTarih(k.vadeTarihi)
      return d !== null && d.getDate() === g
    })
    hucreler.push({
      key: `gun-${g}`,
      gun: g,
      bugun: simdi.getFullYear() === seciliYil.value
        && simdi.getMonth() === seciliAy.value - 1
        && simdi.getDate() === g,
      kalemler: gunKalemleri
    })
  }
  return hucreler
})

const kalemSinif = (k) => {
  if (k.odemeDurumu === 'ODENDI') return 'kalem-odendi'
  if (k.gecikmeGunu > 0) return 'kalem-gecikmis'
  return 'kalem-bekleyen'
}

const durumSeverity = (k) => {
  if (k.odemeDurumu === 'ODENDI') return 'success'
  if (k.gecikmeGunu > 0) return 'danger'
  return 'warning'
}

const durumEtiket = (k) => {
  if (k.odemeDurumu === 'ODENDI') return t('taksitTakvimi.odendi')
  if (k.gecikmeGunu > 0) return t('taksitTakvimi.gecikmis')
  return t('taksitTakvimi.bekliyor')
}

const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (cariFiltre.value) p.cariId = cariFiltre.value
  if (durumFiltre.value !== 'TUMU') p.durum = durumFiltre.value
  if (arama.value.trim()) p.q = arama.value.trim()
  if (tarihAraligi.value?.[0]) p.baslangic = getLocalDateString(tarihAraligi.value[0])
  if (tarihAraligi.value?.[1]) p.bitis = getLocalDateString(tarihAraligi.value[1])
  return p
}

const yukleListe = async () => {
  listeYukleniyor.value = true
  try {
    const r = await taksitAPI.listele(filtreParams())
    liste.value = unwrapList(r)
    toplam.value = r.data?.totalElements ?? liste.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
  listeYukleniyor.value = false
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [takvimRes, ozetRes] = await Promise.all([
      taksitAPI.takvim(seciliYil.value, seciliAy.value),
      taksitAPI.ozet()
    ])
    kalemler.value = takvimRes.data || []
    ozet.value = { ...ozet.value, ...(ozetRes.data || {}) }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
  yukleniyor.value = false
  await yukleListe()
  await planlariYukle()
}

const planlariYukle = async () => {
  planYukleniyor.value = true
  try {
    const r = await taksitAPI.planlar(planArama.value.trim() ? { q: planArama.value.trim() } : {})
    planlar.value = Array.isArray(r.data) ? r.data : []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
  planYukleniyor.value = false
}

const planAramaGirdi = () => {
  clearTimeout(planAramaZamanlayici)
  planAramaZamanlayici = setTimeout(planlariYukle, 300)
}

const planYuzde = (p) => {
  const adet = Number(p?.taksitSayisi || 0)
  const odenen = Number(p?.odenenAdet || 0)
  return adet ? Math.min(100, Math.round((odenen / adet) * 100)) : 0
}

const planAksiyonlari = (p) => {
  const items = [{ etiket: t('taksitTakvimi.detay'), ikon: 'pi pi-search', islem: () => planDetayAc(p) }]
  if (silYetkisi.value) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => planSilOnay(p) })
  }
  return items
}

const planDetayAc = async (p) => {
  seciliPlan.value = p
  planKalemleri.value = []
  planDrawer.value = true
  planKalemYukleniyor.value = true
  try {
    const r = await taksitAPI.planDetay(p.planNo)
    planKalemleri.value = Array.isArray(r.data) ? r.data : []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  } finally {
    planKalemYukleniyor.value = false
  }
}

const planSilOnay = (p) => {
  confirm.require({
    message: t('taksitTakvimi.planSilOnay', { plan: p.planNo }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await taksitAPI.planSil(p.planNo)
        planDrawer.value = false
        toastBildirim.basarili(t('taksitTakvimi.planSilindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.planSilinemedi'))
      }
    }
  })
}

const carileriYukle = async () => {
  if (cariler.value.length) return
  try {
    const r = await cariHesapAPI.getAll({ size: 500 })
    cariler.value = unwrapList(r)
  } catch {
    cariler.value = []
  }
}

const filtreDegisti = () => {
  sayfa.value = 0
  yukleListe()
}

const aramaGirdi = () => {
  clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(filtreDegisti, 300)
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  localStorage.setItem(SAYFA_ANAHTAR, String(e.rows))
  yukleListe()
}

const siralaDegisti = (e) => {
  sirala.value = `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
  sayfa.value = 0
  yukleListe()
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = filtreParams()
    delete params.page
    delete params.size
    delete params.sort
    const res = await excelAPI.taksitler(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Taksitler-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('taksitTakvimi.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('taksitTakvimi.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const kalemAksiyonlari = (k) => {
  const items = [{ etiket: t('taksitTakvimi.detay'), ikon: 'pi pi-search', islem: () => kalemDetay(k) }]
  if (k.odemeDurumu !== 'ODENDI' && odeYetkisi.value) {
    items.push({ etiket: t('taksitTakvimi.ode'), ikon: 'pi pi-check', sinif: 'eylem-pozitif', islem: () => ode(k) })
  }
  if (silYetkisi.value) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(k) })
  }
  return items
}

const ayDegistir = (delta) => {
  let ay = seciliAy.value + delta
  let yil = seciliYil.value
  if (ay < 1) { ay = 12; yil -= 1 }
  if (ay > 12) { ay = 1; yil += 1 }
  seciliAy.value = ay
  seciliYil.value = yil
  yukle()
}

const planDialogAc = async () => {
  form.value = {
    cariId: null,
    toplamTutar: null,
    taksitSayisi: 3,
    periyotAy: 1,
    baslangicTarihi: new Date(),
    kurum: '',
    aciklama: ''
  }
  if (!cariler.value.length) {
    try {
      const r = await cariHesapAPI.getAll({ size: 500 })
      cariler.value = unwrapList(r)
    } catch (err) {
      toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
    }
  }
  planDialog.value = true
}

const planKaydet = async () => {
  if (!form.value.cariId || !form.value.toplamTutar || form.value.toplamTutar <= 0) {
    toastBildirim.uyari(t('taksitTakvimi.formEksik'))
    return
  }
  kaydediliyor.value = true
  try {
    await taksitAPI.planOlustur({
      cariId: form.value.cariId,
      toplamTutar: form.value.toplamTutar,
      taksitSayisi: form.value.taksitSayisi,
      periyotAy: form.value.periyotAy,
      baslangicTarihi: getLocalDateString(form.value.baslangicTarihi),
      kurum: form.value.kurum || null,
      aciklama: form.value.aciklama || null
    })
    planDialog.value = false
    toastBildirim.basarili(t('taksitTakvimi.planOlusturuldu'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
  kaydediliyor.value = false
}

const kalemDetay = (k) => {
  detayKalem.value = k
  detayDrawer.value = true
}

const detayOde = (k) => {
  detayDrawer.value = false
  ode(k)
}

const detaySil = (k) => {
  detayDrawer.value = false
  sil(k)
}

const planDrawerOde = (k) => {
  planDrawer.value = false
  ode(k)
}

// REDTEAM/Faz1.2: Eski `ode` fonksiyonu kaldırıldı. `POST /taksitler/{id}/ode`
// ucu YALNIZCA odemeDurumu alanini yaziyor, hicbir para hareketi uretmiyordu;
// takvim "odendi" derken cari defter "odenmedi" diyordu. Yerine yukarıdaki
// `ode()` (odeme penceresi) + `tahsilatKaydet()` kullaniliyor.

const sil = (k) => {
  confirm.require({
    message: t('taksitTakvimi.silOnay'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await taksitAPI.sil(k.id)
        toastBildirim.basarili(t('taksitTakvimi.silindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
      }
    }
  })
}

onMounted(async () => {
  await carileriYukle()
  await yukle()
})
</script>

<style scoped>
.taksit-takvimi {
  padding: 0;
}

.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.takvim-kart,
.liste-kart {
  padding: 16px 20px;
  border-radius: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  margin-bottom: 20px;
}

.takvim-baslik {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.takvim-ay {
  font-size: 16px;
  text-transform: capitalize;
}

.takvim-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.gun-baslik {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #94a3b8);
  padding-bottom: 4px;
}

.gun-hucre {
  min-height: 84px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: hidden;
}

.gun-hucre.bos {
  opacity: 0.35;
}

.gun-hucre.bugun {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent) inset;
}

.gun-no {
  font-size: 12px;
  font-weight: 600;
}

.kalem {
  border: none;
  border-radius: 6px;
  padding: 2px 5px;
  font-size: 11px;
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #fff;
}

.kalem-bekleyen {
  background: #f59e0b;
}

.kalem-gecikmis {
  background: #ef4444;
}

.kalem-odendi {
  background: #10b981;
}

.liste-baslik {
  font-size: 16px;
  margin: 0 0 12px 0;
}

.taksit-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.arac-arama {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-card);
  min-width: 200px;
  flex: 1 1 220px;
  max-width: 340px;
}
.arac-arama i {
  color: var(--text-muted);
  font-size: 13px;
}
.arac-arama :deep(.arama-input) {
  border: none;
  background: transparent;
  box-shadow: none;
  width: 100%;
  padding-left: 0;
}
.arac-secim {
  min-width: 170px;
}
.arac-tarih {
  min-width: 210px;
}

.form-satir {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  flex: 1;
}

.form-satir label {
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
}

.form-ikili {
  display: flex;
  gap: 12px;
}

.plan-onizleme {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 14px;
}

.plan-ilerleme {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
  min-width: 110px;
}
.plan-bar {
  display: inline-block;
  width: 56px;
  height: 6px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}
.plan-bar-ic {
  display: block;
  height: 100%;
  background: var(--accent);
  border-radius: 999px;
}
.detay-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

.w-full {
  width: 100%;
}

/* REDTEAM/Faz1.2: Taksit odeme penceresi */
.odeme-ozet {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 12px);
  background: var(--bg-subtle, rgba(148, 163, 184, 0.05));
}
.odeme-ozet-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  color: var(--text-secondary);
}
.odeme-ozet-satir strong {
  color: var(--text-primary);
  font-weight: 600;
}
.odeme-ozet-satir.toplam {
  padding-top: 6px;
  margin-top: 2px;
  border-top: 1px solid var(--border);
  font-size: 14px;
}
.odeme-ozet-satir.toplam strong {
  color: var(--accent);
  font-size: 16px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.odeme-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.odeme-form > label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-top: 4px;
}

.taksit-mobil,
.plan-mobil {
  display: none;
}
.taksit-skeleton,
.plan-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.taksit-mobil-kart,
.plan-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.tmk-ust,
.pmk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.tmk-ad,
.pmk-ad {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13.5px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}
.tmk-ad i,
.pmk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.tmk-ad span,
.pmk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tmk-satir,
.pmk-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.tmk-satir i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.tmk-tutar {
  color: var(--accent);
}
.tmk-alt,
.pmk-alt {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-muted);
}
.pmk-oran {
  font-weight: 700;
  color: var(--text-secondary);
}
.pmk-bar {
  flex: 1 1 auto;
  max-width: 120px;
}
.pmk-kalan {
  margin-left: auto;
  color: var(--accent);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 768px) {
  .taksit-masa,
  .plan-masa {
    display: none;
  }
  .taksit-mobil,
  .plan-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .arac-bosluk {
    display: none;
  }
  .arac-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .arac-secim,
  .arac-tarih {
    flex: 1 1 45%;
    min-width: 0;
  }
  .takvim-grid {
    gap: 3px;
  }
  .gun-hucre {
    min-height: 46px;
    padding: 2px;
    flex-direction: row;
    flex-wrap: wrap;
    align-content: flex-start;
    gap: 2px;
  }
  .kalem {
    width: 8px;
    height: 8px;
    padding: 0;
    border-radius: 50%;
    font-size: 0;
    line-height: 0;
  }
  .kalem-metin {
    display: none;
  }
  :deep(.p-tabview-nav) {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  :deep(.p-tabview-nav li) {
    flex-shrink: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kalem,
  .plan-bar-ic {
    transition: none;
  }
}
</style>
