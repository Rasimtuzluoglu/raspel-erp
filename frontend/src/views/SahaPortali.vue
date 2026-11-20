<template>
  <div class="saha-portali-sayfasi">
    <!-- Sade başlık: kim + bağlantı durumu + hızlı işlemler -->
    <header class="saha-header-card">
      <div class="saha-header-content">
        <div class="user-info">
          <div class="user-avatar">
            <i class="pi pi-user" />
          </div>
          <div class="user-text">
            <h1 class="user-name">
              {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username || t('sahaPortali.sahaPersoneli') }}
            </h1>
            <div class="user-meta">
              <span
                class="durum-cip"
                :class="cevrimici ? 'cevrimici' : 'cevrimdisi'"
                role="status"
                :title="cevrimici ? t('sahaPortali.cevrimici') : t('sahaPortali.cevrimdisi')"
              >
                <i :class="cevrimici ? 'pi pi-wifi' : 'pi pi-ban'" />
                {{ cevrimici ? t('sahaPortali.cevrimici') : t('sahaPortali.cevrimdisi') }}
              </span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <button
            type="button"
            class="saha-hizli-buton"
            data-test="saha-hizli-siparis"
            @click="yeniSiparisModal = true"
          >
            <i
              class="pi pi-plus"
              aria-hidden="true"
            />
            <span>{{ t('sahaPortali.hizliSiparis') }}</span>
          </button>
          <button
            type="button"
            class="saha-ikon-buton"
            data-test="saha-yenile"
            :aria-label="t('common.refresh')"
            :disabled="yukleniyor || ozetYukleniyor"
            @click="yenile"
          >
            <i
              class="pi pi-refresh"
              :class="{ 'pi-spin': yukleniyor || ozetYukleniyor }"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>
    </header>

    <!-- Çevrimdışı kuyruk çubuğu (yalnız kuyruk varken) -->
    <SahaKuyrukCubugu
      :kuyruk="kuyrukKayitlari"
      :senkronize-ediliyor="kuyrukGonderiliyor"
      @gonder="kuyrukSenkronize"
      @sil="kuyrukKaldir"
    />

    <!-- Bugün özeti -->
    <div class="saha-ozet-alani">
      <SahaOzetKartlari
        :ozet="sahaOzeti"
        :yukleniyor="ozetYukleniyor"
        @sec="sekmeSec"
      />
      <p
        v-if="ozetHata"
        class="saha-ozet-hata"
        role="alert"
      >
        {{ t('sahaPortali.ozetYuklenemedi') }}
        <button
          type="button"
          class="saha-baglanti-buton"
          @click="ozetYukle({ zorla: true })"
        >
          {{ t('common.tekrarDene') }}
        </button>
      </p>
    </div>

    <!-- Segmented sekmeler -->
    <nav
      class="saha-segmentler"
      role="tablist"
      :aria-label="t('sahaPortali.portalBaslik')"
    >
      <button
        v-for="sekme in sekmeler"
        :key="sekme.id"
        type="button"
        role="tab"
        class="saha-segment"
        :class="{ 'saha-segment--aktif': aktifSekme === sekme.id }"
        :aria-selected="aktifSekme === sekme.id"
        :data-test="`saha-sekme-${sekme.id}`"
        @click="sekmeSec(sekme.id)"
      >
        <i
          :class="sekme.ikon"
          aria-hidden="true"
        />
        <span>{{ t(sekme.etiket) }}</span>
        <span
          v-if="sekme.rozet"
          class="tab-badge"
        >{{ sekme.rozet }}</span>
      </button>
    </nav>

    <!-- TESLİMATLAR -->
    <section
      v-if="aktifSekme === 'teslimat'"
      class="saha-sekme-govde"
      data-test="saha-sekme-icerik-teslimat"
      role="tabpanel"
    >
      <p
        v-if="siraliTeslimatlar.length > 1"
        class="saha-siralama-ipucu"
      >
        <i
          class="pi pi-sort-alt"
          aria-hidden="true"
        />
        {{ t('sahaPortali.siralamaIpucu') }}
      </p>

      <div
        v-if="yukleniyor && !teslimatlar.length"
        class="saha-iskelet-liste"
        aria-hidden="true"
      >
        <span
          v-for="n in 3"
          :key="`teslimat-iskelet-${n}`"
          class="saha-iskelet-kart"
        />
      </div>

      <div class="saha-teslimat-liste">
        <div
          v-for="(teslimat, indeks) in siraliTeslimatlar"
          :key="teslimat.id"
          class="saha-teslimat-satir"
        >
          <SahaTeslimatKarti
            :teslimat="teslimat"
            :sira="indeks + 1"
            :ilk-mi="indeks === 0"
            @imza="imzaAc"
            @durum-sec="durumDialogAc"
          />
          <div
            v-if="siraliTeslimatlar.length > 1"
            class="saha-siralama"
          >
            <button
              type="button"
              class="saha-siralama-buton"
              data-test="saha-teslimat-yukari"
              :aria-label="t('sahaPortali.yukariTasi')"
              :disabled="indeks === 0"
              @click="siralaKarti(teslimat.id, 'yukari')"
            >
              <i
                class="pi pi-chevron-up"
                aria-hidden="true"
              />
            </button>
            <button
              type="button"
              class="saha-siralama-buton"
              data-test="saha-teslimat-asagi"
              :aria-label="t('sahaPortali.asagiTasi')"
              :disabled="indeks === siraliTeslimatlar.length - 1"
              @click="siralaKarti(teslimat.id, 'asagi')"
            >
              <i
                class="pi pi-chevron-down"
                aria-hidden="true"
              />
            </button>
          </div>
        </div>
      </div>

      <EmptyState
        v-if="!yukleniyor && !teslimatlar.length"
        icon="pi pi-truck"
        illustrasyon="depo"
        :message="t('sahaPortali.bosSaha')"
      />
    </section>

    <!-- ZİYARET -->
    <section
      v-else-if="aktifSekme === 'ziyaret'"
      class="saha-sekme-govde"
      data-test="saha-sekme-icerik-ziyaret"
      role="tabpanel"
    >
      <div class="saha-form-kart">
        <div class="saha-form-baslik">
          <h2><i class="pi pi-map-marker saha-baslik-ikon" />{{ t('sahaPortali.ziyaretBaslik') }}</h2>
          <p>{{ t('sahaPortali.ziyaretAciklama') }}</p>
        </div>

        <div class="saha-form-govde">
          <div class="saha-alan">
            <label>{{ t('sahaPortali.ziyaretMusteriZorunlu') }}</label>
            <Dropdown
              v-model="ziyaretForm.cariHesapId"
              data-test="saha-ziyaret-cari"
              :options="cariSonuclari"
              option-label="ad"
              option-value="id"
              :placeholder="t('sahaPortali.musteriSecin')"
              :loading="cariAraniyor"
              filter
              class="w-full"
              @filter="cariAra"
            />
          </div>
          <div class="saha-alan">
            <label>{{ t('sahaPortali.ziyaretAmaci') }}</label>
            <Dropdown
              v-model="ziyaretForm.amac"
              :options="[t('sahaPortali.amacSatisTanitim'), t('sahaPortali.amacSiparisTeklif'), t('sahaPortali.amacTahsilat'), t('sahaPortali.amacRutin'), t('sahaPortali.amacDestek')]"
              class="w-full"
            />
          </div>
          <div class="saha-alan">
            <label>{{ t('sahaPortali.gorusmeNotlariZorunlu') }}</label>
            <Textarea
              v-model="ziyaretForm.notlar"
              data-test="saha-ziyaret-notlar"
              rows="4"
              :placeholder="t('sahaPortali.notlarPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="gps-location-row">
            <div class="gps-bilgi">
              <i class="pi pi-map-marker saha-ikon-kirmizi gps-ikon" />
              <span class="gps-metin">
                {{ ziyaretKonum || t('sahaPortali.konumAlinmadi') }}
              </span>
            </div>
            <Button
              :label="t('sahaPortali.konumAl')"
              icon="pi pi-compass"
              class="p-button-outlined p-button-sm p-button-secondary"
              :loading="konumAliniyor"
              @click="gpsKonumAl"
            />
          </div>
          <div class="saha-alan">
            <label>{{ t('sahaPortali.ziyaretFotografiOpsiyonel') }}</label>
            <label class="foto-sec-etiket">
              <i class="pi pi-camera" /> {{ ziyaretFoto ? ziyaretFoto.name : t('sahaPortali.fotografSec') }}
              <input
                type="file"
                accept="image/*"
                hidden
                @change="(e) => (ziyaretFoto = e.target.files?.[0] || null)"
              >
            </label>
          </div>
          <Button
            data-test="saha-ziyaret-gonder"
            :label="t('sahaPortali.ziyaretIlet')"
            icon="pi pi-send"
            class="p-button-primary w-full mt-2 font-bold"
            :loading="ziyaretKaydediliyor"
            @click="ziyaretKaydet"
          />
        </div>
      </div>

      <div class="saha-form-kart">
        <div class="saha-form-baslik">
          <h2>{{ t('sahaPortali.sonZiyaretler') }}</h2>
        </div>
        <ul
          v-if="notlar.length"
          class="saha-ziyaret-liste"
        >
          <li
            v-for="not in notlar"
            :key="not.id"
            class="saha-ziyaret-satir"
          >
            <span class="saha-ziyaret-baslik">{{ not.baslik || not.icerik }}</span>
            <small class="saha-ziyaret-tarih">{{ formatTarih(not.olusturmaTarihi || not.tarih) }}</small>
          </li>
        </ul>
        <EmptyState
          v-else-if="!yukleniyor"
          icon="pi pi-map-marker"
          :message="t('sahaPortali.ziyaretBulunamadi')"
        />
      </div>

      <GorevNotPaneli
        @not-kaydedildi="ziyaretleriYukle"
        @gorev-sayisi="gorevSayisi = $event"
      />
    </section>

    <!-- TALEPLERİM -->
    <section
      v-else-if="aktifSekme === 'talepler'"
      class="saha-sekme-govde"
      data-test="saha-sekme-icerik-talepler"
      role="tabpanel"
    >
      <div
        class="saha-alt-segmentler"
        role="tablist"
      >
        <button
          type="button"
          role="tab"
          class="saha-alt-segment"
          :class="{ 'saha-alt-segment--aktif': talepBolumu === 'masraf' }"
          :aria-selected="talepBolumu === 'masraf'"
          data-test="saha-talep-masraf"
          @click="talepBolumuSec('masraf')"
        >
          <i
            class="pi pi-receipt"
            aria-hidden="true"
          />
          {{ t('sahaPortali.masraflar') }}
          <span
            v-if="bekleyenMasrafSayisi > 0"
            class="tab-badge"
          >{{ bekleyenMasrafSayisi }}</span>
        </button>
        <button
          type="button"
          role="tab"
          class="saha-alt-segment"
          :class="{ 'saha-alt-segment--aktif': talepBolumu === 'izin' }"
          :aria-selected="talepBolumu === 'izin'"
          data-test="saha-talep-izin"
          @click="talepBolumuSec('izin')"
        >
          <i
            class="pi pi-calendar-plus"
            aria-hidden="true"
          />
          {{ t('sahaPortali.izinler') }}
          <span
            v-if="bekleyenIzinSayisi > 0"
            class="tab-badge"
          >{{ bekleyenIzinSayisi }}</span>
        </button>
      </div>

      <template v-if="talepBolumu === 'masraf'">
        <div class="saha-baslik-satir">
          <h2><i class="pi pi-receipt saha-baslik-ikon" />{{ t('sahaPortali.masrafAvansTaleplerim') }}</h2>
          <Button
            :label="t('sahaPortali.yeniMasrafAvans')"
            data-test="saha-masraf-yeni"

            icon="pi pi-plus"
            class="p-button-primary p-button-sm"
            @click="yeniMasrafModal = true"
          />
        </div>

        <div
          v-if="masraflar && masraflar.length > 0"
          class="expense-list"
        >
          <div
            v-for="m in masraflar"
            :key="m.id"
            class="expense-card"
          >
            <div class="expense-left">
              <div class="expense-ustsatir">
                <span class="expense-type-badge">{{ m.tur === 'AVANS' ? t('sahaPortali.avansTalebi') : m.kategori }}</span>
                <Tag
                  :value="m.durum"
                  :severity="talepDurumSeverity(m.durum)"
                  rounded
                />
              </div>
              <p class="expense-desc">
                {{ m.aciklama || '-' }}
              </p>
              <small class="expense-date">{{ formatTarih(m.tarih) }}</small>
            </div>
            <div class="expense-right">
              <span class="expense-amount">{{ formatPara(m.tutar) }}</span>
            </div>
          </div>
        </div>

        <EmptyState
          v-else
          icon="pi pi-wallet"
          :message="t('sahaPortali.masrafBos')"
        />
      </template>

      <template v-else>
        <div class="saha-baslik-satir">
          <h2><i class="pi pi-calendar-plus saha-baslik-ikon" />{{ t('sahaPortali.izinTaleplerim') }}</h2>
          <Button
            :label="t('sahaPortali.yeniIzinTalebi')"
            data-test="saha-izin-yeni"

            icon="pi pi-plus"
            class="p-button-primary p-button-sm"
            @click="yeniIzinModal = true"
          />
        </div>

        <div
          v-if="izinler && izinler.length > 0"
          class="expense-list"
        >
          <div
            v-for="i in izinler"
            :key="i.id"
            class="expense-card"
          >
            <div class="expense-left">
              <div class="expense-ustsatir">
                <strong class="izin-tur">{{ i.izinTuru }}</strong>
                <Tag
                  :value="i.durum"
                  :severity="talepDurumSeverity(i.durum)"
                  rounded
                />
              </div>
              <p class="expense-desc">
                <i class="pi pi-calendar izin-takvim-ikon" />
                {{ formatTarih(i.baslangic) }} → {{ formatTarih(i.bitis) }}
                <span class="izin-gun">({{ i.gunSayisi }} {{ t('sahaPortali.gun') }})</span>
              </p>
              <small
                v-if="i.aciklama"
                class="expense-date"
              >{{ i.aciklama }}</small>
            </div>
          </div>
        </div>

        <EmptyState
          v-else
          icon="pi pi-calendar"
          :message="t('sahaPortali.izinBos')"
        />
      </template>
    </section>

    <!-- TAHSİLAT -->
    <section
      v-else-if="aktifSekme === 'tahsilat'"
      class="saha-sekme-govde"
      data-test="saha-sekme-icerik-tahsilat"
      role="tabpanel"
    >
      <div class="saha-form-kart">
        <div class="saha-form-baslik">
          <h2><i class="pi pi-money-bill saha-baslik-ikon" />{{ t('sahaPortali.tahsilatBaslik') }}</h2>
          <p>{{ t('sahaPortali.tahsilatAciklama') }}</p>
        </div>
        <div class="saha-form-govde">
          <div class="saha-alan">
            <label>{{ t('sahaPortali.musteriZorunlu') }}</label>
            <Dropdown
              v-model="tahsilatForm.cariHesapId"
              data-test="saha-tahsilat-cari"
              :options="cariSonuclari"
              option-label="ad"
              option-value="id"
              :placeholder="t('sahaPortali.musteriSecin')"
              :loading="cariAraniyor"
              filter
              class="w-full"
              @filter="cariAra"
            />
          </div>
          <div class="saha-satir-2">
            <div class="saha-alan">
              <label>{{ t('sahaPortali.tutarZorunlu') }}</label>
              <input
                v-model.number="tahsilatForm.tutar"
                data-test="saha-tahsilat-tutar"
                type="number"
                min="1"
                step="0.01"
                class="p-inputtext w-full"
                placeholder="0.00"
              >
            </div>
            <div class="saha-alan">
              <label>{{ t('sahaPortali.odemeYontemi') }}</label>
              <Dropdown
                v-model="tahsilatForm.odemeYontemi"
                :options="odemeYontemleri"
                option-label="label"
                option-value="value"
                class="w-full"
              />
            </div>
          </div>
          <small class="tahsilat-ipucu"><i class="pi pi-arrow-down-left" /> {{ t('tahsilat.etkiAzalir') }}</small>
          <div class="saha-alan">
            <label>{{ tahsilatForm.odemeYontemi === 'NAKIT' ? t('sahaPortali.kasa') : t('sahaPortali.banka') }}</label>
            <Dropdown
              v-if="tahsilatForm.odemeYontemi === 'NAKIT'"
              v-model="tahsilatForm.kasaId"
              :options="kasalar"
              option-label="ad"
              option-value="id"
              :placeholder="t('sahaPortali.hesapSecinOpsiyonel')"
              filter
              show-clear
              class="w-full"
            />
            <Dropdown
              v-else
              v-model="tahsilatForm.bankaId"
              :options="bankalar"
              option-label="ad"
              option-value="id"
              :placeholder="t('sahaPortali.hesapSecinOpsiyonel')"
              filter
              show-clear
              class="w-full"
            />
          </div>
          <div class="saha-alan">
            <label>{{ t('common.description') }}</label>
            <Textarea
              v-model="tahsilatForm.aciklama"
              rows="2"
              class="w-full"
            />
          </div>
          <small
            v-if="!cevrimici"
            class="tahsilat-offline"
          ><i class="pi pi-cloud" /> {{ t('sahaPortali.cevrimdisiKuyrukBilgi') }}</small>
          <Button
            :label="t('sahaPortali.tahsilatiKaydet')"
            icon="pi pi-check"
            class="p-button-success w-full font-bold"
            data-test="saha-tahsilat-kaydet"
            :loading="tahsilatGonderiliyor"
            @click="tahsilatKaydet"
          />
        </div>
      </div>
    </section>

    <!-- MODAL: HIZLI SİPARİŞ AL -->
    <Dialog
      v-model:visible="yeniSiparisModal"
      :modal="true"
      :header="t('sahaPortali.sahadaHizliSiparis')"
      :style="{ width: '90%', maxWidth: '520px' }"
    >
      <div class="saha-modal-icerik">
        <div class="saha-alan">
          <label>{{ t('sahaPortali.musteriZorunlu') }}</label>
          <Dropdown
            v-model="yeniSiparisForm.cariHesapId"
            :options="cariSonuclari"
            option-label="ad"
            option-value="id"
            :placeholder="t('sahaPortali.musteriSecin')"
            :loading="cariAraniyor"
            filter
            class="w-full"
            @filter="cariAra"
          />
        </div>
        <div class="saha-alan">
          <label>{{ t('sahaPortali.barkodIleBul') }}</label>
          <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><i class="pi pi-qrcode" /></span>
            <InputText
              v-model="barkodArama"
              :placeholder="t('sahaPortali.barkodPlaceholder')"
              @keyup.enter="barkodlaUrunBul"
            />
            <Button
              icon="pi pi-search"
              :aria-label="t('common.searchAction')"
              class="p-button-outlined"
              @click="barkodlaUrunBul"
            />
          </div>
        </div>
        <div class="saha-alan">
          <label>{{ t('sahaPortali.urunStokZorunlu') }}</label>
          <Dropdown
            v-model="yeniSiparisForm.stokId"
            :options="stokSonuclari"
            option-label="ad"
            option-value="id"
            :placeholder="t('sahaPortali.urunSecin')"
            :loading="stokAraniyor"
            filter
            class="w-full"
            @filter="stokAra"
            @change="hizliSiparisStokSecildi"
          />
          <small
            v-if="seciliStokBilgi"
            class="stok-raf-bilgi"
          >
            <i class="pi pi-box" /> {{ t('sahaPortali.stokKodu') }}: {{ seciliStokBilgi.stokKodu || '-' }} ·
            {{ t('sahaPortali.rafNo') }}: {{ seciliStokBilgi.rafNo || '-' }} ·
            {{ t('sahaPortali.stokMiktar') }}: {{ seciliStokBilgi.miktar ?? '-' }}
          </small>
        </div>
        <div class="saha-satir-2">
          <div class="saha-alan">
            <label>{{ t('sahaPortali.miktar') }}</label>
            <input
              v-model.number="yeniSiparisForm.miktar"
              type="number"
              min="1"
              class="p-inputtext w-full"
            >
          </div>
          <div class="saha-alan">
            <label>{{ t('sahaPortali.birimFiyat') }}</label>
            <input
              v-model.number="yeniSiparisForm.birimFiyat"
              type="number"
              min="0"
              step="0.01"
              class="p-inputtext w-full"
            >
          </div>
        </div>
        <Button
          :label="t('sahaPortali.kalemEkle')"
          icon="pi pi-plus"
          class="p-button-outlined w-full"
          :disabled="!yeniSiparisForm.stokId"
          @click="kalemEkle"
        />
        <div
          v-if="yeniSiparisForm.kalemler.length"
          class="sepet-liste"
        >
          <div
            v-for="(k, i) in yeniSiparisForm.kalemler"
            :key="i"
            class="sepet-satir"
          >
            <span class="sepet-ad">{{ k.ad }} × {{ k.miktar }}</span>
            <span class="sepet-tutar">{{ formatPara(k.tutar) }}</span>
            <button
              type="button"
              class="sepet-sil"
              :aria-label="t('common.delete')"
              @click="kalemSil(i)"
            >
              <i class="pi pi-trash" />
            </button>
          </div>
          <div class="sepet-toplam">
            {{ t('sahaPortali.sepetToplam') }}: {{ formatPara(sepetToplam) }}
          </div>
        </div>
        <div class="saha-alan">
          <label>{{ t('sahaPortali.teslimatAdresi') }}</label>
          <InputText
            v-model="yeniSiparisForm.adres"
            :placeholder="t('sahaPortali.teslimAdresiPlaceholder')"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="yeniSiparisModal = false"
        />
        <Button
          :label="t('sahaPortali.siparisiGonder')"
          icon="pi pi-check"
          class="p-button-success font-bold"
          :loading="siparisKaydediliyor"
          @click="hizliSiparisKaydet"
        />
      </template>
    </Dialog>

    <!-- İMZA + FOTO SHEET -->
    <SahaImzaSheet
      v-model:visible="imzaSheetAcik"
      :teslimat="seciliTeslimat"
      :cevrimici="cevrimici"
      :kaydediliyor="teslimEdiliyor"
      @gonder="imzaGonder"
      @kuyrukla="imzaKuyrukla"
    />

    <!-- MODAL: TESLİMAT DURUMU -->
    <Dialog
      v-model:visible="durumSecModal"
      :modal="true"
      :header="t('sahaPortali.durumGuncelle')"
      :style="{ width: '90%', maxWidth: '400px' }"
    >
      <div class="saha-modal-icerik">
        <div class="saha-alan">
          <label>{{ t('common.status') }}</label>
          <Dropdown
            v-model="seciliYeniDurum"
            :options="durumSecenekleri"
            class="w-full"
            :placeholder="t('sahaPortali.durumSecin')"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="durumSecModal = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="durumKaydediliyor"
          @click="durumKaydet"
        />
      </template>
    </Dialog>

    <!-- MODAL: YENİ İZİN TALEBİ -->
    <Dialog
      v-model:visible="yeniIzinModal"
      :modal="true"
      :header="t('sahaPortali.yeniIzinTalebiAc')"
      :style="{ width: '90%', maxWidth: '420px' }"
    >
      <div class="saha-modal-icerik">
        <div class="saha-alan">
          <label>{{ t('sahaPortali.izinTuru') }}</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="[t('sahaPortali.izinYillik'), t('sahaPortali.izinMazeret'), t('sahaPortali.izinSaglik'), t('sahaPortali.izinEvlilik'), t('sahaPortali.izinUcretsiz')]"
            class="w-full"
          />
        </div>
        <div class="saha-satir-2">
          <div class="saha-alan">
            <label>{{ t('sahaPortali.baslangic') }}</label>
            <InputText
              v-model="izinForm.baslangic"
              data-test="saha-izin-baslangic"
              type="date"
              class="w-full"
            />
          </div>
          <div class="saha-alan">
            <label>{{ t('sahaPortali.bitis') }}</label>
            <InputText
              v-model="izinForm.bitis"
              data-test="saha-izin-bitis"
              type="date"
              class="w-full"
            />
          </div>
        </div>
        <div class="saha-alan">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="izinForm.aciklama"
            rows="2"
            :placeholder="t('sahaPortali.izinGerekcesi')"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="yeniIzinModal = false"
        />
        <Button
          :label="t('sahaPortali.talebiGonder')"
          icon="pi pi-send"
          class="p-button-primary font-bold"
          data-test="saha-izin-gonder"
          :loading="izinGonderiliyor"
          @click="izinTalepGonder"
        />
      </template>
    </Dialog>

    <!-- MODAL: YENİ MASRAF / AVANS -->
    <Dialog
      v-model:visible="yeniMasrafModal"
      :modal="true"
      :header="t('sahaPortali.yeniMasrafAvansTalebi')"
      :style="{ width: '90%', maxWidth: '420px' }"
    >
      <div class="saha-modal-icerik">
        <div class="tab-pill-group">
          <button
            type="button"
            :class="['pill-btn', { active: masrafForm.tur === 'MASRAF' }]"
            @click="masrafForm.tur = 'MASRAF'"
          >
            {{ t('sahaPortali.harcamaMasraf') }}
          </button>
          <button
            type="button"
            :class="['pill-btn', { active: masrafForm.tur === 'AVANS' }]"
            @click="masrafForm.tur = 'AVANS'"
          >
            {{ t('sahaPortali.avansTalebi') }}
          </button>
        </div>

        <div
          v-if="masrafForm.tur === 'MASRAF'"
          class="saha-alan"
        >
          <label>{{ t('sahaPortali.harcamaKategorisi') }}</label>
          <Dropdown
            v-model="masrafForm.kategori"
            :options="['YAKIT', 'YEMEK', 'KONAKLAMA', 'ULASIM', 'MALZEME', 'DIGER']"
            class="w-full"
          />
        </div>

        <div class="saha-alan">
          <label>{{ t('sahaPortali.tutarZorunlu') }}</label>
          <input
            v-model.number="masrafForm.tutar"
            data-test="saha-masraf-tutar"
            type="number"
            min="1"
            step="0.01"
            class="p-inputtext w-full"
            placeholder="0.00"
          >
        </div>

        <div class="saha-alan">
          <label>{{ t('sahaPortali.aciklamaZorunlu') }}</label>
          <InputText
            v-model="masrafForm.aciklama"
            data-test="saha-masraf-aciklama"
            :placeholder="t('sahaPortali.masrafAciklamaPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="saha-alan">
          <label>{{ t('sahaPortali.masrafFisiOpsiyonel') }}</label>
          <label class="foto-sec-etiket">
            <i class="pi pi-camera" /> {{ masrafFoto ? masrafFoto.name : t('sahaPortali.fotografSec') }}
            <input
              type="file"
              accept="image/*"
              hidden
              @change="(e) => (masrafFoto = e.target.files?.[0] || null)"
            >
          </label>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="yeniMasrafModal = false"
        />
        <Button
          :label="t('sahaPortali.talebiGonder')"
          icon="pi pi-send"
          class="p-button-primary font-bold"
          data-test="saha-masraf-gonder"
          :loading="masrafGonderiliyor"
          @click="masrafTalepGonder"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { unwrapList } from '../api/utils/unwrap.js'
import { useAuthStore } from '../stores/authStore.js'
import {
  teslimatAPI,
  notAPI,
  personelIzinAPI,
  personelMasrafTalepAPI,
  tahsilatAPI,
  cariHesapAPI,
  stokAPI,
  kasaAPI,
  bankaAPI,
  siparisAPI,
  belgeAPI
} from '../api/index.js'
import { useSahaOzet } from '../composables/useSahaOzet.js'
import { useSahaKuyruk } from '../composables/useSahaKuyruk.js'
import { sirala, siraIdleri, tasi } from '../utils/sahaSiralama.js'
import SahaOzetKartlari from '../components/saha/SahaOzetKartlari.vue'
import SahaKuyrukCubugu from '../components/saha/SahaKuyrukCubugu.vue'
import SahaTeslimatKarti from '../components/saha/SahaTeslimatKarti.vue'
import SahaImzaSheet from '../components/saha/SahaImzaSheet.vue'
import GorevNotPaneli from '../components/GorevNotPaneli.vue'
import EmptyState from '../components/EmptyState.vue'
import { formatTarih, getLocalDateString } from '../utils/format.js'

const { t } = useI18n()
const authStore = useAuthStore()
const toast = useToast()

const kullaniciId = computed(() => authStore?.kullanici?.id ?? null)

// ---- Sekmeler ----
const SEKMELER = ['teslimat', 'ziyaret', 'talepler', 'tahsilat']
const SEKME_KEY = 'raspel_saha_sekme'
// Eski indeks tabanlı anahtar (7 sekme) yeni ada taşınır; bilinmeyen değer teslimata düşer.
const ESKI_SEKME_ESLEME = { 0: 'teslimat', 1: 'ziyaret', 2: 'talepler', 3: 'talepler', 4: 'tahsilat', 5: 'ziyaret', 6: 'tahsilat' }

const baslangicSekmesi = () => {
  try {
    const kayitli = localStorage.getItem(SEKME_KEY)
    if (SEKMELER.includes(kayitli)) return kayitli
    const eski = Number(localStorage.getItem('raspel_saha_aktif_sekme'))
    return ESKI_SEKME_ESLEME[eski] || 'teslimat'
  } catch {
    return 'teslimat'
  }
}

const aktifSekme = ref(baslangicSekmesi())

// ---- Veri ----
const yukleniyor = ref(false)
const teslimatlar = ref([])
const notlar = ref([])
const izinler = ref([])
const masraflar = ref([])
const kasalar = ref([])
const bankalar = ref([])
const gorevSayisi = ref(0)
const talepBolumu = ref('masraf')
const izinlerYuklendi = ref(false)

const siralamaIdleri = ref([])
try {
  const ham = JSON.parse(localStorage.getItem('raspel_saha_teslimat_sirasi') || '[]')
  siralamaIdleri.value = Array.isArray(ham) ? ham : []
} catch {
  siralamaIdleri.value = []
}

// ---- Bağlantı / kuyruk ----
const cevrimici = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
const baglantiGuncelle = () => {
  cevrimici.value = navigator.onLine
}

const {
  kuyruk: kuyrukKayitlari,
  gonderiliyor: kuyrukGonderiliyor,
  depolamaKisitli: kuyrukDepolamaKisitli,
  yukle: kuyrukYukle,
  ekle: kuyrukEkleHam,
  kaldir: kuyrukKaldir,
  senkronize: kuyrukSenkronizeHam
} = useSahaKuyruk({ gondericiler: kuyrukGondericileri() })

/** Kuyruğa oturum damgası ekler: paylaşılan cihazda başka kullanıcı göndermesin. */
const kuyrukEkle = (kayit) =>
  kuyrukEkleHam({
    ...kayit,
    sirketId: authStore?.sirketId ?? null,
    kullaniciId: kullaniciId.value ?? null
  })

// ---- Formlar ----
const yeniSiparisModal = ref(false)
const yeniIzinModal = ref(false)
const yeniMasrafModal = ref(false)
const durumSecModal = ref(false)
const imzaSheetAcik = ref(false)
const seciliTeslimat = ref(null)
const seciliYeniDurum = ref('YOLDA')

const teslimEdiliyor = ref(false)
const izinGonderiliyor = ref(false)
const masrafGonderiliyor = ref(false)
const ziyaretKaydediliyor = ref(false)
const siparisKaydediliyor = ref(false)
const tahsilatGonderiliyor = ref(false)
const durumKaydediliyor = ref(false)
const konumAliniyor = ref(false)

const izinForm = ref({
  izinTuru: t('sahaPortali.izinYillik'),
  baslangic: getLocalDateString(),
  bitis: getLocalDateString(),
  aciklama: ''
})
const masrafForm = ref({ tur: 'MASRAF', kategori: 'YAKIT', tutar: null, aciklama: '' })
const ziyaretForm = ref({ cariHesapId: null, amac: t('sahaPortali.amacSatisTanitim'), notlar: '' })
const tahsilatForm = ref({ cariHesapId: null, tutar: null, odemeYontemi: 'NAKIT', kasaId: null, bankaId: null, aciklama: '' })
const yeniSiparisForm = ref({ cariHesapId: null, stokId: null, miktar: 1, birimFiyat: 0, adres: '', kalemler: [] })

const ziyaretKonum = ref('')
const ziyaretFoto = ref(null)
const masrafFoto = ref(null)
const barkodArama = ref('')

const odemeYontemleri = computed(() => [
  { label: t('sahaPortali.nakit'), value: 'NAKIT' },
  { label: t('sahaPortali.havale'), value: 'HAVALE' },
  { label: t('sahaPortali.kart'), value: 'KART' }
])

// ---- Sunucu tarafı arama (500'lük listeler kaldırıldı) ----
const cariSonuclari = ref([])
const cariAraniyor = ref(false)
const stokSonuclari = ref([])
const stokAraniyor = ref(false)
let cariZamanlayici = null
let stokZamanlayici = null

const cariAra = (olay) => {
  const sorgu = (typeof olay === 'string' ? olay : olay?.value || '').trim()
  if (cariZamanlayici) clearTimeout(cariZamanlayici)
  if (sorgu.length < 2) {
    cariSonuclari.value = []
    return
  }
  cariAraniyor.value = true
  cariZamanlayici = setTimeout(async () => {
    try {
      cariSonuclari.value = unwrapList(await cariHesapAPI.search(sorgu))
    } catch {
      cariSonuclari.value = []
    } finally {
      cariAraniyor.value = false
    }
  }, 300)
}

const stokAra = (olay) => {
  const sorgu = (typeof olay === 'string' ? olay : olay?.value || '').trim()
  if (stokZamanlayici) clearTimeout(stokZamanlayici)
  if (sorgu.length < 2) {
    stokSonuclari.value = []
    return
  }
  stokAraniyor.value = true
  stokZamanlayici = setTimeout(async () => {
    try {
      stokSonuclari.value = unwrapList(await stokAPI.ara(sorgu))
    } catch {
      stokSonuclari.value = []
    } finally {
      stokAraniyor.value = false
    }
  }, 300)
}

const seciliStokBilgi = computed(
  () => stokSonuclari.value.find((s) => s.id === yeniSiparisForm.value.stokId) || null
)
const sepetToplam = computed(() =>
  yeniSiparisForm.value.kalemler.reduce((toplam, k) => toplam + (Number(k.tutar) || 0), 0)
)

// ---- Özet ----
const {
  ozet: sahaOzeti,
  yukleniyor: ozetYukleniyor,
  hata: ozetHata,
  yukle: ozetYukle
} = useSahaOzet()

const sekmeler = computed(() => [
  {
    id: 'teslimat',
    etiket: 'sahaPortali.teslimatlar',
    ikon: 'pi pi-truck',
    rozet: teslimatRozeti.value
  },
  { id: 'ziyaret', etiket: 'sahaPortali.ziyaretKaydi', ikon: 'pi pi-map-marker', rozet: 0 },
  { id: 'talepler', etiket: 'sahaPortali.taleplerim', ikon: 'pi pi-inbox', rozet: talepRozeti.value },
  { id: 'tahsilat', etiket: 'sahaPortali.tahsilat', ikon: 'pi pi-money-bill', rozet: 0 }
])

const teslimatRozeti = computed(() => {
  const ozet = sahaOzeti.value?.teslimat
  const sayi = Number(ozet?.bekleyen || 0) + Number(ozet?.yolda || 0)
  return sayi > 0 ? sayi : 0
})
const talepRozeti = computed(() => {
  const ozet = sahaOzeti.value?.talepler
  const sayi = Number(ozet?.bekleyenIzin || 0) + Number(ozet?.bekleyenMasraf || 0)
  return sayi > 0 ? sayi : 0
})

// ---- Yükleme (sekme başına 60 sn önbellek) ----
const TAZELEME_MS = 60_000
const sonYukleme = new Map()

const teslimatlariYukle = async () => {
  if (!kullaniciId.value) {
    teslimatlar.value = []
    return
  }
  yukleniyor.value = true
  try {
    teslimatlar.value = unwrapList(await teslimatAPI.teslimatlar(kullaniciId.value))
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || err.message,
      life: 4000
    })
  } finally {
    yukleniyor.value = false
  }
}

const ziyaretleriYukle = async () => {
  yukleniyor.value = true
  try {
    notlar.value = unwrapList(await notAPI.getAll({ size: 30 }))
  } catch {
    notlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

const masraflariYukle = async () => {
  yukleniyor.value = true
  try {
    masraflar.value = unwrapList(await personelMasrafTalepAPI.getKullaniciTalepleri())
  } catch {
    masraflar.value = []
  } finally {
    yukleniyor.value = false
  }
}

const izinleriYukle = async () => {
  yukleniyor.value = true
  try {
    izinler.value = unwrapList(await personelIzinAPI.getAll({ size: 100 }))
    izinlerYuklendi.value = true
  } catch {
    izinler.value = []
  } finally {
    yukleniyor.value = false
  }
}

const talepBolumuSec = async (bolum) => {
  if (bolum !== 'masraf' && bolum !== 'izin') return
  talepBolumu.value = bolum
  if (bolum === 'izin' && !izinlerYuklendi.value) await izinleriYukle()
}

const tahsilatVerileriniYukle = async () => {
  // Açılışta tek istek: varsayılan ödeme yöntemi NAKİT olduğu için yalnız kasalar.
  yukleniyor.value = true
  try {
    kasalar.value = unwrapList(await kasaAPI.getAll({ size: 200 }))
  } catch {
    kasalar.value = []
  } finally {
    yukleniyor.value = false
  }
}

const bankalarYuklendi = ref(false)
const bankalariYukle = async () => {
  if (bankalarYuklendi.value) return
  try {
    bankalar.value = unwrapList(await bankaAPI.getAll({ size: 200 }))
    bankalarYuklendi.value = true
  } catch {
    bankalar.value = []
  }
}

// Havale/kart seçilince banka seçenekleri tembel yüklenir (açılışta ek istek yok).
watch(
  () => tahsilatForm.value.odemeYontemi,
  (yontem) => {
    if (yontem && yontem !== 'NAKIT') bankalariYukle()
  }
)

const sekmeYukle = async (sekme, { zorla = false } = {}) => {
  const son = sonYukleme.get(sekme) || 0
  if (!zorla && son && Date.now() - son < TAZELEME_MS) return
  if (sekme === 'teslimat') await teslimatlariYukle()
  else if (sekme === 'ziyaret') await ziyaretleriYukle()
  else if (sekme === 'talepler') await masraflariYukle()
  else if (sekme === 'tahsilat') await tahsilatVerileriniYukle()
  sonYukleme.set(sekme, Date.now())
}

const sekmeSec = (sekme) => {
  if (!SEKMELER.includes(sekme)) return
  aktifSekme.value = sekme
}

watch(aktifSekme, (sekme) => {
  try {
    localStorage.setItem(SEKME_KEY, sekme)
  } catch {
    // depolama kapalı: seçim yalnızca bu oturumda geçerli
  }
  sekmeYukle(sekme)
})

const yenile = async () => {
  await Promise.all([ozetYukle({ zorla: true }), sekmeYukle(aktifSekme.value, { zorla: true })])
}

// ---- Teslimat sıralaması ----
const aktifTeslimatlar = computed(() => teslimatlar.value.filter((x) => x && x.durum !== 'IPTAL'))
const siraliTeslimatlar = computed(() => sirala(aktifTeslimatlar.value, siralamaIdleri.value))

const siralamaKaydet = (liste) => {
  siralamaIdleri.value = siraIdleri(liste)
  try {
    localStorage.setItem('raspel_saha_teslimat_sirasi', JSON.stringify(siralamaIdleri.value))
  } catch {
    // yoksay
  }
}

const siralaKarti = (id, yon) => {
  siralamaKaydet(tasi(siraliTeslimatlar.value, id, yon))
}

// ---- Çevrimdışı kuyruk göndericileri ----
const sahiplikHatasi = (mesaj) => ({
  response: { status: 422, data: { message: mesaj } },
  message: mesaj
})

/** Kayıt başka bir kullanıcı/şirket oturumunda oluşturulduysa gönderilmez. */
function sahiplikKontrol(kayit) {
  const sirket = authStore?.sirketId ?? null
  const kullanici = kullaniciId.value ?? null
  if (kayit.sirketId && sirket && String(kayit.sirketId) !== String(sirket)) {
    throw sahiplikHatasi(t('sahaPortali.kuyrukBaskaKullanici'))
  }
  if (kayit.kullaniciId && kullanici && String(kayit.kullaniciId) !== String(kullanici)) {
    throw sahiplikHatasi(t('sahaPortali.kuyrukBaskaKullanici'))
  }
}

function kuyrukGondericileri() {
  return {
    TESLIMAT: async (kayit) => {
      sahiplikKontrol(kayit)
      const imza = (kayit.dosyalar || []).find((d) => String(d.ad || '').startsWith('imza'))
      const foto = (kayit.dosyalar || []).find((d) => String(d.ad || '').startsWith('teslim'))
      const imzaDosyasi = imza?.dataUrl ? veriUrlesiDosyaya(imza.dataUrl, imza.ad || 'imza.png') : undefined
      const govde = {
        teslimAlanAd: kayit.payload.teslimAlanAd,
        teslimNotu: kayit.payload.teslimNotu
      }
      const teslimatId = kayit.payload.teslimatId
      const siparisId = kayit.payload.siparisId
      if (teslimatId) {
        await teslimatAPI.teslimEt(teslimatId, govde, imzaDosyasi)
        if (foto?.dataUrl) {
          try {
            await teslimatAPI.fotoYukle(
              teslimatId,
              veriUrlesiDosyaya(foto.dataUrl, foto.ad || 'teslim.jpg')
            )
          } catch {
            // fotoğraf yüklenemedi; teslim kaydı yine de tamamlandı
          }
        }
        return
      }
      // Eski (localStorage) kuyruk kayıtları sipariş üzerinden teslim edilir.
      if (siparisId) {
        await teslimatAPI.teslimEtSiparis(siparisId, govde, imzaDosyasi)
        return
      }
      throw {
        response: { status: 422, data: { message: t('sahaPortali.eksikBilgi') } },
        message: t('sahaPortali.eksikBilgi')
      }
    },
    ZIYARET: async (kayit) => {
      sahiplikKontrol(kayit)
      await notAPI.create(kayit.payload)
      const foto = (kayit.dosyalar || [])[0]
      if (foto?.dataUrl && kayit.payload.cariHesapId) {
        try {
          await belgeAPI.yukle(
            'CariHesap',
            kayit.payload.cariHesapId,
            veriUrlesiDosyaya(foto.dataUrl, foto.ad || 'ziyaret.jpg')
          )
        } catch {
          // ziyaret kaydı oluştu; fotoğraf sonradan elle yüklenebilir
        }
      }
    },
    MASRAF: async (kayit, yardimci) => {
      sahiplikKontrol(kayit)
      // Kısmi gönderim: talep bir kez oluşturulur; fiş yüklenemezse kayıt
      // kuyrukta kalır ve yeniden denemede yalnız belge yüklenir.
      let talepId = kayit.payload.olusanId
      if (!talepId) {
        const olusan = await personelMasrafTalepAPI.create(kayit.payload)
        talepId = olusan?.data?.id
        if (talepId) {
          kayit.payload.olusanId = talepId
          if (typeof yardimci?.guncelle === 'function') await yardimci.guncelle(kayit)
        }
      }
      const fis = (kayit.dosyalar || [])[0]
      if (fis?.dataUrl && talepId) {
        await belgeAPI.yukle(
          'MasrafTalep',
          talepId,
          veriUrlesiDosyaya(fis.dataUrl, fis.ad || 'fis.jpg')
        )
      }
    },
    IZIN: (kayit) => {
      sahiplikKontrol(kayit)
      return personelIzinAPI.create(kayit.payload)
    },
    TAHSILAT: (kayit) => {
      sahiplikKontrol(kayit)
      return tahsilatAPI.gir(kayit.payload, kayit.clientKey)
    }
  }
}

const veriUrlesiDosyaya = (dataUrl, dosyaAdi) => {
  const parcalar = String(dataUrl).split(',')
  const mime = (parcalar[0].match(/data:(.*?);/) || [])[1] || 'image/png'
  const bayt = atob(parcalar[1] || '')
  const dizi = new Uint8Array(bayt.length)
  for (let i = 0; i < bayt.length; i += 1) dizi[i] = bayt.charCodeAt(i)
  return new File([dizi], dosyaAdi, { type: mime })
}

const kuyrukSenkronize = async () => {
  if (!navigator.onLine || !kuyrukKayitlari.value.length) return
  const sonuc = await kuyrukSenkronizeHam()
  if (!sonuc || sonuc.atlandi) return
  if (sonuc.gonderilen > 0) {
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.basarili'),
      detail: t('sahaPortali.kuyrukGonderildi', { n: sonuc.gonderilen }),
      life: 4000
    })
    sonYukleme.delete('teslimat')
    await Promise.all([sekmeYukle(aktifSekme.value), ozetYukle({ zorla: true })])
  }
}

// ---- İmza akışı ----
const imzaAc = (teslimat) => {
  seciliTeslimat.value = teslimat
  imzaSheetAcik.value = true
}

const imzaGonder = async (govde) => {
  const teslimat = seciliTeslimat.value
  if (!teslimat?.id) return
  teslimEdiliyor.value = true
  try {
    const dosya = govde.imzaDataUrl
      ? veriUrlesiDosyaya(govde.imzaDataUrl, `imza-${teslimat.id}.png`)
      : undefined
    await teslimatAPI.teslimEt(
      teslimat.id,
      { teslimAlanAd: govde.teslimAlanAd, teslimNotu: govde.teslimNotu },
      dosya
    )
    if (govde.fotoDataUrl) {
      try {
        await teslimatAPI.fotoYukle(
          teslimat.id,
          veriUrlesiDosyaya(govde.fotoDataUrl, `teslim-${teslimat.id}.jpg`)
        )
      } catch {
        // fotoğraf yüklenemedi; teslim kaydı tamamlandı
      }
    }
    teslimat.durum = 'TESLIM_EDILDI'
    imzaSheetAcik.value = false
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.teslimEdildi'),
      detail: t('sahaPortali.siparisTeslimEdildi'),
      life: 3000
    })
    sonYukleme.delete('teslimat')
    await Promise.all([teslimatlariYukle(), ozetYukle({ zorla: true })])
    sonYukleme.set('teslimat', Date.now())
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || err.message,
      life: 4000
    })
  } finally {
    teslimEdiliyor.value = false
  }
}

const imzaKuyrukla = async (govde) => {
  const teslimat = seciliTeslimat.value
  if (!teslimat?.id) return
  await kuyrukEkle({
    tur: 'TESLIMAT',
    payload: {
      teslimatId: teslimat.id,
      teslimAlanAd: govde.teslimAlanAd,
      teslimNotu: govde.teslimNotu
    },
    dosyalar: [
      ...(govde.imzaDataUrl
        ? [{ ad: `imza-${teslimat.id}.png`, mime: 'image/png', dataUrl: govde.imzaDataUrl }]
        : []),
      ...(govde.fotoDataUrl
        ? [{ ad: `teslim-${teslimat.id}.jpg`, mime: 'image/jpeg', dataUrl: govde.fotoDataUrl }]
        : [])
    ]
  })
  imzaSheetAcik.value = false
  toast.add({
    severity: 'info',
    summary: t('sahaPortali.cevrimdisi'),
    detail: kuyrukDepolamaKisitli.value
      ? t('sahaPortali.cevrimdisiKuyrukDolu')
      : t('sahaPortali.cevrimdisiKaydedildi'),
    life: 5000
  })
}

// ---- Teslimat durumu ----
const durumSecenekleri = ['BEKLEMEDE', 'YOLDA', 'IPTAL']

const durumDialogAc = (teslimat) => {
  // Teslim edilmiş kaydın durumu imzalı teslim akışıyla yönetilir; buradan
  // yeniden gönderilirse sunucu geçersiz durum alır.
  if (teslimat?.durum === 'TESLIM_EDILDI') return
  seciliTeslimat.value = teslimat
  seciliYeniDurum.value = teslimat?.durum || 'YOLDA'
  durumSecModal.value = true
}

const durumKaydet = async () => {
  if (!seciliTeslimat.value?.id) return
  durumKaydediliyor.value = true
  try {
    await teslimatAPI.durumGuncelle(seciliTeslimat.value.id, seciliYeniDurum.value)
    seciliTeslimat.value.durum = seciliYeniDurum.value
    durumSecModal.value = false
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.guncellendi'),
      detail: t('sahaPortali.siparisDurumGuncellendi'),
      life: 2500
    })
    sonYukleme.delete('teslimat')
    await teslimatlariYukle()
    sonYukleme.set('teslimat', Date.now())
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || err.message,
      life: 3000
    })
  } finally {
    durumKaydediliyor.value = false
  }
}

// ---- Talepler ----
const talepDurumSeverity = (durum) => {
  const harita = { BEKLEMEDE: 'warning', ONAYLANDI: 'success', REDDEDILDI: 'danger' }
  return harita[durum] || 'info'
}

const izinTalepGonder = async () => {
  const personelId = authStore?.kullanici?.personelId
  if (!personelId) {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.eksikBilgi'),
      detail: t('sahaPortali.personelKaydiYok'),
      life: 3000
    })
    return
  }
  izinGonderiliyor.value = true
  const govde = { personelId, izinTuru: izinForm.value.izinTuru, baslangic: izinForm.value.baslangic, bitis: izinForm.value.bitis, aciklama: izinForm.value.aciklama }
  try {
    const bas = new Date(izinForm.value.baslangic)
    const bit = new Date(izinForm.value.bitis)
    govde.gunSayisi = Math.max(1, Math.round((bit - bas) / (1000 * 60 * 60 * 24)) + 1)
    if (!navigator.onLine) {
      await kuyrukEkle({ tur: 'IZIN', payload: govde })
      yeniIzinModal.value = false
      toast.add({
        severity: 'info',
        summary: t('sahaPortali.cevrimdisi'),
        detail: kuyrukBilgiDetayi(),
        life: 4000
      })
      return
    }
    await personelIzinAPI.create(govde)
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.basarili'),
      detail: t('sahaPortali.izinIletildi'),
      life: 3000
    })
    yeniIzinModal.value = false
    sonYukleme.delete('talepler-izin')
    await izinleriYukle()
    sonYukleme.set('talepler-izin', Date.now())
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || err.message,
      life: 3000
    })
  } finally {
    izinGonderiliyor.value = false
  }
}

const masrafTalepGonder = async () => {
  if (!masrafForm.value.tutar || masrafForm.value.tutar <= 0) {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.eksik'),
      detail: t('sahaPortali.gecerliTutar'),
      life: 3000
    })
    return
  }
  masrafGonderiliyor.value = true
  const govde = {
    tur: masrafForm.value.tur,
    kategori: masrafForm.value.kategori,
    tutar: masrafForm.value.tutar,
    aciklama: masrafForm.value.aciklama,
    tarih: getLocalDateString()
  }
  const fis = masrafFoto.value
  try {
    if (!navigator.onLine) {
      await kuyrukEkle({
        tur: 'MASRAF',
        payload: govde,
        dosyalar: fis ? [{ ad: fis.name, mime: fis.type, dataUrl: await dosyaVeriUrlesi(fis) }] : []
      })
      masrafFoto.value = null
      yeniMasrafModal.value = false
      toast.add({
        severity: 'info',
        summary: t('sahaPortali.cevrimdisi'),
        detail: kuyrukBilgiDetayi(),
        life: 4000
      })
      return
    }
    const olusan = await personelMasrafTalepAPI.create(govde)
    if (fis && olusan?.data?.id) {
      try {
        await belgeAPI.yukle('MasrafTalep', olusan.data.id, fis)
      } catch {
        // fiş yüklenemedi; talep yine de iletildi
      }
    }
    masrafFoto.value = null
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.basarili'),
      detail: t('sahaPortali.talepIletildi'),
      life: 3000
    })
    yeniMasrafModal.value = false
    sonYukleme.delete('talepler')
    await masraflariYukle()
    sonYukleme.set('talepler', Date.now())
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || t('sahaPortali.hata'),
      life: 3000
    })
  } finally {
    masrafGonderiliyor.value = false
  }
}

const dosyaVeriUrlesi = (dosya) =>
  new Promise((cozumle, reddet) => {
    const okuyucu = new FileReader()
    okuyucu.onload = () => cozumle(okuyucu.result)
    okuyucu.onerror = () => reddet(new Error('dosya okunamadi'))
    okuyucu.readAsDataURL(dosya)
  })

// ---- Ziyaret ----
const gpsKonumAl = () => {
  if (!navigator.geolocation) {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.desteklenmiyor'),
      detail: t('sahaPortali.konumDesteklenmiyor'),
      life: 3000
    })
    return
  }
  konumAliniyor.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      konumAliniyor.value = false
      const lat = pos.coords.latitude.toFixed(5)
      const lng = pos.coords.longitude.toFixed(5)
      ziyaretKonum.value = t('sahaPortali.konum', { enlem: lat, boylam: lng })
      const konumNotu = t('sahaPortali.konumEtiketi', { enlem: lat, boylam: lng })
      if (ziyaretForm.value.notlar) {
        ziyaretForm.value.notlar += `\n[📍 ${konumNotu}]`
      } else {
        ziyaretForm.value.notlar = `[📍 ${konumNotu}] `
      }
      toast.add({
        severity: 'success',
        summary: t('sahaPortali.konumAlindi'),
        detail: t('sahaPortali.konumEklendi'),
        life: 2500
      })
    },
    (err) => {
      konumAliniyor.value = false
      toast.add({
        severity: 'error',
        summary: t('sahaPortali.konumHatasi'),
        detail: t('sahaPortali.konumAlinamadiHata') + err.message,
        life: 3000
      })
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

const ziyaretKaydet = async () => {
  if (!ziyaretForm.value.cariHesapId || !ziyaretForm.value.notlar) {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.eksikBilgi'),
      detail: t('sahaPortali.musteriNotlarZorunlu'),
      life: 3000
    })
    return
  }
  ziyaretKaydediliyor.value = true
  const cari = cariSonuclari.value.find((c) => c?.id === ziyaretForm.value.cariHesapId)
  const notKaydi = {
    baslik: t('sahaPortali.sahaZiyareti', {
      musteri: cari?.ad || t('sahaPortali.musteri'),
      amac: ziyaretForm.value.amac
    }),
    icerik: ziyaretForm.value.notlar,
    cariHesapId: ziyaretForm.value.cariHesapId,
    kategori: 'SAHA_ZIYARET'
  }
  try {
    if (!navigator.onLine) {
      await kuyrukEkle({
        tur: 'ZIYARET',
        payload: notKaydi,
        dosyalar: ziyaretFoto.value
          ? [{ ad: ziyaretFoto.value.name, mime: ziyaretFoto.value.type, dataUrl: await dosyaVeriUrlesi(ziyaretFoto.value) }]
          : []
      })
      toast.add({
        severity: 'info',
        summary: t('sahaPortali.cevrimdisi'),
        detail: kuyrukDepolamaKisitli.value
          ? t('sahaPortali.cevrimdisiKuyrukDolu')
          : t('sahaPortali.cevrimdisiZiyaretKuyruk'),
        life: 4000
      })
      ziyaretForm.value.notlar = ''
      ziyaretForm.value.cariHesapId = null
      ziyaretFoto.value = null
      return
    }
    await notAPI.create(notKaydi)
    if (ziyaretFoto.value) {
      try {
        await belgeAPI.yukle('CariHesap', ziyaretForm.value.cariHesapId, ziyaretFoto.value)
      } catch {
        // fotoğraf yüklenemedi; ziyaret yine de kaydedildi
      }
    }
    ziyaretFoto.value = null
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.basarili'),
      detail: t('sahaPortali.ziyaretKaydedildi'),
      life: 3000
    })
    ziyaretForm.value.notlar = ''
    ziyaretForm.value.cariHesapId = null
    sonYukleme.delete('ziyaret')
    await ziyaretleriYukle()
    sonYukleme.set('ziyaret', Date.now())
    await ozetYukle({ zorla: true })
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || t('sahaPortali.hata'),
      life: 3000
    })
  } finally {
    ziyaretKaydediliyor.value = false
  }
}

// ---- Hızlı sipariş ----
const barkodlaUrunBul = async () => {
  const kod = barkodArama.value?.trim()
  if (!kod) return
  try {
    const sonuclar = unwrapList(await stokAPI.ara(kod))
    const bulunan = sonuclar.find(
      (s) =>
        (s.barkod && String(s.barkod).toLowerCase() === kod.toLowerCase()) ||
        (s.stokKodu && String(s.stokKodu).toLowerCase() === kod.toLowerCase())
    ) || sonuclar[0]
    if (bulunan) {
      stokSonuclari.value = sonuclar
      yeniSiparisForm.value.stokId = bulunan.id
      hizliSiparisStokSecildi()
      toast.add({ severity: 'success', summary: t('sahaPortali.urunBulundu'), detail: bulunan.ad, life: 2000 })
    } else {
      toast.add({
        severity: 'warn',
        summary: t('sahaPortali.bulunamadi'),
        detail: t('sahaPortali.barkodBulunamadi'),
        life: 2500
      })
    }
  } catch {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.bulunamadi'),
      detail: t('sahaPortali.barkodBulunamadi'),
      life: 2500
    })
  }
}

const hizliSiparisStokSecildi = () => {
  const stokId = yeniSiparisForm.value?.stokId
  if (!stokId) return
  const stok = stokSonuclari.value.find((item) => item?.id === stokId)
  if (stok) {
    yeniSiparisForm.value.birimFiyat = stok.fiyat || stok.satisFiyati || 0
  }
}

const kalemEkle = () => {
  const stok = seciliStokBilgi.value
  if (!stok) return
  const miktar = Number(yeniSiparisForm.value.miktar) || 1
  const fiyat = Number(yeniSiparisForm.value.birimFiyat) || 0
  yeniSiparisForm.value.kalemler.push({
    stokId: stok.id,
    ad: stok.ad,
    miktar,
    birimFiyat: fiyat,
    kdvOrani: stok.kdvOrani ?? 20,
    tutar: miktar * fiyat
  })
  yeniSiparisForm.value.stokId = null
  yeniSiparisForm.value.miktar = 1
  yeniSiparisForm.value.birimFiyat = 0
}

const kalemSil = (indeks) => {
  yeniSiparisForm.value.kalemler.splice(indeks, 1)
}

const hizliSiparisKaydet = async () => {
  const form = yeniSiparisForm.value
  if (form.stokId) kalemEkle()
  if (!form.cariHesapId || !form.kalemler.length) {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.eksikBilgi'),
      detail: t('sahaPortali.musteriUrunZorunlu'),
      life: 3000
    })
    return
  }
  siparisKaydediliyor.value = true
  try {
    await siparisAPI.create({
      cariHesapId: form.cariHesapId,
      tarih: getLocalDateString(),
      durum: 'BEKLIYOR',
      aciklama: t('sahaPortali.sahaSiparisi'),
      teslimatAdresi: form.adres || '',
      kalemler: form.kalemler.map((k) => ({
        stokId: k.stokId,
        miktar: k.miktar,
        birimFiyat: k.birimFiyat,
        kdvOrani: k.kdvOrani,
        tutar: k.tutar
      }))
    })
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.basarili'),
      detail: t('sahaPortali.siparisGonderildi'),
      life: 3000
    })
    yeniSiparisModal.value = false
    yeniSiparisForm.value = { cariHesapId: null, stokId: null, miktar: 1, birimFiyat: 0, adres: '', kalemler: [] }
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || t('sahaPortali.hata'),
      life: 3000
    })
  } finally {
    siparisKaydediliyor.value = false
  }
}

// ---- Tahsilat (idempotency anahtarı form dolduğunda üretilir ve hata
// durumunda korunur; ağ zaman aşımında ikinci kayıt oluşmaz) ----
const tahsilatClientKey = ref(null)

const anahtarUret = () =>
  typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `saha-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`

const kuyrukBilgiDetayi = () =>
  kuyrukDepolamaKisitli.value
    ? t('sahaPortali.cevrimdisiKuyrukDolu')
    : t('sahaPortali.cevrimdisiKaydedildi')

const tahsilatKaydet = async () => {
  if (!tahsilatForm.value.cariHesapId || !tahsilatForm.value.tutar || tahsilatForm.value.tutar <= 0) {
    toast.add({
      severity: 'warn',
      summary: t('sahaPortali.eksikBilgi'),
      detail: t('sahaPortali.cariTutarZorunlu'),
      life: 3000
    })
    return
  }
  tahsilatGonderiliyor.value = true
  const govde = {
    cariId: tahsilatForm.value.cariHesapId,
    tutar: tahsilatForm.value.tutar,
    odemeYontemi: tahsilatForm.value.odemeYontemi,
    kasaId: tahsilatForm.value.kasaId,
    bankaId: tahsilatForm.value.bankaId,
    aciklama: tahsilatForm.value.aciklama || t('sahaPortali.sahaTahsilati'),
    hareketTarihi: getLocalDateString()
  }
  if (!tahsilatClientKey.value) tahsilatClientKey.value = anahtarUret()
  const anahtar = tahsilatClientKey.value
  try {
    if (!navigator.onLine) {
      await kuyrukEkle({ tur: 'TAHSILAT', payload: govde, clientKey: anahtar })
      tahsilatClientKey.value = null
      tahsilatForm.value = { cariHesapId: null, tutar: null, odemeYontemi: 'NAKIT', kasaId: null, bankaId: null, aciklama: '' }
      toast.add({
        severity: 'info',
        summary: t('sahaPortali.cevrimdisi'),
        detail: kuyrukBilgiDetayi(),
        life: 5000
      })
      return
    }
    await tahsilatAPI.gir(govde, anahtar)
    tahsilatClientKey.value = null
    toast.add({
      severity: 'success',
      summary: t('sahaPortali.basarili'),
      detail: t('sahaPortali.tahsilatKaydedildi'),
      life: 3000
    })
    tahsilatForm.value = { cariHesapId: null, tutar: null, odemeYontemi: 'NAKIT', kasaId: null, bankaId: null, aciklama: '' }
    await ozetYukle({ zorla: true })
  } catch (err) {
    // Anahtar korunur: kullanıcı tekrar denerese aynı istek mükerrer yazılmaz.
    toast.add({
      severity: 'error',
      summary: t('sahaPortali.hata'),
      detail: err?.response?.data?.message || err.message,
      life: 4000
    })
  } finally {
    tahsilatGonderiliyor.value = false
  }
}

// ---- Yaşam döngüsü ----
let kuyrukZamanlayici = null

onMounted(async () => {
  await Promise.all([ozetYukle({ zorla: true }), sekmeYukle(aktifSekme.value)])
  await kuyrukYukle()
  if (kuyrukKayitlari.value.length && navigator.onLine) {
    await kuyrukSenkronize()
  }
  window.addEventListener('online', baglantiGuncelle)
  window.addEventListener('offline', baglantiGuncelle)
  window.addEventListener('online', kuyrukSenkronize)
  kuyrukZamanlayici = setInterval(() => {
    if (navigator.onLine && kuyrukKayitlari.value.length) kuyrukSenkronize()
  }, 60_000)
})

onUnmounted(() => {
  window.removeEventListener('online', baglantiGuncelle)
  window.removeEventListener('offline', baglantiGuncelle)
  window.removeEventListener('online', kuyrukSenkronize)
  if (kuyrukZamanlayici) clearInterval(kuyrukZamanlayici)
  if (cariZamanlayici) clearTimeout(cariZamanlayici)
  if (stokZamanlayici) clearTimeout(stokZamanlayici)
})
</script>

<style scoped>
.saha-portali-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 40px;
  max-width: 1100px;
  margin: 0 auto;
  min-width: 0;
}

.saha-header-card {
  background: linear-gradient(135deg, #1e40af 0%, var(--accent) 100%);
  color: #fff;
  padding: 12px 14px;
  border-radius: 1rem;
  box-shadow: 0 8px 20px -4px rgba(30, 64, 175, 0.3);
}

.saha-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex: 0 0 auto;
}

.user-text {
  min-width: 0;
}

.user-name {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0 0 0.2rem 0;
  overflow-wrap: anywhere;
}

.user-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

/* Baglanti durumu cipi: cevrimici/cevrimdisi. */
.durum-cip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 9999px;
  border: 1px solid transparent;
  line-height: 1.5;
}
.durum-cip.cevrimici {
  background: rgba(16, 185, 129, 0.18);
  color: #d1fae5;
  border-color: rgba(16, 185, 129, 0.4);
}
.durum-cip.cevrimdisi {
  background: rgba(239, 68, 68, 0.2);
  color: #fee2e2;
  border-color: rgba(239, 68, 68, 0.45);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.saha-hizli-buton,
.saha-ikon-buton,
.saha-baglanti-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}
.saha-ikon-buton {
  width: 44px;
  padding: 0;
}
.saha-hizli-buton:focus-visible,
.saha-ikon-buton:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 2px;
}
.saha-ikon-buton:disabled {
  opacity: 0.6;
  cursor: progress;
}

.saha-ozet-alani {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.saha-ozet-hata {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 12px;
  color: var(--danger);
}
.saha-baglanti-buton {
  min-height: 32px;
  padding: 0 10px;
  border-color: var(--border);
  background: var(--bg-card);
  color: var(--accent);
}

/* Segmented sekmeler: mobilde yatay kaydırma, 44px hedef. */
.saha-segmentler {
  display: flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-secondary);
  overflow-x: auto;
  scrollbar-width: none;
}
.saha-segmentler::-webkit-scrollbar {
  display: none;
}
.saha-segment {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1 0 auto;
  min-height: 44px;
  padding: 0 12px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}
.saha-segment--aktif {
  background: var(--bg-card);
  color: var(--accent);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}
.saha-segment:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.saha-sekme-govde {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* Talep alt sekmeleri (masraf/izin): her biri tek istekle yüklenir. */
.saha-alt-segmentler {
  display: flex;
  gap: 6px;
  min-width: 0;
}
.saha-alt-segment {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1 1 0;
  min-height: 44px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  min-width: 0;
}
.saha-alt-segment--aktif {
  border-color: var(--accent-border, var(--accent));
  color: var(--accent);
  background: var(--accent-soft, var(--bg-secondary));
}
.saha-alt-segment:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.saha-teslimat-liste {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.saha-teslimat-satir {
  position: relative;
  min-width: 0;
}
.saha-siralama {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 4px;
}
.saha-siralama-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  min-height: 36px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
}
.saha-siralama-buton:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.saha-siralama-ipucu {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 11.5px;
  color: var(--text-muted);
}

.saha-iskelet-liste {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.saha-iskelet-kart {
  height: 108px;
  border-radius: 14px;
  background: var(--bg-secondary);
  animation: saha-iskelet-nabiz 1.4s ease-in-out infinite;
}
@keyframes saha-iskelet-nabiz {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 0.9;
  }
}

.saha-ziyaret-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.saha-ziyaret-satir {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  min-width: 0;
}
.saha-ziyaret-baslik {
  font-size: 12.5px;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}
.saha-ziyaret-tarih {
  font-size: 11px;
  color: var(--text-muted);
  flex: 0 0 auto;
}

.tab-badge {
  background: #ef4444;
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 9999px;
}

.tahsilat-ipucu {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: -0.4rem;
  font-size: 0.75rem;
  color: var(--success, #22c55e);
}
.tahsilat-ipucu i {
  font-size: 0.8rem;
}

.expense-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.expense-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 0.875rem;
  padding: 1rem 1.25rem;
  min-width: 0;
}

.expense-left {
  min-width: 0;
}

.expense-type-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--accent-soft);
  color: var(--accent);
}

.expense-desc {
  font-size: 0.85rem;
  margin: 0.25rem 0;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}

.expense-date {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.expense-amount {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--primary-color, var(--accent));
  white-space: nowrap;
}

.tab-pill-group {
  display: flex;
  border-radius: 0.625rem;
  border: 1px solid var(--border);
  overflow: hidden;
  margin-bottom: 0.75rem;
}

.pill-btn {
  flex: 1;
  min-height: 44px;
  padding: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  border: none;
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
}

.pill-btn.active {
  background: var(--primary-color, var(--accent));
  color: white;
}

.stok-raf-bilgi {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}
.sepet-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--bg-muted, rgba(0, 0, 0, 0.04));
  border-radius: 10px;
  padding: 8px 10px;
}
.sepet-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}
.sepet-ad {
  flex: 1;
  min-width: 0;
}
.sepet-tutar {
  font-weight: 700;
}
.sepet-sil {
  border: none;
  background: transparent;
  color: var(--danger, #ef4444);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
}
.sepet-toplam {
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px solid var(--border);
  font-size: 12.5px;
  font-weight: 700;
  text-align: right;
}
.foto-sec-etiket {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px dashed var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.foto-sec-etiket:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* GPS konum kutusu: sabit renk siniflari yerine tema token'lari. */
.gps-location-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-muted, rgba(0, 0, 0, 0.03));
}
.gps-bilgi {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.gps-ikon {
  font-size: 1.1rem;
}
.gps-metin {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

/* Masraf/izin kart ust satiri. */
.expense-ustsatir {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}
.izin-tur {
  font-size: 1rem;
  color: var(--primary-color, var(--accent));
}
.izin-takvim-ikon {
  color: var(--primary-color, var(--accent));
  margin-right: 4px;
}
.izin-gun {
  font-weight: 700;
  color: var(--primary-color, var(--accent));
  margin-left: 4px;
}

/* Cevrimdisi tahsilat uyarisi. */
.tahsilat-offline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger, #ef4444);
  font-size: 0.78rem;
  font-weight: 600;
}

.saha-baslik-satir--ikinci {
  margin-top: 8px;
}

/* Mobil: saha portalı telefondan kullanılır. */
@media (max-width: 640px) {
  .saha-header-content {
    flex-direction: row;
    align-items: center;
  }
  .header-actions {
    flex: 0 0 auto;
  }
  .saha-hizli-buton span {
    display: none;
  }
  .saha-hizli-buton {
    width: 44px;
    padding: 0;
  }
  .expense-card {
    padding: 12px;
  }
  .expense-amount {
    font-size: 1rem;
  }
}
@media (prefers-reduced-motion: reduce) {
  .saha-iskelet-kart {
    animation: none;
  }
}
</style>
