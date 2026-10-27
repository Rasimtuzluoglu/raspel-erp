<template>
  <div class="butce-sayfasi">
    <PageHeader
      :title="t('butceler.title')"
      :description="t('butceler.altBaslik')"
      icon="pi pi-wallet"
    >
      <template #actions>
        <Button
          :label="t('butceler.excel')"
          icon="pi pi-file-excel"
          outlined
          :loading="listeExcelYukleniyor"
          @click="listeExcelIndir"
        />
        <Button
          :label="t('butceler.yeniButce')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </template>
    </PageHeader>

    <TabView v-model:active-index="aktifSekme">
      <TabPanel :header="t('butceler.tabButceler')">
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
              :baslik="t('butceler.kpiButce')"
              :deger="ozet.butceToplam"
              ikon="pi pi-wallet"
              renk="#3b82f6"
            />
            <KpiKart
              :baslik="t('butceler.kpiGerceklesen')"
              :deger="ozet.gerceklesenToplam"
              ikon="pi pi-chart-line"
              renk="#10b981"
            />
            <KpiKart
              :baslik="t('butceler.kpiSapma')"
              :deger="ozet.sapma"
              ikon="pi pi-arrows-h"
              :renk="Number(ozet.sapma) > 0 ? '#ef4444' : '#64748b'"
            />
            <KpiKart
              :baslik="t('butceler.kpiKullanim')"
              :deger="Number(ozet.kullanimYuzde) || 0"
              ikon="pi pi-percentage"
              :renk="Number(ozet.kullanimYuzde) > 100 ? '#ef4444' : '#8b5cf6'"
              :para-birimi="false"
            >
              <template #deger>
                {{ ozet.kullanimYuzde != null ? ozet.kullanimYuzde + '%' : '-' }}
              </template>
            </KpiKart>
          </template>
        </div>

        <div class="butce-arac">
          <Dropdown
            v-model="yil"
            :options="yilSecenekleri"
            class="arac-secim"
            :aria-label="t('butceler.yil')"
            @change="filtreDegisti"
          />
          <Dropdown
            v-model="ay"
            :options="aySecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('butceler.tumAylar')"
            :aria-label="t('butceler.ay')"
            @change="filtreDegisti"
          />
          <Dropdown
            v-model="kategori"
            :options="kategoriler"
            show-clear
            class="arac-secim"
            :placeholder="t('butceler.kategori')"
            :aria-label="t('butceler.kategori')"
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
            @change="filtreDegisti"
          />
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="q"
              :placeholder="t('butceler.aramaPlaceholder')"
              :aria-label="t('butceler.aramaPlaceholder')"
              @input="aramaDegisti"
            />
          </IconField>
          <div
            class="tur-seg"
            role="group"
            :aria-label="t('butceler.tur')"
          >
            <button
              v-for="secenek in turSecenekleri"
              :key="secenek.value"
              type="button"
              :class="{ aktif: tur === secenek.value }"
              :aria-pressed="tur === secenek.value"
              @click="turSec(secenek.value)"
            >
              {{ secenek.label }}
            </button>
          </div>
          <span class="arac-bosluk" />
          <Button
            v-if="filtreVar"
            :label="t('denetim.temizle')"
            icon="pi pi-filter-slash"
            class="p-button-text p-button-sm"
            @click="filtreTemizle"
          />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            class="p-button-text p-button-sm"
            :loading="yukleniyor"
            @click="yukle"
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
          :total-records="toplam"
          :rows="boyut"
          :loading="yukleniyor"
          striped-rows
          class="butce-masa"
          gorunum-anahtari="butce-listesi"
          :empty-message="filtreVar ? t('butceler.filtreYok') : t('butceler.empty')"
          @page="sayfaDegisti"
          @sort="siralaDegisti"
        >
          <template #empty>
            <EmptyState
              v-if="!yukleniyor"
              :message="filtreVar ? t('butceler.filtreYok') : t('butceler.empty')"
              :sub-message="filtreVar ? null : t('butceler.emptyHint')"
              icon="pi pi-chart-bar"
              :action-label="t('butceler.yeniButce')"
              action-icon="pi pi-plus"
              @action="dialogAc()"
            />
          </template>
          <Column
            field="kategori"
            :header="t('butceler.kategori')"
            sortable
          />
          <Column
            field="ad"
            :header="t('butceler.ad')"
            sortable
          />
          <Column
            :header="t('butceler.donem')"
            style="width: 110px"
          >
            <template #body="{ data }">
              {{ data.ay }}/{{ data.yil }}
            </template>
          </Column>
          <Column
            field="tur"
            :header="t('butceler.tur')"
            style="width: 110px"
            sortable
          >
            <template #body="{ data }">
              <Tag
                :value="data.tur === 'GELIR' ? t('butceler.gelir') : t('butceler.gider')"
                :severity="data.tur === 'GELIR' ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            field="durum"
            :header="t('common.status')"
            style="width: 120px"
          >
            <template #body="{ data }">
              <Tag
                :value="durumAdi(data.durum)"
                :severity="durumSeverity(data.durum)"
              />
            </template>
          </Column>
          <Column
            field="tutar"
            :header="t('common.amount')"
            style="width: 150px"
            sortable
          >
            <template #body="{ data }">
              <span class="gizli-veri">{{ formatCurrency(data.tutar) }}</span>
            </template>
          </Column>
          <Column
            :header="t('butceler.islem')"
            style="width: 70px"
          >
            <template #body="{ data }">
              <SatirEylemleri
                :baslik="data.ad"
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="satirEylemleri(data)"
              />
            </template>
          </Column>
        </AppDataTable>

        <!-- Mobil kart görünümü -->
        <div class="butce-mobil">
          <SkeletonLoader
            v-if="yukleniyor && !list.length"
            :count="4"
            height="92px"
          />
          <EmptyState
            v-else-if="!list.length"
            :message="filtreVar ? t('butceler.filtreYok') : t('butceler.empty')"
            icon="pi pi-chart-bar"
          />
          <div
            v-for="b in list"
            :key="b.id"
            class="butce-mobil-kart"
            @click="dialogAc(b)"
          >
            <div class="bm-ust">
              <strong>{{ b.ad }}</strong>
              <SatirEylemleri
                :baslik="b.ad"
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="satirEylemleri(b)"
                @click.stop
              />
            </div>
            <div class="bm-satir">
              <Tag
                :value="b.tur === 'GELIR' ? t('butceler.gelir') : t('butceler.gider')"
                :severity="b.tur === 'GELIR' ? 'success' : 'danger'"
              />
              <span>{{ b.kategori || '-' }} · {{ b.ay }}/{{ b.yil }}</span>
            </div>
            <div class="bm-tutar gizli-veri">
              {{ formatCurrency(b.tutar) }}
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel :header="t('butceler.tabGerceklesen')">
        <div class="butce-arac">
          <div
            class="tur-seg"
            role="group"
            :aria-label="t('butceler.tur')"
          >
            <button
              v-for="secenek in gerTurSecenekleri"
              :key="secenek.value"
              type="button"
              :class="{ aktif: gerTur === secenek.value }"
              :aria-pressed="gerTur === secenek.value"
              @click="gerTurSec(secenek.value)"
            >
              {{ secenek.label }}
            </button>
          </div>
          <Dropdown
            v-model="gerYil"
            :options="yilSecenekleri"
            class="arac-secim"
            :aria-label="t('butceler.yil')"
            @change="gerYukle"
          />
          <Dropdown
            v-model="gerAy"
            :options="aySecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('butceler.tumAylar')"
            :aria-label="t('butceler.ay')"
            @change="gerYukle"
          />
          <span class="arac-bosluk" />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            class="p-button-text p-button-sm"
            :loading="gerYukleniyor"
            @click="gerYukle"
          />
          <Button
            :label="t('butceler.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="gerExcelYukleniyor"
            @click="gerExcelIndir"
          />
          <Button
            :label="t('butceler.pdf')"
            icon="pi pi-file-pdf"
            outlined
            size="small"
            :loading="gerPdfYukleniyor"
            @click="gerPdfIndir"
          />
        </div>

        <p
          v-if="gerTur === 'GELIR'"
          class="gelir-notu"
        >
          <i class="pi pi-info-circle" /> {{ t('butceler.gelirNotu') }}
        </p>

        <SkeletonLoader
          v-if="gerYukleniyor && !ger.satirlar?.length"
          class="tablo-iskelet"
          :count="5"
          height="48px"
        />

        <template v-else>
          <div class="ger-ozet">
            <div class="ger-metrik">
              <span>{{ t('butceler.butce') }}</span>
              <strong class="gizli-veri">{{ formatCurrency(ger.butceToplam) }}</strong>
            </div>
            <div class="ger-metrik">
              <span>{{ t('butceler.gerceklesen') }}</span>
              <strong class="gizli-veri">{{ formatCurrency(ger.gerceklesenToplam) }}</strong>
            </div>
            <div class="ger-metrik">
              <span>{{ t('butceler.sapma') }}</span>
              <strong
                class="gizli-veri"
                :class="Number(ger.sapma) > 0 ? 'negative' : 'positive'"
              >{{ formatCurrency(ger.sapma) }}</strong>
            </div>
            <div class="ger-metrik">
              <span>{{ t('butceler.kullanim') }}</span>
              <strong>{{ ger.kullanimYuzde != null ? ger.kullanimYuzde + '%' : '-' }}</strong>
            </div>
          </div>

          <EmptyState
            v-if="!ger.satirlar?.length"
            icon="pi pi-chart-line"
            :message="t('butceler.gerceklesenBos')"
          />

          <AppDataTable
            v-else
            :value="ger.satirlar"
            :paginator="false"
            striped-rows
            :empty-message="t('butceler.gerceklesenBos')"
          >
            <Column
              field="kategori"
              :header="t('butceler.kategori')"
            />
            <Column
              :header="t('butceler.butce')"
              style="width: 150px"
            >
              <template #body="{ data }">
                <span class="gizli-veri">{{ formatCurrency(data.butce) }}</span>
              </template>
            </Column>
            <Column
              :header="t('butceler.gerceklesen')"
              style="width: 150px"
            >
              <template #body="{ data }">
                <span
                  v-if="data.gerceklesen != null"
                  class="gizli-veri"
                >{{ formatCurrency(data.gerceklesen) }}</span>
                <span
                  v-else
                  class="muted"
                >—</span>
              </template>
            </Column>
            <Column
              :header="t('butceler.sapma')"
              style="width: 150px"
            >
              <template #body="{ data }">
                <span
                  v-if="data.sapma != null"
                  class="gizli-veri"
                  :class="Number(data.sapma) > 0 ? 'negative' : 'positive'"
                >{{ formatCurrency(data.sapma) }}</span>
                <span
                  v-else
                  class="muted"
                >—</span>
              </template>
            </Column>
            <Column
              :header="t('butceler.kullanim')"
              style="width: 180px"
            >
              <template #body="{ data }">
                <div
                  v-if="data.kullanimYuzdesi != null"
                  class="kullanim-hucre"
                >
                  <div
                    class="kullanim-bar"
                    role="progressbar"
                    :aria-valuenow="Math.min(Number(data.kullanimYuzdesi), 100)"
                    aria-valuemin="0"
                    aria-valuemax="100"
                  >
                    <div
                      class="kullanim-dolu"
                      :class="{ asim: Number(data.kullanimYuzdesi) > 100 }"
                      :style="{ width: `${Math.min(Number(data.kullanimYuzdesi), 100)}%` }"
                    />
                  </div>
                  <span>{{ data.kullanimYuzdesi }}%</span>
                </div>
                <span
                  v-else
                  class="muted"
                >—</span>
              </template>
            </Column>
          </AppDataTable>
        </template>
      </TabPanel>

      <TabPanel :header="t('butceler.tabMatris')">
        <div class="butce-arac">
          <Dropdown
            v-model="matrisYil"
            :options="yilSecenekleri"
            class="arac-secim"
            :aria-label="t('butceler.yil')"
            @change="matrisYukle"
          />
          <Dropdown
            v-model="matrisTur"
            :options="matrisTurSecenekleri"
            option-label="label"
            option-value="value"
            class="arac-secim"
            :aria-label="t('butceler.tur')"
            @change="matrisYukle"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('butceler.matrisKopyala')"
            icon="pi pi-copy"
            outlined
            size="small"
            @click="kopyalaDialogAc"
          />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            class="p-button-text p-button-sm"
            :loading="matrisYukleniyor"
            @click="matrisYukle"
          />
          <Button
            :label="t('butceler.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="matrisExcelYukleniyor"
            @click="matrisExcelIndir"
          />
        </div>

        <SkeletonLoader
          v-if="matrisYukleniyor && !matris.satirlar?.length"
          class="tablo-iskelet"
          :count="5"
          height="44px"
        />

        <EmptyState
          v-else-if="!matris.satirlar?.length"
          icon="pi pi-table"
          :message="t('butceler.matrisBos')"
        />

        <div
          v-else
          class="matris-sarmal"
        >
          <table class="matris">
            <thead>
              <tr>
                <th class="matris-kategori">
                  {{ t('butceler.kategori') }}
                </th>
                <th
                  v-for="(ayAdi, i) in ayEtiketleri"
                  :key="i"
                  class="matris-ay"
                >
                  {{ ayAdi }}
                </th>
                <th class="matris-toplam">
                  {{ t('butceler.toplam') }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="satir in matris.satirlar"
                :key="satir.kategori"
              >
                <th class="matris-kategori">
                  {{ satir.kategori }}
                </th>
                <td
                  v-for="(deger, i) in satir.aylar"
                  :key="i"
                  class="matris-hucre"
                >
                  {{ hucre(deger) }}
                </td>
                <td class="matris-toplam gizli-veri">
                  {{ formatCurrency(satir.toplam) }}
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <th class="matris-kategori">
                  {{ t('butceler.toplam') }}
                </th>
                <td
                  v-for="(deger, i) in matris.aylikToplam || []"
                  :key="i"
                  class="matris-hucre toplam-hucre"
                >
                  {{ hucre(deger) }}
                </td>
                <td class="matris-toplam gizli-veri">
                  {{ formatCurrency(matris.genelToplam) }}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </TabPanel>

      <TabPanel :header="t('butceler.tabOnay')">
        <div class="butce-arac">
          <div class="onay-baslik">
            <i class="pi pi-inbox" />
            <span>{{ t('butceler.onayBekleyenBaslik') }}</span>
            <Tag
              :value="String(onayListe.length)"
              severity="warn"
            />
          </div>
          <span class="arac-bosluk" />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            class="p-button-text p-button-sm"
            :loading="onayYukleniyor"
            @click="onayYukle"
          />
        </div>

        <SkeletonLoader
          v-if="onayYukleniyor && !onayListe.length"
          class="tablo-iskelet"
          :count="3"
          height="56px"
        />

        <EmptyState
          v-else-if="!onayListe.length"
          icon="pi pi-check-circle"
          :message="t('butceler.onayBekleyenYok')"
        />

        <div
          v-else
          class="onay-liste"
        >
          <div
            v-for="b in onayListe"
            :key="b.id"
            class="onay-kart"
          >
            <div class="onay-kart-ust">
              <strong>{{ b.ad }}</strong>
              <div class="onay-rozetler">
                <Tag
                  :value="b.tur === 'GELIR' ? t('butceler.gelir') : t('butceler.gider')"
                  :severity="b.tur === 'GELIR' ? 'success' : 'danger'"
                />
                <Tag
                  :value="`v${b.revizyon || 1}`"
                  severity="secondary"
                />
              </div>
            </div>
            <div class="onay-meta">
              <span>{{ b.kategori || '-' }} · {{ b.ay }}/{{ b.yil }}</span>
              <strong class="gizli-veri">{{ formatCurrency(b.tutar) }}</strong>
            </div>
            <div class="onay-aksiyonlar">
              <Button
                :label="t('butceler.onayla')"
                icon="pi pi-check"
                size="small"
                severity="success"
                @click="onayla(b)"
              />
              <Button
                :label="t('butceler.reddet')"
                icon="pi pi-times"
                size="small"
                severity="danger"
                outlined
                @click="reddetAc(b)"
              />
              <Button
                :label="t('butceler.detay')"
                icon="pi pi-search"
                size="small"
                text
                @click="detayAc(b)"
              />
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <ButceDetayDrawer
      v-model:visible="detayDrawer"
      :kayit="detayKayit"
      :revizyonlar="detayRevizyonlar"
      :revizyon-yukleniyor="detayYukleniyor"
      @duzenle="drawerDuzenle"
      @onayla="onayla"
      @reddet="reddetAc"
    />

    <Dialog
      v-model:visible="redDialog"
      :header="t('butceler.reddetBaslik')"
      modal
      :style="{ width: '420px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <p
        v-if="redHedef"
        class="red-ozet"
      >
        {{ redHedef.ad }} · {{ redHedef.kategori || '-' }} · {{ redHedef.ay }}/{{ redHedef.yil }}
      </p>
      <Textarea
        v-model="redGerekce"
        rows="3"
        class="w-full"
        :placeholder="t('butceler.redGerekcesiPlaceholder')"
      />
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="redDialog = false"
        />
        <Button
          :label="t('butceler.reddet')"
          icon="pi pi-times"
          severity="danger"
          @click="reddetKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('butceler.adZorunlu') }}</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('butceler.yil') }}</label><InputNumber
              v-model="form.yil"
              class="w-full"
              :min="2000"
              :max="2100"
              :use-grouping="false"
            />
          </div>
          <div class="field">
            <label>{{ t('butceler.ayAralik') }}</label><InputNumber
              v-model="form.ay"
              class="w-full"
              :min="1"
              :max="12"
              :use-grouping="false"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('butceler.turZorunlu') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekleri.filter((s) => s.value !== 'TUMU')"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.amount') }}</label><InputNumber
            v-model="form.tutar"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('butceler.kategori') }}</label><InputText
            v-model="form.kategori"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
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
      v-model:visible="kopyalaDialog"
      :header="t('butceler.kopyalaBaslik')"
      modal
      :style="{ width: '420px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('butceler.kaynakYil') }}</label>
            <InputNumber
              v-model="kopyalaForm.kaynakYil"
              class="w-full"
              :min="2000"
              :max="2100"
              :use-grouping="false"
            />
          </div>
          <div class="field">
            <label>{{ t('butceler.hedefYil') }}</label>
            <InputNumber
              v-model="kopyalaForm.hedefYil"
              class="w-full"
              :min="2000"
              :max="2100"
              :use-grouping="false"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('butceler.tur') }}</label>
          <Dropdown
            v-model="kopyalaForm.tur"
            :options="gerTurSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <small class="muted">{{ t('butceler.kopyalaIpucu') }}</small>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kopyalaDialog = false"
        />
        <Button
          :label="t('butceler.kopyala')"
          icon="pi pi-copy"
          @click="kopyalaKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { butceAPI, excelAPI } from '../api/index.js'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import ButceDetayDrawer from '../components/ButceDetayDrawer.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t, locale } = useI18n()

// ---- Hafıza ----
const TAB_ANAHTAR = 'raspel_butce_tab'
const FILTRE_ANAHTAR = 'raspel_butce_filtre'
const kayitliFiltre = (() => {
  try {
    return JSON.parse(localStorage.getItem(FILTRE_ANAHTAR) || '{}') || {}
  } catch {
    return {}
  }
})()

const aktifSekme = ref(Number(localStorage.getItem(TAB_ANAHTAR)) || 0)

const yil = ref(kayitliFiltre.yil || new Date().getFullYear())
const ay = ref(kayitliFiltre.ay || null)
const tur = ref(kayitliFiltre.tur || 'GIDER')
const kategori = ref(kayitliFiltre.kategori || null)
const durum = ref(kayitliFiltre.durum || null)
const q = ref(kayitliFiltre.q || '')

const sayfa = ref(0)
const boyut = ref(25)
const sirala = ref('yil,desc')

const list = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)
const ozet = ref({})
const ozetYuklendi = ref(false)
const kategoriler = ref([])
let aramaZaman = null

const listeExcelYukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref(bosForm())

function bosForm() {
  return {
    ad: '',
    yil: new Date().getFullYear(),
    ay: new Date().getMonth() + 1,
    tur: 'GIDER',
    tutar: 0,
    kategori: '',
    aciklama: ''
  }
}

const turSecenekleri = computed(() => [
  { value: 'TUMU', label: t('butceler.tumu') },
  { value: 'GELIR', label: t('butceler.gelir') },
  { value: 'GIDER', label: t('butceler.gider') }
])

const durumSecenekleri = computed(() => [
  { value: 'TASLAK', label: t('butceler.durumTaslak') },
  { value: 'ONAYLANDI', label: t('butceler.durumOnaylandi') },
  { value: 'REDDEDILDI', label: t('butceler.durumReddedildi') }
])

const yilSecenekleri = computed(() => {
  const buYil = new Date().getFullYear()
  return Array.from({ length: 7 }, (_, i) => buYil - 5 + i).reverse()
})
const aySecenekleri = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((a) => ({ label: a, value: a }))

const filtreVar = computed(
  () => !!q.value?.trim() || !!ay.value || !!kategori.value || !!durum.value || tur.value !== 'TUMU'
)
const ozetIskelet = computed(() => !ozetYuklendi.value)
const listeIskelet = computed(() => yukleniyor.value && !list.value.length)
const listeDuyuru = computed(() =>
  yukleniyor.value ? '' : t('butceler.kayitDuyuru', { gorunen: list.value.length, toplam: toplam.value })
)

// ---- Gerçekleşen raporu (tür ayrımlı) ----
const gerTur = ref('GIDER')
const gerYil = ref(new Date().getFullYear())
const gerAy = ref(null)
const ger = ref({ satirlar: [] })
const gerYukleniyor = ref(false)
const gerYuklendi = ref(false)
const gerExcelYukleniyor = ref(false)
const gerPdfYukleniyor = ref(false)

const gerTurSecenekleri = computed(() => [
  { value: 'GIDER', label: t('butceler.gider') },
  { value: 'GELIR', label: t('butceler.gelir') }
])

// ---- 12 aylık matris ----
const matrisYil = ref(yil.value)
const matrisTur = ref('GIDER')
const matris = ref({ satirlar: [] })
const matrisYukleniyor = ref(false)
const matrisYuklendi = ref(false)
const matrisExcelYukleniyor = ref(false)
const kopyalaDialog = ref(false)
const kopyalaForm = ref({
  kaynakYil: new Date().getFullYear() - 1,
  hedefYil: new Date().getFullYear(),
  tur: 'GIDER'
})

const matrisTurSecenekleri = computed(() => [
  { value: 'TUMU', label: t('butceler.tumu') },
  { value: 'GELIR', label: t('butceler.gelir') },
  { value: 'GIDER', label: t('butceler.gider') }
])
const ayEtiketleri = computed(() =>
  Array.from({ length: 12 }, (_, i) =>
    new Date(2024, i, 1).toLocaleDateString(locale.value === 'en' ? 'en-US' : 'tr-TR', { month: 'short' })
  )
)
const hucre = (v) => {
  const n = Number(v) || 0
  return n === 0
    ? '–'
    : n.toLocaleString(locale.value === 'en' ? 'en-US' : 'tr-TR', { maximumFractionDigits: 2 })
}

const matrisYukle = async () => {
  matrisYukleniyor.value = true
  try {
    const params = { yil: matrisYil.value }
    if (matrisTur.value !== 'TUMU') params.tur = matrisTur.value
    const r = await butceAPI.matris(params)
    matris.value = r.data || { satirlar: [] }
    matrisYuklendi.value = true
  } catch (err) {
    matris.value = { satirlar: [] }
    toastBildirim.hata(err?.response?.data?.message || t('butceler.hataYukleme'))
  } finally {
    matrisYukleniyor.value = false
  }
}

const matrisExcelIndir = async () => {
  matrisExcelYukleniyor.value = true
  try {
    const params = { yil: matrisYil.value }
    if (matrisTur.value !== 'TUMU') params.tur = matrisTur.value
    const res = await excelAPI.butceMatris(params)
    indir(res.data, `butce-matris-${matrisYil.value}.xlsx`)
    toastBildirim.basarili(t('butceler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('butceler.excelHata'))
  } finally {
    matrisExcelYukleniyor.value = false
  }
}

const kopyalaDialogAc = () => {
  kopyalaForm.value = {
    kaynakYil: matrisYil.value - 1,
    hedefYil: matrisYil.value,
    tur: matrisTur.value === 'TUMU' ? 'GIDER' : matrisTur.value
  }
  kopyalaDialog.value = true
}

const kopyalaKaydet = async () => {
  try {
    const r = await butceAPI.kopyala(kopyalaForm.value)
    toastBildirim.basarili(
      t('butceler.kopyalandi', {
        kopyalanan: r.data?.kopyalanan ?? 0,
        atlanan: r.data?.atlanan ?? 0
      })
    )
    matrisYil.value = kopyalaForm.value.hedefYil
    kopyalaDialog.value = false
    matrisYukle()
    yukle()
    if (gerYuklendi.value) gerYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.kopyalanamadi'))
  }
}

const filtreParams = () => {
  const p = { yil: yil.value }
  if (ay.value) p.ay = ay.value
  if (tur.value !== 'TUMU') p.tur = tur.value
  if (kategori.value) p.kategori = kategori.value
  if (durum.value) p.durum = durum.value
  if (q.value?.trim()) p.q = q.value.trim()
  return p
}

const listeYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await butceAPI.filtreli({
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
    toastBildirim.hata(err?.response?.data?.message || t('butceler.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const ozetYukle = async () => {
  try {
    const r = await butceAPI.ozet(filtreParams())
    ozet.value = r.data || {}
  } catch {
    ozet.value = {}
  } finally {
    ozetYuklendi.value = true
  }
}

const kategorileriYukle = async () => {
  try {
    const r = await butceAPI.kategoriler({ yil: yil.value, tur: tur.value !== 'TUMU' ? tur.value : undefined })
    kategoriler.value = r.data || []
  } catch {
    kategoriler.value = []
  }
}

const yukle = () => {
  ozetYukle()
  kategorileriYukle()
  listeYukle()
}

const filtreDegisti = () => {
  sayfa.value = 0
  yukle()
}

const aramaDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(filtreDegisti, 300)
}

const turSec = (deger) => {
  if (tur.value === deger) return
  tur.value = deger
  kategori.value = null
  filtreDegisti()
}

const filtreTemizle = () => {
  q.value = ''
  ay.value = null
  kategori.value = null
  durum.value = null
  tur.value = 'TUMU'
  sayfa.value = 0
  yukle()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  boyut.value = e.rows
  listeYukle()
}

const siralaDegisti = (e) => {
  if (!e.sortField || !e.sortOrder) {
    sirala.value = 'yil,desc'
  } else {
    sirala.value = `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
  }
  sayfa.value = 0
  listeYukle()
}

const listeExcelIndir = async () => {
  listeExcelYukleniyor.value = true
  try {
    const res = await excelAPI.butceler(filtreParams())
    indir(res.data, `butceler-${yil.value}.xlsx`)
    toastBildirim.basarili(t('butceler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('butceler.excelHata'))
  } finally {
    listeExcelYukleniyor.value = false
  }
}

const gerYukle = async () => {
  gerYukleniyor.value = true
  try {
    const params = { yil: gerYil.value, tur: gerTur.value }
    if (gerAy.value) params.ay = gerAy.value
    const r = await butceAPI.gerceklesen(params)
    ger.value = r.data || { satirlar: [] }
    gerYuklendi.value = true
  } catch (err) {
    ger.value = { satirlar: [] }
    toastBildirim.hata(err?.response?.data?.message || t('butceler.hataRapor'))
  } finally {
    gerYukleniyor.value = false
  }
}

const gerTurSec = (deger) => {
  if (gerTur.value === deger) return
  gerTur.value = deger
  gerYukle()
}

const gerExcelIndir = async () => {
  gerExcelYukleniyor.value = true
  try {
    const params = { yil: gerYil.value, tur: gerTur.value }
    if (gerAy.value) params.ay = gerAy.value
    const res = await excelAPI.butceGerceklesen(params)
    indir(res.data, `butce-gerceklesen-${gerTur.value.toLowerCase()}-${gerYil.value}.xlsx`)
    toastBildirim.basarili(t('butceler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('butceler.excelHata'))
  } finally {
    gerExcelYukleniyor.value = false
  }
}

const gerPdfIndir = async () => {
  gerPdfYukleniyor.value = true
  try {
    const params = { yil: gerYil.value, tur: gerTur.value }
    if (gerAy.value) params.ay = gerAy.value
    const r = await butceAPI.gerceklesenPdf(params)
    indir(r.data, `butce-gerceklesen-${gerTur.value.toLowerCase()}-${gerYil.value}.pdf`)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.hataPdf'))
  } finally {
    gerPdfYukleniyor.value = false
  }
}

// ---- Onay akışı / revizyon / detay ----
const detayDrawer = ref(false)
const detayKayit = ref(null)
const detayRevizyonlar = ref([])
const detayYukleniyor = ref(false)
const redDialog = ref(false)
const redHedef = ref(null)
const redGerekce = ref('')
const onayListe = ref([])
const onayYukleniyor = ref(false)
const onayYuklendi = ref(false)

const durumAdi = (d) =>
  ({
    TASLAK: t('butceler.durumTaslak'),
    ONAYLANDI: t('butceler.durumOnaylandi'),
    REDDEDILDI: t('butceler.durumReddedildi')
  })[d] || d || '-'
const durumSeverity = (d) =>
  ({ TASLAK: 'warn', ONAYLANDI: 'success', REDDEDILDI: 'danger' })[d] || 'secondary'

const reloadAll = () => {
  yukle()
  onayYukle()
  if (gerYuklendi.value) gerYukle()
  if (matrisYuklendi.value) matrisYukle()
}

const detayAc = async (b) => {
  detayKayit.value = b
  detayRevizyonlar.value = []
  detayDrawer.value = true
  detayYukleniyor.value = true
  try {
    const r = await butceAPI.revizyonlar(b.id)
    detayRevizyonlar.value = r.data || []
  } catch {
    detayRevizyonlar.value = []
  } finally {
    detayYukleniyor.value = false
  }
}

const drawerDuzenle = (b) => {
  detayDrawer.value = false
  dialogAc(b)
}

const onayla = (b) => {
  confirm.require({
    message: t('butceler.onayOnay', { ad: b.ad }),
    header: t('butceler.onayla'),
    icon: 'pi pi-check',
    acceptLabel: t('butceler.onayla'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await butceAPI.onayla(b.id)
        toastBildirim.basarili(t('butceler.onaylandi'))
        detayDrawer.value = false
        reloadAll()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('butceler.onaylanamadi'))
      }
    }
  })
}

const reddetAc = (b) => {
  redHedef.value = b
  redGerekce.value = ''
  redDialog.value = true
}

const reddetKaydet = async () => {
  try {
    await butceAPI.reddet(redHedef.value.id, redGerekce.value)
    toastBildirim.basarili(t('butceler.reddedildi'))
    redDialog.value = false
    detayDrawer.value = false
    reloadAll()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.reddedilemedi'))
  }
}

const onayYukle = async () => {
  onayYukleniyor.value = true
  try {
    const r = await butceAPI.onayBekleyen({ yil: yil.value })
    onayListe.value = r.data || []
    onayYuklendi.value = true
  } catch {
    onayListe.value = []
  } finally {
    onayYukleniyor.value = false
  }
}

const satirEylemleri = (b) => {
  const items = [
    { etiket: t('butceler.detay'), ikon: 'pi pi-search', islem: () => detayAc(b) }
  ]
  if (b.durum === 'TASLAK') {
    items.push({ etiket: t('butceler.onayla'), ikon: 'pi pi-check', sinif: 'eylem-pozitif', islem: () => onayla(b) })
    items.push({ etiket: t('butceler.reddet'), ikon: 'pi pi-times', islem: () => reddetAc(b) })
  }
  items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(b) })
  items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(b) })
  return items
}

const indir = (data, dosyaAdi) => {  const blob = new Blob([data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = dosyaAdi
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 60000)
}

watch([yil, ay, tur, kategori, durum, q], () => {
  try {
    localStorage.setItem(
      FILTRE_ANAHTAR,
      JSON.stringify({
        yil: yil.value,
        ay: ay.value || null,
        tur: tur.value,
        kategori: kategori.value || null,
        durum: durum.value || null,
        q: q.value || ''
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
  if (v === 1 && !gerYuklendi.value) gerYukle()
  if (v === 2 && !matrisYuklendi.value) matrisYukle()
  if (v === 3 && !onayYuklendi.value) onayYukle()
})

const dialogHeader = computed(() => (duzenleme.value ? t('butceler.butceDuzenle') : t('butceler.yeniButce')))

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : bosForm()
  dialog.value = true
}

const kaydet = async () => {
  if (kaydediliyor.value) return
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await butceAPI.update(form.value.id, form.value)
      toastBildirim.basarili(t('butceler.guncellendi'))
    } else {
      await butceAPI.create(form.value)
      toastBildirim.basarili(t('butceler.olusturuldu'))
    }
    dialog.value = false
    reloadAll()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

const sil = (data) => {
  confirm.require({
    message: t('butceler.silOnayMesaj', { ad: data.ad }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await butceAPI.delete(data.id)
        toast.add({ severity: 'success', summary: t('butceler.silindi'), detail: t('butceler.butceSilindi'), life: 3000 })
        reloadAll()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('butceler.silmeBasarisiz'))
      }
    }
  })
}

onMounted(() => {
  yukle()
  if (aktifSekme.value === 1) gerYukle()
  if (aktifSekme.value === 2) matrisYukle()
  if (aktifSekme.value === 3) onayYukle()
})
</script>

<style scoped>
.butce-sayfasi {
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
.tablo-iskelet {
  padding: 4px 0;
}
.butce-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.butce-arac .arac-secim {
  min-width: 130px;
}
.filtre-arama {
  min-width: 220px;
  max-width: 320px;
  flex: 1 1 220px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.tur-seg {
  display: inline-flex;
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-card);
}
.tur-seg button {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  padding: 7px 14px;
  cursor: pointer;
}
.tur-seg button.aktif {
  background: var(--accent-soft);
  color: var(--accent);
}
.tur-seg button:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.butce-mobil {
  display: none;
}
.butce-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  margin-bottom: 8px;
}
.bm-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.bm-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bm-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.bm-tutar {
  font-size: 14px;
  font-weight: 700;
}
.gelir-notu {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--text-muted);
}
.ger-ozet {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.ger-metrik {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  font-size: 11.5px;
  color: var(--text-muted);
}
.ger-metrik strong {
  font-size: 16px;
  color: var(--text-primary);
}
.kullanim-hucre {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-secondary);
}
.kullanim-bar {
  flex: 1;
  height: 7px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}
.kullanim-dolu {
  height: 100%;
  border-radius: 999px;
  background: var(--accent);
}
.kullanim-dolu.asim {
  background: var(--danger, #ef4444);
}
.muted {
  color: var(--text-muted);
  font-size: 12px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field-row {
  display: flex;
  gap: 12px;
}
.field-row .field {
  flex: 1;
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
.positive {
  color: #10b981;
  font-weight: 600;
}
.negative {
  color: #ef4444;
  font-weight: 600;
}
.matris-sarmal {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.matris {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  font-size: 12px;
}
.matris th,
.matris td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
  text-align: right;
  white-space: nowrap;
}
.matris thead th {
  position: sticky;
  top: 0;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
  z-index: 1;
}
.matris-kategori {
  position: sticky;
  left: 0;
  background: var(--bg-card);
  text-align: left !important;
  min-width: 160px;
  z-index: 2;
}
.matris thead .matris-kategori {
  background: var(--bg-primary);
  z-index: 3;
}
.matris-toplam {
  font-weight: 700;
  color: var(--text-primary);
}
.matris tfoot th,
.matris tfoot td {
  border-top: 2px solid var(--border);
  border-bottom: none;
  background: var(--bg-muted);
  font-weight: 700;
}
.matris tbody tr:hover .matris-hucre {
  background: var(--accent-soft);
}
.onay-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-primary);
}
.onay-liste {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}
.onay-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.onay-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.onay-kart-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.onay-rozetler {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.onay-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.onay-aksiyonlar {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.red-ozet {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
}
@media (max-width: 768px) {
  .butce-masa {
    display: none;
  }
  .butce-mobil {
    display: block;
  }
  .butce-arac .arac-secim {
    flex: 1 1 130px;
  }
  .filtre-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .tur-seg {
    width: 100%;
    justify-content: space-between;
  }
  .tur-seg button {
    flex: 1;
  }
}
</style>
