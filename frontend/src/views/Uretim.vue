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
    <div class="kpi-satiri">
      <KpiKart
        :baslik="t('uretim.kpiTaslak')"
        :deger="ozet.taslak"
        icon="pi pi-file-edit"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('uretim.kpiUretimde')"
        :deger="ozet.uretimde"
        icon="pi pi-cog"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('uretim.kpiBuAy')"
        :deger="ozet.buAyTamamlanan"
        icon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('uretim.kpiGeciken')"
        :deger="ozet.geciken"
        icon="pi pi-clock"
        renk="#ef4444"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('uretim.kpiFireOrani')"
        :deger="Number(ozet.fireOrani) || 0"
        icon="pi pi-exclamation-triangle"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('uretim.kpiOrtSure')"
        :deger="Number(ozet.ortalamaSureSaat) || 0"
        icon="pi pi-hourglass"
        renk="#8b5cf6"
        :para-birimi="false"
      />
    </div>

    <TabView>
      <!-- EMİRLER -->
      <TabPanel :header="t('uretim.emirlerTab')">
        <AppDataTable
          :value="filtreliEmirler"
          striped-rows
          arama-aktif
          :arama-placeholder="t('common.searchPlaceholder')"
          :loading="yukleniyor"
          :empty-message="t('uretim.emirYok')"
          gorunum-anahtari="uretim-emirler"
        >
          <template #header>
            <div class="tablo-arac">
              <Dropdown
                v-model="durumFiltresi"
                :options="durumSecenekleri"
                option-label="etiket"
                option-value="deger"
                class="durum-filtre"
                :placeholder="t('common.status')"
              />
            </div>
          </template>
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
              <span class="planlanan-hucre">
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
              v-if="!yukleniyor && !emirler.length"
              :message="t('uretim.emirYok')"
              icon="pi pi-cog"
              :action-label="t('uretim.yeniEmir')"
              action-icon="pi pi-plus"
              @action="emirDialogAc()"
            />
          </template>
        </AppDataTable>
      </TabPanel>

      <!-- REÇETELER -->
      <TabPanel :header="t('uretim.recetelerTab')">
        <div class="sekme-arac">
          <Button
            :label="t('uretim.yeniRecete')"
            icon="pi pi-plus"
            class="p-button-sm"
            @click="receteDialogAc(null)"
          />
        </div>
        <AppDataTable
          :value="receteler"
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
          <h3>{{ t('uretim.durumDagilimi') }}</h3>
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
      :style="{ width: '480px' }"
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
      :style="{ width: '420px' }"
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

    <!-- Detay / Geçmiş Dialog -->
    <Dialog
      v-model:visible="detayDialog"
      :header="t('uretim.emirDetay')"
      modal
      :style="{ width: '640px' }"
    >
      <div v-if="seciliEmir">
        <div class="detay-grid">
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.mamul') }}</span>
            <span>{{ seciliEmir.urunAd }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('common.status') }}</span>
            <Tag
              :value="durumAdi(seciliEmir.durum)"
              :severity="durumSeverity(seciliEmir.durum)"
            />
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.miktar') }}</span>
            <span>{{ seciliEmir.miktar }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.uretilen') }}</span>
            <span>{{ seciliEmir.uretilenMiktar ?? '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.fire') }}</span>
            <span>{{ seciliEmir.fireMiktar ?? '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.oncelik') }}</span>
            <span>{{ oncelikAdi(seciliEmir.oncelik) }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.planlananBaslangic') }}</span>
            <span>{{ tarih(seciliEmir.planlananBaslangic) }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.planlananBitis') }}</span>
            <span>{{ tarih(seciliEmir.planlananBitis) }}</span>
          </div>
        </div>

        <h4>{{ t('uretim.maliyetBilgisi') }}</h4>
        <div class="detay-grid">
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.hammaddeMaliyeti') }}</span>
            <span>{{ para(seciliEmir.hammaddeMaliyeti) }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.iscilikMaliyeti') }}</span>
            <span>{{ para(seciliEmir.iscilikMaliyeti) }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('uretim.toplamMaliyet') }}</span>
            <strong>{{ para(seciliEmir.toplamMaliyet) }}</strong>
          </div>
        </div>

        <div
          v-if="seciliEmir.aciklama"
          class="detay-aciklama"
        >
          {{ seciliEmir.aciklama }}
        </div>

        <h4>{{ t('uretim.gecmis') }}</h4>
        <div
          v-if="detayYukleniyor"
          class="gecmis-yukleniyor"
        >
          <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
        </div>
        <ul
          v-else-if="seciliGecmis.length"
          class="gecmis-liste"
        >
          <li
            v-for="log in seciliGecmis"
            :key="log.id"
            class="gecmis-oge"
          >
            <span class="gecmis-nokta" />
            <div class="gecmis-icerik">
              <span class="gecmis-durum">
                {{ durumAdi(log.yeniDurum) }}
                <span
                  v-if="log.aciklama"
                  class="muted"
                >— {{ log.aciklama }}</span>
              </span>
              <span class="muted gecmis-tarih">{{ tarihSaat(log.olusturmaTarihi) }}</span>
            </div>
          </li>
        </ul>
        <p
          v-else
          class="muted"
        >
          {{ t('common.noData') }}
        </p>
      </div>
    </Dialog>

    <!-- Reçete Dialog -->
    <Dialog
      v-model:visible="receteDialog"
      :header="receteForm.id ? t('uretim.duzenle') : t('uretim.yeniRecete')"
      modal
      :style="{ width: '640px' }"
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
          <div class="field field-switch">
            <label>{{ t('common.status') }}</label>
            <div class="switch-satir">
              <InputSwitch v-model="receteForm.aktif" />
              <span>{{ receteForm.aktif ? t('uretim.aktif') : t('uretim.pasif') }}</span>
            </div>
          </div>
        </div>
        <div class="field">
          <label>{{ t('uretim.hammaddeler') }}</label>
          <div
            v-for="(k, i) in receteForm.kalemler"
            :key="i"
            class="kalem-satir"
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
            <Button
              icon="pi pi-trash"
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
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { uretimAPI, stokAPI } from '../api/index.js'
import KpiKart from '../components/KpiKart.vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { formatCurrency as para, formatDate as tarih, formatDateTime as tarihSaat } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

const receteler = ref([])
const emirler = ref([])
const stoklar = ref([])
const ozet = ref({})
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const ihtiyacYukleniyor = ref(false)

const durumFiltresi = ref(null)

const emirDialog = ref(false)
const tamamlaDialog = ref(false)
const iptalDialog = ref(false)
const detayDialog = ref(false)
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
  return { urunId: null, miktar: null, oncelik: 'NORMAL', planlananBaslangic: null, planlananBitis: null, aciklama: '' }
}
function bosTamamlaForm() {
  return { uretilenMiktar: null, fireMiktar: 0, iscilikMaliyeti: 0, aciklama: '' }
}
function bosReceteForm() {
  return { id: null, ad: '', urunId: null, fireOrani: 0, aktif: true, kalemler: [] }
}

const durumSecenekleri = computed(() => [
  { deger: null, etiket: t('uretim.tumDurumlar') },
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

const filtreliEmirler = computed(() =>
  durumFiltresi.value ? emirler.value.filter((e) => e.durum === durumFiltresi.value) : emirler.value
)

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
    const [r, e, o, s] = await Promise.all([
      uretimAPI.receteler(),
      uretimAPI.emirler(),
      uretimAPI.ozet(),
      stokAPI.getAll({ size: 500 })
    ])
    receteler.value = r.data || []
    emirler.value = e.data || []
    ozet.value = o.data || {}
    stoklar.value = unwrapList(s)
  } catch {
    toastBildirim.hata(t('uretim.hataYukleme'))
  } finally {
    yukleniyor.value = false
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
  detayDialog.value = true
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

const receteDialogAc = (r) => {
  if (r) {
    receteForm.value = {
      id: r.id,
      ad: r.ad || '',
      urunId: r.urunId || null,
      fireOrani: Number(r.fireOrani) || 0,
      aktif: r.aktif !== false,
      kalemler: (r.kalemler || []).map((k) => ({
        hammaddeId: k.hammaddeId,
        miktar: Number(k.miktar) || 0,
        birim: k.birim || '',
        fireOrani: Number(k.fireOrani) || 0
      }))
    }
  } else {
    receteForm.value = bosReceteForm()
  }
  receteDialog.value = true
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

onMounted(yukle)
</script>

<style scoped>
.uretim-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
}

.tablo-arac {
  display: flex;
  align-items: center;
  gap: 10px;
}
.durum-filtre {
  min-width: 160px;
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

.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.detay-aciklama {
  padding: 10px;
  border-radius: 8px;
  background: var(--bg-hover, rgba(148, 163, 184, 0.1));
  font-size: 13px;
  margin-bottom: 16px;
}

.gecmis-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.gecmis-oge {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.gecmis-nokta {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--accent);
  margin-top: 5px;
  flex-shrink: 0;
}
.gecmis-icerik {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.gecmis-durum {
  font-size: 13px;
  font-weight: 600;
}
.gecmis-tarih {
  font-variant-numeric: tabular-nums;
}
.gecmis-yukleniyor {
  color: var(--text-muted);
  font-size: 13px;
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
