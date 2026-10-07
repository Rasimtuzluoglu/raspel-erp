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
        <!-- Ekrandaki değerler seçilen para birimine çevrilir; indirilen PDF/Excel
             ise sunucudan TRY üretilir. Farkı kullanıcıya açıkça bildir. -->
        <small
          v-if="dovizStore.aktifParaBirimi && dovizStore.aktifParaBirimi !== 'TRY'"
          class="doviz-notu"
        >{{ t('raporlar.exportParaBirimiNotu') }}</small>
      </div>
      <div class="rapor-arama">
        <AutoComplete
          v-model="raporArama"
          :suggestions="raporOnerileri"
          option-label="ad"
          :placeholder="t('raporlar.raporAra')"
          :dropdown="true"
          :force-selection="true"
          class="rapor-arama-girdi"
          @complete="raporAra"
          @item-select="raporSec"
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
              :options="cariOnerileri"
              option-label="ad"
              option-value="id"
              :placeholder="t('faturalar.seciniz')"
              class="w-full"
              filter
              filter-by="ad,vergiNumarasi,telefon"
              :loading="cariOnerileriYukleniyor"
              @filter="cariAra"
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
            <p
              v-if="ekstreData.cariVergiNo"
              class="ekstre-meta"
            >
              {{ t('raporlar.vergiNo') }}: {{ ekstreData.cariVergiNo }}
              <span v-if="ekstreData.cariTelefon"> · {{ ekstreData.cariTelefon }}</span>
              <span v-if="ekstreData.cariEmail"> · {{ ekstreData.cariEmail }}</span>
            </p>
            <p
              v-if="ekstreData.cariAdres"
              class="ekstre-meta"
            >
              {{ ekstreData.cariAdres }}
            </p>
            <div class="ekstre-ozet">
              <span>{{ t('raporlar.donemBasBakiye') }}:
                <strong :class="ekstreData.donemBasBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ekstreData.donemBasBakiye) }}</strong>
              </span>
              <span>{{ t('raporlar.toplamBorc') }}:
                <strong class="negative">{{ formatCurrency(ekstreData.toplamBorc) }}</strong>
              </span>
              <span>{{ t('raporlar.toplamAlacak') }}:
                <strong class="positive">{{ formatCurrency(ekstreData.toplamAlacak) }}</strong>
              </span>
              <span>{{ t('raporlar.donemSonBakiye') }}:
                <strong :class="ekstreData.donemSonBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ekstreData.donemSonBakiye) }}</strong>
              </span>
            </div>
            <div class="rapor-aksiyonlar">
              <Button
                icon="pi pi-file-pdf"
                :label="t('raporlar.pdf')"
                class="p-button-sm p-button-outlined"
                @click="ekstrePdfIndir"
              />
              <Button
                icon="pi pi-file-excel"
                :label="t('raporlar.excelIndir')"
                class="p-button-sm p-button-outlined"
                @click="ekstreCsvIndir"
              />
              <Button
                icon="pi pi-envelope"
                :label="t('raporlar.epostaGonder')"
                class="p-button-sm p-button-outlined"
                @click="epostaGonder(ekstreData.cariAd, t('raporlar.cariEkstreRaporu'), ekstrePdfIstek)"
              />
            </div>
          </div>
          <DataTable
            :value="ekstreSatirlari"
            striped-rows
            :rows="15"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <template #empty>
              <EmptyState />
            </template>
            <Column
              field="tarih"
              :header="t('common.date')"
              style="width: 110px"
            >
              <template #body="s">
                {{ s.data.tarih ? formatDate(s.data.tarih) : '-' }}
              </template>
            </Column>
            <Column
              field="tur"
              :header="t('raporlar.tur')"
              style="width: 150px"
            >
              <template #body="s">
                <span :class="['badge', ekstreTurSinif(s.data.tur)]">
                  {{ ekstreTurLabel(s.data.tur) }}
                </span>
              </template>
            </Column>
            <Column
              field="aciklama"
              :header="t('common.description')"
            />
            <!-- Fatura no ve vade, ekstredeki satiri kaynagina baglar. DTO'da
                 mevcuttu ama gosterilmiyordu; vade kolonu olmadan tahsilat
                 planlamasi yapilamiyor. -->
            <Column
              field="faturaNumarasi"
              :header="t('raporlar.faturaNo')"
              style="width: 130px"
            >
              <template #body="s">
                <span v-if="s.data.faturaNumarasi">{{ s.data.faturaNumarasi }}</span>
                <span v-else>-</span>
              </template>
            </Column>
            <Column
              field="vadeTarihi"
              :header="t('raporlar.vadeTarihi')"
              style="width: 110px"
            >
              <template #body="s">
                <span v-if="s.data.vadeTarihi">{{ formatDate(s.data.vadeTarihi) }}</span>
                <span v-else>-</span>
              </template>
            </Column>
            <Column
              field="borc"
              :header="t('raporlar.borc')"
              style="width: 120px"
            >
              <template #body="s">
                <span
                  v-if="s.data.borc"
                  class="negative"
                >{{ formatCurrency(s.data.borc) }}</span>
              </template>
            </Column>
            <Column
              field="alacak"
              :header="t('raporlar.alacak')"
              style="width: 120px"
            >
              <template #body="s">
                <span
                  v-if="s.data.alacak"
                  class="positive"
                >{{ formatCurrency(s.data.alacak) }}</span>
              </template>
            </Column>
            <Column
              field="yuruyenBakiye"
              :header="t('raporlar.bakiye')"
              style="width: 140px"
            >
              <template #body="s">
                <strong :class="(s.data.yuruyenBakiye || 0) >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.yuruyenBakiye) }}</strong>
              </template>
            </Column>
          </DataTable>
          <Message
            v-if="!ekstreData.hareketler || ekstreData.hareketler.length === 0"
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
                icon="pi pi-file-pdf"
                :label="t('raporlar.pdf')"
                class="p-button-sm p-button-outlined"
                @click="ggPdfIndir"
              />
              <Button
                icon="pi pi-envelope"
                :label="t('raporlar.epostaGonder')"
                class="p-button-sm p-button-outlined"
                @click="epostaGonder(t('raporlar.gelirGiderOzeti'), t('raporlar.gelirGiderRaporu'), ggPdfIstek)"
              />
            </div>
          </div>

          <div class="rapor-grafik">
            <Bar
              :data="ggChartData"
              :options="barOpt"
            />
          </div>

          <h3 style="margin-top: 25px">
            {{ $t('raporlar.aylikDagilim') }}
          </h3>
          <DataTable
            :value="ggData.aylikDagilim"
            striped-rows
          >
            <template #empty>
              <EmptyState />
            </template>
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
          <div class="rapor-grafik rapor-grafik-daire">
            <Doughnut
              :data="kdvChartData"
              :options="doughnutOpt"
            />
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
          <div class="form-group">
            <label>{{ t('raporlar.referansTarih') }}</label>
            <DatePicker
              v-model="yasReferansTarih"
              date-format="dd.mm.yy"
              show-icon
              :max-date="new Date()"
            />
          </div>
          <Button
            :label="t('raporlar.raporGetir')"
            icon="pi pi-search"
            :loading="yasLoading"
            @click="getYaslandirma"
          />
          <Button
            :label="t('raporlar.pdfIndir')"
            icon="pi pi-file-pdf"
            severity="secondary"
            outlined
            :disabled="!yasData || yasData.satirlar.length === 0"
            @click="yaslandirmaPdfIndir"
          />
        </div>
        <div
          v-if="yasData"
          class="rapor-sonuc"
        >
          <!-- Kova toplamlari: raporun asil okunmasi gereken kisim. Satir toplamlari
               tek tek cari bazlidir; burada tum carilerin toplami gosterilir ki
               "toplam alacak ne kadar, ne kadari gecmis" sorusu cevaplanabilsin. -->
          <div class="yas-kova-ozet">
            <div
              v-for="k in yasKovalar"
              :key="k"
              class="yas-kova-kart"
            >
              <span class="yas-kova-ad">{{ $t(`raporlar.kova_${k.toLowerCase()}`) }}</span>
              <span class="yas-kova-tutar">{{ formatCurrency(yasData.ozet?.kovalar?.[k] ?? 0) }}</span>
              <span
                v-if="yasOzetPay(k)"
                class="yas-kova-pay"
              >{{ yasOzetPay(k) }}</span>
            </div>
            <div class="yas-kova-kart toplam">
              <span class="yas-kova-ad">{{ t('raporlar.toplamAlacak') }}</span>
              <span class="yas-kova-tutar">{{ formatCurrency(yasData.ozet?.toplam ?? 0) }}</span>
              <span class="yas-kova-pay">{{ t('raporlar.cariSayisi', { n: yasData.ozet?.cariSayisi ?? 0 }) }}</span>
            </div>
          </div>
          <Message
            v-if="yasData.ozet && yasData.ozet.gecikmisTutar > 0"
            severity="warn"
            :text="$t('raporlar.gecikmisTutarUyarisi', { tutar: formatCurrency(yasData.ozet.gecikmisTutar) })"
          />
          <DataTable
            :value="yasData.satirlar"
            :sort-field="enFazlaGecikmeGun"
            :sort-order="-1"
            striped-rows
            :rows="10"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <template #empty>
              <EmptyState />
            </template>
            <Column
              field="cariAd"
              :header="$t('raporlar.cariHesap')"
            />
            <Column
              v-for="k in yasKovalar"
              :key="k"
              :header="$t(`raporlar.kova_${k.toLowerCase()}`)"
              style="width: 120px"
            >
              <template #body="s">
                <span :class="kovaDegerSinif(k, s.data)">{{ formatCurrency(s.data.kovalar?.[k] ?? 0) }}</span>
              </template>
            </Column>
            <Column
              field="toplam"
              :header="$t('raporlar.toplam')"
              style="width: 130px"
            >
              <template #body="s">
                <span class="positive">{{ formatCurrency(s.data.toplam) }}</span>
              </template>
            </Column>
            <Column
              field="enFazlaGecikmeGun"
              :header="$t('raporlar.enFazlaGecikme')"
              style="width: 110px"
            >
              <template #body="s">
                <span :class="['vade-badge', vadeRiskSinifi(s.data.enFazlaGecikmeGun)]">
                  {{ s.data.enFazlaGecikmeGun }} {{ $t('raporlar.gun') }}
                </span>
              </template>
            </Column>
            <Column
              field="ortalamaGecikmeGun"
              :header="$t('raporlar.ortalamaGecikme')"
              style="width: 110px"
            />
          </DataTable>
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
                icon="pi pi-file-pdf"
                :label="t('raporlar.pdf')"
                class="p-button-sm p-button-outlined"
                @click="ckPdfIndir"
              />
            </div>
          </div>

          <div class="rapor-grafik">
            <Bar
              :data="ckChartData"
              :options="barOpt"
            />
          </div>

          <DataTable
            :value="ckData.satirlar"
            striped-rows
            :rows="10"
            :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          >
            <template #empty>
              <EmptyState />
            </template>
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
        <RaporTedarikciUrunler v-if="aktifSekme === 5" />
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
        <RaporUrunKarlilik v-if="aktifSekme === 6" />
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
        <RaporNakitAkisi v-if="aktifSekme === 7" />
      </TabPanel>

      <TabPanel :header="$t('raporlar.pivotTablo')">
        <PivotRaporTab
          :format-currency="formatCurrency"
          :format-date-for-api="formatDateForApi"
        />
      </TabPanel>

      <!-- Temsilci sekmesi bilincli olarak SONDUR: favori sekme indeksleri
           localStorage'da sayisal index ile saklaniyor (raporFavoriDegistir),
           araya eklemek kayitli favorileri kaydirirdi. -->
      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i
              class="pi pi-star"
              :class="{ favori: raporFavori('temsilci') }"
              @click.stop="raporFavoriDegistir('temsilci', 9, t('raporlar.temsilciPerformans'))"
            />
            {{ t('raporlar.temsilciPerformans') }}
          </div>
        </template>
        <RaporTemsilci
          v-if="aktifSekme === 9"
          :tarih-araligi="tarihAraligi || []"
        />
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="epostaDialog"
      :header="t('raporlar.epostaGonder')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="form-group">
        <label>{{ t('raporlar.aliciEposta') }}</label>
        <InputText
          v-model="epostaAlici"
          type="email"
          class="w-full"
          :placeholder="t('raporlar.epostaYerTutucu')"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="epostaDialog = false"
        />
        <Button
          :label="t('raporlar.epostaGonder')"
          icon="pi pi-send"
          :loading="epostaGonderiliyor"
          @click="epostaGonderOnayla"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useCariOnerileri } from '../composables/useCariOnerileri.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { raporAPI } from '../api/index.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import PivotRaporTab from '../components/PivotRaporTab.vue'
import RaporTedarikciUrunler from '../components/RaporTedarikciUrunler.vue'
import RaporUrunKarlilik from '../components/RaporUrunKarlilik.vue'
import RaporNakitAkisi from '../components/RaporNakitAkisi.vue'
import RaporTemsilci from '../components/RaporTemsilci.vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Bar, Doughnut } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement } from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement)

const barOpt = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { y: { ticks: { callback: (v) => formatCurrency(v) } } }
}
const doughnutOpt = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom' } }
}

const dovizStore = useDovizStore()
import { safeGet, safeSet } from '../utils/safeStorage.js'
const toast = useToast()
const toastBildirim = useToastBildirim()
const { t } = useI18n()

const router = useRouter()

const raporArama = ref(null)
const raporOnerileri = ref([])
const RAPOR_SEKMELERI = computed(() => [
  { key: 'cariEkstre', index: 0, ad: t('raporlar.cariEkstre') },
  { key: 'gelirGider', index: 1, ad: t('raporlar.gelirGiderOzeti') },
  { key: 'kdv', index: 2, ad: t('raporlar.kdvRaporu') },
  { key: 'yaslandirma', index: 3, ad: t('raporlar.yaslandirma') },
  { key: 'cariKarlilik', index: 4, ad: t('raporlar.cariKarlilik') },
  { key: 'tedarikciUrunler', index: 5, ad: t('raporlar.tedarikciUrunleri') },
  { key: 'urunKarlilik', index: 6, ad: t('raporlar.urunKarliligi') },
  { key: 'nakitAkisi', index: 7, ad: t('raporlar.nakitAkisi') },
  { key: 'pivot', index: 8, ad: t('raporlar.pivotTablo') },
  { key: 'temsilci', index: 9, ad: t('raporlar.temsilciPerformans') },
  { key: 'karlilikAnalizi', ad: t('nav.karlilikAnalizi'), path: '/raporlar/karlilik-analizi' },
  { key: 'faturaGecmis', ad: t('nav.faturaGecmisRaporu'), path: '/raporlar/fatura-gecmis' }
])

const raporAra = (e) => {
  const q = (e?.query || '').toLowerCase()
  raporOnerileri.value = RAPOR_SEKMELERI.value.filter((r) => !q || r.ad.toLowerCase().includes(q))
}

const raporSec = (e) => {
  const secili = e?.value
  if (!secili) return
  if (secili.path) {
    router.push(secili.path)
  } else {
    aktifSekme.value = secili.index
  }
  raporArama.value = null
}

const FAVORI_ANAHTAR = 'raspel_favori_raporlar'
const aktifSekme = ref(0)
const favoriRaporlar = ref(safeGet(FAVORI_ANAHTAR, []))
const tarihAraligi = ref(null)


const raporFavori = (key) => favoriRaporlar.value.some((r) => r.key === key)

// Favoriler ARTIK key tabanlı (sekme eklenince/sırası değişince kırılmaz).
// `index` parametresi geriye uyum için alınır ama kaydedilmez.
const raporFavoriDegistir = (key, _index, ad) => {
  const mevcut = favoriRaporlar.value.findIndex((r) => r.key === key)
  if (mevcut > -1) {
    favoriRaporlar.value.splice(mevcut, 1)
  } else {
    favoriRaporlar.value.push({ key, ad, tarih: tarihAraligi.value ? [...tarihAraligi.value] : null })
  }
  safeSet(FAVORI_ANAHTAR, favoriRaporlar.value)
}

const favoriAc = (r) => {
  const hedef = RAPOR_SEKMELERI.value.find((s) => s.key === r.key)
  if (hedef?.path) {
    router.push(hedef.path)
    return
  }
  // key -> güncel sekme index'i; eski (index'li) kayıtlar için fallback.
  if (hedef && hedef.index != null) aktifSekme.value = hedef.index
  else if (r.index != null) aktifSekme.value = r.index
  if (r.tarih) tarihAraligi.value = [...r.tarih]
}

const ekstreKart = ref(null)
const ggKart = ref(null)

const epostaDialog = ref(false)
const epostaAlici = ref('')
const epostaGonderiliyor = ref(false)
const epostaBekleyen = ref(null)

const epostaGonder = (baslik, raporAdi, pdfIstek) => {
  if (!pdfIstek) {
    toast.add({ severity: 'info', summary: t('raporlar.paylasim'), detail: t('raporlar.epostaPaylasilacak', { rapor: raporAdi }), life: 4000 })
    return
  }
  epostaBekleyen.value = { baslik, raporAdi, pdfIstek }
  epostaAlici.value = ''
  epostaDialog.value = true
}

const epostaGonderOnayla = async () => {
  const alici = (epostaAlici.value || '').trim()
  if (!alici || !alici.includes('@')) {
    toastBildirim.hata(t('raporlar.gecersizEposta'))
    return
  }
  const bekleyen = epostaBekleyen.value
  if (!bekleyen?.pdfIstek) return
  epostaGonderiliyor.value = true
  try {
    const res = await bekleyen.pdfIstek()
    await raporAPI.epostaGonderPdf(res.data, alici, bekleyen.raporAdi || bekleyen.baslik, `${bekleyen.raporAdi || 'rapor'}.pdf`)
    toastBildirim.basarili(t('raporlar.epostaGonderildi'))
    epostaDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('raporlar.epostaHata'))
  } finally {
    epostaGonderiliyor.value = false
  }
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

// Yaşlandırma referans tarihi (gecikme günleri buna göre hesaplanır).
const yasReferansTarih = ref(new Date())
const yasData = ref(null)
const yasLoading = ref(false)

// Kova anahtarları backend ile aynı sırada ve aynı adlarla. Backend kova
// etiketlerini Türkçe sabit metin olarak döndürüyordu; EN kullanıcısı "0-30 Gün"
// görüyordu. Artık anahtar gönderiliyor, etiket burada çevriliyor.
const YAS_KOVALARI = ['VADEDI_GELMEMIS', 'GUN_0_30', 'GUN_31_60', 'GUN_61_90', 'GUN_90_PLUS']
// Sunucu kova sırasını da gönderiyor; bilinmeyen bir kova eklendiyse rapor boş
// görünmesin diye varsayılan sıraya düşülür.
const yasKovalar = computed(() => {
  const sirali = yasData.value?.ozet?.kovaSirasi
  return Array.isArray(sirali) && sirali.length ? sirali : YAS_KOVALARI
})

/** Kovanın toplam içindeki payı; küçük kovalarda sütunu doldurmaz. */
const yasOzetPay = (kova) => {
  const toplam = Number(yasData.value?.ozet?.toplam ?? 0)
  if (!toplam) return ''
  const pay = (Number(yasData.value?.ozet?.kovalar?.[kova] ?? 0) / toplam) * 100
  return pay < 0.1 ? '' : `%${pay.toFixed(1)}`
}

/** Kova hücresi: boşsa sessiz, gecikmişse risk rengi. */
const kovaDegerSinif = (kova, satir) => {
  const tutar = Number(satir?.kovalar?.[kova] ?? 0)
  if (!tutar) return 'kova-bos'
  if (kova === 'VADEDI_GELMEMIS') return ''
  if (kova === 'GUN_90_PLUS') return 'risk-yuksek'
  if (kova === 'GUN_61_90') return 'risk-orta'
  return 'risk-az'
}

// Temsilci performansı (sekme 9) `RaporTemsilci.vue` bileşenine taşındı.

const ckBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ckBit = ref(new Date())
const ckData = ref(null)
const ckLoading = ref(false)
const ckKart = ref(null)

// Tedarikçi (5), ürün kârlılığı (6) ve nakit akışı (7) sekmeleri ayrı
// bileşenlere (RaporTedarikciUrunler / RaporUrunKarlilik / RaporNakitAkisi)
// taşındı; her biri kendi verisini kendi yükler.

const ggChartData = computed(() => {
  const liste = ggData.value?.aylikDagilim || []
  return {
    labels: liste.map((x) => x.ay),
    datasets: [{
      label: t('raporlar.netTutar'),
      data: liste.map((x) => Number(x.net) || 0),
      backgroundColor: liste.map((x) => ((Number(x.net) || 0) < 0 ? '#ef4444' : '#10b981'))
    }]
  }
})

const kdvChartData = computed(() => {
  const d = kdvData.value
  return {
    labels: [t('raporlar.cikisKdvSatis'), t('raporlar.girisKdvAlis')],
    datasets: [{
      data: [Number(d?.toplamKdvCikis) || 0, Number(d?.toplamKdvGiris) || 0],
      backgroundColor: ['#3b82f6', '#f59e0b']
    }]
  }
})

const ckChartData = computed(() => {
  const liste = (ckData.value?.satirlar || []).slice(0, 10)
  return {
    labels: liste.map((x) => x.cariAd),
    datasets: [{
      label: t('raporlar.kar'),
      data: liste.map((x) => Number(x.kar) || 0),
      backgroundColor: liste.map((x) => ((Number(x.kar) || 0) < 0 ? '#ef4444' : '#10b981'))
    }]
  }
})

// ÜST tarih aralığını sekmenin kendi filtresine uygular (tek yetkili kaynak).
// 0 cari ekstre, 1 gelir/gider, 2 kdv, 4 cari kârlılık çift tarih;
// 3 yaşlandırma tek referans tarihi kullanır (bitiş tarihi).
// 9 temsilci artık kendi bileşeninde `tarihAraligi` prop'u ile uygular.
const tarihAraliginiUygula = (idx) => {
  const v = tarihAraligi.value
  if (!v || v.length !== 2 || !v[0]) return
  const [bas, bit] = v
  if (idx === 0) { ekstreBas.value = bas; ekstreBit.value = bit }
  else if (idx === 1) { ggBas.value = bas; ggBit.value = bit }
  else if (idx === 2) { kdvBas.value = bas; kdvBit.value = bit }
  else if (idx === 3) { yasReferansTarih.value = bit }
  else if (idx === 4) { ckBas.value = bas; ckBit.value = bit }
}

watch(tarihAraligi, () => {
  // Tarih değişince aktif sekmenin filtresini güncelle ve yeniden yükle.
  tarihAraliginiUygula(aktifSekme.value)
  sekmeYukle(aktifSekme.value)
})

const formatDateForApi = (d) => {
  if (!d) return ''
  return getLocalDateString(d)
}

// Cari ekstre filtresi sunucu aramali. Once `getAllCariHesaplar()` ile ilk 50
// kayit cekiliyordu; 50. kayittan sonraki bir cari icin ekstre raporu hic
// uretilemiyordu (secim zorunlu, dropdown filtresizdi).
const { oneriler: cariOnerileri, ara: cariAra, hemenAra: cariOnerileriYukle, yukleniyor: cariOnerileriYukleniyor } =
  useCariOnerileri()

onMounted(async () => {
  try {
    await cariOnerileriYukle()
  } catch {
    /* cari listesi yuklenemezse rapor sekmeleri yine de acilir */
  }
  sekmeYukle(aktifSekme.value)
})

// Raporları yalnızca ilgili sekme açıldığında yükle (ilk açılışta donmayı önler).
// 5/6/7/9 sekmeleri kendi bileşenlerinde (v-if ile) açılışta yükler.
const sekmeYukle = (idx) => {
  // Sekmeye girerken üst tarih aralığı seçiliyse onu uygula (tek yetkili kaynak).
  tarihAraliginiUygula(idx)
  if (idx === 1) getGelirGider()
  else if (idx === 2) getKdv()
  else if (idx === 3) getYaslandirma()
  else if (idx === 4) getCariKarlilik()
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
    const params = {}
    if (yasReferansTarih.value) params.referansTarih = formatDateForApi(yasReferansTarih.value)
    const r = await raporAPI.yaslandirma(params)
    yasData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.yaslandirmaHata'))
    yasData.value = null
  } finally {
    yasLoading.value = false
  }
}

// Dışa aktarılan PDF/Excel, ekrandaki para birimiyle aynı olmalı: seçili birim
// TRY değilse `doviz` parametresi backend'e gönderilir ve tutarlar çevrilir.
const dovizParam = () => {
  const k = dovizStore.aktifParaBirimi
  return k && k !== 'TRY' ? { doviz: k } : {}
}

const yaslandirmaPdfIndir = () => {
  const params = { ...dovizParam() }
  if (yasReferansTarih.value) params.referansTarih = formatDateForApi(yasReferansTarih.value)
  pdfIndir(raporAPI.yaslandirmaPdf(params), `yaslandirma-${formatDateForApi(yasReferansTarih.value)}.pdf`)
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

const pdfIndir = async (istek, dosyaAdi) => {
  try {
    const res = await istek
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const link = document.createElement('a')
    link.href = url
    link.download = dosyaAdi
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => window.URL.revokeObjectURL(url), 60000)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('raporlar.pdfIndirilemedi'))
  }
}

const ekstrePdfIstek = () =>
  raporAPI.cariEkstrePdf({
    cariHesapId: ekstreCariId.value,
    baslangic: formatDateForApi(ekstreBas.value),
    bitis: formatDateForApi(ekstreBit.value),
    ...dovizParam()
  })

const ggPdfIstek = () =>
  raporAPI.gelirGiderPdf({
    baslangic: formatDateForApi(ggBas.value),
    bitis: formatDateForApi(ggBit.value),
    ...dovizParam()
  })

const ekstrePdfIndir = () => pdfIndir(ekstrePdfIstek(), 'cari-ekstre.pdf')

const ggPdfIndir = () => pdfIndir(ggPdfIstek(), 'gelir-gider.pdf')

const ckPdfIndir = () =>
  pdfIndir(
    raporAPI.cariKarlilikPdf({
      baslangic: formatDateForApi(ckBas.value),
      bitis: formatDateForApi(ckBit.value),
      ...dovizParam()
    }),
    'cari-karlilik.pdf'
  )

// Vade riski renklendirmesi makine-okunur gun alanindan hesaplanir. Yaşlandirma
// sekmesi artık kova anahtarlariyla çalisiyor (`GUN_0_30` ... `GUN_90_PLUS`),
// hücre rengi kova üzerinden seçiliyor; `vadeRiskSinifi` yalnızca maksimum
// gecikme günü rozeti içinde kullanılır.

const formatCurrency = (v) => {
  const deger = v ?? 0
  const birim = dovizStore.aktifParaBirimi
  if (birim && birim !== 'TRY') {
    const cevrilen = dovizStore.convert(deger, 'TRY', birim)
    return dovizStore.formatPara(cevrilen, birim)
  }
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(deger)
}

const ekstreTurLabel = (tur) =>
  ({
    TAHSILAT: t('raporlar.tahsilat'),
    ODEME: t('raporlar.odeme'),
    SATIS_FATURA: t('raporlar.satisFatura'),
    ALIS_FATURA: t('raporlar.alimFatura'),
    BORC: t('raporlar.borclandirma'),
    DEVIR: t('raporlar.devir'),
    KAPANIS: t('raporlar.kapanis')
  })[tur] || tur

const ekstreTurSinif = (tur) =>
  ({ TAHSILAT: 'tahsilat', ALIS_FATURA: 'tahsilat', ODEME: 'odeme', SATIS_FATURA: 'odeme', BORC: 'odeme' })[tur] || 'odeme'

// Tabloya devir (ilk) ve kapanis (son) satirlari eklenir.
const ekstreSatirlari = computed(() => {
  const d = ekstreData.value
  if (!d) return []
  // Hareket yoksa devir/kapanis satirlari eklenmez: aksi halde tablo hic bos
  // gorunmedigi icin "hareket yok" mesaji asla tetiklenmiyordu.
  if (!d.hareketler || d.hareketler.length === 0) return []
  const rows = [{ tur: 'DEVIR', tarih: null, aciklama: t('raporlar.devir'), borc: null, alacak: null, yuruyenBakiye: d.donemBasBakiye }]
  for (const h of d.hareketler) rows.push(h)
  rows.push({ tur: 'KAPANIS', tarih: null, aciklama: t('raporlar.kapanis'), borc: d.toplamBorc, alacak: d.toplamAlacak, yuruyenBakiye: d.donemSonBakiye })
  return rows
})

const ekstreCsvIndir = () => {
  const d = ekstreData.value
  if (!d) return
  // Tablo ile CSV ayni sutunlari icermeli; aksi halde indirilen dosya ekrandaki
  // rapordan farkli olur (fatura no ve vade tabloya eklendi).
  const basliklar = ['Tarih', 'Tur', 'Aciklama', 'Fatura No', 'Vade', 'Borc', 'Alacak', 'Bakiye']
  // CSV de ekrandaki para birimiyle aynı olsun.
  const csvDoviz = (v) => {
    if (v == null || v === '') return ''
    const birim = dovizStore.aktifParaBirimi
    if (birim === 'TRY') return v
    return dovizStore.convert(Number(v), 'TRY', birim).toFixed(2)
  }
  const satirlar = ekstreSatirlari.value.map((s) => [
    s.tarih ? formatDate(s.tarih) : '',
    ekstreTurLabel(s.tur),
    (s.aciklama || '').replace(/;/g, ','),
    s.faturaNumarasi || '',
    s.vadeTarihi ? formatDate(s.vadeTarihi) : '',
    csvDoviz(s.borc),
    csvDoviz(s.alacak),
    csvDoviz(s.yuruyenBakiye)
  ])
  const csv = [basliklar, ...satirlar].map((r) => r.join(';')).join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `cari-ekstre-${d.cariAd || ''}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

import { formatTarih as formatDate, getLocalDateString, vadeRiskSinifi } from '../utils/format.js'
</script>

<style scoped>
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
.doviz-notu {
  font-size: 10.5px;
  color: var(--text-muted);
  max-width: 150px;
  line-height: 1.25;
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
  color: var(--text-secondary);
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
  background: var(--bg-secondary);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 15px;
}
.rapor-bilgi h3 {
  margin: 0 0 10px 0;
  color: var(--accent);
}
.rapor-bilgi p {
  margin: 5px 0;
}
.ekstre-meta {
  color: var(--text-secondary);
  font-size: 13px;
}
.ekstre-ozet {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 22px;
  margin: 8px 0 10px;
  font-size: 13px;
  color: var(--text-secondary);
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
  color: var(--text-muted);
  margin-bottom: 8px;
}
.ozet-kart strong {
  font-size: 22px;
}
.ozet-kart.gelir strong {
  color: var(--success);
}
.ozet-kart.gider strong {
  color: var(--danger);
}
.ozet-kart.kar strong {
  color: var(--success);
}
.ozet-kart.zarar strong {
  color: var(--danger);
}
.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  line-height: 1.4;
}
.badge.tahsilat {
  background: var(--success-soft);
  color: var(--success);
}
.badge.odeme {
  background: var(--danger-soft);
  color: var(--danger);
}
.positive {
  color: var(--success);
  font-weight: bold;
}
.negative {
  color: var(--danger);
  font-weight: bold;
}
.vade-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.risk-yok {
  background: var(--success-soft);
  color: var(--success);
}
.risk-az {
  background: #fff3e0;
  color: #e65100;
}
.risk-orta {
  background: var(--danger-soft);
  color: var(--danger);
}
.risk-yuksek {
  background: #fce4ec;
  color: #880e4f;
}
/* Yaşlandırma kova özeti: her kova bir kart, altında payı. Kartlar yatayda
   kaydırılabilir; 5 kova dar ekranda sığmaz. */
.yas-kova-ozet {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.yas-kova-kart {
  flex: 1 1 150px;
  min-width: 140px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}
.yas-kova-kart.toplam {
  border-color: var(--accent);
  background: var(--accent-soft, rgba(59, 130, 246, 0.08));
}
.yas-kova-ad {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.yas-kova-tutar {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.yas-kova-pay {
  font-size: 11px;
  color: var(--text-muted, var(--text-secondary));
}
/* Kova hücresi risk renklerini kullanır; boş hücre gürültü yaratmasın diye
   saydam bırakılır (0,00 yazmak yerine). */
.kova-bos {
  color: var(--text-muted, var(--text-secondary));
  opacity: 0.45;
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
  color: var(--accent);
}
.rapor-grafik {
  position: relative;
  height: 260px;
  margin-top: 20px;
}
.rapor-grafik-daire {
  height: 240px;
  max-width: 360px;
}
.rapor-arama-girdi {
  width: 220px;
}
.rapor-arama-girdi :deep(input) {
  width: 100%;
}
</style>
