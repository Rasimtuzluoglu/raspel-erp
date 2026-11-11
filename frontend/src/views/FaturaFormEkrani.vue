<template>
  <div class="fatura-form-sayfa">
    <PageHeader
      :title="sayfaBasligi"
      icon="pi pi-file-invoice"
    >
      <template #actions>
        <Button
          v-if="!duzenlemeMi"
          :label="t('faturalar.kaynaktanDoldur')"
          icon="pi pi-download"
          class="p-button-outlined"
          data-test="fatura-kaynaktan-doldur"
          @click="kaynakDialogAc"
        />
        <Button
          :label="t('faturalar.listeyeDon')"
          icon="pi pi-arrow-left"
          class="p-button-text"
          @click="listeyeDon"
        />
        <Tag
          v-if="mevcutFatura"
          :value="durumEtiket(mevcutFatura.durum)"
          :severity="durumSeverity(mevcutFatura.durum)"
        />
      </template>
    </PageHeader>

    <div
      v-if="yukleniyor"
      class="form-yukleniyor"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="4"
        height="52px"
      />
    </div>

    <Message
      v-else-if="yuklemeHatasi"
      severity="error"
      :closable="false"
      :text="yuklemeHatasi"
    />

    <div
      v-else
      class="fatura-form-grid"
    >
      <!-- SOL: cari + temel bilgiler -->
      <section class="form-panel form-panel-sol">
        <h2 class="form-panel-baslik">
          <i class="pi pi-info-circle" />
          {{ t('faturalar.bolumFaturaBilgileri') }}
        </h2>

        <div class="form-alan">
          <label class="form-etiket">
            {{ form.tur === 'ALIS' ? t('faturalar.tedarikci') : t('faturalar.musteri') }}
            <span class="zorunlu">*</span>
          </label>
          <AutoComplete
            v-model="seciliCariNesnesi"
            :suggestions="cariOnerileri"
            option-label="ad"
            :force-selection="false"
            :loading="cariOnerileriYukleniyor"
            :placeholder="form.tur === 'ALIS' ? t('faturalar.tedarikciAra') : t('faturalar.cariAra')"
            :disabled="!duzenlenebilir"
            dropdown
            class="w-full"
            data-test="fatura-cari"
            @complete="cariAra"
            @option-select="cariSecildi"
          >
            <template #option="slotProps">
              <div class="cari-opsiyon">
                <span>{{ slotProps.option.ad }}</span>
                <span class="cari-opsiyon-detay">{{ slotProps.option.vergiNumarasi || slotProps.option.telefon || '' }}</span>
              </div>
            </template>
          </AutoComplete>
          <Button
            v-if="form.cariHesapId && duzenlenebilir"
            :label="t('faturalar.sonFaturaKopyala')"
            icon="pi pi-copy"
            class="p-button-text p-button-sm son-fatura-kopyala"
            @click="sonFaturayiKopyala"
          />
        </div>

        <div class="form-alan">
          <label class="form-etiket">
            {{ t('faturalar.faturaTuru') }}
            <span class="zorunlu">*</span>
          </label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekleri"
            option-label="label"
            option-value="value"
            :disabled="!duzenlenebilir"
            :placeholder="t('faturalar.faturaTuruSeciniz')"
            class="w-full"
            data-test="fatura-tur"
          />
        </div>

        <div class="form-alan">
          <label class="form-etiket">
            {{ t('common.date') }}
            <span class="zorunlu">*</span>
          </label>
          <DatePicker
            v-model="form.tarih"
            :disabled="!duzenlenebilir"
            date-format="dd.mm.yy"
            show-icon
            class="w-full"
            data-test="fatura-tarih"
          />
        </div>

        <div class="form-alan">
          <label class="form-etiket">{{ t('faturalar.paraBirimi') }}</label>
          <Dropdown
            v-model="form.paraBirimi"
            :options="['TRY', 'USD', 'EUR', 'GBP', 'SAR', 'GAU']"
            :disabled="!duzenlenebilir"
            class="w-full"
            placeholder="TRY (₺)"
            data-test="fatura-para-birimi"
          />
        </div>

        <div
          v-if="form.paraBirimi && form.paraBirimi !== 'TRY'"
          class="form-alan"
        >
          <label class="form-etiket">{{ t('faturalar.kurManuel') }}</label>
          <InputNumber
            v-model="form.kur"
            :min="0"
            :min-fraction-digits="2"
            :max-fraction-digits="6"
            :disabled="kurKilitli || !duzenlenebilir"
            class="w-full"
            data-test="fatura-kur"
          />
          <small class="form-ipucu">
            1 {{ form.paraBirimi }} =
            <span v-if="kurDegeri != null">{{ formatPara(kurDegeri, 'TRY') }}</span>
            <span v-else>{{ t('faturalar.kurYuklenemedi') }}</span>
            <template v-if="kurKilitli"> · {{ t('faturalar.kurDegistirilemez') }}</template>
          </small>
        </div>

        <div class="form-alan">
          <label class="form-etiket">{{ t('faturalar.depo') }}</label>
          <Dropdown
            v-model="form.depoId"
            :options="depolar"
            option-label="ad"
            option-value="id"
            :placeholder="t('faturalar.depoSecin')"
            :disabled="depoKilitli || !duzenlenebilir"
            show-clear
            class="w-full"
            data-test="fatura-depo"
          />
          <small
            v-if="depoAdi"
            class="form-ipucu"
          >{{ depoAdi }}</small>
          <small
            v-else
            class="form-ipucu"
          >{{ t('faturalar.varsayilanDepo') }}</small>
        </div>

        <div class="form-alan">
          <label class="form-etiket">{{ t('common.description') }}</label>
          <Textarea
            v-model="form.aciklama"
            :disabled="!duzenlenebilir"
            rows="3"
            auto-resize
            class="w-full"
            data-test="fatura-aciklama"
          />
        </div>

        <div class="form-ayrac" />
        <h3 class="form-alt-baslik">
          <i class="pi pi-truck" />
          {{ t('faturalar.teslimatBilgileri') }}
        </h3>

        <div class="form-alan">
          <label class="form-etiket">{{ t('faturalar.teslimEdenPersonel') }}</label>
          <Dropdown
            v-model="form.teslimEden"
            :options="personelSecenekleri"
            :placeholder="t('faturalar.teslimEdenPlaceholder')"
            :disabled="!duzenlenebilir"
            editable
            show-clear
            class="w-full"
            data-test="fatura-teslim-eden"
          />
        </div>

        <div class="form-alan">
          <label class="form-etiket">{{ t('faturalar.teslimDurumu') }}</label>
          <Dropdown
            v-model="form.teslimDurumu"
            :options="teslimDurumSecenekleri"
            option-label="label"
            option-value="value"
            :disabled="!duzenlenebilir"
            class="w-full"
          />
        </div>

        <div class="form-alan">
          <label class="form-etiket">{{ t('faturalar.teslimNotu') }}</label>
          <Textarea
            v-model="form.teslimNotu"
            :disabled="!duzenlenebilir"
            rows="2"
            auto-resize
            class="w-full"
          />
        </div>

        <template v-if="form.tur === 'SATIS'">
          <div
            v-if="teslimatVar"
            class="teslimat-mevcut"
            data-test="fatura-mevcut-teslimat"
          >
            <Tag
              :value="t('faturalar.mevcutTeslimat')"
              severity="info"
            />
            <span v-if="mevcutFatura?.driverAd">{{ mevcutFatura.driverAd }}</span>
            <Button
              :label="t('faturalar.teslimatAc')"
              icon="pi pi-external-link"
              class="p-button-text p-button-sm"
              @click="router.push({ name: 'Teslimatlar' })"
            />
          </div>
          <template v-else>
            <div class="form-alan">
              <label class="form-etiket">{{ t('faturalar.sofor') }}</label>
              <Dropdown
                v-model="form.driverId"
                :options="suruculer"
                option-label="ad"
                option-value="id"
                :placeholder="t('faturalar.soforSecin')"
                :disabled="!duzenlenebilir"
                show-clear
                class="w-full"
                data-test="fatura-sofor"
              />
            </div>

            <div class="form-alan">
              <label class="form-etiket">{{ t('faturalar.teslimatAdresi') }}</label>
              <Textarea
                v-model="form.teslimatAdresi"
                :disabled="!duzenlenebilir"
                rows="2"
                auto-resize
                class="w-full"
                data-test="fatura-teslimat-adresi"
              />
            </div>

            <div class="form-alan">
              <label class="form-etiket">{{ t('faturalar.beklenenTeslimTarihi') }}</label>
              <DatePicker
                v-model="form.beklenenTeslimTarihi"
                :disabled="!duzenlenebilir"
                date-format="dd.mm.yy"
                show-icon
                class="w-full"
              />
            </div>

            <div class="form-alan">
              <label class="form-etiket">{{ t('faturalar.teslimatNotu') }}</label>
              <Textarea
                v-model="form.teslimatNotu"
                :disabled="!duzenlenebilir"
                rows="2"
                auto-resize
                class="w-full"
              />
            </div>
          </template>
        </template>

        <Message
          v-if="!duzenlenebilir"
          severity="warn"
          :closable="false"
          :text="kilitNedeni"
          data-test="fatura-kilitli"
        />
      </section>

      <!-- ORTA: kalemler -->
      <section class="form-panel form-panel-orta">
        <h2 class="form-panel-baslik">
          <i class="pi pi-list" />
          {{ t('faturalar.faturaKalemleri') }}
        </h2>
        <FaturaKalemleri
          :kalemler="form.kalemler"
          :ara-toplam="araToplam"
          :kdv-toplam="kdvToplam"
          :genel-toplam="genelToplam"
          :kdv-secenekleri="[0, 1, 8, 10, 18, 20]"
          @add="kalemEkle"
          @remove="removeKalem"
          @geri-al="kalemGeriAl"
        />
      </section>

      <!-- SAG: toplamlar, odeme, aksiyonlar -->
      <aside class="form-panel form-panel-sag">
        <h2 class="form-panel-baslik">
          <i class="pi pi-calculator" />
          {{ t('faturalar.toplamlar') }}
        </h2>

        <div class="ozet-satir">
          <span>{{ t('faturaDetay.araToplam') }}</span>
          <strong>{{ formatPara(araToplam, form.paraBirimi) }}</strong>
        </div>
        <div class="ozet-satir">
          <span>{{ t('faturalar.kdvToplam') }}</span>
          <strong>{{ formatPara(kdvToplam, form.paraBirimi) }}</strong>
        </div>
        <div class="ozet-satir ozet-genel">
          <span>{{ t('faturaDetay.genelToplam') }}</span>
          <strong data-test="fatura-genel-toplam">{{ formatPara(genelToplam, form.paraBirimi) }}</strong>
        </div>

        <div class="odeme-kutu">
          <div class="ozet-satir">
            <span>{{ t('faturaDetay.odemeDurumu') }}</span>
            <span :class="['odeme-badge', (odemeDurumu || '').toLowerCase()]">{{ odemeEtiket(odemeDurumu) }}</span>
          </div>
          <div class="ozet-satir">
            <span>{{ t('faturalar.kalanTutar') }}</span>
            <strong>{{ formatPara(kalanTutar, form.paraBirimi) }}</strong>
          </div>
        </div>

        <Message
          v-if="hata"
          severity="error"
          :closable="false"
          :text="hata"
          class="form-hata"
        />

        <div class="form-aksiyonlar">
          <Button
            :label="t('common.vazgec')"
            icon="pi pi-times"
            class="p-button-text"
            @click="listeyeDon"
          />
          <Button
            :label="mevcutFatura && mevcutFatura.durum === 'KESILDI' ? t('faturalar.guncelle') : t('faturalar.kaydetTaslak')"
            icon="pi pi-save"
            class="w-full"
            :loading="saving"
            :disabled="!duzenlenebilir"
            data-test="fatura-kaydet"
            @click="taslakKaydet"
          />
          <Button
            v-if="kesilebilir"
            :label="t('faturalar.kaydetVeKes')"
            icon="pi pi-check-circle"
            severity="success"
            class="w-full"
            :loading="saving"
            :disabled="!duzenlenebilir"
            data-test="fatura-kaydet-kes"
            @click="kesOnayla"
          />
        </div>

        <div
          v-if="aktifFaturaId"
          class="form-aksiyonlar form-aksiyonlar-ek"
          data-test="fatura-aksiyonlar"
        >
          <h3 class="form-alt-baslik">
            <i class="pi pi-print" />
            {{ t('faturalar.aksiyonlar') }}
          </h3>
          <Button
            :label="t('faturalar.fisYazdir')"
            icon="pi pi-print"
            class="p-button-outlined w-full"
            :disabled="!aktifFaturaId"
            data-test="fatura-fis-yazdir"
            @click="fisYazdir"
          />
          <Button
            :label="t('faturalar.urunEtiketi')"
            icon="pi pi-barcode"
            class="p-button-outlined w-full"
            :loading="etiketYukleniyor"
            :disabled="!etiketlenebilirKalemler.length"
            :title="etiketlenebilirKalemler.length ? '' : t('faturalar.etiketYok')"
            data-test="fatura-urun-etiketi"
            @click="urunEtiketiYazdir"
          />

          <template v-if="mevcutFatura?.durum === 'KESILDI'">
            <Button
              :label="t('faturalar.pdfIndir')"
              icon="pi pi-file-pdf"
              class="p-button-outlined w-full"
              data-test="fatura-pdf"
              @click="pdfIndir"
            />
            <Button
              :label="t('faturalar.onizle')"
              icon="pi pi-eye"
              class="p-button-outlined w-full"
              data-test="fatura-onizle"
              @click="onizleFatura"
            />
            <Button
              :label="t('faturalar.epostaGonder')"
              icon="pi pi-envelope"
              class="p-button-outlined w-full"
              data-test="fatura-eposta"
              @click="epostaGonder"
            />

            <h3 class="form-alt-baslik efatura-baslik">
              <i class="pi pi-send" />
              {{ t('faturalar.efaturaBaslik') }}
            </h3>

            <div
              v-if="eFatura"
              class="efatura-bilgi"
              data-test="fatura-efatura-bilgi"
            >
              <div class="efatura-satir">
                <span>{{ t('faturalar.ettn') }}</span>
                <strong class="efatura-ettn">{{ eFatura.ettn || '-' }}</strong>
              </div>
              <div class="efatura-satir">
                <span>{{ t('faturalar.gibDurum') }}</span>
                <strong>{{ eFatura.gibDurumKodu || '-' }} {{ eFatura.gibDurumAciklama || '' }}</strong>
              </div>
              <div class="efatura-butonlar">
                <Button
                  v-if="Number(eFatura.gibDurumKodu || 0) < 1200"
                  :label="t('faturalar.efaturaGonder')"
                  icon="pi pi-send"
                  class="p-button-sm"
                  :loading="efaturaIslem"
                  data-test="fatura-gib-gonder"
                  @click="efaturaGonder"
                />
                <Button
                  :label="t('faturalar.efaturaDurum')"
                  icon="pi pi-refresh"
                  class="p-button-sm p-button-outlined"
                  :loading="efaturaIslem"
                  data-test="fatura-gib-durum"
                  @click="efaturaDurum"
                />
                <Button
                  :label="t('faturalar.efaturaXml')"
                  icon="pi pi-download"
                  class="p-button-sm p-button-outlined"
                  data-test="fatura-efatura-xml"
                  @click="efaturaXml"
                />
              </div>
            </div>
            <Button
              v-else
              :label="t('faturalar.efaturaOlustur')"
              icon="pi pi-send"
              class="p-button-outlined w-full"
              :loading="efaturaIslem"
              data-test="fatura-efatura-olustur"
              @click="efaturaOlustur"
            />
          </template>
          <small
            v-else-if="mevcutFatura"
            class="form-ipucu"
          >{{ t('faturalar.kesildiktenSonra') }}</small>
        </div>
      </aside>
    </div>

    <FaturaTasarimModal
      v-model:visible="tasarimAcik"
      :fatura-id="aktifFaturaId"
    />

    <AppDialog
      v-model:visible="kaynakDialog"
      :header="t('faturalar.kaynaktanDoldur')"
      width="560px"
    >
      <div class="form-alan">
        <label class="form-etiket">{{ t('faturalar.kaynakTuru') }}</label>
        <SelectButton
          v-model="kaynakTuru"
          :options="kaynakTurleri"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          @change="kaynakTurDegisti"
        />
      </div>
      <div class="form-alan">
        <label class="form-etiket">{{ t('faturalar.kaynakSec') }}</label>
        <Dropdown
          v-model="seciliKaynakId"
          :options="kaynakSecenekleri"
          option-label="etiket"
          option-value="id"
          :loading="kaynaklarYukleniyor"
          :placeholder="t('faturalar.kaynakSec')"
          class="w-full"
          data-test="fatura-kaynak-secim"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.vazgec')"
          class="p-button-text"
          @click="kaynakDialog = false"
        />
        <Button
          :label="t('faturalar.kaynakGetir')"
          icon="pi pi-check"
          :loading="kaynakDolduruluyor"
          :disabled="!seciliKaynakId"
          data-test="fatura-kaynak-getir"
          @click="kaynaktanDoldurTikla"
        />
      </template>
    </AppDialog>

    <!-- Mobil: alt sticky ozet + kaydet -->
    <div
      v-if="!yukleniyor && !yuklemeHatasi"
      class="fatura-form-mobil-bar"
    >
      <div class="mobil-ozet">
        <span>{{ t('faturaDetay.genelToplam') }}</span>
        <strong>{{ formatPara(genelToplam, form.paraBirimi) }}</strong>
      </div>
      <Button
        :label="t('faturalar.kaydetTaslak')"
        icon="pi pi-save"
        :loading="saving"
        :disabled="!duzenlenebilir"
        @click="taslakKaydet"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useDovizStore } from '../stores/dovizStore.js'
import { formatPara, formatDate } from '../utils/format.js'
import { stokAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useFaturaForm } from '../composables/useFaturaForm.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import FaturaKalemleri from '../components/FaturaKalemleri.vue'
import FaturaTasarimModal from '../components/FaturaTasarimModal.vue'
import AppDialog from '../components/AppDialog.vue'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const dovizStore = useDovizStore()
const toast = useToastBildirim()

const {
  form,
  seciliCariNesnesi,
  mevcutFatura,
  yukleniyor,
  yuklemeHatasi,
  saving,
  hata,
  kirli,
  kaydedildi,
  sonKaydedilenId,
  duzenlenebilir,
  kilitNedeni,
  sayfaBasligi,
  depolar,
  depoAdi,
  depoKilitli,
  kurKilitli,
  personelSecenekleri,
  suruculer,
  teslimatVar,
  kaynaklar,
  kaynaklarYukleniyor,
  kaynakYukle,
  kaynaktanDoldur,
  eFatura,
  efaturaIslem,
  pdfIndir,
  onizleFatura,
  epostaGonder,
  efaturaOlustur,
  efaturaGonder,
  efaturaDurum,
  efaturaXml,
  araToplam,
  kdvToplam,
  genelToplam,
  cariOnerileri,
  cariOnerileriYukleniyor,
  cariAra,
  cariSecildi,
  kalemEkle,
  removeKalem,
  kalemGeriAl,
  sonFaturayiKopyala,
  hazirla,
  taslakKaydet,
  kaydetVeKes
} = useFaturaForm()

const turSecenekleri = computed(() => [
  { label: t('faturalar.satis'), value: 'SATIS' },
  { label: t('faturalar.alis'), value: 'ALIS' }
])

const teslimDurumSecenekleri = computed(() => [
  { label: t('faturalar.durumBekliyor'), value: 'BEKLIYOR' },
  { label: t('faturalar.durumYolda'), value: 'YOLDA' },
  { label: t('faturalar.durumTeslimEdildi'), value: 'TESLIM_EDILDI' }
])

// C4: kaynaktan doldurma.
const kaynakDialog = ref(false)
const kaynakTuru = ref('SIPARIS')
const seciliKaynakId = ref(null)
const kaynakDolduruluyor = ref(false)

const kaynakTurleri = computed(() => [
  { label: t('faturalar.siparis'), value: 'SIPARIS' },
  { label: t('faturalar.irsaliye'), value: 'IRSALIYE' },
  { label: t('faturalar.teklif'), value: 'TEKLIF' }
])

const kaynakSecenekleri = computed(() =>
  (kaynaklar.value || []).map((k) => {
    const no = k.siparisNo || k.irsaliyeNo || k.teklifNo || `#${k.id}`
    const cari = k.cariHesapAdi || k.cariHesapAd || ''
    return { ...k, etiket: [no, cari, formatDate(k.tarih)].filter(Boolean).join(' · ') }
  })
)

const kaynakDialogAc = async () => {
  kaynakTuru.value = 'SIPARIS'
  seciliKaynakId.value = null
  kaynakDialog.value = true
  await kaynakYukle('SIPARIS')
}

const kaynakTurDegisti = async () => {
  seciliKaynakId.value = null
  await kaynakYukle(kaynakTuru.value)
}

const kaynaktanDoldurTikla = async () => {
  if (!seciliKaynakId.value) return
  kaynakDolduruluyor.value = true
  try {
    const oldu = await kaynaktanDoldur(kaynakTuru.value, seciliKaynakId.value)
    if (oldu) kaynakDialog.value = false
  } finally {
    kaynakDolduruluyor.value = false
  }
}

const kurDegeri = computed(() => {
  if (!form.value.paraBirimi || form.value.paraBirimi === 'TRY') return null
  const kur = dovizStore.getKur?.(form.value.paraBirimi) || {}
  return kur.satisFiyati || kur.satisKuru || null
})

const odemeDurumu = computed(() => mevcutFatura.value?.odemeDurumu || 'ODENMEDI')
const kalanTutar = computed(() => Number(mevcutFatura.value?.kalanTutar || 0))

// C3: kaydedilmis faturada etiket/yazdirma aksiyonlari.
const tasarimAcik = ref(false)
const etiketYukleniyor = ref(false)
const aktifFaturaId = computed(() => mevcutFatura.value?.id || sonKaydedilenId.value)

const etiketlenebilirKalemler = computed(() => {
  const harita = new Map()
  for (const k of form.value.kalemler || []) {
    if (k.stokId == null || !Number(k.adet)) continue
    harita.set(k.stokId, (harita.get(k.stokId) || 0) + Number(k.adet))
  }
  return [...harita.entries()].map(([stokId, adet]) => ({ stokId, adet }))
})

const blobIndir = (blob, ad) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = ad
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => window.URL.revokeObjectURL(url), 30000)
}

const fisYazdir = () => {
  if (aktifFaturaId.value) tasarimAcik.value = true
}

const urunEtiketiYazdir = async () => {
  if (!etiketlenebilirKalemler.value.length) return
  etiketYukleniyor.value = true
  try {
    const r = await stokAPI.topluEtiket({ tip: 'BARKOD', kalemler: etiketlenebilirKalemler.value })
    blobIndir(new Blob([r.data], { type: 'application/pdf' }), `urun-etiketleri-${aktifFaturaId.value}.pdf`)
    toast.basarili(t('faturalar.etiketIndirildi'))
  } catch {
    toast.hata(t('faturalar.etiketIndirilemedi'))
  } finally {
    etiketYukleniyor.value = false
  }
}

// KESILDI revizyonda yeniden "kes" gerekmez; yalnizca TASLAK ve yeni kayitta sunulur.
const kesilebilir = computed(() => {
  if (!duzenlenebilir.value) return false
  if (!mevcutFatura.value) return true
  return mevcutFatura.value.durum === 'TASLAK'
})

const durumEtiket = (durum) => {
  const map = {
    TASLAK: t('faturalar.durumTaslak'),
    TEKLIF: t('faturalar.durumTeklif'),
    KESILDI: t('faturalar.durumKesildi'),
    IPTAL: t('faturalar.durumIptal')
  }
  return map[durum] || durum || '-'
}

const durumSeverity = (durum) =>
  ({ TASLAK: 'warn', KESILDI: 'success', IPTAL: 'danger', TEKLIF: 'info' })[durum] || 'secondary'

const odemeEtiket = (durum) => {
  const map = {
    ODENDI: t('faturaDetay.odendi'),
    KISMI_ODENDI: t('faturaDetay.kismiOdedi'),
    ODENMEDI: t('faturaDetay.odenmedi')
  }
  return map[durum] || t('faturaDetay.odenmedi')
}

const listeyeDon = () => router.push({ name: 'Faturalar' })

const kesOnayla = () => {
  if (!confirm?.require) {
    kaydetVeKes()
    return
  }
  confirm.require({
    message: t('faturalar.kesOnayMesaj'),
    header: t('faturalar.kesOnayBaslik'),
    icon: 'pi pi-check-circle',
    acceptLabel: t('faturalar.kaydetVeKes'),
    rejectLabel: t('common.vazgec'),
    accept: () => kaydetVeKes()
  })
}

onBeforeRouteLeave(() => {
  if (kaydedildi.value || !kirli.value) return true
  return window.confirm(t('faturalar.kirliMesaj'))
})

useKisayollar({
  kaydet: () => taslakKaydet(),
  iptal: () => listeyeDon()
})

onMounted(hazirla)
</script>

<style scoped>
.fatura-form-sayfa {
  padding: 0;
  max-width: 100%;
}

.fatura-form-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) 340px;
  gap: 14px;
  align-items: start;
}

.form-panel {
  min-width: 0;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}

.form-panel-sag {
  position: sticky;
  top: 12px;
}

.form-panel-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--text-muted);
}

.form-panel-baslik i {
  color: var(--accent);
}

.form-alan {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.form-etiket {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
}

.zorunlu {
  color: var(--danger, #ef4444);
}

.son-fatura-kopyala {
  align-self: flex-start;
  padding: 0;
}

.form-ipucu {
  font-size: 11.5px;
  color: var(--text-muted);
  line-height: 1.35;
}

.form-ayrac {
  margin: 14px 0 10px;
  border-top: 1px solid var(--border);
}

.form-alt-baslik {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--text-muted);
}

.form-alt-baslik i {
  color: var(--accent);
}

.teslimat-mevcut {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 10px;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 12.5px;
  color: var(--text-secondary);
}

.efatura-baslik {
  margin-top: 14px;
}

.efatura-bilgi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 10px;
}

.efatura-satir {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-secondary);
}

.efatura-satir strong {
  color: var(--text-primary);
  text-align: right;
}

.efatura-ettn {
  font-family: monospace;
  font-size: 10.5px;
  word-break: break-all;
}

.efatura-butonlar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.ozet-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 6px 0;
}

.ozet-satir strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.ozet-genel {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
  font-size: 15px;
}

.ozet-genel strong {
  font-size: 18px;
  font-weight: 800;
}

.odeme-kutu {
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 10px;
}

.form-hata {
  margin-top: 10px;
}

.form-aksiyonlar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.form-aksiyonlar-ek {
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.form-yukleniyor {
  padding: 10px 0;
}

.fatura-form-mobil-bar {
  display: none;
}

@media (max-width: 1199px) {
  .fatura-form-grid {
    grid-template-columns: minmax(0, 1fr) 320px;
  }
  .form-panel-orta {
    grid-column: 1 / -1;
  }
}

@media (max-width: 767px) {
  .fatura-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .form-panel-sag {
    position: static;
  }
  .fatura-form-sayfa {
    padding-bottom: 86px;
  }
  .fatura-form-mobil-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: calc(56px + env(safe-area-inset-bottom));
    z-index: 800;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 14px;
    background: var(--bg-card);
    border-top: 1px solid var(--border);
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.25);
  }
  .mobil-ozet {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .mobil-ozet span {
    font-size: 11px;
    color: var(--text-muted);
  }
  .mobil-ozet strong {
    font-size: 15px;
    font-weight: 800;
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
  }
}
</style>
