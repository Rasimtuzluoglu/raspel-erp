<template>
  <div class="raporlar-container">
    <div class="raporlar-header-bar">
      <h1 class="page-title">
        {{ t('raporlar.title') }}
      </h1>
      <TarihHizliSecim
        v-model="tarihAraligi"
        style="margin-right: 12px"
      />
      <div class="rapor-doviz-secim">
        <label><i class="pi pi-dollar" /> {{ t('raporlar.raporParaBirimi') }}</label>
        <Dropdown
          v-model="dovizStore.aktifParaBirimi"
          :options="['TRY', 'USD', 'EUR', 'GBP', 'SAR', 'GAU']"
          class="rapor-doviz-dropdown"
        />
      </div>
    </div>

    <div
      v-if="favoriRaporlar && favoriRaporlar.length"
      class="favori-raporlar"
    >
      <span class="favori-baslik"><i
        class="pi pi-star-fill"
        style="color: #fbbf24"
      /> {{ t('raporlar.sikKullanilanlar') }}</span>
      <Button
        v-for="r in favoriRaporlar"
        :key="r.key"
        :label="r.ad"
        size="small"
        class="p-button-sm p-button-outlined"
        @click="favoriAc(r)"
      />
    </div>

    <TabView
      v-model:active-index="aktifSekme"
      :lazy="true"
    >
      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('cariEkstre') }"
              @click.stop="raporFavoriDegistir('cariEkstre', 0, t('raporlar.cariEkstre'))"
            />
            {{ t('raporlar.cariEkstre') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>{{ t('raporlar.cariHesap') }}</label>
            <Dropdown
              v-model="ekstreCariId"
              :options="cariHesapStore?.cariHesaplar || []"
              option-label="ad"
              option-value="id"
              :placeholder="t('faturalar.seciniz')"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('raporlar.baslangic') }}</label>
            <DatePicker
              v-model="ekstreBas"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('raporlar.bitis') }}</label>
            <DatePicker
              v-model="ekstreBit"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button
              :label="t('raporlar.raporGetir')"
              icon="pi pi-search"
              :loading="ekstreLoading"
              @click="getCariEkstre"
            />
          </div>
        </div>

        <div
          v-if="ekstreData"
          ref="ekstreKart"
          class="rapor-sonuc"
        >
          <div class="rapor-bilgi">
            <h3>{{ ekstreData.cariAd }}</h3>
            <p>
              {{ t('raporlar.donemBasBakiye') }}
              <strong :class="ekstreData.donemBasBakiye >= 0 ? 'positive' : 'negative'">{{
                formatCurrency(ekstreData.donemBasBakiye)
              }}</strong>
            </p>
            <p>
              {{ t('raporlar.donemSonBakiye') }}
              <strong :class="ekstreData.donemSonBakiye >= 0 ? 'positive' : 'negative'">{{
                formatCurrency(ekstreData.donemSonBakiye)
              }}</strong>
            </p>
            <div class="rapor-aksiyonlar">
              <Button
                icon="pi pi-print"
                :label="t('raporlar.pdf')"
                class="p-button-sm p-button-outlined"
                @click="yazdir(ekstreKart)"
              />
              <Button
                icon="pi pi-envelope"
                :label="t('raporlar.epostaGonder')"
                class="p-button-sm p-button-outlined"
                @click="epostaGonder(ekstreData.cariAd, t('raporlar.cariEkstreRaporu'))"
              />
            </div>
          </div>
          <DataTable
            :value="ekstreData.hareketler"
            striped-rows
            :rows="10"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <Column
              field="hareketTarihi"
              :header="t('common.date')"
              style="width: 100px"
            >
              <template #body="s">
                {{ formatDate(s.data.hareketTarihi) }}
              </template>
            </Column>
            <Column
              field="tur"
              :header="t('raporlar.tur')"
              style="width: 90px"
            >
              <template #body="s">
                <span :class="['badge', s.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
                  {{ s.data.tur === 'TAHSILAT' ? t('raporlar.tahsilat') : t('raporlar.odeme') }}
                </span>
              </template>
            </Column>
            <Column
              field="tutar"
              :header="t('common.amount')"
              style="width: 120px"
            >
              <template #body="s">
                <span :class="s.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">{{
                  formatCurrency(s.data.tutar)
                }}</span>
              </template>
            </Column>
            <Column
              field="aciklama"
              :header="t('common.description')"
            />
          </DataTable>
          <Message
            v-if="ekstreData.hareketler.length === 0"
            severity="info"
            :text="t('raporlar.hareketYok')"
          />
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('gelirGider') }"
              @click.stop="raporFavoriDegistir('gelirGider', 1, t('raporlar.gelirGiderOzeti'))"
            />
            {{ t('raporlar.gelirGiderOzeti') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>{{ t('raporlar.baslangic') }}</label>
            <DatePicker
              v-model="ggBas"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('raporlar.bitis') }}</label>
            <DatePicker
              v-model="ggBit"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button
              :label="t('raporlar.raporGetir')"
              icon="pi pi-search"
              :loading="ggLoading"
              @click="getGelirGider"
            />
          </div>
        </div>

        <div
          v-if="ggData"
          ref="ggKart"
          class="rapor-sonuc"
        >
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir">
              <span>{{ $t('raporlar.toplamGelir') }}</span><strong>{{ formatCurrency(ggData.toplamGelir) }}</strong>
            </div>
            <div class="ozet-kart gider">
              <span>{{ $t('raporlar.toplamGider') }}</span><strong>{{ formatCurrency(ggData.toplamGider) }}</strong>
            </div>
            <div
              class="ozet-kart"
              :class="ggData.netKarZarar >= 0 ? 'kar' : 'zarar'"
            >
              <span>{{ $t('raporlar.netKarZarar') }}</span><strong>{{ formatCurrency(ggData.netKarZarar) }}</strong>
            </div>
            <div class="rapor-aksiyonlar">
              <Button
                icon="pi pi-print"
                :label="t('raporlar.pdf')"
                class="p-button-sm p-button-outlined"
                @click="yazdir(ggKart)"
              />
              <Button
                icon="pi pi-envelope"
                :label="t('raporlar.epostaGonder')"
                class="p-button-sm p-button-outlined"
                @click="epostaGonder(t('raporlar.gelirGiderOzeti'), t('raporlar.gelirGiderRaporu'))"
              />
            </div>
          </div>

          <h3 style="margin-top: 25px">
            {{ $t('raporlar.aylikDagilim') }}
          </h3>
          <DataTable
            :value="ggData.aylikDagilim"
            striped-rows
          >
            <Column
              field="ay"
              :header="$t('raporlar.ay')"
            />
            <Column
              field="net"
              :header="$t('raporlar.netTutar')"
            >
              <template #body="s">
                <span :class="s.data.net >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.net) }}</span>
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('kdv') }"
              @click.stop="raporFavoriDegistir('kdv', 2, t('raporlar.kdvRaporu'))"
            />
            {{ t('raporlar.kdvRaporu') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>{{ t('raporlar.baslangic') }}</label>
            <DatePicker
              v-model="kdvBas"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('raporlar.bitis') }}</label>
            <DatePicker
              v-model="kdvBit"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button
              :label="t('raporlar.raporGetir')"
              icon="pi pi-search"
              :loading="kdvLoading"
              @click="getKdv"
            />
          </div>
        </div>

        <div
          v-if="kdvData"
          class="rapor-sonuc"
        >
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir">
              <span>{{ $t('raporlar.cikisKdvSatis') }}</span><strong>{{ formatCurrency(kdvData.toplamKdvCikis) }}</strong>
            </div>
            <div class="ozet-kart gider">
              <span>{{ $t('raporlar.girisKdvAlis') }}</span><strong>{{ formatCurrency(kdvData.toplamKdvGiris) }}</strong>
            </div>
            <div
              class="ozet-kart"
              :class="kdvData.kdvFarki >= 0 ? 'kar' : 'zarar'"
            >
              <span>{{ $t('raporlar.kdvFarki') }}</span><strong>{{ formatCurrency(kdvData.kdvFarki) }}</strong>
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('yaslandirma') }"
              @click.stop="raporFavoriDegistir('yaslandirma', 3, t('raporlar.yaslandirma'))"
            />
            {{ t('raporlar.yaslandirma') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <Button
            :label="t('raporlar.raporGetir')"
            icon="pi pi-search"
            :loading="yasLoading"
            @click="getYaslandirma"
          />
        </div>
        <div
          v-if="yasData"
          class="rapor-sonuc"
        >
          <DataTable
            :value="yasData"
            striped-rows
            :rows="10"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <Column
              field="cariAd"
              :header="$t('raporlar.cariHesap')"
            />
            <Column
              field="bakiye"
              :header="$t('raporlar.alacakBakiyesi')"
              style="width: 140px"
            >
              <template #body="s">
                <span class="positive">{{ formatCurrency(s.data.bakiye) }}</span>
              </template>
            </Column>
            <Column
              field="gun"
              :header="$t('raporlar.gun')"
              style="width: 80px"
            />
            <Column
              field="aralik"
              :header="$t('raporlar.vadeAraligi')"
              style="width: 130px"
            >
              <template #body="s">
                <span :class="['vade-badge', vadeClass(s.data.aralik)]">{{ s.data.aralik }}</span>
              </template>
            </Column>
          </DataTable>
          <Message
            v-if="yasData && yasData.length === 0"
            severity="info"
            :text="$t('raporlar.alacakliCariYok')"
          />
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('cariKarlilik') }"
              @click.stop="raporFavoriDegistir('cariKarlilik', 4, t('raporlar.cariKarlilik'))"
            />
            {{ t('raporlar.cariKarlilik') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>{{ t('raporlar.baslangic') }}</label>
            <DatePicker
              v-model="ckBas"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('raporlar.bitis') }}</label>
            <DatePicker
              v-model="ckBit"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button
              :label="t('raporlar.raporGetir')"
              icon="pi pi-search"
              :loading="ckLoading"
              @click="getCariKarlilik"
            />
          </div>
        </div>

        <div
          v-if="ckData"
          ref="ckKart"
          class="rapor-sonuc"
        >
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir">
              <span>{{ $t('raporlar.toplamSatis') }}</span><strong>{{ formatCurrency(ckData.toplamSatis) }}</strong>
            </div>
            <div class="ozet-kart gider">
              <span>{{ $t('raporlar.toplamMaliyet') }}</span><strong>{{ formatCurrency(ckData.toplamMaliyet) }}</strong>
            </div>
            <div
              class="ozet-kart"
              :class="ckData.toplamKar >= 0 ? 'kar' : 'zarar'"
            >
              <span>{{ $t('raporlar.toplamKar') }}</span><strong>{{ formatCurrency(ckData.toplamKar) }}</strong>
            </div>
            <div class="rapor-aksiyonlar">
              <Button
                icon="pi pi-print"
                :label="t('raporlar.pdf')"
                class="p-button-sm p-button-outlined"
                @click="yazdir(ckKart)"
              />
            </div>
          </div>

          <DataTable
            :value="ckData.satirlar"
            striped-rows
            :rows="10"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <Column
              field="cariAd"
              :header="$t('raporlar.cariHesap')"
            />
            <Column
              field="faturaSayisi"
              :header="$t('raporlar.fatura')"
              style="width: 90px"
            />
            <Column
              field="toplamSatis"
              :header="$t('raporlar.satis')"
              style="width: 140px"
            >
              <template #body="s">
                {{ formatCurrency(s.data.toplamSatis) }}
              </template>
            </Column>
            <Column
              field="toplamMaliyet"
              :header="$t('raporlar.maliyet')"
              style="width: 140px"
            >
              <template #body="s">
                <span class="gizli-veri">{{ formatCurrency(s.data.toplamMaliyet) }}</span>
              </template>
            </Column>
            <Column
              field="kar"
              :header="$t('raporlar.kar')"
              style="width: 140px"
            >
              <template #body="s">
                <span
                  class="gizli-veri"
                  :class="s.data.kar >= 0 ? 'positive' : 'negative'"
                >{{ formatCurrency(s.data.kar) }}</span>
              </template>
            </Column>
            <Column
              field="karMarji"
              :header="$t('raporlar.karMarji')"
              style="width: 110px"
            >
              <template #body="s">
                <span
                  class="gizli-veri"
                  :class="s.data.karMarji >= 0 ? 'positive' : 'negative'"
                >%{{ s.data.karMarji }}</span>
              </template>
            </Column>
          </DataTable>
          <Message
            v-if="!ckData.satirlar.length"
            severity="info"
            :text="$t('raporlar.donemdeSatisYok')"
          />
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('tedarikciUrunler') }"
              @click.stop="raporFavoriDegistir('tedarikciUrunler', 5, t('raporlar.tedarikciUrunleri'))"
            />
            {{ t('raporlar.tedarikciUrunleri') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>{{ $t('raporlar.tedarikciFiltresi') }}</label>
            <Dropdown
              v-model="tuFiltre"
              :options="tuTedarikciler"
              :placeholder="$t('raporlar.tumTedarikciler')"
              class="w-full"
              :show-clear="true"
            />
          </div>
          <Button
            :label="t('raporlar.yenile')"
            icon="pi pi-refresh"
            size="small"
            class="p-button-outlined"
            :loading="tuLoading"
            @click="getTedarikciUrunler"
          />
        </div>
        <DataTable
          :value="tuFiltrelenmisData"
          size="small"
          striped-rows
          :loading="tuLoading"
          row-group-mode="subheader"
          group-rows-by="cariHesapAd"
        >
          <template #groupheader="{ group }">
            <span class="tedarikci-grup"><i class="pi pi-building" /> {{ group.value }} ({{ $t('raporlar.tedarikci') }})</span>
          </template>
          <Column
            field="stokKodu"
            :header="$t('raporlar.stokKodu')"
          />
          <Column
            field="stokAd"
            :header="$t('raporlar.urun')"
          />
          <Column
            field="toplamMiktar"
            :header="$t('raporlar.toplamMiktar')"
          />
          <Column :header="$t('raporlar.sonBirimFiyat')">
            <template #body="s">
              {{ formatCurrency(s.data.sonBirimFiyat) }}
            </template>
          </Column>
          <Column :header="$t('raporlar.sonAlisTarihi')">
            <template #body="s">
              {{ s.data.sonTarih }}
            </template>
          </Column>
        </DataTable>
        <Message
          v-if="(!tuData || !tuData.length)"
          severity="info"
          :text="$t('raporlar.henuzAlisFaturasiYok')"
        />
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('urunKarlilik') }"
              @click.stop="raporFavoriDegistir('urunKarlilik', 6, t('raporlar.urunKarliligi'))"
            />
            {{ t('raporlar.urunKarliligi') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <Button
            :label="t('raporlar.yenile')"
            icon="pi pi-refresh"
            size="small"
            class="p-button-outlined"
            :loading="ukLoading"
            @click="getUrunKarlilik"
          />
        </div>
        <DataTable
          :value="ukData"
          size="small"
          striped-rows
          :loading="ukLoading"
        >
          <Column
            field="stokKodu"
            :header="$t('raporlar.stokKodu')"
          />
          <Column
            field="stokAd"
            :header="$t('raporlar.urun')"
          />
          <Column :header="$t('raporlar.alisMaliyeti')">
            <template #body="s">
              <span class="gizli-veri">{{ formatCurrency(s.data.alisFiyat) }}</span>
            </template>
          </Column>
          <Column :header="$t('raporlar.satisFiyati')">
            <template #body="s">
              {{ formatCurrency(s.data.satisFiyati) }}
            </template>
          </Column>
          <Column :header="$t('raporlar.kar')">
            <template #body="s">
              <span
                class="gizli-veri"
                :class="s.data.kar >= 0 ? 'positive' : 'negative'"
              >{{ formatCurrency(s.data.kar) }}</span>
            </template>
          </Column>
          <Column :header="$t('raporlar.karMarji')">
            <template #body="s">
              <span
                class="gizli-veri"
                :class="s.data.karMarji >= 0 ? 'positive' : 'negative'"
              >%{{ s.data.karMarji }}</span>
            </template>
          </Column>
        </DataTable>
        <Message
          v-if="(!ukData || !ukData.length)"
          severity="info"
          :text="$t('raporlar.henuzUrunYok')"
        />
      </TabPanel>

      <!-- 8. Nakit Akışı Projeksiyonu -->
      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('nakitAkisi') }"
              @click.stop="raporFavoriDegistir('nakitAkisi', 7, t('raporlar.nakitAkisi'))"
            />
            {{ t('raporlar.nakitAkisiProjeksiyonu') }}
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>{{ $t('raporlar.projeksiyonSuresi') }}</label>
            <Dropdown
              v-model="nakitGun"
              :options="[
                { label: $t('raporlar.projeksiyonGun', { n: 30 }), value: 30 },
                { label: $t('raporlar.projeksiyonGun', { n: 60 }), value: 60 },
                { label: $t('raporlar.projeksiyonGun', { n: 90 }), value: 90 }
              ]"
              option-label="label"
              option-value="value"
              class="w-full"
              @change="getNakitAkisi"
            />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button
              :label="t('raporlar.yenile')"
              icon="pi pi-refresh"
              :loading="nakitLoading"
              @click="getNakitAkisi"
            />
          </div>
        </div>

        <div
          v-if="nakitData"
          class="rapor-sonuc"
        >
          <div class="ozet-kartlar">
            <div class="ozet-kart">
              <span>{{ $t('raporlar.mevcutLikidite') }}</span>
              <strong>{{ formatCurrency(nakitData.baslangicBakiyesi) }}</strong>
            </div>
            <div class="ozet-kart gelir">
              <span>{{ $t('raporlar.beklenenTahsilatlar') }}</span>
              <strong class="positive">+{{ formatCurrency(nakitData.toplamBeklenenGiris) }}</strong>
            </div>
            <div class="ozet-kart gider">
              <span>{{ $t('raporlar.beklenenOdemeler') }}</span>
              <strong class="negative">-{{ formatCurrency(nakitData.toplamBeklenenCikis) }}</strong>
            </div>
            <div
              class="ozet-kart"
              :class="nakitData.tahminiBitisBakiyesi >= 0 ? 'kar' : 'zarar'"
            >
              <span>{{ $t('raporlar.gunSonrakiTahminiKasa', { n: nakitGun }) }}</span>
              <strong>{{ formatCurrency(nakitData.tahminiBitisBakiyesi) }}</strong>
            </div>
          </div>

          <h3 style="margin-top: 25px">
            {{ $t('raporlar.gunlukNakitAkisiDetayi') }}
          </h3>
          <DataTable
            :value="nakitData.gunlukAkis"
            striped-rows
            size="small"
            :rows="15"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <Column
              field="tarih"
              :header="t('common.date')"
              style="width: 120px"
            >
              <template #body="s">
                {{ formatDate(s.data.tarih) }}
              </template>
            </Column>
            <Column
              field="beklenenGiris"
              :header="$t('raporlar.girisTahsilat')"
              style="width: 140px"
            >
              <template #body="s">
                <span :class="s.data.beklenenGiris > 0 ? 'positive' : ''">{{ formatCurrency(s.data.beklenenGiris) }}</span>
              </template>
            </Column>
            <Column
              field="beklenenCikis"
              :header="$t('raporlar.cikisOdeme')"
              style="width: 140px"
            >
              <template #body="s">
                <span :class="s.data.beklenenCikis > 0 ? 'negative' : ''">{{ formatCurrency(s.data.beklenenCikis) }}</span>
              </template>
            </Column>
            <Column
              field="netAkis"
              :header="$t('raporlar.netGunlukAkis')"
              style="width: 140px"
            >
              <template #body="s">
                <span :class="s.data.netAkis >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.netAkis) }}</span>
              </template>
            </Column>
            <Column
              field="kumulatifBakiye"
              :header="$t('raporlar.tahminiKasaBakiyesi')"
              style="width: 170px"
            >
              <template #body="s">
                <strong :class="s.data.kumulatifBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.kumulatifBakiye) }}</strong>
              </template>
            </Column>
            <Column
              field="aciklama"
              :header="t('common.description')"
            />
          </DataTable>
        </div>
      </TabPanel>

      <TabPanel :header="$t('raporlar.pivotTablo')">
        <div class="rapor-icerik">
          <div class="pivot-kontroller">
            <div class="pivot-alan">
              <label>{{ $t('raporlar.satir') }}</label>
              <Dropdown
                v-model="pivotSatir"
                :options="pivotBoyutlar"
                option-label="label"
                option-value="value"
              />
            </div>
            <div class="pivot-alan">
              <label>{{ $t('raporlar.sutun') }}</label>
              <Dropdown
                v-model="pivotSutun"
                :options="pivotBoyutlar"
                option-label="label"
                option-value="value"
              />
            </div>
            <div class="pivot-alan">
              <label>{{ $t('raporlar.deger') }}</label>
              <Dropdown
                v-model="pivotDeger"
                :options="pivotMetrikler"
                option-label="label"
                option-value="value"
              />
            </div>
            <div class="pivot-alan">
              <label>{{ $t('raporlar.tarihAraligi') }}</label>
              <TarihHizliSecim v-model="pivotTarih" />
            </div>
            <Button
              :label="$t('raporlar.uygula')"
              icon="pi pi-search"
              class="p-button-sm"
              @click="pivotYukle"
            />
          </div>

          <div
            v-if="pivotYukleniyor"
            class="loading"
          >
            <i class="pi pi-spin pi-spinner" /> {{ $t('raporlar.hesaplaniyor') }}
          </div>

          <div
            v-else-if="pivotVerisi"
            class="pivot-sonuc"
          >
            <DataTable
              :value="pivotSatirlar"
              size="small"
              striped-rows
              scrollable
              scroll-height="500px"
            >
              <Column
                header=""
                frozen
                style="min-width: 160px"
              >
                <template #body="{ data }">
                  <strong>{{ data }}</strong>
                </template>
              </Column>
              <Column
                v-for="s in pivotVerisi.sutunlar"
                :key="s"
                :header="s"
                style="min-width: 110px; text-align: right"
              >
                <template #body="{ data }">
                  {{ pivotDeger === 'adet' ? pivotHucre(data, s) : formatCurrency(pivotHucre(data, s)) }}
                </template>
              </Column>
              <Column
                :header="$t('raporlar.toplam')"
                style="min-width: 110px; text-align: right"
              >
                <template #body="{ data }">
                  <strong>{{ pivotDeger === 'adet' ? pivotSatirToplami(data) : formatCurrency(pivotSatirToplami(data)) }}</strong>
                </template>
              </Column>
            </DataTable>
            <div class="pivot-genel">
              {{ $t('raporlar.genelToplamEtiketi') }}
              <strong>{{ pivotDeger === 'adet' ? pivotVerisi.genelToplam : formatCurrency(pivotVerisi.genelToplam) }}</strong>
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { raporAPI } from '../api/index.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import { useI18n } from 'vue-i18n'

const dovizStore = useDovizStore()
import { safeGet, safeSet } from '../utils/safeStorage.js'
const toast = useToast()
const toastBildirim = useToastBildirim()
const { t } = useI18n()

const cariHesapStore = useCariHesapStore()

const FAVORI_ANAHTAR = 'raspel_favori_raporlar'
const aktifSekme = ref(0)
const favoriRaporlar = ref(safeGet(FAVORI_ANAHTAR, []))
const tarihAraligi = ref(null)

// Pivot tablo state
const pivotSatir = ref('cari')
const pivotSutun = ref('ay')
const pivotDeger = ref('tutar')
const pivotTarih = ref(null)
const pivotVerisi = ref(null)
const pivotYukleniyor = ref(false)
const pivotBoyutlar = computed(() => [
  { label: t('raporlar.pivotCari'), value: 'cari' },
  { label: t('raporlar.pivotUrun'), value: 'stok' },
  { label: t('raporlar.pivotKategori'), value: 'kategori' },
  { label: t('raporlar.pivotTur'), value: 'tur' },
  { label: t('raporlar.pivotOdemeDurumu'), value: 'odeme' },
  { label: t('raporlar.ay'), value: 'ay' }
])
const pivotMetrikler = computed(() => [
  { label: t('raporlar.metrikTutar'), value: 'tutar' },
  { label: t('raporlar.metrikAdet'), value: 'adet' }
])

const pivotSatirlar = computed(() => pivotVerisi.value?.satirlar || [])
const pivotHucre = (satir, sutun) => pivotVerisi.value?.hucreler?.[satir]?.[sutun] ?? 0
const pivotSatirToplami = (satir) => pivotVerisi.value?.satirToplamlari?.[satir] ?? 0

const pivotYukle = async () => {
  pivotYukleniyor.value = true
  try {
    const params = { satir: pivotSatir.value, sutun: pivotSutun.value, deger: pivotDeger.value }
    if (pivotTarih.value && pivotTarih.value.length === 2) {
      params.baslangic = pivotTarih.value[0]
      params.bitis = pivotTarih.value[1]
    }
    const r = await raporAPI.pivot(params)
    pivotVerisi.value = r.data
  } catch {
    pivotVerisi.value = null
  } finally {
    pivotYukleniyor.value = false
  }
}

const raporFavori = (key) => favoriRaporlar.value.some((r) => r.key === key)

const raporFavoriDegistir = (key, index, ad) => {
  const mevcut = favoriRaporlar.value.findIndex((r) => r.key === key)
  if (mevcut > -1) {
    favoriRaporlar.value.splice(mevcut, 1)
  } else {
    favoriRaporlar.value.push({ key, index, ad, tarih: tarihAraligi.value ? [...tarihAraligi.value] : null })
  }
  safeSet(FAVORI_ANAHTAR, favoriRaporlar.value)
}

const favoriAc = (r) => {
  aktifSekme.value = r.index
  if (r.tarih) tarihAraligi.value = [...r.tarih]
}

const ekstreKart = ref(null)
const ggKart = ref(null)

const yazdir = (hedef) => {
  const icerik = hedef.value?.outerHTML || ''
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) {
    toastBildirim.hata(t('raporlar.pencereEngellendi'))
    return
  }
  win.document.write(`<html><head><title>${t('raporlar.rapor')}</title><style>
    body { font-family: Arial, sans-serif; padding: 24px; color: #1e293b; }
    table { width: 100%; border-collapse: collapse; margin: 12px 0; }
    th { background: #1976d2; color: white; padding: 8px; text-align: left; font-size: 12px; }
    td { padding: 8px; border-bottom: 1px solid #e2e8f0; font-size: 12px; }
    h3 { margin: 0 0 8px; }
    .rapor-aksiyonlar, .p-button, .p-paginator, .p-dropdown, .p-inputtext { display: none !important; }
    .positive { color: #16a34a; } .negative { color: #dc2626; }
    .ozet-kartlar { display: flex; gap: 12px; margin: 12px 0; flex-wrap: wrap; }
    .ozet-kart { border: 1px solid #e2e8f0; padding: 12px; border-radius: 8px; }
    .ozet-kart span { display: block; font-size: 11px; color: #64748b; }
    .ozet-kart strong { font-size: 16px; }
  </style></head><body>${icerik}</body></html>`)
  win.document.close()
  setTimeout(() => {
    win.focus()
    win.print()
  }, 200)
}

const epostaGonder = (baslik, raporAdi) => {
  toast.add({ severity: 'info', summary: t('raporlar.paylasim'), detail: t('raporlar.epostaPaylasilacak', { rapor: raporAdi }), life: 4000 })
}

const ekstreCariId = ref(null)
const ekstreBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ekstreBit = ref(new Date())
const ekstreData = ref(null)
const ekstreLoading = ref(false)

const ggBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ggBit = ref(new Date())
const ggData = ref(null)
const ggLoading = ref(false)

const kdvBas = ref(new Date(new Date().getFullYear(), 0, 1))
const kdvBit = ref(new Date())
const kdvData = ref(null)
const kdvLoading = ref(false)

const yasData = ref(null)
const yasLoading = ref(false)

const ckBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ckBit = ref(new Date())
const ckData = ref(null)
const ckLoading = ref(false)
const ckKart = ref(null)

const tuData = ref([])
const tuLoading = ref(false)
const tuFiltre = ref(null)

const tuTedarikciler = computed(() => {
  const set = new Set(tuData.value.map((d) => d.cariHesapAd).filter(Boolean))
  return [...set].sort()
})

const tuFiltrelenmisData = computed(() => {
  if (!tuFiltre.value) return tuData.value
  return tuData.value.filter((d) => d.cariHesapAd === tuFiltre.value)
})

const getTedarikciUrunler = async () => {
  tuLoading.value = true
  try {
    const r = await raporAPI.tedarikciUrunler()
    tuData.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.tedarikciRaporuHata'))
  }
  tuLoading.value = false
}

const ukData = ref([])
const ukLoading = ref(false)

const getUrunKarlilik = async () => {
  ukLoading.value = true
  try {
    const r = await raporAPI.urunKarlilik()
    ukData.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.urunKarlilikHata'))
  }
  ukLoading.value = false
}

const nakitGun = ref(30)
const nakitData = ref(null)
const nakitLoading = ref(false)

const getNakitAkisi = async () => {
  nakitLoading.value = true
  try {
    const r = await raporAPI.nakitAkisiProjeksiyonu(nakitGun.value)
    nakitData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.nakitAkisiHata'))
  }
  nakitLoading.value = false
}

watch(tarihAraligi, (v) => {
  if (!v || v.length !== 2 || !v[0]) return
  if (aktifSekme.value === 0) {
    ekstreBas.value = v[0]
    ekstreBit.value = v[1]
  } else if (aktifSekme.value === 1) {
    ggBas.value = v[0]
    ggBit.value = v[1]
    getGelirGider()
  } else if (aktifSekme.value === 2) {
    kdvBas.value = v[0]
    kdvBit.value = v[1]
    getKdv()
  }
})

const formatDateForApi = (d) => {
  if (!d) return ''
  return getLocalDateString(d)
}

onMounted(async () => {
  await cariHesapStore.getAllCariHesaplar()
  sekmeYukle(aktifSekme.value)
})

// Raporları yalnızca ilgili sekme açıldığında yükle (ilk açılışta donmayı önler).
const sekmeYukle = (idx) => {
  if (idx === 1) getGelirGider()
  else if (idx === 2) getKdv()
  else if (idx === 3) getYaslandirma()
  else if (idx === 4) getCariKarlilik()
  else if (idx === 5) getTedarikciUrunler()
  else if (idx === 6) getUrunKarlilik()
  else if (idx === 7) getNakitAkisi()
}

watch(aktifSekme, (idx) => sekmeYukle(idx))

const getCariEkstre = async () => {
  if (!ekstreCariId.value) {
    toastBildirim.uyari(t('raporlar.cariHesapSeciniz'))
    return
  }
  ekstreLoading.value = true
  try {
    const r = await raporAPI.cariEkstre({
      cariHesapId: ekstreCariId.value,
      baslangic: formatDateForApi(ekstreBas.value),
      bitis: formatDateForApi(ekstreBit.value)
    })
    ekstreData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.cariEkstreHata'))
    ekstreData.value = null
  } finally {
    ekstreLoading.value = false
  }
}

const getGelirGider = async () => {
  ggLoading.value = true
  try {
    const r = await raporAPI.gelirGider({
      baslangic: formatDateForApi(ggBas.value),
      bitis: formatDateForApi(ggBit.value)
    })
    ggData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.gelirGiderHata'))
    ggData.value = null
  } finally {
    ggLoading.value = false
  }
}

const getKdv = async () => {
  kdvLoading.value = true
  try {
    const r = await raporAPI.kdv({
      baslangic: formatDateForApi(kdvBas.value),
      bitis: formatDateForApi(kdvBit.value)
    })
    kdvData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.kdvHata'))
    kdvData.value = null
  } finally {
    kdvLoading.value = false
  }
}

const getYaslandirma = async () => {
  yasLoading.value = true
  try {
    const r = await raporAPI.yaslandirma()
    yasData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.yaslandirmaHata'))
    yasData.value = null
  } finally {
    yasLoading.value = false
  }
}

const getCariKarlilik = async () => {
  ckLoading.value = true
  try {
    const r = await raporAPI.cariKarlilik({
      baslangic: formatDateForApi(ckBas.value),
      bitis: formatDateForApi(ckBit.value)
    })
    ckData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.cariKarlilikHata'))
    ckData.value = null
  } finally {
    ckLoading.value = false
  }
}

const vadeClass = (aralik) => {
  if (aralik.startsWith('0')) return 'risk-yok'
  if (aralik.startsWith('31')) return 'risk-az'
  if (aralik.startsWith('61')) return 'risk-orta'
  return 'risk-yuksek'
}

const formatCurrency = (v) => {
  const deger = v ?? 0
  const birim = dovizStore.aktifParaBirimi
  if (birim && birim !== 'TRY') {
    const cevrilen = dovizStore.convert(deger, 'TRY', birim)
    return dovizStore.formatPara(cevrilen, birim)
  }
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(deger)
}

import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'
</script>

<style scoped>
.pivot-kontroller {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pivot-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pivot-alan label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.pivot-sonuc {
  margin-top: 8px;
}
.pivot-genel {
  margin-top: 12px;
  text-align: right;
  font-size: 15px;
}
.pivot-genel strong {
  color: var(--accent);
}
.raporlar-container {
  padding: 0;
  max-width: 100%;
}
.raporlar-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.raporlar-header-bar h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.rapor-doviz-secim {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  padding: 6px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
}
.rapor-doviz-dropdown {
  width: 110px;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.rapor-filtre {
  display: flex;
  gap: 15px;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  padding: 20px;
  border-radius: 12px;
}
.form-group {
  min-width: min(200px, 100%);
  flex: 1 1 160px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: #333;
  font-size: 13px;
}
.filtre-btn {
  min-width: auto;
}
.rapor-sonuc {
  margin-top: 20px;
}
.rapor-aksiyonlar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.rapor-sekme-baslik {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rapor-sekme-baslik .pi-star {
  font-size: 13px;
  opacity: 0.35;
  cursor: pointer;
}
.rapor-sekme-baslik .pi-star:hover {
  opacity: 0.8;
  color: #fbbf24;
}
.rapor-sekme-baslik .pi-star.favori {
  opacity: 1;
  color: #fbbf24;
}
.rapor-sekme-baslik .pi-star.favori:before {
  content: '\e936';
}
.favori-raporlar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 16px;
}
.favori-baslik {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.rapor-bilgi {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
}
.rapor-bilgi h3 {
  margin: 0 0 10px 0;
  color: #1976d2;
}
.rapor-bilgi p {
  margin: 5px 0;
}
.ozet-kartlar {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  gap: 15px;
}
.ozet-kart {
  background: var(--bg-card);
  padding: 20px;
  border-radius: 14px;
  border: 1px solid var(--border);
  text-align: center;
}
.ozet-kart span {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.ozet-kart strong {
  font-size: 22px;
}
.ozet-kart.gelir strong {
  color: #4caf50;
}
.ozet-kart.gider strong {
  color: #f44336;
}
.ozet-kart.kar strong {
  color: #4caf50;
}
.ozet-kart.zarar strong {
  color: #f44336;
}
.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.badge.tahsilat {
  background: #e8f5e9;
  color: #2e7d32;
}
.badge.odeme {
  background: #ffebee;
  color: #c62828;
}
.positive {
  color: #4caf50;
  font-weight: bold;
}
.negative {
  color: #f44336;
  font-weight: bold;
}
.vade-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.risk-yok {
  background: #e8f5e9;
  color: #2e7d32;
}
.risk-az {
  background: #fff3e0;
  color: #e65100;
}
.risk-orta {
  background: #ffebee;
  color: #c62828;
}
.risk-yuksek {
  background: #fce4ec;
  color: #880e4f;
}
.w-full {
  width: 100% !important;
}
.tedarikci-grup {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-primary);
}
.tedarikci-grup .pi {
  color: #3b82f6;
}
</style>
