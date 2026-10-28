<template>
  <div class="ceksenet-sayfasi">
    <PageHeader
      :title="t('cekSenet.title')"
      :description="t('cekSenet.altBaslik')"
      icon="pi pi-credit-card"
    >
      <template #actions>
        <Button
          :label="t('cekSenet.excel')"
          icon="pi pi-file-excel"
          outlined
          :loading="excelYukleniyor"
          @click="excelIndir"
        />
        <Button
          :label="t('cekSenet.yeniCekSenet')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </template>
    </PageHeader>

    <!-- KPI kartları -->
    <div class="kpi-satiri">
      <template v-if="ozetIskelet">
        <div
          v-for="i in 5"
          :key="i"
          class="kpi-iskelet"
        >
          <SkeletonLoader height="92px" />
        </div>
      </template>
      <template v-else>
        <KpiKart
          :baslik="t('cekSenet.kpiPortfoy')"
          :deger="ozet.portfoy || 0"
          ikon="pi pi-credit-card"
          renk="#3b82f6"
          :para-birimi="false"
        >
          <template #alt>
            {{ t('cekSenet.kpiToplamTutar') }}: {{ formatCurrency(ozet.toplamTutar) }}
          </template>
        </KpiKart>
        <KpiKart
          :baslik="t('cekSenet.kpiTeminatta')"
          :deger="ozet.teminatta || 0"
          ikon="pi pi-shield"
          renk="#f59e0b"
          :para-birimi="false"
        />
        <KpiKart
          :baslik="t('cekSenet.kpiVadesiGecen')"
          :deger="ozet.vadesiGecen || 0"
          ikon="pi pi-exclamation-triangle"
          renk="#ef4444"
          :para-birimi="false"
          trend-iyi-yonu="asagi"
        />
        <KpiKart
          :baslik="t('cekSenet.kpiBuAy')"
          :deger="ozet.buAyVadesiGelen || 0"
          ikon="pi pi-calendar-clock"
          renk="#14b8a6"
          :para-birimi="false"
        />
        <KpiKart
          :baslik="t('cekSenet.kpiProtesto')"
          :deger="ozet.protesto || 0"
          ikon="pi pi-ban"
          renk="#64748b"
          :para-birimi="false"
        />
      </template>
    </div>

    <p
      class="sr-only"
      aria-live="polite"
    >
      {{ canliDuyuru }}
    </p>

    <TabView v-model:active-index="aktifSekme">
      <!-- 1) PORTFÖY -->
      <TabPanel :header="t('cekSenet.tabPortfoy')">
        <div class="cs-arac">
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="q"
              :placeholder="t('cekSenet.aramaPlaceholder')"
              :aria-label="t('cekSenet.aramaPlaceholder')"
              @input="aramaDegisti"
            />
          </IconField>

          <div
            class="tur-seg"
            role="group"
            :aria-label="t('cekSenet.tur')"
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

          <Dropdown
            v-model="durum"
            :options="durumSecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('cekSenet.durumFiltre')"
            :aria-label="t('cekSenet.durumFiltre')"
            @change="filtreDegisti"
          />

          <Dropdown
            v-model="cariId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            filter
            show-clear
            class="arac-secim cari-secim"
            :placeholder="t('cekSenet.cariFiltre')"
            :aria-label="t('cekSenet.cariFiltre')"
            @change="filtreDegisti"
          />

          <DatePicker
            v-model="vadeAraligi"
            selection-mode="range"
            date-format="dd.mm.yy"
            show-button-bar
            show-icon
            class="arac-tarih"
            :placeholder="t('cekSenet.vadeAraligi')"
            :aria-label="t('cekSenet.vadeAraligi')"
            @update:model-value="vadeTarihiDegisti"
          />

          <InputNumber
            v-model="minTutar"
            class="arac-secim min-tutar"
            :min="0"
            :placeholder="t('cekSenet.minTutar')"
            :aria-label="t('cekSenet.minTutar')"
            @input="aramaDegisti"
          />

          <button
            type="button"
            class="protesto-sec"
            :class="{ aktif: sadeceProtesto }"
            :aria-pressed="sadeceProtesto"
            @click="protestoToggle"
          >
            <i class="pi pi-exclamation-triangle" />
            {{ t('cekSenet.protestoFiltre') }}
          </button>

          <span class="arac-bosluk" />

          <Button
            v-if="filtreVar"
            :label="t('cekSenet.filtreTemizle')"
            icon="pi pi-filter-slash"
            class="p-button-text p-button-sm"
            @click="filtreTemizle"
          />
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            class="p-button-text p-button-sm"
            :loading="yukleniyor"
            @click="yenile"
          />
        </div>

        <div
          v-if="vadeAralik"
          class="aktif-filtre"
        >
          <Tag
            :value="vadeAralikEtiketi"
            severity="info"
          />
          <Button
            icon="pi pi-times"
            :aria-label="t('cekSenet.filtreTemizle')"
            class="p-button-rounded p-button-text p-button-sm"
            @click="vadeAralikTemizle"
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
          :value="list"
          lazy
          data-key="id"
          :total-records="toplam"
          :rows="boyut"
          :loading="yukleniyor"
          striped-rows
          class="cs-masa"
          gorunum-anahtari="cek-senet-listesi"
          :empty-message="filtreVar ? t('cekSenet.filtreYok') : t('cekSenet.empty')"
          @page="sayfaDegisti"
          @sort="siralaDegisti"
          @row-click="satirTiklandi"
        >
          <template #empty>
            <EmptyState
              v-if="!yukleniyor"
              :message="filtreVar ? t('cekSenet.filtreYok') : t('cekSenet.empty')"
              :sub-message="filtreVar ? null : t('cekSenet.emptyHint')"
              icon="pi pi-credit-card"
              :action-label="t('cekSenet.yeniCekSenet')"
              action-icon="pi pi-plus"
              @action="dialogAc()"
            />
          </template>

          <Column
            field="tur"
            :header="t('cekSenet.tur')"
            style="width: 110px"
            sortable
          >
            <template #body="{ data }">
              <Tag
                :value="turAdi(data.tur)"
                :severity="data.tur === 'CEK' ? 'info' : 'warn'"
              />
            </template>
          </Column>
          <Column
            field="cariHesapAdi"
            :header="t('cekSenet.cariHesap')"
            sortable
          />
          <Column
            field="cekNo"
            :header="t('cekSenet.cekNo')"
            sortable
          >
            <template #body="{ data }">
              {{ data.cekNo || '-' }}
            </template>
          </Column>
          <Column
            field="bankaAdi"
            :header="t('cekSenet.banka')"
          >
            <template #body="{ data }">
              {{ data.bankaAdi || '-' }}
            </template>
          </Column>
          <Column
            field="vadeTarihi"
            :header="t('cekSenet.vade')"
            style="width: 130px"
            sortable
          >
            <template #body="{ data }">
              {{ formatTarih(data.vadeTarihi) }}
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
            :header="t('cekSenet.islem')"
            style="width: 70px"
          >
            <template #body="{ data }">
              <div @click.stop>
                <SatirEylemleri
                  :baslik="data.cekNo || data.cariHesapAdi || ''"
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="satirEylemleri(data)"
                />
              </div>
            </template>
          </Column>
        </AppDataTable>

        <!-- Mobil kart görünümü -->
        <div class="cs-mobil">
          <SkeletonLoader
            v-if="yukleniyor && !list.length"
            :count="4"
            height="92px"
          />
          <EmptyState
            v-else-if="!list.length"
            :message="filtreVar ? t('cekSenet.filtreYok') : t('cekSenet.empty')"
            :sub-message="filtreVar ? null : t('cekSenet.emptyHint')"
            icon="pi pi-credit-card"
            :action-label="t('cekSenet.yeniCekSenet')"
            action-icon="pi pi-plus"
            @action="dialogAc()"
          />
          <div
            v-for="k in list"
            :key="k.id"
            class="cs-mobil-kart"
            @click="detayAc(k)"
          >
            <div class="cm-ust">
              <strong>{{ k.cekNo || k.cariHesapAdi || '-' }}</strong>
              <SatirEylemleri
                :baslik="k.cekNo || k.cariHesapAdi || ''"
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="satirEylemleri(k)"
                @click.stop
              />
            </div>
            <div class="cm-satir">
              <Tag
                :value="turAdi(k.tur)"
                :severity="k.tur === 'CEK' ? 'info' : 'warn'"
              />
              <Tag
                :value="durumAdi(k.durum)"
                :severity="durumSeverity(k.durum)"
              />
              <span>{{ k.cariHesapAdi || '-' }}</span>
            </div>
            <div class="cm-alt">
              <span>{{ t('cekSenet.vade') }}: {{ formatTarih(k.vadeTarihi) }}</span>
              <strong class="gizli-veri">{{ formatCurrency(k.tutar) }}</strong>
            </div>
          </div>
        </div>
      </TabPanel>

      <!-- 2) VADE TAKVİMİ -->
      <TabPanel :header="t('cekSenet.tabTakvim')">
        <div class="aging-ust">
          <h3>{{ t('cekSenet.agingBaslik') }}</h3>
          <small class="muted">{{ t('cekSenet.agingIpucu') }}</small>
        </div>

        <div class="aging-grid">
          <button
            v-for="kart in agingKartlari"
            :key="kart.anahtar"
            type="button"
            class="aging-kart"
            :class="kart.sinif"
            :aria-pressed="agingAktif(kart)"
            @click="agingSec(kart)"
          >
            <span class="aging-baslik">{{ kart.etiket }}</span>
            <strong class="aging-deger gizli-veri">{{ formatCurrency(kart.tutar) }}</strong>
            <span class="aging-adet">{{ t('cekSenet.kpiKayitSayisi', { n: kart.adet }) }}</span>
          </button>
        </div>

        <div class="takvim-kart">
          <div class="takvim-ust">
            <Button
              icon="pi pi-chevron-left"
              text
              rounded
              :aria-label="t('cekSenet.takvimOncekiAy')"
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
              :aria-label="t('cekSenet.takvimSonrakiAy')"
              @click="ayDegistir(1)"
            />
            <span class="arac-bosluk" />
            <div
              class="tur-seg"
              role="group"
              :aria-label="t('cekSenet.tabTakvim')"
            >
              <button
                type="button"
                :class="{ aktif: takvimGorunum === 'grid' }"
                :aria-pressed="takvimGorunum === 'grid'"
                @click="takvimGorunum = 'grid'"
              >
                <i class="pi pi-calendar" /> {{ t('cekSenet.takvimGorunumGrid') }}
              </button>
              <button
                type="button"
                :class="{ aktif: takvimGorunum === 'liste' }"
                :aria-pressed="takvimGorunum === 'liste'"
                @click="takvimGorunum = 'liste'"
              >
                <i class="pi pi-list" /> {{ t('cekSenet.takvimGorunumListe') }}
              </button>
            </div>
          </div>

          <SkeletonLoader
            v-if="takvimIskelet"
            class="tablo-iskelet"
            :count="6"
            height="64px"
          />

          <div
            v-else-if="takvimGorunum === 'grid'"
            class="takvim-grid"
            role="grid"
            :aria-label="ayAdi"
          >
            <div
              v-for="gun in gunBasliklari"
              :key="gun"
              class="gun-baslik"
              role="columnheader"
            >
              {{ gun }}
            </div>
            <div
              v-for="hucre in takvimHucreleri"
              :key="hucre.key"
              class="gun-hucre"
              :class="{ bugun: hucre.bugun, bos: !hucre.gun, dolu: hucre.adet > 0 }"
              role="gridcell"
            >
              <template v-if="hucre.gun">
                <button
                  v-if="hucre.adet > 0"
                  type="button"
                  class="gun-buton"
                  :aria-label="`${formatTarih(hucre.tarih)} · ${t('cekSenet.kpiKayitSayisi', { n: hucre.adet })}`"
                  @click="gunSec(hucre)"
                >
                  <span class="gun-no">{{ hucre.gun }}</span>
                  <span class="gun-adet">{{ t('cekSenet.kpiKayitSayisi', { n: hucre.adet }) }}</span>
                  <span class="gun-tutar gizli-veri">{{ formatCurrency(hucre.tutar) }}</span>
                </button>
                <span
                  v-else
                  class="gun-no"
                >{{ hucre.gun }}</span>
              </template>
            </div>
          </div>

          <div
            v-else
            class="takvim-liste"
          >
            <EmptyState
              v-if="!doluGunler.length"
              icon="pi pi-calendar"
              :message="t('cekSenet.takvimBos')"
            />
            <button
              v-for="gun in doluGunler"
              :key="gun.tarih"
              type="button"
              class="takvim-liste-satir"
              @click="gunSec(gun)"
            >
              <span class="tl-tarih">{{ formatTarih(gun.tarih) }}</span>
              <Tag
                :value="t('cekSenet.kpiKayitSayisi', { n: gun.adet })"
                severity="info"
              />
              <strong class="gizli-veri">{{ formatCurrency(gun.tutar) }}</strong>
            </button>
          </div>
        </div>
      </TabPanel>

      <!-- 3) GEÇMİŞ -->
      <TabPanel :header="t('cekSenet.tabGecmis')">
        <div class="cs-arac">
          <div
            class="tur-seg"
            role="group"
            :aria-label="t('cekSenet.tur')"
          >
            <button
              v-for="secenek in turSecenekleri"
              :key="secenek.value"
              type="button"
              :class="{ aktif: gecmisTur === secenek.value }"
              :aria-pressed="gecmisTur === secenek.value"
              @click="gecmisTur = secenek.value"
            >
              {{ secenek.label }}
            </button>
          </div>

          <Dropdown
            v-model="gecmisIslem"
            :options="gecmisIslemSecenekleri"
            option-label="label"
            option-value="value"
            show-clear
            class="arac-secim"
            :placeholder="t('cekSenet.gecmisIslem')"
            :aria-label="t('cekSenet.gecmisIslem')"
          />

          <DatePicker
            v-model="gecmisAraligi"
            selection-mode="range"
            date-format="dd.mm.yy"
            show-button-bar
            show-icon
            class="arac-tarih"
            :placeholder="t('cekSenet.gecmisTarih')"
            :aria-label="t('cekSenet.gecmisTarih')"
          />

          <span class="arac-bosluk" />
          <small class="muted">{{ t('cekSenet.gecmisIpucu', { n: gecmisLimit }) }}</small>
          <Button
            icon="pi pi-refresh"
            :aria-label="t('common.refresh')"
            class="p-button-text p-button-sm"
            :loading="gecmisYukleniyor"
            @click="gecmisYukle"
          />
        </div>

        <SkeletonLoader
          v-if="gecmisIskelet"
          class="tablo-iskelet"
          :count="5"
          height="46px"
        />

        <template v-else>
          <EmptyState
            v-if="!gecmisFiltreli.length"
            icon="pi pi-history"
            :message="t('cekSenet.gecmisBos')"
          />

          <AppDataTable
            v-else
            :value="gecmisFiltreli"
            data-key="anahtar"
            :paginator="true"
            :rows="20"
            striped-rows
            :empty-message="t('cekSenet.gecmisBos')"
          >
            <Column
              :header="t('cekSenet.gecmisTarih')"
              style="width: 160px"
            >
              <template #body="{ data }">
                {{ formatTarihSaat(data.olusturmaTarihi) }}
              </template>
            </Column>
            <Column
              :header="t('cekSenet.gecmisKaynak')"
            >
              <template #body="{ data }">
                <div class="gecmis-kaynak">
                  <Tag
                    :value="turAdi(data.tur)"
                    :severity="data.tur === 'CEK' ? 'info' : 'warn'"
                  />
                  <span>{{ data.cekNo || '-' }}</span>
                </div>
              </template>
            </Column>
            <Column
              :header="t('cekSenet.gecmisIslem')"
              style="width: 170px"
            >
              <template #body="{ data }">
                {{ islemAdi(data.islem) }}
              </template>
            </Column>
            <Column
              :header="t('common.status')"
              style="width: 200px"
            >
              <template #body="{ data }">
                <span class="gecmis-gecis">
                  {{ data.eskiDurum ? durumAdi(data.eskiDurum) : '—' }}
                  <i class="pi pi-arrow-right" />
                  {{ data.yeniDurum ? durumAdi(data.yeniDurum) : '—' }}
                </span>
              </template>
            </Column>
            <Column
              :header="t('common.description')"
            >
              <template #body="{ data }">
                {{ data.aciklama || '-' }}
              </template>
            </Column>
          </AppDataTable>
        </template>
      </TabPanel>
    </TabView>

    <CekSenetDetayDrawer
      v-model:visible="detayDrawer"
      :kayit="detayKayit"
      :yukleniyor="detayYukleniyor"
      @duzenle="drawerDuzenle"
      @sil="sil"
      @tahsil="tahsilDialogAc"
      @tahsil-iptal="iptalDialogAc"
      @ciro="ciroDialogAc"
      @ciro-iade="ciroIadeAc"
      @iade-al="iadeAlAc"
      @teminat="teminatDialogAc"
      @teminat-iade="teminatIadeAc"
      @protesto="protestoDialogAc"
    />

    <!-- Yeni / Düzenle -->
    <Dialog
      v-model:visible="dialog"
      :header="duzenlenenId ? t('cekSenet.duzenle') : t('cekSenet.yeniCekSenet')"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.turZorunlu') }}</label>
            <Dropdown
              v-model="form.tur"
              :options="turSecenekleri.filter((s) => s.value !== 'TUMU')"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.cariHesapZorunlu') }}</label>
            <Dropdown
              v-model="form.cariHesapId"
              :options="cariler"
              option-label="ad"
              option-value="id"
              filter
              :placeholder="t('common.select')"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.banka') }}</label><InputText
              v-model="form.bankaAdi"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.sube') }}</label><InputText
              v-model="form.sube"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.cekNo') }}</label><InputText
              v-model="form.cekNo"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.hesapNo') }}</label><InputText
              v-model="form.hesapNo"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.vadeTarihiZorunlu') }}</label><DatePicker
              v-model="form.vadeTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.tutarZorunlu') }}</label><InputNumber
              v-model="form.tutar"
              mode="currency"
              currency="TRY"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.kesinmeTarihi') }}</label><DatePicker
              v-model="form.kesinmeTarihi"
              date-format="dd.mm.yy"
              show-clear
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="2"
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

    <!-- Tahsil -->
    <Dialog
      v-model:visible="tahsilDialog"
      :header="t('cekSenet.tahsilBaslik')"
      modal
      :style="{ width: '480px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <p class="modal-aciklama">
          {{ t('cekSenet.tahsilAciklama') }}
        </p>
        <div
          v-if="tahsilHedef"
          class="modal-ozet"
        >
          <span>{{ tahsilHedef.cekNo || '-' }}</span>
          <strong class="gizli-veri">{{ formatCurrency(tahsilHedef.tutar) }}</strong>
        </div>
        <div class="field">
          <label>{{ t('cekSenet.fatura') }}</label>
          <Dropdown
            v-model="tahsilFaturaId"
            :options="tahsilFaturalar"
            option-label="faturaNo"
            option-value="id"
            show-clear
            filter
            :loading="tahsilFaturaYukleniyor"
            :placeholder="t('cekSenet.faturaYok')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('cekSenet.tahsilHesap') }}</label>
          <Dropdown
            v-model="tahsilHesapTipi"
            :options="tahsilHesapSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div
          v-if="tahsilHesapTipi === 'KASA'"
          class="field"
        >
          <label>{{ t('cekSenet.kasa') }}</label>
          <Dropdown
            v-model="tahsilKasaId"
            :options="kasalar"
            option-label="ad"
            option-value="id"
            :placeholder="t('common.select')"
            show-clear
            class="w-full"
          />
        </div>
        <div
          v-if="tahsilHesapTipi === 'BANKA'"
          class="field"
        >
          <label>{{ t('cekSenet.bankaHesabi') }}</label>
          <Dropdown
            v-model="tahsilBankaId"
            :options="bankalar"
            option-label="ad"
            option-value="id"
            :placeholder="t('common.select')"
            show-clear
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="tahsilDialog = false"
        />
        <Button
          :label="t('cekSenet.tahsilEt')"
          icon="pi pi-check"
          severity="success"
          :loading="islemde"
          @click="tahsilEt"
        />
      </template>
    </Dialog>

    <!-- Tahsil iptali -->
    <Dialog
      v-model:visible="iptalDialog"
      :header="t('cekSenet.tahsilIptalBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <p class="modal-aciklama">
          {{ t('cekSenet.tahsilIptalAciklama') }}
        </p>
        <div class="field">
          <label>{{ t('cekSenet.tahsilIptalGerekce') }}</label>
          <Textarea
            v-model="iptalGerekce"
            rows="3"
            class="w-full"
            :placeholder="t('cekSenet.tahsilIptalGerekcePlaceholder')"
          />
          <small
            v-if="iptalHata"
            class="hata-metin"
          >{{ iptalHata }}</small>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="iptalDialog = false"
        />
        <Button
          :label="t('cekSenet.tahsilIptal')"
          icon="pi pi-times"
          severity="danger"
          :loading="islemde"
          @click="iptalKaydet"
        />
      </template>
    </Dialog>

    <!-- Ciro -->
    <Dialog
      v-model:visible="ciroDialog"
      :header="t('cekSenet.ciroBaslik')"
      modal
      :style="{ width: '460px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <p class="modal-aciklama">
          {{ t('cekSenet.ciroAciklama') }}
        </p>
        <div class="field">
          <label>{{ t('cekSenet.ciroHedefCari') }}</label>
          <Dropdown
            v-model="ciroForm.cariId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            filter
            :placeholder="t('common.select')"
            class="w-full"
          />
          <small
            v-if="ciroHata"
            class="hata-metin"
          >{{ ciroHata }}</small>
        </div>
        <div class="field">
          <label>{{ t('cekSenet.ciroNotu') }}</label>
          <Textarea
            v-model="ciroForm.not"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="ciroDialog = false"
        />
        <Button
          :label="t('cekSenet.ciroEt')"
          icon="pi pi-sync"
          :loading="islemde"
          @click="ciroKaydet"
        />
      </template>
    </Dialog>

    <!-- Teminat -->
    <Dialog
      v-model:visible="teminatDialog"
      :header="t('cekSenet.teminatBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('cekSenet.teminatAciklama') }}</label>
          <Textarea
            v-model="teminatAciklama"
            rows="3"
            class="w-full"
            :placeholder="t('cekSenet.teminatAciklamaPlaceholder')"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="teminatDialog = false"
        />
        <Button
          :label="t('cekSenet.teminatVer')"
          icon="pi pi-shield"
          :loading="islemde"
          @click="teminatKaydet"
        />
      </template>
    </Dialog>

    <!-- Protesto -->
    <Dialog
      v-model:visible="protestoDialog"
      :header="t('cekSenet.protestoBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <p class="modal-aciklama uyari">
          <i class="pi pi-exclamation-triangle" /> {{ t('cekSenet.protestoUyari') }}
        </p>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="protestoAciklama"
            rows="3"
            class="w-full"
            :placeholder="t('cekSenet.protestoAciklamaPlaceholder')"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="protestoDialog = false"
        />
        <Button
          :label="t('cekSenet.protestoEt')"
          icon="pi pi-exclamation-triangle"
          severity="danger"
          :loading="islemde"
          @click="protestoKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { cekSenetAPI, cariHesapAPI, kasaAPI, bankaAPI, faturaAPI, excelAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import CekSenetDetayDrawer from '../components/CekSenetDetayDrawer.vue'
import { formatCurrency, formatTarih, formatTarihSaat, getLocalDateString } from '../utils/format.js'

const { t, locale } = useI18n()
const confirm = useConfirm()
const toastBildirim = useToastBildirim()

// ---- Hafıza ----
const TAB_ANAHTAR = 'raspel_cek_senet_tab'
const FILTRE_ANAHTAR = 'raspel_cek_senet_filtre'
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

// ---- Yardımcılar ----
const tarihOku = (deger) => {
  if (!deger) return null
  if (typeof deger === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(deger)) {
    const [y, m, d] = deger.split('-').map(Number)
    return new Date(y, m - 1, d)
  }
  const d = new Date(deger)
  return isNaN(d.getTime()) ? null : d
}

const bugunBaslangic = () => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
}

const gunEkle = (tarih, gun) => {
  const d = new Date(tarih)
  d.setDate(d.getDate() + gun)
  return d
}

const gunFarki = (a, b) => Math.round((a.getTime() - b.getTime()) / 86400000)

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

// ---- Filtreler ----
const baslangicKayit = kayitliFiltre.baslangic ? tarihOku(kayitliFiltre.baslangic) : null
const bitisKayit = kayitliFiltre.bitis ? tarihOku(kayitliFiltre.bitis) : null

const q = ref(kayitliFiltre.q || '')
const tur = ref(kayitliFiltre.tur || 'TUMU')
const durum = ref(kayitliFiltre.durum || null)
const cariId = ref(kayitliFiltre.cariId ?? null)
const minTutar = ref(kayitliFiltre.minTutar ?? null)
const sadeceProtesto = ref(!!kayitliFiltre.protesto)
const vadeAralik = ref(kayitliFiltre.vadeAralik || null)
const vadeAraligi = ref(
  baslangicKayit || bitisKayit ? [baslangicKayit, bitisKayit] : null
)

const sayfa = ref(0)
const boyut = ref(25)
const sirala = ref('vadeTarihi,asc')
let aramaZaman = null

// ---- Veri ----
const list = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)
const ozet = ref({})
const ozetYuklendi = ref(false)
const cariler = ref([])
const kasalar = ref([])
const bankalar = ref([])
const excelYukleniyor = ref(false)
const kaydediliyor = ref(false)

// ---- Seçenekler ----
const turSecenekleri = computed(() => [
  { value: 'TUMU', label: t('cekSenet.tumu') },
  { value: 'CEK', label: t('cekSenet.cek') },
  { value: 'SENET', label: t('cekSenet.senet') }
])

const durumSecenekleri = computed(() => [
  { value: 'PORTFOY', label: t('cekSenet.durumPortfoy') },
  { value: 'TEMINATTA', label: t('cekSenet.durumTeminatta') },
  { value: 'CIRO_EDILDI', label: t('cekSenet.durumCiroEdildi') },
  { value: 'TAHSIL_EDILDI', label: t('cekSenet.durumTahsilEdildi') },
  { value: 'PROTESTO', label: t('cekSenet.durumProtesto') },
  { value: 'IADE', label: t('cekSenet.durumIade') }
])

const hesapSecenekleri = computed(() => [
  { value: 'YOK', label: t('cekSenet.hesapYok') },
  { value: 'KASA', label: t('cekSenet.kasa') },
  { value: 'BANKA', label: t('cekSenet.bankaHesabi') }
])

const VADE_ARALIK_ETIKET = {
  BU_HAFTA: 'agingBuHafta',
  BU_AY: 'agingBuAy',
  GECIKEN: 'agingGeciken',
  '31-60': 'aging30',
  '61-90': 'aging60',
  '90+': 'aging90'
}

const vadeAralikEtiketi = computed(() =>
  vadeAralik.value ? t(`cekSenet.${VADE_ARALIK_ETIKET[vadeAralik.value] || 'agingGeciken'}`) : ''
)

const filtreVar = computed(
  () =>
    !!q.value?.trim() ||
    tur.value !== 'TUMU' ||
    !!durum.value ||
    cariId.value != null ||
    !!vadeAralik.value ||
    !!vadeAraligi.value?.[0] ||
    !!vadeAraligi.value?.[1] ||
    (minTutar.value != null && minTutar.value !== '') ||
    sadeceProtesto.value
)

const ozetIskelet = computed(() => !ozetYuklendi.value)
const listeIskelet = computed(() => yukleniyor.value && !list.value.length)

const canliDuyuru = computed(() => {
  if (yukleniyor.value || takvimYukleniyor.value || gecmisYukleniyor.value) return ''
  if (aktifSekme.value === 1) {
    return t('cekSenet.takvimDuyuru', { ay: ayAdi.value, n: doluGunler.value.length })
  }
  if (aktifSekme.value === 2) {
    return t('cekSenet.gecmisDuyuru', { n: gecmisFiltreli.value.length })
  }
  return t('cekSenet.kayitDuyuru', { gorunen: list.value.length, toplam: toplam.value })
})

// ---- Etiketler ----
const DURUM_ANAHTAR = {
  PORTFOY: 'durumPortfoy',
  TEMINATTA: 'durumTeminatta',
  CIRO_EDILDI: 'durumCiroEdildi',
  TAHSIL_EDILDI: 'durumTahsilEdildi',
  PROTESTO: 'durumProtesto',
  IADE: 'durumIade',
  ODENDI: 'durumTahsilEdildi'
}

const ISLEM_ANAHTAR = {
  OLUSTUR: 'islemOlustur',
  GUNCELLE: 'islemGuncelle',
  TEMINAT: 'islemTeminat',
  TEMINAT_IADE: 'islemTeminatIade',
  CIRO: 'islemCiro',
  CIRO_IADE: 'islemCiroIade',
  IADE_AL: 'islemIadeAl',
  TAHSIL: 'islemTahsil',
  TAHSIL_IPTAL: 'islemTahsilIptal',
  PROTESTO: 'islemProtesto'
}

const durumAdi = (d) => (DURUM_ANAHTAR[d] ? t(`cekSenet.${DURUM_ANAHTAR[d]}`) : d || '-')
const turAdi = (deger) => (deger === 'CEK' ? t('cekSenet.cek') : t('cekSenet.senet'))
const islemAdi = (i) => (ISLEM_ANAHTAR[i] ? t(`cekSenet.${ISLEM_ANAHTAR[i]}`) : i || '-')

const durumSeverity = (d) =>
  ({
    PORTFOY: 'info',
    TEMINATTA: 'warn',
    CIRO_EDILDI: 'secondary',
    TAHSIL_EDILDI: 'success',
    PROTESTO: 'danger',
    IADE: 'warn',
    ODENDI: 'success'
  })[d] || 'secondary'

// ---- Parametreler ----
const filtreParams = () => {
  const p = {}
  if (q.value?.trim()) p.q = q.value.trim()
  if (tur.value !== 'TUMU') p.tur = tur.value
  if (durum.value) p.durum = durum.value
  if (cariId.value != null) p.cariId = cariId.value
  const bas = vadeAraligi.value?.[0]
  const bit = vadeAraligi.value?.[1]
  if (bas) p.baslangic = getLocalDateString(bas)
  if (bit) p.bitis = getLocalDateString(bit)
  if (vadeAralik.value) p.vadeAralik = vadeAralik.value
  if (minTutar.value != null && minTutar.value !== '') p.minTutar = minTutar.value
  if (sadeceProtesto.value) p.protesto = true
  return p
}

const agingParams = () => {
  const p = filtreParams()
  delete p.baslangic
  delete p.bitis
  delete p.vadeAralik
  return p
}

// ---- Yükleme ----
const listeYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await cekSenetAPI.filtreli({
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
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const ozetYukle = async () => {
  try {
    const r = await cekSenetAPI.ozet()
    ozet.value = r.data || {}
  } catch {
    ozet.value = {}
  } finally {
    ozetYuklendi.value = true
  }
}

const yukle = () => {
  ozetYukle()
  listeYukle()
}

const yenile = () => {
  yukle()
  if (takvimYuklendi.value) takvimYukle()
  if (gecmisYuklendi.value) gecmisYukle()
}

const filtreDegisti = () => {
  sayfa.value = 0
  listeYukle()
}

const aramaDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(filtreDegisti, 300)
}

const turSec = (deger) => {
  if (tur.value === deger) return
  tur.value = deger
  sayfa.value = 0
  listeYukle()
}

const protestoToggle = () => {
  sadeceProtesto.value = !sadeceProtesto.value
  sayfa.value = 0
  listeYukle()
}

const vadeAralikTemizle = () => {
  vadeAralik.value = null
  sayfa.value = 0
  listeYukle()
}

const vadeTarihiDegisti = () => {
  vadeAralik.value = null
  filtreDegisti()
}

const filtreTemizle = () => {
  q.value = ''
  tur.value = 'TUMU'
  durum.value = null
  cariId.value = null
  vadeAralik.value = null
  vadeAraligi.value = null
  minTutar.value = null
  sadeceProtesto.value = false
  sayfa.value = 0
  listeYukle()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  boyut.value = e.rows
  listeYukle()
}

const siralaDegisti = (e) => {
  if (!e.sortField || !e.sortOrder) {
    sirala.value = 'vadeTarihi,asc'
  } else {
    sirala.value = `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
  }
  sayfa.value = 0
  listeYukle()
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const res = await excelAPI.cekSenet(filtreParams())
    indir(res.data, 'cek-senet.xlsx')
    toastBildirim.basarili(t('cekSenet.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('cekSenet.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

// ---- Vade takvimi + yaşlandırma ----
const bugun = new Date()
const seciliYil = ref(bugun.getFullYear())
const seciliAy = ref(bugun.getMonth() + 1)
const takvimVerisi = ref([])
const agingKayitlari = ref([])
const takvimYukleniyor = ref(false)
const takvimYuklendi = ref(false)
const takvimGorunum = ref('grid')

const ayAraligi = () => {
  const ilk = new Date(seciliYil.value, seciliAy.value - 1, 1)
  const son = new Date(seciliYil.value, seciliAy.value, 0)
  return { baslangic: getLocalDateString(ilk), bitis: getLocalDateString(son) }
}

const takvimYukle = async () => {
  takvimYukleniyor.value = true
  try {
    const [takvimRes, agingRes] = await Promise.all([
      cekSenetAPI.takvim(ayAraligi()),
      cekSenetAPI
        .filtreli({ ...agingParams(), page: 0, size: 500, sort: 'vadeTarihi,asc' })
        .catch(() => null)
    ])
    takvimVerisi.value = takvimRes.data || []
    agingKayitlari.value = agingRes?.data?.content || []
    takvimYuklendi.value = true
  } catch (err) {
    takvimVerisi.value = []
    agingKayitlari.value = []
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.takvimHata'))
  } finally {
    takvimYukleniyor.value = false
  }
}

const ayAdi = computed(() =>
  new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    month: 'long',
    year: 'numeric'
  }).format(new Date(seciliYil.value, seciliAy.value - 1, 1))
)

const gunBasliklari = computed(() => {
  const bicim = new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    weekday: 'short'
  })
  // 2024-01-01 Pazartesi; hafta Pazartesi başlar.
  return Array.from({ length: 7 }, (_, i) => bicim.format(new Date(2024, 0, 1 + i)))
})

const ayDegistir = (yon) => {
  let ay = seciliAy.value + yon
  let yil = seciliYil.value
  if (ay > 12) {
    ay = 1
    yil++
  } else if (ay < 1) {
    ay = 12
    yil--
  }
  seciliAy.value = ay
  seciliYil.value = yil
  takvimYukle()
}

const takvimHucreleri = computed(() => {
  const ilk = new Date(seciliYil.value, seciliAy.value - 1, 1)
  const gunSayisi = new Date(seciliYil.value, seciliAy.value, 0).getDate()
  let offset = ilk.getDay() - 1
  if (offset < 0) offset = 6

  const harita = new Map()
  for (const gun of takvimVerisi.value) {
    if (!gun?.tarih) continue
    harita.set(String(gun.tarih).slice(0, 10), gun)
  }

  const simdi = bugunBaslangic()
  const hucreler = []
  for (let i = 0; i < offset; i++) {
    hucreler.push({ key: `bos-${i}`, gun: null, adet: 0, tutar: 0 })
  }
  for (let g = 1; g <= gunSayisi; g++) {
    const tarih = `${seciliYil.value}-${String(seciliAy.value).padStart(2, '0')}-${String(g).padStart(2, '0')}`
    const kayit = harita.get(tarih)
    hucreler.push({
      key: `gun-${g}`,
      gun: g,
      tarih,
      adet: Number(kayit?.adet) || 0,
      tutar: Number(kayit?.tutar) || 0,
      bugun:
        simdi.getFullYear() === seciliYil.value &&
        simdi.getMonth() === seciliAy.value - 1 &&
        simdi.getDate() === g
    })
  }
  while (hucreler.length % 7 !== 0) {
    hucreler.push({ key: `bos-son-${hucreler.length}`, gun: null, adet: 0, tutar: 0 })
  }
  return hucreler
})

const doluGunler = computed(() =>
  [...takvimVerisi.value]
    .filter((g) => g?.tarih && Number(g.adet) > 0)
    .sort((a, b) => String(a.tarih).localeCompare(String(b.tarih)))
)

const takvimIskelet = computed(() => takvimYukleniyor.value && !takvimVerisi.value.length)

const yaslandirma = computed(() => {
  const sifir = () => ({ adet: 0, tutar: 0 })
  const kovalar = {
    buHafta: sifir(),
    buAy: sifir(),
    geciken: sifir(),
    g30: sifir(),
    g60: sifir(),
    g90: sifir()
  }
  const bugunTarih = bugunBaslangic()
  const ayBasi = new Date(bugunTarih.getFullYear(), bugunTarih.getMonth(), 1)
  const aySonu = new Date(bugunTarih.getFullYear(), bugunTarih.getMonth() + 1, 0)

  for (const kayit of agingKayitlari.value) {
    if (!kayit?.vadeTarihi) continue
    if (kayit.durum === 'TAHSIL_EDILDI' || kayit.durum === 'ODENDI' || kayit.durum === 'PROTESTO') continue
    const vade = tarihOku(kayit.vadeTarihi)
    if (!vade) continue
    const tutar = Number(kayit.tutar) || 0
    const fark = gunFarki(vade, bugunTarih)
    if (fark < 0) {
      const gecikme = -fark
      kovalar.geciken.adet++
      kovalar.geciken.tutar += tutar
      if (gecikme > 90) {
        kovalar.g90.adet++
        kovalar.g90.tutar += tutar
      } else if (gecikme > 60) {
        kovalar.g60.adet++
        kovalar.g60.tutar += tutar
      } else if (gecikme > 30) {
        kovalar.g30.adet++
        kovalar.g30.tutar += tutar
      }
    } else if (fark <= 7) {
      kovalar.buHafta.adet++
      kovalar.buHafta.tutar += tutar
    }
    // Backend "BU_AY" kovasıyla aynı kapsam: ayın tamamı.
    if (vade >= ayBasi && vade <= aySonu) {
      kovalar.buAy.adet++
      kovalar.buAy.tutar += tutar
    }
  }
  return kovalar
})

const agingKartlari = computed(() => {
  const bugunTarih = bugunBaslangic()
  return [
    {
      anahtar: 'buHafta',
      etiket: t('cekSenet.agingBuHafta'),
      sinif: 'aging-yakin',
      adet: yaslandirma.value.buHafta.adet,
      tutar: yaslandirma.value.buHafta.tutar,
      bucket: 'BU_HAFTA'
    },
    {
      anahtar: 'buAy',
      etiket: t('cekSenet.agingBuAy'),
      sinif: 'aging-yakin',
      adet: yaslandirma.value.buAy.adet,
      tutar: yaslandirma.value.buAy.tutar,
      bucket: 'BU_AY'
    },
    {
      anahtar: 'geciken',
      etiket: t('cekSenet.agingGeciken'),
      sinif: 'aging-gecikmis',
      adet: yaslandirma.value.geciken.adet,
      tutar: yaslandirma.value.geciken.tutar,
      bucket: 'GECIKEN'
    },
    {
      // 31-60 gün gecikme: vade bugünden 60..31 gün önce.
      anahtar: 'g30',
      etiket: t('cekSenet.aging30'),
      sinif: 'aging-orta',
      adet: yaslandirma.value.g30.adet,
      tutar: yaslandirma.value.g30.tutar,
      baslangic: getLocalDateString(gunEkle(bugunTarih, -60)),
      bitis: getLocalDateString(gunEkle(bugunTarih, -31))
    },
    {
      // 61-90 gün gecikme.
      anahtar: 'g60',
      etiket: t('cekSenet.aging60'),
      sinif: 'aging-yuksek',
      adet: yaslandirma.value.g60.adet,
      tutar: yaslandirma.value.g60.tutar,
      baslangic: getLocalDateString(gunEkle(bugunTarih, -90)),
      bitis: getLocalDateString(gunEkle(bugunTarih, -61))
    },
    {
      // 90+ gün gecikme: yalnız üst sınır gönderilir.
      anahtar: 'g90',
      etiket: t('cekSenet.aging90'),
      sinif: 'aging-kritik',
      adet: yaslandirma.value.g90.adet,
      tutar: yaslandirma.value.g90.tutar,
      bitis: getLocalDateString(gunEkle(bugunTarih, -91))
    }
  ]
})

const agingAktif = (kart) => {
  if (kart.bucket) return vadeAralik.value === kart.bucket
  const seciliBas = vadeAraligi.value?.[0] ? getLocalDateString(vadeAraligi.value[0]) : null
  const seciliBit = vadeAraligi.value?.[1] ? getLocalDateString(vadeAraligi.value[1]) : null
  return seciliBas === (kart.baslangic || null) && seciliBit === (kart.bitis || null)
}

const agingSec = (kart) => {
  if (kart.bucket) {
    vadeAralik.value = kart.bucket
    vadeAraligi.value = null
  } else {
    vadeAralik.value = null
    vadeAraligi.value = [
      kart.baslangic ? tarihOku(kart.baslangic) : null,
      kart.bitis ? tarihOku(kart.bitis) : null
    ]
  }
  aktifSekme.value = 0
  sayfa.value = 0
  listeYukle()
}

const gunSec = (hucre) => {
  if (!hucre?.tarih) return
  const gun = tarihOku(hucre.tarih)
  vadeAralik.value = null
  vadeAraligi.value = [gun, gun]
  aktifSekme.value = 0
  sayfa.value = 0
  listeYukle()
}

// ---- Geçmiş (durum logları) ----
const gecmisLimit = 50
const gecmisOlaylari = ref([])
const gecmisYukleniyor = ref(false)
const gecmisYuklendi = ref(false)
const gecmisTur = ref('TUMU')
const gecmisIslem = ref(null)
const gecmisAraligi = ref(null)

const gecmisIslemSecenekleri = computed(() =>
  Object.keys(ISLEM_ANAHTAR).map((islem) => ({ value: islem, label: t(`cekSenet.${ISLEM_ANAHTAR[islem]}`) }))
)

const gecmisIskelet = computed(() => gecmisYukleniyor.value && !gecmisOlaylari.value.length)

const gecmisFiltreli = computed(() => {
  const bas = gecmisAraligi.value?.[0] ? getLocalDateString(gecmisAraligi.value[0]) : null
  const bit = gecmisAraligi.value?.[1] ? getLocalDateString(gecmisAraligi.value[1]) : null
  return gecmisOlaylari.value.filter((olay) => {
    if (gecmisTur.value !== 'TUMU' && olay.tur !== gecmisTur.value) return false
    if (gecmisIslem.value && olay.islem !== gecmisIslem.value) return false
    const tarih = String(olay.olusturmaTarihi || '').slice(0, 10)
    if (!tarih) return true
    if (bas && tarih < bas) return false
    if (bit && tarih > bit) return false
    return true
  })
})

const gecmisYukle = async () => {
  gecmisYukleniyor.value = true
  try {
    const r = await cekSenetAPI.filtreli({
      ...filtreParams(),
      page: 0,
      size: gecmisLimit,
      sort: 'vadeTarihi,desc'
    })
    const kayitlar = (r.data?.content || []).slice(0, gecmisLimit)
    const detaylar = await Promise.all(
      kayitlar.map((kayit) =>
        cekSenetAPI
          .getById(kayit.id)
          .then((yanit) => ({ kayit, detay: yanit.data }))
          .catch(() => null)
      )
    )
    const olaylar = []
    for (const kayit of detaylar) {
      if (!kayit?.detay) continue
      const detay = kayit.detay
      for (const [i, olay] of (detay.gecmis || []).entries()) {
        olaylar.push({
          anahtar: `${kayit.kayit.id}-${olay.id ?? i}`,
          cekSenetId: kayit.kayit.id,
          cekNo: detay.cekNo || kayit.kayit.cekNo,
          tur: detay.tur || kayit.kayit.tur,
          islem: olay.islem,
          eskiDurum: olay.eskiDurum,
          yeniDurum: olay.yeniDurum,
          aciklama: olay.aciklama,
          olusturmaTarihi: olay.olusturmaTarihi
        })
      }
    }
    olaylar.sort((a, b) =>
      String(b.olusturmaTarihi || '').localeCompare(String(a.olusturmaTarihi || ''))
    )
    gecmisOlaylari.value = olaylar
    gecmisYuklendi.value = true
  } catch (err) {
    gecmisOlaylari.value = []
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.gecmisHata'))
  } finally {
    gecmisYukleniyor.value = false
  }
}

// ---- Detay drawer ----
const detayDrawer = ref(false)
const detayKayit = ref(null)
const detayYukleniyor = ref(false)

const detayAc = async (kayit) => {
  if (!kayit?.id) return
  detayKayit.value = kayit
  detayDrawer.value = true
  detayYukleniyor.value = true
  try {
    const r = await cekSenetAPI.getById(kayit.id)
    detayKayit.value = r.data || kayit
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataYukleme'))
  } finally {
    detayYukleniyor.value = false
  }
}

const satirTiklandi = (event) => {
  if (event?.data) detayAc(event.data)
}

const drawerDuzenle = (kayit) => {
  detayDrawer.value = false
  dialogAc(kayit)
}

// ---- Yeni / düzenle ----
const dialog = ref(false)
const duzenlenenId = ref(null)
const bosForm = () => ({
  tur: 'CEK',
  cariHesapId: null,
  bankaAdi: '',
  sube: '',
  cekNo: '',
  hesapNo: '',
  vadeTarihi: new Date(),
  kesinmeTarihi: null,
  tutar: null,
  aciklama: ''
})
const form = ref(bosForm())

const dialogAc = (kayit = null) => {
  duzenlenenId.value = kayit?.id || null
  form.value = kayit
    ? {
        tur: kayit.tur,
        cariHesapId: kayit.cariHesapId ?? null,
        bankaAdi: kayit.bankaAdi || '',
        sube: kayit.sube || '',
        cekNo: kayit.cekNo || '',
        hesapNo: kayit.hesapNo || '',
        vadeTarihi: kayit.vadeTarihi ? new Date(kayit.vadeTarihi) : new Date(),
        kesinmeTarihi: kayit.kesinmeTarihi ? new Date(kayit.kesinmeTarihi) : null,
        tutar: kayit.tutar ?? null,
        aciklama: kayit.aciklama || ''
      }
    : bosForm()
  dialog.value = true
}

const kaydet = async () => {
  if (kaydediliyor.value) return
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      vadeTarihi: getLocalDateString(form.value.vadeTarihi),
      kesinmeTarihi: form.value.kesinmeTarihi ? getLocalDateString(form.value.kesinmeTarihi) : null
    }
    if (duzenlenenId.value) {
      await cekSenetAPI.update(duzenlenenId.value, payload)
      toastBildirim.basarili(t('cekSenet.guncellendi'))
    } else {
      await cekSenetAPI.create(payload)
      toastBildirim.basarili(t('cekSenet.olusturuldu'))
    }
    dialog.value = false
    yenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataKaydet'))
  } finally {
    kaydediliyor.value = false
  }
}

// ---- Aksiyonlar ----
const islemde = ref(false)

const islemSonrasi = (mesaj) => {
  toastBildirim.basarili(mesaj)
  detayDrawer.value = false
  yenile()
}

const tahsilDialog = ref(false)
const tahsilHedef = ref(null)
const tahsilHesapTipi = ref('YOK')
const tahsilKasaId = ref(null)
const tahsilBankaId = ref(null)
const tahsilFaturaId = ref(null)
const tahsilFaturalar = ref([])
const tahsilFaturaYukleniyor = ref(false)
const tahsilHesapSecenekleri = hesapSecenekleri

const tahsilDialogAc = (kayit) => {
  tahsilHedef.value = kayit
  tahsilHesapTipi.value = 'YOK'
  tahsilKasaId.value = null
  tahsilBankaId.value = null
  tahsilFaturaId.value = null
  tahsilFaturalar.value = []
  tahsilDialog.value = true
  faturaYukle(kayit)
}

const faturaYukle = async (kayit) => {
  if (!kayit?.cariHesapId) return
  tahsilFaturaYukleniyor.value = true
  try {
    const r = await faturaAPI.getAll({ cariHesapId: kayit.cariHesapId, size: 200 })
    tahsilFaturalar.value = unwrapList(r)
  } catch {
    tahsilFaturalar.value = []
  } finally {
    tahsilFaturaYukleniyor.value = false
  }
}

const tahsilEt = async () => {
  if (!tahsilHedef.value || islemde.value) return
  islemde.value = true
  try {
    await cekSenetAPI.durumGuncelle(tahsilHedef.value.id, {
      durum: 'TAHSIL_EDILDI',
      faturaId: tahsilFaturaId.value || null,
      kasaId: tahsilHesapTipi.value === 'KASA' ? tahsilKasaId.value : null,
      bankaId: tahsilHesapTipi.value === 'BANKA' ? tahsilBankaId.value : null
    })
    tahsilDialog.value = false
    islemSonrasi(t('cekSenet.tahsilEdildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataDurum'))
  } finally {
    islemde.value = false
  }
}

const iptalDialog = ref(false)
const iptalHedef = ref(null)
const iptalGerekce = ref('')
const iptalHata = ref('')

const iptalDialogAc = (kayit) => {
  iptalHedef.value = kayit
  iptalGerekce.value = ''
  iptalHata.value = ''
  iptalDialog.value = true
}

const iptalKaydet = async () => {
  if (!iptalHedef.value || islemde.value) return
  const gerekce = (iptalGerekce.value || '').trim()
  if (gerekce.length < 3) {
    iptalHata.value = t('cekSenet.tahsilIptalGerekceKisa')
    return
  }
  iptalHata.value = ''
  islemde.value = true
  try {
    await cekSenetAPI.tahsilIptal(iptalHedef.value.id, gerekce)
    iptalDialog.value = false
    islemSonrasi(t('cekSenet.tahsilIptalEdildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataDurum'))
  } finally {
    islemde.value = false
  }
}

const ciroDialog = ref(false)
const ciroHedef = ref(null)
const ciroHata = ref('')
const ciroForm = ref({ cariId: null, not: '' })

const ciroDialogAc = (kayit) => {
  ciroHedef.value = kayit
  ciroForm.value = { cariId: null, not: '' }
  ciroHata.value = ''
  ciroDialog.value = true
}

const ciroKaydet = async () => {
  if (!ciroHedef.value || islemde.value) return
  if (ciroForm.value.cariId == null) {
    ciroHata.value = t('cekSenet.ciroHedefZorunlu')
    return
  }
  ciroHata.value = ''
  islemde.value = true
  try {
    await cekSenetAPI.ciro(ciroHedef.value.id, {
      cariId: ciroForm.value.cariId,
      not: ciroForm.value.not || null
    })
    ciroDialog.value = false
    islemSonrasi(t('cekSenet.ciroYapildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataDurum'))
  } finally {
    islemde.value = false
  }
}

const teminatDialog = ref(false)
const teminatHedef = ref(null)
const teminatAciklama = ref('')

const teminatDialogAc = (kayit) => {
  teminatHedef.value = kayit
  teminatAciklama.value = ''
  teminatDialog.value = true
}

const teminatKaydet = async () => {
  if (!teminatHedef.value || islemde.value) return
  islemde.value = true
  try {
    await cekSenetAPI.teminat(teminatHedef.value.id, teminatAciklama.value || null)
    teminatDialog.value = false
    islemSonrasi(t('cekSenet.teminatVerildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataDurum'))
  } finally {
    islemde.value = false
  }
}

const protestoDialog = ref(false)
const protestoHedef = ref(null)
const protestoAciklama = ref('')

const protestoDialogAc = (kayit) => {
  protestoHedef.value = kayit
  protestoAciklama.value = ''
  protestoDialog.value = true
}

const protestoKaydet = async () => {
  if (!protestoHedef.value || islemde.value) return
  islemde.value = true
  try {
    await cekSenetAPI.protesto(protestoHedef.value.id, protestoAciklama.value || null)
    protestoDialog.value = false
    islemSonrasi(t('cekSenet.protestoEdildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataDurum'))
  } finally {
    islemde.value = false
  }
}

const onayliAksiyon = (mesaj, baslik, basariMesaji, islem) => {
  confirm.require({
    message: mesaj,
    header: baslik,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evet'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await islem()
        islemSonrasi(basariMesaji)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.islemBasarisiz'))
      }
    }
  })
}

const ciroIadeAc = (kayit) => {
  onayliAksiyon(
    t('cekSenet.ciroIadeOnay'),
    t('cekSenet.ciroIade'),
    t('cekSenet.ciroIadeEdildi'),
    () => cekSenetAPI.ciroIade(kayit.id)
  )
}

const teminatIadeAc = (kayit) => {
  onayliAksiyon(
    t('cekSenet.teminatIadeOnay'),
    t('cekSenet.teminatIade'),
    t('cekSenet.teminatIadeEdildi'),
    () => cekSenetAPI.teminatIade(kayit.id)
  )
}

const iadeAlAc = (kayit) => {
  onayliAksiyon(
    t('cekSenet.iadeAlOnay'),
    t('cekSenet.iadeAl'),
    t('cekSenet.iadeAlindi'),
    () => cekSenetAPI.iadeAl(kayit.id)
  )
}

const sil = (kayit) => {
  confirm.require({
    message: t('cekSenet.silOnay'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await cekSenetAPI.delete(kayit.id)
        detayDrawer.value = false
        toastBildirim.basarili(t('cekSenet.silindi'))
        yenile()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataSil'))
      }
    }
  })
}

const satirEylemleri = (kayit) => {
  const d = kayit?.durum
  const odendi = d === 'TAHSIL_EDILDI' || d === 'ODENDI'
  const items = [{ etiket: t('cekSenet.detay'), ikon: 'pi pi-search', islem: () => detayAc(kayit) }]
  if (d === 'PORTFOY' || d === 'TEMINATTA') {
    items.push({
      etiket: t('cekSenet.tahsilEt'),
      ikon: 'pi pi-check',
      sinif: 'eylem-pozitif',
      islem: () => tahsilDialogAc(kayit)
    })
  }
  if (d === 'PORTFOY') {
    items.push({ etiket: t('cekSenet.ciroEt'), ikon: 'pi pi-sync', islem: () => ciroDialogAc(kayit) })
    items.push({ etiket: t('cekSenet.teminatVer'), ikon: 'pi pi-shield', islem: () => teminatDialogAc(kayit) })
  }
  if (d === 'TEMINATTA') {
    items.push({ etiket: t('cekSenet.teminatIade'), ikon: 'pi pi-shield', islem: () => teminatIadeAc(kayit) })
  }
  if (d === 'PORTFOY' || d === 'TEMINATTA') {
    items.push({
      etiket: t('cekSenet.protestoEt'),
      ikon: 'pi pi-exclamation-triangle',
      islem: () => protestoDialogAc(kayit)
    })
  }
  if (d === 'CIRO_EDILDI') {
    items.push({ etiket: t('cekSenet.ciroIade'), ikon: 'pi pi-undo', islem: () => ciroIadeAc(kayit) })
  }
  if (d === 'IADE') {
    items.push({ etiket: t('cekSenet.iadeAl'), ikon: 'pi pi-inbox', islem: () => iadeAlAc(kayit) })
  }
  if (odendi) {
    items.push({ etiket: t('cekSenet.tahsilIptal'), ikon: 'pi pi-times', islem: () => iptalDialogAc(kayit) })
  }
  items.push({ ayrac: true })
  items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(kayit) })
  if (!odendi && d !== 'PROTESTO') {
    items.push({
      etiket: t('common.delete'),
      ikon: 'pi pi-trash',
      sinif: 'eylem-sil',
      islem: () => sil(kayit)
    })
  }
  return items
}

// ---- Hafıza yazımı ----
watch(
  [q, tur, durum, cariId, vadeAraligi, minTutar, sadeceProtesto, vadeAralik],
  () => {
    try {
      localStorage.setItem(
        FILTRE_ANAHTAR,
        JSON.stringify({
          q: q.value || '',
          tur: tur.value,
          durum: durum.value || null,
          cariId: cariId.value ?? null,
          baslangic: vadeAraligi.value?.[0] ? getLocalDateString(vadeAraligi.value[0]) : null,
          bitis: vadeAraligi.value?.[1] ? getLocalDateString(vadeAraligi.value[1]) : null,
          vadeAralik: vadeAralik.value || null,
          minTutar: minTutar.value ?? null,
          protesto: sadeceProtesto.value
        })
      )
    } catch {
      /* depolama kapalı olabilir */
    }
  }
)

watch(aktifSekme, (v) => {
  try {
    localStorage.setItem(TAB_ANAHTAR, String(v))
  } catch {
    /* depolama kapalı olabilir */
  }
  if (v === 1 && !takvimYuklendi.value) takvimYukle()
  if (v === 2 && !gecmisYuklendi.value) gecmisYukle()
})

onMounted(async () => {
  yukle()
  if (aktifSekme.value === 1) takvimYukle()
  if (aktifSekme.value === 2) gecmisYukle()
  try {
    const [c, k, b] = await Promise.all([
      cariHesapAPI.getAll({ size: 500 }),
      kasaAPI.getAll().catch(() => ({ data: [] })),
      bankaAPI.getAll().catch(() => ({ data: [] }))
    ])
    cariler.value = unwrapList(c)
    kasalar.value = unwrapList(k)
    bankalar.value = unwrapList(b)
  } catch {
    /* seçim listeleri yüklenemezse filtreler boş kalır */
  }
})

onBeforeUnmount(() => {
  if (aramaZaman) clearTimeout(aramaZaman)
})
</script>

<style scoped>
.ceksenet-sayfasi {
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
}
.kpi-iskelet {
  min-width: 0;
}
.tablo-iskelet {
  padding: 4px 0;
}
.cs-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.cs-arac .arac-secim {
  min-width: 130px;
}
.cs-arac .cari-secim {
  min-width: 170px;
  max-width: 240px;
}
.arac-secim.min-tutar {
  max-width: 140px;
}
.arac-tarih {
  min-width: 210px;
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
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.tur-seg button.aktif {
  background: var(--accent-soft);
  color: var(--accent);
}
.tur-seg button:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.protesto-sec {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  padding: 7px 12px;
  cursor: pointer;
}
.protesto-sec.aktif {
  background: rgba(239, 68, 68, 0.14);
  border-color: #ef4444;
  color: #ef4444;
}
.protesto-sec:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.aktif-filtre {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.aktif-filtre :deep(.p-button) {
  width: 26px;
  height: 26px;
}
.cs-mobil {
  display: none;
}
.cs-mobil-kart {
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
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-secondary);
}
.cm-alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.cm-alt strong {
  font-size: 14px;
  color: var(--text-primary);
}
.aging-ust {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.aging-ust h3 {
  margin: 0;
  font-size: 15px;
}
.aging-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}
.aging-kart {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  cursor: pointer;
  color: inherit;
}
.aging-kart:hover {
  border-color: var(--accent-border);
}
.aging-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.aging-kart[aria-pressed='true'] {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-soft);
}
.aging-baslik {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.aging-deger {
  font-size: 1.05rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.aging-adet {
  font-size: 11.5px;
  color: var(--text-muted);
}
.aging-yakin .aging-deger {
  color: #14b8a6;
}
.aging-gecikmis .aging-deger {
  color: #ef4444;
}
.aging-orta .aging-deger {
  color: #f97316;
}
.aging-yuksek .aging-deger {
  color: #dc2626;
}
.aging-kritik .aging-deger {
  color: #991b1b;
}
.takvim-kart {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  padding: 12px;
}
.takvim-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.takvim-ay {
  font-size: 14px;
  text-transform: capitalize;
}
.takvim-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}
.gun-baslik {
  text-align: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding: 4px 0;
}
.gun-hucre {
  min-height: 72px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 4px;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}
.gun-hucre.bos {
  background: transparent;
  border-color: transparent;
}
.gun-hucre.bugun {
  border-color: var(--accent);
  box-shadow: inset 0 0 0 1px var(--accent);
}
.gun-hucre.dolu {
  background: var(--accent-soft);
}
.gun-no {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  padding: 2px;
}
.gun-buton {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
  border: none;
  background: transparent;
  padding: 2px;
  cursor: pointer;
  color: inherit;
  border-radius: 6px;
}
.gun-buton:hover {
  background: var(--accent-soft-strong, rgba(20, 184, 166, 0.2));
}
.gun-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.gun-buton .gun-no {
  font-size: 11px;
}
.gun-adet {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--accent);
}
.gun-tutar {
  font-size: 10.5px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.takvim-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.takvim-liste-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-primary);
  cursor: pointer;
  color: inherit;
  text-align: left;
}
.takvim-liste-satir:hover {
  border-color: var(--accent-border);
}
.tl-tarih {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
}
.gecmis-kaynak {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.gecmis-gecis {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-secondary);
}
.gecmis-gecis i {
  font-size: 9px;
  color: var(--text-muted);
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
.modal-aciklama {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.5;
}
.modal-aciklama.uyari {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ef4444;
}
.modal-ozet {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-muted);
  font-size: 13px;
  font-weight: 600;
}
.hata-metin {
  color: #ef4444;
  font-size: 12px;
}
.w-full {
  width: 100%;
}
@media (max-width: 768px) {
  .cs-masa {
    display: none;
  }
  .cs-mobil {
    display: block;
  }
  .cs-arac .arac-secim,
  .cs-arac .cari-secim {
    flex: 1 1 140px;
    max-width: none;
  }
  .arac-secim,
  .arac-secim.min-tutar,
  .arac-tarih {
    max-width: none;
    flex: 1 1 140px;
    min-width: 0;
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
    justify-content: center;
  }
  .protesto-sec {
    flex: 1 1 100%;
    justify-content: center;
  }
  .gun-hucre {
    min-height: 58px;
  }
  .gun-tutar {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .gun-buton,
  .takvim-liste-satir {
    transition: none;
  }
}
</style>
