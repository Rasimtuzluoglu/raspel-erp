<template>
  <div
    class="pos-container"
    :class="{ 'pos-buyuk': buyukYazi }"
  >
    <div class="pos-header">
      <div class="pos-marka">
        <span class="pos-marka-ikon"><i class="pi pi-bolt" /></span>
        <div class="pos-marka-metin">
          <strong>{{ t('hizliSatis.breadcrumb') }}</strong>
          <small>{{ authStore?.sirketAdi || 'RasPel ERP' }}</small>
        </div>
      </div>
      <div class="pos-header-sag">
        <div
          v-if="authStore?.kullanici"
          class="user-info"
        >
          <i class="pi pi-user" /> {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username }}
        </div>
        <button
          type="button"
          class="pos-ikon-btn bugunku-btn"
          :title="t('hizliSatis.bugunkuSatislar', { n: gunlukSatislar.length })"
          :aria-label="t('hizliSatis.bugunkuSatislar', { n: gunlukSatislar.length })"
          @click="bugunkuDialog = true"
        >
          <i class="pi pi-clock" />
          <span
            v-if="gunlukSatislar.length > 0"
            class="bugunku-rozet"
          >{{ gunlukSatislar.length }}</span>
        </button>
        <button
          type="button"
          class="pos-tercih-btn"
          :class="{ aktif: buyukYazi || onayIste }"
          :title="t('hizliSatis.tercihler')"
          :aria-label="t('hizliSatis.tercihler')"
          @click="tercihPopover.toggle($event)"
        >
          <i class="pi pi-cog" />
        </button>
        <Popover ref="tercihPopover">
          <div class="tercih-panel">
            <div class="tercih-baslik">
              {{ t('hizliSatis.tercihler') }}
            </div>
            <!-- Tercih anahtarlari: v-model ile baglanir; kalicilik `usePosTercih`
     icindeki `watch` tarafindan otomatik yapildigi icin ayri @change gerekmez. -->
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.buyukYazi') }}</span>
              <ToggleSwitch v-model="buyukYazi" />
            </label>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.onayIste') }}</span>
              <ToggleSwitch v-model="onayIste" />
            </label>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.otomatikYazdir') }}</span>
              <ToggleSwitch v-model="otomatikYazdir" />
            </label>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.kisayolIpucu') }}</span>
              <ToggleSwitch v-model="ipucuAcik" />
            </label>
          </div>
        </Popover>
        <button
          type="button"
          class="pos-ipucu-btn"
          :title="t('hizliSatis.kisayolIpucu')"
          :aria-label="t('hizliSatis.kisayolIpucu')"
          :aria-pressed="ipucuAcik"
          @click="ipucuToggle"
        >
          <i class="pi pi-question-circle" />
        </button>
      </div>
    </div>

    <div
      v-if="ipucuAcik"
      class="pos-ipucu"
    >
      <span><kbd>F1</kbd> {{ $t('hizliSatis.barkod') }}</span>
      <span><kbd>F2</kbd> {{ $t('hizliSatis.temizle') }}</span>
      <span><kbd>F3</kbd> {{ $t('hizliSatis.ipucuUrunAra') }}</span>
      <span><kbd>↓</kbd> {{ $t('hizliSatis.ipucuIzgara') }}</span>
      <span><kbd>F4</kbd> {{ $t('hizliSatis.musteri') }}</span>
      <span><kbd>F5</kbd> {{ $t('hizliSatis.ipucuYeniMusteri') }}</span>
      <span><kbd>F6</kbd> {{ $t('hizliSatis.ipucuKamera') }}</span>
      <span><kbd>F7</kbd> {{ $t('hizliSatis.ipucuBugunkuSatis') }}</span>
      <span><kbd>F8</kbd> {{ $t('hizliSatis.ipucuYazdir') }}</span>
      <span><kbd>F11</kbd> {{ $t('hizliSatis.ipucuTermal') }}</span>
      <span><kbd>F9</kbd>/<kbd>F10</kbd> {{ $t('hizliSatis.odeme') }}</span>
      <span><kbd>N</kbd>/<kbd>K</kbd>/<kbd>H</kbd>/<kbd>T</kbd> {{ $t('hizliSatis.ipucuYontem') }}</span>
      <span><kbd>G</kbd>/<kbd>Ctrl+Z</kbd> {{ $t('hizliSatis.ipucuGeriAl') }}</span>
      <span><kbd>D</kbd> {{ $t('hizliSatis.ipucuSatirCogalt') }}</span>
      <span><kbd>P</kbd> {{ $t('hizliSatis.ipucuFisModu') }}</span>
      <span><kbd>↑</kbd>/<kbd>↓</kbd> {{ $t('hizliSatis.ipucuSatir') }}</span>
      <span><kbd>Alt+↑/↓</kbd> {{ $t('hizliSatis.miktar') }}</span>
      <span><kbd>Del</kbd> {{ $t('hizliSatis.ipucuSil') }}</span>
      <button
        type="button"
        class="pos-ipucu-kapat"
        :aria-label="$t('common.close')"
        @click="ipucuKapat"
      >
        <i class="pi pi-times" />
      </button>
    </div>

    <div class="pos-body">
      <div class="pos-left">
        <div class="pos-arac-cubugu">
          <!-- REDTEAM/Faz3.3: `p-input-icon-left` PrimeVue 4'te kaldirildi; Faz 2'de
           ayrica `:deep()` eksikligi duzeltildi. Simdi dogrustru API. -->
          <IconField class="arama-kutusu barkod-kutu">
            <InputIcon class="pi pi-barcode" />
            <InputText
              ref="barkodInputRef"
              v-model="globalBarkod"
              :placeholder="t('hizliSatis.barkodPlaceholder')"
              class="w-full"
              autofocus
              @keyup.enter="globalBarkodEkle"
            />
            <button
              type="button"
              class="alan-ikon-sag"
              :title="t('hizliSatis.barkodKamera')"
              :aria-label="t('hizliSatis.barkodKamera')"
              @click="scannerAcik = true"
            >
              <i class="pi pi-camera" />
            </button>
          </IconField>
          <!-- REDTEAM/Faz3.3: `p-input-icon-left` -> IconField/InputIcon. -->
          <IconField class="arama-kutusu">
            <InputIcon class="pi pi-search" />
            <!-- Sunucu taraflı yazarken arama (typeahead). Önceden bu kutu kart
                 ızgarasını yalnızca YÜKLÜ 50 üründe filtreliyordu; katalog
                 büyüdükçe ürünler hiç bulunamıyordu.
                 REDTEAM/Faz3: bu kutu AYNI ARAMA YUZEYININ bir parcasi;
                 yazilan metin `katalogArama`ya gider ve kart izgarasi sunucu
                 tarafi aramayla yenilenir. Öneri listesi ayrica tam barkod/
                 stok kodu eslesmesi icin sunucudan gelir. -->
            <AutoComplete
              ref="urunAraAutoRef"
              v-model="urunOneri"
              :suggestions="urunOnerileri"
              option-label="ad"
              :placeholder="t('hizliSatis.aramaPlaceholder')"
              class="w-full"
              :min-length="2"
              :delay="0"
              :force-selection="false"
              :panel-style="URUN_PANEL_STILI"
              :scroll-height="'320px'"
              @input="katalogArama = $event.value ?? $event"
              @complete="urunOneriAra"
              @option-select="urunOneriSecildi"
            >
              <!--
                REDTEAM/Faz2.2: `dropdown` (ok dugmesi) KALDIRILDI.
                `dropdown` + `min-length="2"` birlikte kullanildiginda ok
                ISLEVSIZDI: ok'a tiklaninca PrimeVue `onDropdownClick`
                `search(event, '', 'dropdown')` cagirir ve `@complete`
                `query: ''` ile tetiklenir. `urunOneriAra` `q.length < 2`
                oldugu icin listeyi BOSALTIR; `suggestions` watcher'i
                `searching=true` iken bos listeyi gorup `!visibleOptions
                .length && hide()` calistirir. Sonuc: panel acilir, 1 tick
                sonra kendini kapatir.
                `minLength=2` ile ok zaten anlamsiz (bos sorguda ne
                gosterilecek?); kullanicinin yazmaya baslamasi zaten
                yeterli sinyal.
              -->
              <template #option="slotProps">
                <div class="urun-oneri">
                  <span class="urun-oneri-ad">{{ slotProps.option.ad }}</span>
                  <span class="urun-oneri-kod">{{ slotProps.option.stokKodu || slotProps.option.barkod || '' }}</span>
                  <span class="urun-oneri-fiyat">{{ formatCurrency(satisFiyati(slotProps.option)) }}</span>
                </div>
              </template>
            </AutoComplete>
          </IconField>
          <button
            type="button"
            class="filtre-btn"
            :class="{ 'filtre-aktif': aktifFiltreSayisiPos > 0 }"
            :title="t('hizliSatis.filtreler')"
            :aria-label="t('hizliSatis.filtreler')"
            @click="filtrePopover.toggle($event)"
          >
            <i class="pi pi-filter" />
            <span class="filtre-btn-metin">{{ t('hizliSatis.filtreler') }}</span>
            <span
              v-if="aktifFiltreSayisiPos > 0"
              class="filtre-rozet"
            >{{ aktifFiltreSayisiPos }}</span>
          </button>
          <Popover ref="filtrePopover">
            <div class="filtre-panel">
              <div class="filtre-panel-baslik">
                {{ t('hizliSatis.filtreler') }}
              </div>
              <!-- Secenekler urun kartlarindaki degerlerden turetilir; ayrica
                   kategori/stok grubu tanimi yapmak gerekmez. -->
              <div class="filtre-alan">
                <label>{{ t('hizliSatis.kategori') }}</label>
                <Dropdown
                  v-model="filtreKategori"
                  :options="kategoriler"
                  :placeholder="t('hizliSatis.tumu')"
                  class="w-full"
                  show-clear
                />
              </div>
              <div class="filtre-alan">
                <label>{{ t('hizliSatis.marka') }}</label>
                <Dropdown
                  v-model="filtreMarka"
                  :options="markalar"
                  :placeholder="t('hizliSatis.tumu')"
                  class="w-full"
                  show-clear
                />
              </div>
              <div class="filtre-alan">
                <label>{{ t('hizliSatis.stokGrubu') }}</label>
                <Dropdown
                  v-model="filtreStokGrubu"
                  :options="stokGruplari"
                  :placeholder="t('hizliSatis.tumu')"
                  class="w-full"
                  show-clear
                />
              </div>
              <label class="filtre-alan filtre-alan-toggle">
                <span>{{ t('hizliSatis.sadeceStokta') }}</span>
                <ToggleSwitch v-model="sadeceStokta" />
              </label>
            </div>
          </Popover>
        </div>

        <!-- Aktif filtreler: tek tıkla temizlenebilir çipler -->
        <div
          v-if="aktifFiltreSayisiPos > 0"
          class="aktif-filtreler"
        >
          <span
            v-if="filtreKategori"
            class="aktif-filtre-cip"
          >
            <i class="pi pi-tag" /> {{ filtreKategori }}
            <button
              type="button"
              :aria-label="t('hizliSatis.filtreKaldir')"
              @click="filtreKategori = null"
            >
              <i class="pi pi-times" />
            </button>
          </span>
          <span
            v-if="filtreMarka"
            class="aktif-filtre-cip"
          >
            <i class="pi pi-star" /> {{ filtreMarka }}
            <button
              type="button"
              :aria-label="t('hizliSatis.filtreKaldir')"
              @click="filtreMarka = null"
            >
              <i class="pi pi-times" />
            </button>
          </span>
          <span
            v-if="filtreStokGrubu"
            class="aktif-filtre-cip"
          >
            <i class="pi pi-box" /> {{ filtreStokGrubu }}
            <button
              type="button"
              :aria-label="t('hizliSatis.filtreKaldir')"
              @click="filtreStokGrubu = null"
            >
              <i class="pi pi-times" />
            </button>
          </span>
          <button
            type="button"
            class="aktif-filtre-temizle"
            @click="filtreTemizle"
          >
            {{ t('hizliSatis.filtreTemizle') }}
          </button>
        </div>

        <div
          v-if="cokSatanlar.length > 0"
          class="cok-satanlar-section"
        >
          <div class="product-header">
            <h3><i class="pi pi-star-fill" /> {{ t('hizliSatis.cokSatanlar') }}</h3>
          </div>
          <div class="cok-satanlar-grid">
            <button
              v-for="u in cokSatanlar"
              :key="u.id"
              type="button"
              class="cok-satan-chip"
              :class="{ 'stok-yok': stokYokMu(u) }"
              :disabled="stokYokMu(u)"
              @click="sepeteEkle(u)"
            >
              <span class="cok-satan-ad">{{ u.ad }}</span>
              <span class="cok-satan-fiyat">{{ formatCurrency(satisFiyati(u)) }}</span>
            </button>
          </div>
        </div>

        <div class="product-section">
          <div class="product-header">
            <h3>
              <i class="pi pi-box" />
              {{ t('hizliSatis.mevcutUrunler') }}
              <!--
                REDTEAM/Faz2.4: Sayaç yalnızca YÜKLÜ ve FİLTRELENMİŞ ürün
                sayısını gösteriyordu (en fazla 50), kullanıcı katalogda 4.000
                ürün olduğunu sanıp "ürün yok" diye kasaya giriyordu.
                Filtre aktifken filtrelenmiş sayı, filtre yoksa sunucudan
                gelen gerçek toplam gösterilir.
              -->
              <!-- Sonuc sayaci: katalog artik sunucudan SAYFALI geliyor.
                   Once yerel `stokStore.stoklar.length` gosteriliyordu; bu
                   yalniz yuklenen sayfayi sayar ve "200 urun" tavani gercek
                   toplamla karistirilirdi. Artik sunucunun `totalElements`
                   degeri kullanilir. -->
              <span class="urun-sayaci">{{
                katalogSonucOzeti != null ? katalogSonucOzeti : katalogToplam
              }}</span>
            </h3>
            <div class="product-header-sag">
              <span class="siralama-etiket">{{ t('hizliSatis.sirala') }}</span>
              <Dropdown
                v-model="siralama"
                :options="siralamaSecenekleri"
                option-label="label"
                option-value="value"
                class="siralama-dropdown"
              />
            </div>
          </div>

          <!-- Kategori hızlı filtre çipleri. Sayaçlar TÜM katalogdan gelir
               (`gruplama-dagilimi`); once yalnizca yuklenen 200 urunden
               turetiyordu ve yanlis sayi gosteriyordu. -->
          <div
            v-if="kategoriCipleri.length > 1"
            class="kategori-cipler"
          >
            <button
              type="button"
              class="kategori-cip"
              :class="{ aktif: !filtreKategori }"
              @click="filtreKategori = null"
            >
              {{ t('hizliSatis.tumu') }}
            </button>
            <button
              v-for="k in kategoriCipleri"
              :key="k.deger"
              type="button"
              class="kategori-cip"
              :class="{ aktif: filtreKategori === k.deger }"
              @click="filtreKategori = k.deger"
            >
              {{ k.deger }}
              <span class="kategori-cip-adet">{{ k.adet }}</span>
            </button>
            <button
              v-if="gizliKategoriSayisi > 0 || tumKategorilerGoster"
              type="button"
              class="kategori-cip kategori-cip-daha"
              :class="{ aktif: tumKategorilerGoster }"
              @click="tumKategorilerGoster = !tumKategorilerGoster"
            >
              {{ tumKategorilerGoster ? t('hizliSatis.dahaAz') : '+' + gizliKategoriSayisi }}
            </button>
          </div>

          <div
            class="product-grid"
            :class="{ 'izgara-odak': urunIzgaraOdak }"
            role="listbox"
            :aria-label="t('hizliSatis.urunler')"
            :aria-busy="katalogYukleniyor"
          >
            <!-- Izgara klavyeyle gezilebilir (bir metin alanindayken ↓ ya da F3
                   sonrasi ↓). ↑↓←→ kart secar, Enter sepete ekler, rakamlar + Enter
                   miktari belirler, Shift+Enter adet penceresini acar, Esc izgaradan
                   cikip barkod alanina doner. Once izgara YALNIZCA fareyle
                   kullanilabiliyordu: ok tuslari SEPETI geziyordu ve urun karti hicbir
                   zaman odaklanamiyordu. -->
            <PosUrunKarti
              v-for="(u, i) in gorunenUrunler"
              :key="u.id"
              :urun="u"
              :cari-fiyat="cariFiyati(u.id)"
              :sepette-adet="sepetteAdet(u.id)"
              :odakli="urunIzgaraOdak && i === urunIzgaraIndeks"
              :tabindex="urunIzgaraOdak ? (i === urunIzgaraIndeks ? 0 : -1) : undefined"
              :dom-id="urunKartDomId(i)"
              @sec="urunKartiTikla(u)"
              @adet-ist="(olay) => adetPopoverAc(u, i, olay)"
            />

            <!-- REDTEAM/Faz3: Yukleme / bos / hata AYRI gosterilir. Once ucu de
                 ayniydi: istek sirasinda izgara bos gorunup "urun bulunamadi"
                 yaziyordu, hata halinde de ayni mesaj cikiyordu. Boylece kasiyer
                 "urun yok" sanip malzeme aramaya devam ediyordu. -->
            <div
              v-if="katalogYukleniyor && !gorunenUrunler.length"
              class="product-durum"
            >
              <i class="pi pi-spin pi-spinner" />
              <p>{{ t('hizliSatis.katalogYukleniyor') }}</p>
            </div>
            <div
              v-else-if="katalogHatasi"
              class="product-durum product-durum-hata"
            >
              <i class="pi pi-exclamation-triangle" />
              <p>{{ t('hizliSatis.katalogYuklenemedi') }}</p>
              <Button
                :label="t('common.refresh')"
                icon="pi pi-refresh"
                class="p-button-outlined p-button-sm"
                @click="katalogYukle"
              />
            </div>
            <div
              v-else-if="katalogBos"
              class="product-durum"
            >
              <i class="pi pi-inbox" />
              <p>{{ t('hizliSatis.urunBulunamadi') }}</p>
            </div>
          </div>
          <div
            v-if="katalogDahaFazlaVar"
            class="daha-fazla"
          >
            <!-- Sonraki sayfa sunucudan istenir. Once buton yerel listeyi 60'ar
                 buyutuyordu; katalog 200 urunle sinirli oldugu icin "daha fazla"
                 200'de bitiyordu. -->
            <Button
              :label="t('hizliSatis.dahaFazlaGoster', {
                n: Math.min(60, Math.max(0, katalogToplam - gorunenUrunler.length))
              })"
              icon="pi pi-angle-down"
              class="p-button-outlined"
              :loading="katalogYukleniyor"
              @click="dahaFazlaYukle"
            />
          </div>
          <!-- Adet penceresi: kart uzerinde sag tik veya izgarada Shift+Enter.
               Tiklanan noktanin yanina konumlanir (`adetPopoverStil`); once
               sabit yerde acildigi icin hangi karta ait oldugu belirsizdi. -->
          <div
            v-if="adetPopoverUrun"
            class="adet-popover"
            role="dialog"
            :style="adetPopoverStil"
            :aria-label="t('hizliSatis.adetSec')"
          >
            <div class="adet-popover-ust">
              <span class="adet-popover-ad">{{ adetPopoverUrun.ad }}</span>
              <span class="adet-popover-stok">
                {{ t('hizliSatis.stokAdet', { n: adetPopoverUrun.miktar, birim: adetPopoverUrun.birim || t('hizliSatis.adetBirimi') }) }}
              </span>
            </div>
            <div class="adet-popover-govde">
              <button
                type="button"
                class="adet-btn"
                :aria-label="t('hizliSatis.miktarAzalt')"
                @click="adetPopoverOnayla = Math.max(1, (adetPopoverOnayla || 1) - 1)"
              >
                <i class="pi pi-minus" />
              </button>
              <InputNumber
                v-model="adetPopoverOnayla"
                :min="1"
                class="adet-popover-girdi"
                @keyup.enter="adetPopoverOnaylandi"
              />
              <button
                type="button"
                class="adet-btn"
                :aria-label="t('hizliSatis.miktarArtir')"
                @click="adetPopoverOnayla = (adetPopoverOnayla || 1) + 1"
              >
                <i class="pi pi-plus" />
              </button>
              <Button
                :label="t('common.add')"
                size="small"
                class="p-button-success"
                @click="adetPopoverOnaylandi"
              />
              <Button
                :label="t('common.cancel')"
                size="small"
                severity="secondary"
                text
                @click="adetPopoverKapat"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="pos-right">
        <Card class="siparis-kart">
          <template #content>
            <PosMusteriPaneli
              ref="musteriPaneliRef"
              v-model:musteri-acik="musteriAcik"
              v-model:musteri-modu="musteriModu"
              v-model:musteri-giris="musteriGiris"
              v-model:teslimat-acik="teslimatAcik"
              v-model:secili-sofor="seciliSofor"
              v-model:teslimat-adresi="teslimatAdresi"
              v-model:teslim-durumu="teslimDurumu"
              v-model:teslim-notu="teslimNotu"
              :hatalar="hatalar"
              :musteri-modlari="musteriModlari"
              :musteri-onerileri="musteriOnerileri"
              :secili-musteri="seciliMusteri"
              :musteri-bakiye-uyarisi="musteriBakiyeUyarisi"
              :degisim-iade-id="degisimIadeId"
              :soforler="soforler"
              :soforler-yukleniyor="soforlerYukleniyor"
              :teslim-durum-secenekleri="teslimDurumSecenekleri"
              @musteri-ara="musteriAra"
              @musteri-sec="musteriSec"
              @musteri-temizle="musteriTemizle"
              @degisim-kapat="degisimIadeId = null"
              @yeni-musteri="yeniMusteriDialog = true"
            />

            <PosSepetPaneli
              ref="sepetListeRef"
              v-model:indirim-tipi="indirimTipi"
              v-model:indirim-degeri="indirimDegeri"
              :sepet="sepet"
              :acik="sepetAcik"
              :aktif-satir="aktifSatir"
              :vurgulu-id="vurguluId"
              :suruklenen-idx="suruklenenIdx"
              :geri-al-sepet="geriAlSepet"
              :geri-al-satir="!!geriAlSatir"
              :urun-degistir-satir="urunDegistirSatir"
              :urun-onerileri="urunDegistirOnerileri"
              :kayitli-sepet-var="kayitliSepetVar"
              :detay-acik="detayAcik"
              :toplam-ft3="toplamFt3"
              :toplam="toplam"
              :genel-toplam="genelToplam"
              :musteri-adi="seciliMusteri?.ad || ''"
              :indirim-tipleri="indirimTipleri"
              @toggle="sepetAcikDegistir"
              @kaydet="sepetKaydet"
              @yukle="sepetYukle"
              @temizle="sepetiGeriAlinabilirTemizle()"
              @geri-al="sepetGeriAl"
              @satir-geri-al="geriAlSatirYap"
              @urun-degistir-ac="satirAktifYap(urunDegistirAc)"
              @urun-degistir-ara="urunDegistirAra"
              @urun-degistir-sec="urunDegistirSec"
              @urun-degistir-vazgec="urunDegistirKapat"
              @sil="satirAktifYap(sepetSil)"
              @cogalt="satirAktifYap(satiriCogalt)"
              @adedi-sifirla="satirAktifYap(adediSifirla)"
              @miktar-azalt="satirAktifYap(miktarAzalt)"
              @miktar-artir="satirAktifYap((i) => miktarDegistir(i, 1, 'adim'))"
              @miktar-degistir="({ idx, miktar }) => { aktifSatir = idx; miktarDegistir(idx, miktar) }"
              @satir-sec="(i) => (aktifSatir = i)"
              @surukleme-basla="suruklemeBasla"
              @surukleme-uzerine="suruklemeUzerine"
              @surukleme-birak="suruklemeBirak"
              @surukleme-bitir="suruklemeBitir"
              @fiyat-tipi-degisti="fiyatTipiDegisti"
              @detay-toggle="detayAclicDegistir"
            />

            <PosOdemePaneli
              v-model:odeme-durumu="odemeDurumu"
              v-model:odeme-yontemi="odemeYontemi"
              v-model:odenen-tutar="odenenTutar"
              v-model:alinan-nakit="alinanNakit"
              v-model:taksit-kurum="taksitKurum"
              v-model:taksit-tutar="taksitTutar"
              v-model:taksit-sayisi="taksitSayisi"
              v-model:secili-kasa="seciliKasa"
              v-model:secili-banka="seciliBanka"
              v-model:secili-pos="seciliPos"
              :acik="odemeAcik"
              :odeme-tipleri="odemeTipleri"
              :odeme-yontemleri="odemeYontemleri"
              :para-ustu="paraUstu"
              :hizli-nakit="hizliNakit"
              :kasalar="kasalar"
              :bankalar="bankalar"
              :pos-terminalleri="posTerminalleri"
              :secili-pos-bilgi="seciliPosBilgi"
              :hesaplanan-komisyon="hesaplananKomisyon"
              :genel-toplam="genelToplam"
              :kalan-tutar="kalanTutar"
              :odeme-durum-text="odemeDurumText"
              :odeme-durum-severity="odemeDurumSeverity"
              :hatalar="hatalar"
              @toggle="odemeAcikDegistir"
            />

            <div class="sticky-tamamla">
              <div class="fis-modu-satir">
                <span class="fis-modu-etiket"><i class="pi pi-print" /> {{ t('hizliSatis.fisModu') }}</span>
                <SelectButton
                  v-model="fisFiyatliGecici"
                  :options="fisModuSecenekleri"
                  option-label="label"
                  option-value="value"
                  size="small"
                  :allow-empty="false"
                />
              </div>
              <div class="sticky-tutar">
                <span>{{ t('hizliSatis.genelToplam') }}</span>
                <strong>{{ formatCurrency(genelToplam) }}</strong>
              </div>
              <Button
                :label="t('hizliSatis.satisiTamamla')"
                icon="pi pi-check"
                class="p-button-success w-full satis-buton"
                :loading="kaydediliyor"
                :disabled="sepet.length === 0 || (!anlikMusteri && !seciliMusteri)"
                @click="satisiTamamla"
              />
              <Button
                v-if="sonSatis"
                :label="t('hizliSatis.sonSatisiIptal')"
                icon="pi pi-undo"
                class="p-button-outlined p-button-danger w-full"
                @click="sonSatisiIptalEt"
              />
            </div>

            <!-- Sepet bos olsa bile `sonSatis` varsa gosterilir: sattiktan sonra fisin
                 YENIDEN yazdirilmasi bu bolum uzerinden yapilir. Once yalnizca
                 `sepet.length > 0` ile aciliyordu, yani satis bittikten sonra
                 fiy yeniden basilamiyordu (fare ile bile: bolum kayboluyordu). -->
            <details
              v-if="sepet.length > 0 || sonSatis"
              ref="fisDetayRef"
              class="fis-detay pos-bolum"
              @toggle="fisAclic = $event.target.open"
            >
              <summary class="fis-baslik-satir">
                <span
                  class="katlanir-sol fis-detay-ozet"
                >
                  <i class="pi pi-print" /> {{ t('hizliSatis.fisOnizleme') }}
                  <i class="pi katlanir-ok pi-chevron-down" />
                </span>
                <div
                  class="fis-ayarlar"
                  @click.prevent.stop
                >
                  <Button
                    :label="t('hizliSatis.yazdirF8')"
                    icon="pi pi-print"
                    size="small"
                    @click="fisiYazdir(sonSatis?.faturaNumarasi, satisOzet?.fisModu)"
                  />
                  <Button
                    :label="t('hizliSatis.termalF11')"
                    icon="pi pi-send"
                    size="small"
                    severity="secondary"
                    outlined
                    @click="termalYazdir"
                  />
                </div>
              </summary>
              <div class="fis-onizleme-kapsam">
                <PosFisOnizleme
                  :sirket-logosu="sirketLogosu"
                  :sirket-adi="sirketAdi"
                  :simdiki-tarih="simdikiTarih"
                  :fis-no="fisNo"
                  :musteri-adi="musteriAdi"
                  :sepet="sepet"
                  :fis-fiyatli="fisFiyatli"
                  :toplam="toplam"
                  :indirim-degeri="indirimDegeri"
                  :indirim-tipi="indirimTipi"
                  :indirim-tutari="indirimTutari"
                  :genel-toplam="genelToplam"
                  :odenen-tutar="odenenTutar"
                  :kalan-tutar="kalanTutar"
                  :odeme-durum-text="odemeDurumText"
                  :fis-alt-notu="fisAltNotu"
                />
              </div>
            </details>
          </template>
        </Card>
      </div>
    </div>
  </div>


  <Dialog
    v-model:visible="yeniMusteriDialog"
    :header="t('hizliSatis.yeniCari')"
    :modal="true"
    :style="{ width: '520px' }"
    class="yeni-musteri-dialog"
  >
    <div class="ym-form-grid">
      <div class="field full-width">
        <label for="ym-ad">{{ t('hizliSatis.adFirma') }} <span class="required">*</span></label>
        <InputText
          id="ym-ad"
          v-model="yeniMusteri.ad"
          :placeholder="t('hizliSatis.adFirmaPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-telefon">{{ t('hizliSatis.telefon') }}</label>
        <InputText
          id="ym-telefon"
          v-model="yeniMusteri.telefon"
          placeholder="05XX XXX XX XX"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-email">{{ t('hizliSatis.eposta') }}</label>
        <InputText
          id="ym-email"
          v-model="yeniMusteri.email"
          placeholder="ornek@domain.com"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-vergi">{{ t('hizliSatis.vergiNo') }}</label>
        <InputText
          id="ym-vergi"
          v-model="yeniMusteri.vergiNo"
          :placeholder="t('hizliSatis.vergiPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-tur">{{ t('hizliSatis.cariTuru') }}</label>
        <Dropdown
          id="ym-tur"
          v-model="yeniMusteri.tur"
          :options="cariTurSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="$t('hizliSatis.musteriPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="field full-width">
        <label for="ym-adres">{{ t('hizliSatis.adres') }}</label>
        <Textarea
          id="ym-adres"
          v-model="yeniMusteri.adres"
          rows="2"
          :placeholder="t('hizliSatis.adresPlaceholder')"
          class="w-full"
        />
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer-btns">
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="yeniMusteriDialog = false"
        />
        <Button
          :label="t('hizliSatis.kaydetSec')"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="musteriKaydediliyor"
          @click="musteriKaydet"
        />
      </div>
    </template>
  </Dialog>

  <BarcodeScannerModal
    v-model:visible="scannerAcik"
    @scan="barkodTarandi"
  />

  <Dialog
    v-model:visible="onayDialog"
    :header="t('hizliSatis.onayBaslik')"
    :modal="true"
    style="width: 380px"
  >
    <div class="satis-onay">
      <i class="pi pi-question-circle satis-onay-ikon" />
      <p class="satis-onay-metin">
        {{ t('hizliSatis.onayMetin') }}
      </p>
      <p class="satis-onay-tutar">
        {{ t('hizliSatis.toplam') }}: <strong>{{ formatCurrency(genelToplam) }}</strong>
      </p>
    </div>
    <template #footer>
      <Button
        :label="t('hizliSatis.onayIptal')"
        severity="secondary"
        text
        @click="satisOnayIptal"
      />
      <Button
        :label="t('hizliSatis.onayOnayla')"
        icon="pi pi-check"
        @click="satisOnayla"
      />
    </template>
  </Dialog>

  <PosBugunkuSatislarDialog
    :visible="bugunkuDialog"
    :satislar="gunlukSatislar"
    @update:visible="bugunkuDialog = $event"
    @yenile="gunlukSatislariYukle"
    @goruntule="bugunkuSatisGoruntule"
  />

  <PosSatisOzetDialog
    :visible="satisOzetDialog"
    :satis-ozet="satisOzet"
    @update:visible="satisOzetDialog = $event"
    @yeni-satis="yeniSatisaBasla"
  />

  <Dialog
    v-model:visible="hizliUrunDialog"
    :header="t('hizliSatis.hizliUrunBaslik')"
    :modal="true"
    style="width: 420px"
  >
    <div class="form-grup">
      <label>{{ t('stoklar.barkod') }}</label>
      <InputText
        v-model="hizliUrun.barkod"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ t('hizliSatis.urunAdi') }}</label>
      <InputText
        v-model="hizliUrun.ad"
        class="w-full"
        autofocus
      />
    </div>
    <div class="kurulum-iki-kolon">
      <div class="form-grup">
        <label>{{ t('stoklar.satisFiyati') }}</label>
        <InputNumber
          v-model="hizliUrun.fiyat"
          mode="currency"
          currency="TRY"
          locale="tr-TR"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('hizliSatis.miktar') }}</label>
        <InputNumber
          v-model="hizliUrun.miktar"
          :min="0"
          class="w-full"
        />
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        class="p-button-text"
        @click="hizliUrunDialog = false"
      />
      <Button
        :label="t('hizliSatis.kaydetSec')"
        icon="pi pi-check"
        :loading="hizliUrunKaydediliyor"
        @click="hizliUrunKaydet"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useMarka } from '../composables/useMarka.js'
import { useI18n } from 'vue-i18n'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'

// REDTEAM/Faz2.2: Oneri panelinin stilini SABIT referansla ver.
// previously `:panel-style="{ minWidth: '380px' }"` (template literal) her
// render'da YENI bir obje olusturuyordu. PrimeVue `alignOverlay` overlay'i
// acilista input genisligine yazar, sonraki parent re-render'inda (yani
// kullanicinin ilk harfi tikladiginda) Vue `patchStyle` deger farkini
// gozemedigi icin 380px'e geri yaziyordu -> panel acilir, kullanici ilk
// harfi yazinca 380px'e "snap" olup zipliyordu.
// Sabit referans ayni isi her render'da tekrarliyor; bkz. FaturaKalemleri.vue
// (PANEL_STILI deseni).
const URUN_PANEL_STILI = Object.freeze({ minWidth: '380px' })
import PosFisOnizleme from '../components/PosFisOnizleme.vue'
import PosSatisOzetDialog from '../components/PosSatisOzetDialog.vue'
import PosBugunkuSatislarDialog from '../components/PosBugunkuSatislarDialog.vue'
import PosUrunKarti from '../components/PosUrunKarti.vue'
import PosOdemePaneli from '../components/PosOdemePaneli.vue'
import PosSepetPaneli from '../components/PosSepetPaneli.vue'
import PosMusteriPaneli from '../components/PosMusteriPaneli.vue'
import { useCariOnerileri, cariHesapCoz } from '../composables/useCariOnerileri.js'
import { faturaAPI, cariHesapAPI, stokAPI, sirketAPI, teslimatAPI } from '../api/index.js'
import { useOfflineSatisKuyrugu } from '../composables/useOfflineSatisKuyrugu.js'
import SelectButton from 'primevue/selectbutton'
import { useKisayollar } from '../composables/useKisayollar.js'
import { usePosTercih } from '../composables/usePosTercih.js'
import { usePosKisayollar } from '../composables/usePosKisayollar.js'
import { usePosKatalog } from '../composables/usePosKatalog.js'
import { usePosSepet } from '../composables/usePosSepet.js'
import { usePosOdeme } from '../composables/usePosOdeme.js'
import { usePosSatisGecmisi } from '../composables/usePosSatisGecmisi.js'
import { formatCurrency, formatDateTime, getLocalDateString } from '../utils/format.js'
import { escPosFisiUret, escPosYazdir } from '../utils/escpos.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import { satisPayloadUret } from '../utils/satisPayload.js'
import { posFisHtml } from '../utils/posFis.js'
import { useRoute, useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'

const toast = useToast()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const stokStore = useStokStore()
const offlineKuyruk = useOfflineSatisKuyrugu()
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const confirm = useConfirm()

// Değişim akışı: İadeler ekranından "Yeni Satışa Geç" ile gelindiğinde gösterilir.
const degisimIadeId = ref(null)
const { sirketLogosu } = useMarka()

const offlineKuyruguSenkronizeEt = async () => {
  try {
    const gonderilen = await offlineKuyruk.senkronizeEt(
      (s, anahtar) => faturaAPI.create(s, anahtar),
      (meta, yanit) => teslimatKaydiniOlustur(yanit?.data?.id, meta)
    )
    if (gonderilen > 0) {
      toast.add({ severity: 'success', summary: t('hizliSatis.senkronizeEdildi'), detail: t('hizliSatis.satisGonderildi', { n: gonderilen }), life: 3000 })
    }
  } catch {
    /* empty */
  }
}

useKisayollar({
  kaydet: () => satisiTamamla(),
  iptal: () => {
    if (yeniMusteriDialog.value) yeniMusteriDialog.value = false
    else if (scannerAcik.value) scannerAcik.value = false
    else if (satisOzetDialog.value) satisOzetDialog.value = false
    else if (adetPopoverUrun.value) adetPopoverKapat()
    else if (urunIzgaraOdak.value) urunIzgaradanCik()
  },
  yazdir: () => fisiYazdir(),
  // `?` POS'ta yerel kisayol seridini acar (global KisayolRehberi degil).
  ipucu: () => ipucuToggle(),
  // POS `n/k/h/t/p/g` harflerini kullaniyor; `g`+harf gezinmesi bu harfleri
  // yutuyordu ve POS'tan cikis kisayolu (`g h`) sessizce oldu.
  gezinmeKapat: true
})

// `girdideMi` artik `usePosKisayollar` icinde (yazarken koruma kuralinin parcası).
const odakla = (r) => {
  r.value?.$el?.focus?.() || r.value?.focus?.()
}

// F7: bugunku satislar diyalogunu acar (yalnizca okunur; POS'tan cikmaz).
const bugunkuSatislariAc = () => {
  bugunkuDialog.value = true
}

// F8: fis onizlemesini acar ve YAZDIR. Fis onizlemesi kapaliysa once acilir;
// `fisiYazdir` sepet bosken geri doner (satistan sonra fiyenin yeniden
// yazdirilmasi bu yoldan yapilir, `sonSatis` uzerinden).
const fisOnizlemeToggle = () => {
  if (!fisAcik.value) {
    fisAcik.value = true
    nextTick(() => {
      const kok = fisDetayRef.value?.$el || fisDetayRef.value
      kok?.querySelector?.('.fis-detay-ozet')?.scrollIntoView?.({ block: 'nearest' })
      // `<details>` acilmasi icin bir sonraki tick gerekiyor.
      nextTick(() => {
        const d = kok?.querySelector?.('details.fis-detay')
        if (d) d.open = true
      })
    })
  }
  if (sonSatis.value || sepet.value.length) {
    const fisNoYaz = sonSatis.value?.faturaNumarasi || null
    const fisModu = satisOzet.value?.fisModu ?? null
    setTimeout(() => fisiYazdir(fisNoYaz, fisModu), 60)
  }
}

// P: fis modunu (fiyatli / fiyatsiz) tek tusla degistirir.
const fisDegiskeniniDegistir = () => {
  fisFiyatliGecici.value = !fisFiyatliGecici.value
}

const odaklaAktifAdet = () => {
  const kok = sepetListeRef.value?.$el || sepetListeRef.value
  const el = kok?.querySelector?.('.sepet-item.aktif-satir .sepet-adet-input')
  el?.focus?.()
  el?.select?.()
}

// POS tercihleri (gorunurluk, buyuk yazi, onay adimi, fis ayarlari).
// Once bu view'da 14 `localStorage` anahtari ve DORT farkli kalicilik yontemi
// vardi: bazi `watch` ile, bazilari yalniz `@change` aninda, bazilari dogrudan
// `setItem` ile, bir kismi hic yazilmiyordu. Artik hepsi `usePosTercih`
// icinde; okuma/yazma/varsayilan tek yerde, degisikligin aninda kalicilik
// garantili. Bkz. composables/usePosTercih.js.
const {
  ipucuAcik,
  buyukYazi,
  onayIste,
  otomatikYazdir,
  musteriAcik,
  sepetAcik,
  odemeAcik,
  teslimatAcik,
  fisAcik,
  detayAcik,
  fisFiyatli,
  fisAltNotu,
  fisGenislik,
  degistir: tercihDegistir,
  ipucuKapat,
  sunucuAyarlariniUygula
} = usePosTercih({ t })

const ipucuToggle = () => tercihDegistir('ipucuAcik')

// Bugunku satislar: sag sutunda yer kaplamasin diye dialog'da gosterilir.
const bugunkuDialog = ref(false)
// Fis onizleme `<details>` blogu (F8 ile acilip yazdirilir).
const fisDetayRef = ref(null)

// Panel basliklarindaki katlanir oklar. Once her biri `setItem` cagrisiyordu;
// artik `usePosTercih` icindeki `watch` kaliciligi otomatik yapiyor.
const sepetAcikDegistir = () => tercihDegistir('sepetAcik')
const odemeAcikDegistir = () => tercihDegistir('odemeAcik')
const detayAclicDegistir = () => tercihDegistir('detayAcik')

const onayDialog = ref(false)
const onayBekleyenSatis = ref(false)

onMounted(() => {
  window.addEventListener('online', offlineKuyruguSenkronizeEt)
  if (navigator.onLine) offlineKuyruguSenkronizeEt()
})

onUnmounted(() => {
  window.removeEventListener('online', offlineKuyruguSenkronizeEt)
})


const sirketAdi = computed(() => authStore.sirketAdi || '')

// REDTEAM/Faz3: `siralama` ve filtre ref'leri (`filtreKategori`,
// `filtreMarka`, `filtreStokGrubu`, `sadeceStokta`) artik `usePosKatalog`
// icinden geliyor; `gosterilenAdet` yerini "daha fazla" sayfa butonuna birakildi.
const globalBarkod = ref('')
const scannerAcik = ref(false)
// Araç çubuğu popover'ları (filtreler + tercihler)
const filtrePopover = ref(null)
const tercihPopover = ref(null)
const barkodInputRef = ref(null)
const urunAraAutoRef = ref(null)
const musteriPaneliRef = ref(null)
const sepetListeRef = ref(null)
// `ipucuAcik` yukarida `usePosTercih` icinden geldi.
const hizliUrunDialog = ref(false)
const hizliUrun = ref({ barkod: '', ad: '', fiyat: 0, miktar: 1 })
const hizliUrunKaydediliyor = ref(false)

const globalBarkodEkle = () => {
  const barkod = globalBarkod.value.trim()
  if (!barkod) return
  barkodTarandi(barkod)
  globalBarkod.value = ''
  // Odak tekrar bu inputa gelsin ki USB tarayıcı kesintisiz okusun
  barkodInputRef.value?.$el?.focus?.() || barkodInputRef.value?.focus?.()
}

const barkodTarandi = async (barkod) => {
  if (!barkod) return
  // REDTEAM/Faz5: basarili ekleme toast'i KALDIRILDI. Hizli bir kasada 5-6
  // kalem eklenince 5-6 bildirim ust uste diziliyor ve `position="top-right"`
  // oldugu icin TUM SAG PANELI (sepet dahil) ortuyordu; kullanicinin hangi
  // kalemi ekledigini gormesi engelleniyordu.
  //
  // Geri bildirim zaten GORSEL: satir aninda belirip pulse animasyonu oynuyor
  // (`vurguluId` -> `.sepet-item.yeni-satir`) ve urun kartinda adet rozeti
  // guncelleniyor. Hata/uyari bildirimleri (stok yok, barkod bulunamadi)
  // KORUNDU.
  //
  // Etiketlerde barkod alani bos urunlerde stok kodu kodlanir; arama her ikisini
  // ve seri numarasini kapsar.
  const eslesir = (s) => s.barkod === barkod || s.seriNo === barkod || s.stokKodu === barkod
  let urun = stokStore.stoklar.find(eslesir)
  if (urun) {
    sepeteEkle(urun)
  } else {
    // Sunucuda ara (büyük envanterde tümü yüklenmemiş olabilir)
    try {
      const r = await stokAPI.ara(barkod)
      const bulunan = (r.data || []).find(eslesir)
      if (bulunan) {
        sepeteEkle(bulunan)
        return
      }
    } catch {
      /* sunucu araması başarısız olabilir */
    }
    toast.add({ severity: 'warn', summary: t('hizliSatis.bulunamadi'), detail: t('hizliSatis.barkodluUrunBulunamadi', { barkod }), life: 3000 })
    hizliUrun.value = { barkod, ad: '', fiyat: 0, miktar: 1 }
    hizliUrunDialog.value = true
  }
}

// REDTEAM/Faz3: `siralama`, `filtreKategori` / `filtreMarka` /
// `filtreStokGrubu` / `sadeceStokta` artik `usePosKatalog` icinden geliyor.
// `tumKategorilerGoster` (çip listesinin tamamını açma) sunucu tarafi bir sey
// olmadigi icin burada kaliyor.
const tumKategorilerGoster = ref(false)

// REDTEAM/Faz3: filtre ref'leri ve "filtreleri temizle" `usePosKatalog`
// icine tasindi. `tumKategorilerGoster` (çip listesinin tamamını açma)
// sunucu tarafi bir sey olmadigi icin burada kaliyor.

const seciliMusteri = ref(null)
const musteriGiris = ref('')
const seciliSofor = ref(null)
const soforler = ref([])
const soforlerYukleniyor = ref(false)
const teslimatAdresi = ref('')
const teslimDurumu = ref('BEKLIYOR')
const teslimNotu = ref('')
const musteriModu = ref('perakende')
const cariTurSecenekleri = computed(() => [
  { label: t('cariTur.musteri'), value: 'Musteri' },
  { label: t('cariTur.tedarikci'), value: 'Tedarikci' },
  { label: t('cariTur.herIkisi'), value: 'Her Ikisi' }
])
const musteriModlari = computed(() => [
  { label: t('hizliSatis.perakende'), value: 'perakende', icon: 'pi pi-shopping-cart' },
  { label: t('hizliSatis.musteri'), value: 'musteri', icon: 'pi pi-users' }
])
const anlikMusteri = computed(() => musteriModu.value === 'perakende')

watch(musteriModu, (mod) => {
  if (mod === 'perakende') {
    seciliMusteri.value = null
    musteriGiris.value = ''
  }
})
const yeniMusteriDialog = ref(false)
const yeniMusteri = ref({ ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' })
const musteriKaydediliyor = ref(false)

const kaydediliyor = ref(false)
const fisNo = ref('')
// `fisFiyatli` / `fisAltNotu` / `fisGenislik` artik `usePosTercih` icinde
// tanimli; kalici yazim onun `watch`'i ile otomatik.
// Fiş fiyatlı/fiyatsız seçimi satış başına geçicidir; satış tamamlanınca sunucu ayarına döner.
const fisFiyatliGecici = ref(fisFiyatli.value)

// Sekmeler arası canlı senkron: Ayarlar'da değişince POS'a anında yansır
const dinleyici = (e) => {
  if (e.key === 'raspel_fis_fiyatli' && e.newValue !== null) {
    fisFiyatli.value = e.newValue !== 'false'
    // Kullanıcı bu satış için farklı bir mod seçmediyse varsayılan güncellenir.
    if (fisFiyatliGecici.value === !fisFiyatli.value) fisFiyatliGecici.value = fisFiyatli.value
  } else if (e.key === 'raspel_fis_notu' && e.newValue !== null) {
    fisAltNotu.value = e.newValue
  } else if (e.key === 'raspel_fis_genislik' && e.newValue !== null) {
    fisGenislik.value = e.newValue
  }
}
onMounted(() => window.addEventListener('storage', dinleyici))
onUnmounted(() => window.removeEventListener('storage', dinleyici))

// REDTEAM/Faz7: indirim / odeme yontemi / taksit / kasa-banka-POS secimleri ve
// bunlardan turetilen tutarlar `usePosOdeme` icinde toplandi. Satis SONUCU ve
// gunluk satis gecmisi `usePosSatisGecmisi` icinde; satisi OLUSTURAN
// orkestrasyon (satisiTamamla*) burada kalir (bkz. asagidaki aciklama).

// REDTEAM/Faz3: `benzersizDegerler` KALDIRILDI. Filtre secenekleri artik
// `gruplama-dagilimi` ucundan TUM katalogdan gelir ve gercek urun sayisiyla
// etiketlenir (`kategoriDagilimi` vb.). Once yalnizca yuklenen sayfadan
// turetiyordu; sayilar yanlitti ve 200. urunden sonraki gruplara ulasmak
// mumkun degildi.
const kategoriler = computed(() =>
  kategoriDagilimi.value.map((k) => k.deger).filter(Boolean)
)
const markalar = computed(() =>
  markaDagilimi.value.map((k) => k.deger).filter(Boolean)
)
const stokGruplari = computed(() =>
  stokGrubuDagilimi.value.map((k) => k.deger).filter(Boolean)
)
const cokSatanlar = ref([])

// Ürün başına çoklu fiyat listesi (stok fiyatları endpoint'inden)
const urunFiyatlari = ref({})
// Cariye özel fiyatlar (stokId -> fiyat)
const cariOzelFiyatlar = ref({})

// Müşterinin ürüne özel fiyatını döndürür
const cariFiyati = (urunId) => cariOzelFiyatlar.value[urunId] || null

// Görünen ürünlerin fiyat listelerini topluca çeker
const urunFiyatlariniYukle = async (urunler) => {
  const yeni = { ...urunFiyatlari.value }
  const eksik = (urunler || []).filter((u) => u?.id != null && !yeni[u.id]).map((u) => u.id)
  if (!eksik.length) return
  try {
    // Ürün başına ayrı istek yerine tek toplu istek.
    const r = await stokAPI.getFiyatlarToplu(eksik)
    const harita = r.data || {}
    for (const id of eksik) {
      const liste = harita[id] || []
      if (liste.length) {
        yeni[id] = liste.map((f) => ({ ad: f.ad || f.fiyatTipi || f.tip || t('hizliSatis.fiyat'), fiyat: Number(f.fiyat) }))
      }
    }
    urunFiyatlari.value = yeni
  } catch {
    /* fiyat listesi alınamadı */
  }
}

// Sepete ürün eklenirken çoklu fiyat listesini de getirir
const urunFiyatlariniYukleTek = async (urun) => {
  if (urunFiyatlari.value[urun.id]) return urunFiyatlari.value[urun.id]
  try {
    const r = await stokAPI.getFiyatlar(urun.id)
    const liste = r.data || []
    if (liste.length) {
      const map = liste.map((f) => ({ ad: f.ad || f.fiyatTipi || 'Fiyat', fiyat: Number(f.fiyat) }))
      urunFiyatlari.value = { ...urunFiyatlari.value, [urun.id]: map }
      return map
    }
  } catch {
    /* */
  }
  return []
}

const cokSatanlariYukle = async () => {
  try {
    const r = await stokAPI.enCokSatanlar(12)
    cokSatanlar.value = r.data || []
    urunFiyatlariniYukle(cokSatanlar.value)
  } catch {
    cokSatanlar.value = []
  }
}

// Sepet kaydet / yükle
const SEPET_KEY = 'raspel_kayitli_sepet'
const kayitliSepetVar = ref(false)

const sepetKaydet = () => {
  if (!sepet.value.length) return
  localStorage.setItem(SEPET_KEY, JSON.stringify(sepet.value))
  kayitliSepetVar.value = true
  toast.add({ severity: 'success', summary: t('hizliSatis.sepetKaydedildi'), detail: t('hizliSatis.sepetKaydedildiDetay'), life: 3000 })
}

const sepetYukle = () => {
  try {
    const kayit = JSON.parse(localStorage.getItem(SEPET_KEY) || '[]')
    sepet.value = kayit
    kayitliSepetVar.value = false
    localStorage.removeItem(SEPET_KEY)
    toast.add({ severity: 'info', summary: t('hizliSatis.sepetYuklendi'), detail: t('hizliSatis.sepetYuklendiDetay'), life: 3000 })
  } catch {
    /* empty */
  }
}


// `agirlikMetni` ve `teslimDurumEtiketi` artik `utils/posFis.js` icinde:
// fis gorunumunun tamami tek yerde toplandigi icin bu iki bicimlendirici de
// orada. Ekran onizlemesi de ayni metni kullandigi icin tek kaynak dogru.

// REDTEAM/Faz7: indirimTutari / genelToplam / kalanTutar / odemeDurum* ve
// bunlarin odeme durumu <-> odenen tutar senkronu `usePosOdeme` icinde.

const musteriAdi = computed(() => {
  if (anlikMusteri.value) return t('hizliSatis.anlikMusteri')
  return seciliMusteri.value?.ad || ''
})

const musteriBakiyeUyarisi = computed(() => {
  if (!seciliMusteri.value) return null
  const bakiye = seciliMusteri.value.bakiye
  const krediLimiti = seciliMusteri.value.krediLimiti
  if (krediLimiti != null && bakiye != null && bakiye < 0 && Math.abs(bakiye) >= krediLimiti) {
    return { seviye: 'danger', mesaj: t('hizliSatis.krediLimitiAsildi', { tutar: formatCurrency(Math.abs(bakiye)) }) }
  }
  if (bakiye != null && bakiye < 0) {
    return { seviye: 'warn', mesaj: t('hizliSatis.borc', { tutar: formatCurrency(Math.abs(bakiye)) }) }
  }
  if (bakiye != null && bakiye > 0) {
    return { seviye: 'info', mesaj: t('hizliSatis.alacak', { tutar: formatCurrency(bakiye) }) }
  }
  return null
})

// REDTEAM/Faz3: `filtrelenmisUrunler` KALDIRILDI. Filtreleme artik sunucu
// tarafindadir (`usePosKatalog`); ayni filtreyi hem sunucuda hem istemcide
// uygulamak "iki kaynak, iki gercek" durumu yaratirdi ve katalog 200 urunle
// sinirli kaldigi icin sonuc zaten yanlitti.
//
// ---------------------------------------------------------------------------
// KATALOG: sunucu tarafi sayfali arama (REDTEAM/Faz3)
// ---------------------------------------------------------------------------
// KATALOG: sunucu tarafi sayfali arama (REDTEAM/Faz3)
// ---------------------------------------------------------------------------
// Once katalog 200 urunle TEK seferde cekilip filtreleme/arama ISTEMCI
// tarafinda yapiliyordu; 200. urunden sonraki malzemeler POS'ta hic
// bulunamiyordu. Artik arama, kategori, marka, stok grubu ve "sadece stokta"
// filtreleri sunucuya gidiyor; "daha fazla" sonraki sayfayi istiyor.
// Ayrinti ve gerekce: composables/usePosKatalog.js
const {
  urunler: katalogUrunleri,
  toplam: katalogToplam,
  yukleniyor: katalogYukleniyor,
  hata: katalogHatasi,
  bosMu: katalogBos,
  dahaFazlaVar: katalogDahaFazlaVar,
  sonucOzeti: katalogSonucOzeti,
  aramaMetni: katalogArama,
  kategori: filtreKategori,
  marka: filtreMarka,
  stokGrubu: filtreStokGrubu,
  sadeceStokta,
  siralama,
  aktifFiltreSayisi: aktifFiltreSayisiPos,
  kategoriDagilimi,
  markaDagilimi,
  stokGrubuDagilimi,
  katalogYukle,
  dahaFazlaYukle,
  filtreTemizle,
  dagilimYukle
} = usePosKatalog()

// ---------------------------------------------------------------------------
// SEPET CEKIRDEGI: satir durumu, islemler, geri alma ve toplamlar.
// ---------------------------------------------------------------------------
// Once bu mantik bu dosyada 400+ satirdi ve view'in geri kaniyla ic ice
// girmisti; sepet davranisini anlamak icin dosyada gezinmek gerekiyordu.
// Kurallar (adet tavani, satir benzersizligi, geri alma) saf fonksiyonlara
// dayanir ve `utils/posAdet.js` + `utils/posSepet.js` icinde birim testlidir.
const {
  sepet,
  aktifSatir,
  vurguluId,
  geriAlSatir,
  geriAlSepet,
  suruklenenIdx,
  toplam,
  toplamFt3,
  toplamAgirlik,
  agirlikVarMi,
  satiriVurgula,
  sepetteAdet,
  satirAktifYap,
  sepeteEkle,
  fiyatTipiDegisti,
  sepeteCariFiyatUygula,
  miktarDegistir,
  miktarAzalt,
  adediSifirla,
  sepetSil,
  satiriCogalt,
  aktifSatiriCogalt,
  geriAlSatirYap,
  sepetiGeriAlinabilirTemizle,
  sepetGeriAl,
  geriAlYap: geriAlCekirdek,
  sepetiSifirla,
  suruklemeBasla,
  suruklemeUzerine,
  suruklemeBirak,
  suruklemeBitir
} = usePosSepet({
  t,
  bildir: toastBildirim,
  seciliMusteri,
  urunFiyatlariniYukleTek,
  cariUrunFiyatGecmisi: (cariId, stokId) => faturaAPI.cariUrunFiyatGecmisi(cariId, stokId)
})

// Geri al: cekirdek once SATIR penceresini, sonra sepetin tamamini dener.
// Geri alinacak bir sey yoksa kullaniciyi bilgilendirir (cekirdek yalnizca
// `false` doner; bildirim view'in karari).
const geriAlYap = () => {
  if (!geriAlCekirdek()) {
    toast.add({
      severity: 'info',
      summary: t('common.toastInfo'),
      detail: t('hizliSatis.geriAlinacakSatisYok'),
      life: 2000
    })
  }
}

// ---------------------------------------------------------------------------
// ODEME / INDIRIM: odeme yontemi, taksit, kasa/banka/POS, indirim ve
// bunlardan turetilen tutarlar. `genelToplam` sepet toplamina bagli oldugu
// icin `toplam` disaridan verilir (tek bag).
const {
  indirimTipi,
  indirimTipleri,
  indirimDegeri,
  indirimTutari,
  genelToplam,
  odemeDurumu,
  odemeTipleri,
  odenenTutar,
  odemeYontemi,
  odemeYontemleri,
  taksitKurum,
  taksitTutar,
  taksitSayisi,
  seciliKasa,
  kasalar,
  seciliBanka,
  bankalar,
  seciliPos,
  posTerminalleri,
  seciliPosBilgi,
  hesaplananKomisyon,
  alinanNakit,
  paraUstu,
  hizliNakit,
  kalanTutar,
  odemeDurumText,
  odemeDurumEnum,
  odemeDurumSeverity,
  kasalariYukle,
  bankalariYukle,
  poslariYukle,
  odemeSifirla
} = usePosOdeme({ t, toplam })

// ---------------------------------------------------------------------------
// SATIS SONUCU ve GUNLUK GECMIS. Satisi OLUSTURAN orkestrasyon
// (`satisiTamamlaOnaysiz`, `satisBasarili`) bilerek view'da kalir: o akis
// sepet + odeme + teslimat + yazdirma + cari olmak uzere ~25 birimi sirayla
// baglar; composable'a tasinsaydi 25 bagimlilik enjekte eden bir "god object"
// olurdu. View'in isi zaten modulleri baglamaktir.
const {
  satisOzet,
  satisOzetDialog,
  sonSatis,
  gunlukSatislar,
  satisSonucunuKaydet,
  satisOzetiKapat,
  gunlukSatislariYukle,
  bugunkuSatisGoruntule: satisGecmisiGoruntule,
  sonSatisiIptalEt: sonSatisiIptalEtCekirdek
} = usePosSatisGecmisi({ t, bildir: toastBildirim, router })

// Liste kapanip faturaya gidilir; dialog kapatma bu ekrana ozgu bir adim.
const bugunkuSatisGoruntule = (satis) => {
  bugunkuDialog.value = false
  satisGecmisiGoruntule(satis)
}

/** Izgarada gosterilen urunler: katalogun SIRALANMIS hali. */
const gorunenUrunler = computed(() => {
  const liste = [...katalogUrunleri.value]
  if (siralama.value === 'fiyat') return liste.sort((a, b) => satisFiyati(b) - satisFiyati(a))
  if (siralama.value === 'stok') return liste.sort((a, b) => (b.miktar || 0) - (a.miktar || 0))
  return liste.sort((a, b) => (a.ad || '').localeCompare(b.ad || '', 'tr'))
})

// Görünen ürünler değiştiğinde çoklu fiyat listelerini besle
watch(gorunenUrunler, (list) => {
  if (list && list.length) urunFiyatlariniYukle(list)
}, { immediate: true })

const siralamaSecenekleri = computed(() => [
  { label: t('hizliSatis.siralaAd'), value: 'ad' },
  { label: t('hizliSatis.siralaFiyat'), value: 'fiyat' },
  { label: t('hizliSatis.siralaStok'), value: 'stok' }
])

const fisModuSecenekleri = computed(() => [
  { label: t('hizliSatis.fiyatliFis'), value: true },
  { label: t('hizliSatis.fiyatsizFis'), value: false }
])

// Çip listesi: tum katalogdan gelen dagilimin ilk 12'si (+"daha fazla").
// Artik her çip GERCEK urun sayisini tasir (dagilim sorgusu `count(*)` donuyor).
const kategoriCipleri = computed(() =>
  tumKategorilerGoster.value ? kategoriDagilimi.value : kategoriDagilimi.value.slice(0, 12)
)

const gizliKategoriSayisi = computed(() => Math.max(0, kategoriDagilimi.value.length - 12))

// POS'ta satış fiyatı önceliklidir; tanımlı değilse alış fiyatına düşülür.
const satisFiyati = (u) => Number(u?.satisFiyati || u?.fiyat || 0)

const stokYokMu = (u) => Number(u?.miktar || 0) <= 0


// ---------------------------------------------------------------------------

// Adet parametresi: 1 (fare/Enter) veya izgarada yazilan rakam.
const urunKartiTikla = (u, adet = 1) => {
  if (stokYokMu(u)) {
    toastBildirim.uyari(t('hizliSatis.stokYokUyari', { ad: u.ad }))
    return
  }
  sepeteEkle(u, adet)
}

// ---------------------------------------------------------------------------
// Izgara klabye modu (Faz4)
// ---------------------------------------------------------------------------
// Once urun kartlari yalnizca FARE ile secilebiliyordu: `↑/↓` oklari SEPETI
// geziyor, kartlarin `tabindex` degeri vardi ama hicbir sey onlari odaklamiyordu.
// Simdi:
//   F3 / ↑↓ (barkod alanindayken) -> izgaraya gir, ilk kart odaklanir
//   ↑↓←→ -> karti gez (grid genisligine gore satir sonu sarar)
//   Enter -> secili urunu sepete ekle
//   3 + Enter -> 3 adet ekle
//   Sag tik / Shift+Enter -> adet penceresi
//   Esc -> izgaradan cik, barkod alanina don
const urunIzgaraOdak = ref(false)
const urunIzgaraIndeks = ref(0)
// Izgara modunda biriken rakamlar ("3" yazinca Enter bekleniyor).
const urunIzgaraRakam = ref('')
const urunKartDomId = (i) => `pos-urun-kart-${i}`

// Izgaraya girerken seçili ürünü seçer ve kartı odaklar.
// Varsayılan olarak STOKTA OLAN ilk karta odaklanır: stoksuz kart Enter'de
// sadece uyarı üretir, kasiyer ölü bir seçimle karşılaşır.
const urunIzgarayaGir = (yoksaIlkKart = false) => {
  const liste = gorunenUrunler.value
  if (!liste.length) return
  urunIzgaraOdak.value = true
  if (!yoksaIlkKart && urunIzgaraIndeks.value >= liste.length) {
    urunIzgaraIndeks.value = 0
  }
  if (yoksaIlkKart || stokYokMu(liste[urunIzgaraIndeks.value])) {
    const ilkStoklu = liste.findIndex((u) => !stokYokMu(u))
    urunIzgaraIndeks.value = ilkStoklu === -1 ? 0 : ilkStoklu
  }
  urunIzgaraRakam.value = ''
  urunIzgarayiOdakla()
}

const urunIzgarayiOdakla = () => {
  nextTick(() => {
    const el = document.getElementById(urunKartDomId(urunIzgaraIndeks.value))
    el?.focus?.()
    el?.scrollIntoView?.({ block: 'nearest' })
  })
}

const urunIzgaradanCik = () => {
  urunIzgaraOdak.value = false
  urunIzgaraRakam.value = ''
  nextTick(() => odakla(barkodInputRef))
}

const urunIzgaraHareket = (dx, dy) => {
  const liste = gorunenUrunler.value
  if (!liste.length) return
  // Kac sütun var? Izgaranin gercek genisligini olcup en kucuk kart genisligiyle
  // sutun sayisi hesaplanir (CSS `minmax(178px, 1fr)` + 12px gap).
  const gridEl = document.querySelector('.product-grid')
  const sutun = Math.max(1, Math.floor(((gridEl?.clientWidth || 178) + 12) / 190))
  let i = urunIzgaraIndeks.value
  if (dx) i += dx
  if (dy) i += dy * sutun
  urunIzgaraIndeks.value = Math.min(liste.length - 1, Math.max(0, i))
  urunIzgaraRakam.value = ''
  urunIzgarayiOdakla()
}

// Izgarada Enter: once rakam yazildiysa miktarli ekleme, yoksa 1 adet.
const urunIzgaraSec = () => {
  const liste = gorunenUrunler.value
  const u = liste[urunIzgaraIndeks.value]
  if (!u) return
  const adet = urunIzgaraRakam.value ? parseInt(urunIzgaraRakam.value, 10) || 1 : 1
  urunIzgaraRakam.value = ''
  urunKartiTikla(u, adet)
}

// Sag tik / Shift+Enter ile acilan adet penceresi.
const adetPopoverUrun = ref(null)
const adetPopoverIndeks = ref(-1)
const adetPopoverDeger = ref(null)

// ---------------------------------------------------------------------------
// ADET PENCERESI KONUMU (tiklanan kartin yanina)
// ---------------------------------------------------------------------------
// SORUN: Pencere `position: fixed` idi ama `top/left` YOKTU; bu yuzden
// DOM akisindaki sabit yerinde, tiklanan karttan bagimsiz (genelde ekranin
// ortasinda) aciliyordu. Kasiyer "hangi karta tiklamistim?" diye kaybediyordu.
// Artik konum, sag tik / Shift+Enter'in geldigi noktadan (veya odakli kartin
// kutusundan) hesaplanir ve ekran disina tasmayacak sekilde sinirlanir.
const adetPopoverPoz = ref({ x: null, y: null })

/** Pencerenin tahmini olculeri (viewport'a sigdirma icin). */
const ADET_POPOVER_GENISLIK = 320
const ADET_POPOVER_YUKSEKLIK = 190

const adetPopoverStil = computed(() => {
  const { x, y } = adetPopoverPoz.value
  if (x == null || typeof window === 'undefined') return {}
  const genislik = Math.min(ADET_POPOVER_GENISLIK, window.innerWidth - 16)
  const sol = Math.min(Math.max(8, x), Math.max(8, window.innerWidth - genislik - 8))
  const ust = Math.min(Math.max(8, y), Math.max(8, window.innerHeight - ADET_POPOVER_YUKSEKLIK - 8))
  return { left: `${Math.round(sol)}px`, top: `${Math.round(ust)}px` }
})

/** Konumu olaydan (varsa) veya odakli karttan cikarir. */
const adetPopoverKonumlandir = (olay, indeks) => {
  if (olay && typeof olay.clientX === 'number' && (olay.clientX || olay.clientY)) {
    adetPopoverPoz.value = { x: olay.clientX + 8, y: olay.clientY + 8 }
    return
  }
  const el = typeof document !== 'undefined' ? document.getElementById(urunKartDomId(indeks)) : null
  const kutu = el?.getBoundingClientRect?.()
  adetPopoverPoz.value = kutu
    ? { x: kutu.left + 12, y: kutu.bottom + 8 }
    : { x: 8, y: 8 }
}

const adetPopoverAc = (u, i, olay = null) => {
  if (stokYokMu(u)) {
    toastBildirim.uyari(t('hizliSatis.stokYokUyari', { ad: u.ad }))
    return
  }
  const indeks = i ?? urunIzgaraIndeks.value
  adetPopoverKonumlandir(olay, indeks)
  adetPopoverUrun.value = u
  adetPopoverIndeks.value = indeks
  adetPopoverDeger.value = sepetteAdet(u.id) || 1
  adetPopoverOnayla.value = adetPopoverDeger.value
  if (urunIzgaraOdak.value && i != null) urunIzgaraIndeks.value = i
}

const adetPopoverOnayla = ref(1)
const adetPopoverOnaylandi = () => {
  const u = adetPopoverUrun.value
  const ham = adetPopoverOnayla.value
  adetPopoverKapat()
  if (!u) return
  // Dogrulama `sepeteEkle` icinde: adim yuvarlama, stok tavani ve ondalik
  // (kg/m2) kurallari orada tek yerde.
  sepeteEkle(u, Number(ham) || 1)
}

const adetPopoverKapat = () => {
  adetPopoverUrun.value = null
  adetPopoverIndeks.value = -1
  adetPopoverDeger.value = null
}

// Liste daraldiginda (filtre degisimi, urun silme) indeks sinir disinda kalir.
watch(gorunenUrunler, (list) => {
  if (urunIzgaraIndeks.value >= list.length) urunIzgaraIndeks.value = Math.max(0, list.length - 1)
  if (adetPopoverUrun.value && !list.some((u) => u.id === adetPopoverUrun.value.id)) {
    adetPopoverKapat()
  }
})

// ---------------------------------------------------------------------------
// Sunucu taraflı ürün araması (typeahead)
// ---------------------------------------------------------------------------
// Önceki davranış: ara kutusu YÜKLENMİŞ 50 ürünü client-side filtreliyordu.
// Katalog 50'yi aşınca ürünler hiç bulunamıyordu. Artık sunucu sorgulanıyor.
const urunOneri = ref(null)
const urunOnerileri = ref([])
let urunOneriZamanlayici = null
let urunOneriSeq = 0

const urunOneriAra = (event) => {
  const q = (event?.query || '').trim()
  clearTimeout(urunOneriZamanlayici)
  if (q.length < 2) {
    urunOnerileri.value = []
    return
  }
  // REDTEAM/Faz2.3: Gecikme ASIMDIR ULUSTU BINIYORDU.
  // PrimeVue `search()` zaten `this.searchTimeout = setTimeout(..., this.delay)`
  // ile kendi debounce'unu uygular ve `@complete` OLURKEN cagrilir. Yani
  // :delay="250" varken akis suydu:
  //     tus -> 250ms (PrimeVue :delay) -> @complete -> 250ms (buradaki) -> HTTP
  // = ~500ms + RTT. Asagida :delay="0" yapildigi icin TEK gecikme kaldi.
  urunOneriZamanlayici = setTimeout(async () => {
    const seq = ++urunOneriSeq
    try {
      const r = await stokAPI.satisOnerileri(q, 20)
      // Yavaş yanıtlar yarış koşulunu bozmasın.
      if (seq !== urunOneriSeq) return
      urunOnerileri.value = Array.isArray(r.data) ? r.data : (r.data?.content || [])
    } catch {
      if (seq === urunOneriSeq) urunOnerileri.value = []
    }
  }, 250)
}

const urunOneriSecildi = (event) => {
  const u = event?.value
  urunOneri.value = null
  urunOnerileri.value = []
  if (!u) return
  if (stokYokMu(u)) {
    toastBildirim.uyari(t('hizliSatis.stokYokUyari', { ad: u.ad }))
    return
  }
  sepeteEkle(u)
  // Ardışık ürün eklemede odak alanda kalsın.
  nextTick(() => odaklaUrunArama())
}

const odaklaUrunArama = () => {
  const el = urunAraAutoRef.value?.$el || urunAraAutoRef.value
  el?.querySelector?.('input')?.focus?.()
}

// ---------------------------------------------------------------------------
// Sepette ürün değiştirme
// ---------------------------------------------------------------------------
// Yanlış ürün seçildiyse satır silip yeniden eklemek adedi ve seçili fiyat tipini
// kaybettiriyordu. Artık satır yerinde değiştirilir; adet korunur, fiyat tipi
// yeni ürünün listesinde varsa ona göre güncellenir.
const urunDegistirSatir = ref(-1)
const urunDegistirOnerileri = ref([])
let urunDegistirZamanlayici = null
let urunDegistirSeq = 0

const urunDegistirAc = (idx) => {
  const kalem = sepet.value[idx]
  if (!kalem) return
  urunDegistirSatir.value = idx
  urunDegistirOnerileri.value = []
  aktifSatir.value = idx
}

const urunDegistirKapat = () => {
  urunDegistirSatir.value = -1
  urunDegistirOnerileri.value = []
  clearTimeout(urunDegistirZamanlayici)
}

const urunDegistirAra = (event) => {
  const q = (event?.query || '').trim()
  clearTimeout(urunDegistirZamanlayici)
  if (q.length < 2) {
    urunDegistirOnerileri.value = []
    return
  }
  urunDegistirZamanlayici = setTimeout(async () => {
    const seq = ++urunDegistirSeq
    try {
      const r = await stokAPI.satisOnerileri(q, 20)
      if (seq !== urunDegistirSeq) return
      urunDegistirOnerileri.value = Array.isArray(r.data) ? r.data : (r.data?.content || [])
    } catch {
      if (seq === urunDegistirSeq) urunDegistirOnerileri.value = []
    }
  }, 250)
}

const urunDegistirSec = async ({ idx, urun }) => {
  const kalem = sepet.value[idx]
  urunDegistirKapat()
  if (!kalem || !urun) return
  if (urun.id === kalem.id) return

  // Aynı ürün sepette zaten varsa miktarları birleştir, ayrı satır açma.
  const varOlanIndex = sepet.value.findIndex((i, i2) => i2 !== idx && i.id === urun.id)
  if (varOlanIndex >= 0) {
    sepet.value[varOlanIndex].miktar += kalem.miktar
    sepetSil(idx)
    satiriVurgula(urun.id)
    toastBildirim.basarili(t('hizliSatis.urunBirlestirildi', { ad: urun.ad }))
    return
  }

  const seciliTip = kalem.fiyatTipi
  const stdFiyat = Number(urun.satisFiyati || urun.fiyat || 0)

  // Ürün kimliğini ve özelliklerini değiştir; MİKTAR KORUNUR.
  kalem.id = urun.id
  kalem.ad = urun.ad
  kalem.stokKodu = urun.stokKodu
  kalem.barkod = urun.barkod
  kalem.kdvOrani = urun.kdvOrani != null ? Number(urun.kdvOrani) : 0
  kalem.birim = urun.birim || kalem.birim
  kalem.birimHacim = urun.birimHacim || kalem.birimHacim
  kalem.agirlik = Number(urun.agirlik) || 0
  kalem.sonAldigiFiyat = null
  kalem.sonAldigiTarih = null

  // Fiyat listesi yeni ürüne göre; aynı fiyat tipi varsa o korunur, yoksa ilk
  // fiyat seçilir. sepete ekleme ile aynı mantık.
  let fiyatlar = [
    { ad: t('hizliSatis.fiyatPerakende'), fiyat: stdFiyat },
    { ad: t('hizliSatis.fiyatToptan'), fiyat: Math.round(stdFiyat * 0.9 * 100) / 100 },
    { ad: t('hizliSatis.fiyatOzel'), fiyat: Math.round(stdFiyat * 0.8 * 100) / 100 }
  ]
  const tckilen = await urunFiyatlariniYukleTek(urun)
  if (tckilen && tckilen.length > 0) fiyatlar = tckilen

  // Kullanıcı bu arada satırı sildiyse/başka ürün çevirdiyse dokunma.
  const guncel = sepet.value[idx]
  if (!guncel || guncel.id !== urun.id) return

  guncel.fiyatlar = fiyatlar
  if (fiyatlar.some((f) => f.ad === seciliTip)) {
    guncel.fiyatTipi = seciliTip
    guncel.fiyat = fiyatlar.find((f) => f.ad === seciliTip).fiyat
  } else {
    guncel.fiyatTipi = fiyatlar[0].ad
    guncel.fiyat = fiyatlar[0].fiyat
  }
  satiriVurgula(urun.id)
}


const simdikiTarih = computed(() => formatDateTime(new Date()))


onMounted(async () => {
  try {
    await Promise.all([
      // POS musteri secici sunucu aramali; 50 kayitlik onbellek yerine ilk
      // sayfa onerileri yukleniyor, sonraki aramalar sunucuya gidiyor.
      musteriOnerileriYukle(),
      // REDTEAM/Faz3: Katalog artik sunucu tarafi SAYFALI geliyor
      // (`GET /api/stoklar/filtreli`). Once `stokStore.getAll({ size: 200 })`
      // tek seferde 200 urun cekiyor, filtreleme/arama BU KADAR urun uzerinde
      // yerel yapiliyordu; 200. urunden sonraki hicbir malzeme POS'ta
      // bulunamiyordu. "Daha fazla" butonu artik sonraki sayfayi istiyor.
      katalogYukle(),
      // Filtre secenekleri tum katalogdan, gercek urun sayilariyla gelir.
      dagilimYukle(),
      soforleriYukle(),
      cokSatanlariYukle(),
      kasalariYukle(),
      bankalariYukle(),
      poslariYukle(),
      gunlukSatislariYukle()
    ])
    kayitliSepetVar.value = !!localStorage.getItem('raspel_kayitli_sepet')
    await fisAyarlariSunucudanYukle()
    await degisimSorgusunuUygula()
  } catch (e) {
    // Kullanici bos urun listesi gorup "urun yok" sanmasin; yukleme hatasini bildir.
    toastBildirim.hata(e?.response?.data?.message || t('hizliSatis.yuklemeHatasi'))
  }
})

const degisimSorgusunuUygula = async () => {
  const qCari = route.query.cariHesapId
  if (qCari) {
    // Once onbellek, olmazsa tek kayit: liste 50 kayitla sinirli oldugu icin
    // cari listede yoksa musteri sessizce secilmemis kaliyordu.
    const c = await cariHesapCoz(qCari, musteriOnerileri.value)
    if (c) seciliMusteri.value = c
  }
  if (route.query.degisim) {
    degisimIadeId.value = route.query.degisim
    // Sorgu temizlenir; sayfa yenilenince banner tekrar açılmasın.
    router.replace({ path: route.path })
  }
}

const fisAyarlariSunucudanYukle = async () => {
  const sirketId = authStore?.sirketId
  if (!sirketId) return
  try {
    const res = await sirketAPI.getPosFisAyarlari(sirketId)
    if (res.data?.ayarlar) {
      // Sunucu degerleri tercih ref'lerine yazilir; kalicilik `usePosTercih`
      // icindeki `watch` ile ayni yola gider. Once burada ayri `setItem`
      // cagrilari vardi ve kalicilik kurali ikiye bolunmustu.
      sunucuAyarlariniUygula(JSON.parse(res.data.ayarlar))
    }
  } catch {
    /* sunucu yoksa yerel önbellek kullanılır */
  }
}

const teslimDurumSecenekleri = computed(() => [
  { label: t('faturalar.durumBekliyor'), value: 'BEKLIYOR' },
  { label: t('faturalar.durumYolda'), value: 'YOLDA' },
  { label: t('faturalar.durumTeslimEdildi'), value: 'TESLIM_EDILDI' }
])

// POS'ta secilen soforun adi; fis/fatura uzerine "Teslim Eden" olarak yazilir.
const teslimEden = computed(() => seciliSofor.value?.ad || '')

const soforleriYukle = async () => {
  soforlerYukleniyor.value = true
  try {
    const r = await teslimatAPI.suruculer()
    soforler.value = unwrapList(r)
  } catch {
    soforler.value = []
  } finally {
    soforlerYukleniyor.value = false
  }
}

// POS teslim durumu -> Teslimat kaydi durumu eslemesi.
const TESLIMAT_DURUM_ESLEME = { BEKLIYOR: 'BEKLEMEDE', YOLDA: 'YOLDA', TESLIM_EDILDI: 'TESLIM_EDILDI' }

const teslimatMetaUret = () => {
  if (!seciliSofor.value?.id) return null
  return {
    driverId: seciliSofor.value.id,
    teslimatAdresi: teslimatAdresi.value.trim(),
    notlar: teslimNotu.value?.trim() || null,
    durum: TESLIMAT_DURUM_ESLEME[teslimDurumu.value] || 'BEKLEMEDE'
  }
}

const teslimatKaydiniOlustur = async (faturaId, meta = null) => {
  const veri = meta || teslimatMetaUret()
  if (!veri || !faturaId) return
  try {
    await teslimatAPI.olustur({ faturaId, ...veri })
  } catch (e) {
    // Satis tamamlandi; teslimat kaydi acilamazsa kullanici bilgilendirilir.
    toastBildirim.uyari(e?.response?.data?.message || t('hizliSatis.teslimatKaydedilemedi'))
  }
}

// Sofor secilince cari adresi otomatik dolar (kullanici elle degistirebilir).
watch([seciliSofor, seciliMusteri], () => {
  if (!seciliSofor.value || teslimatAdresi.value.trim()) return
  const adres = seciliMusteri.value?.adres
  if (adres) teslimatAdresi.value = adres
})

// POS musteri secici sunucu aramali. Once 50 kayitlik onbellek istemci
// tarafindan filtreleniyordu; kasa basinda 50'den fazla cari olan isletmede
// musterinin bulunmamasi kullaniciyi "yeni cari" yoluna zorluyordu.
const { oneriler: musteriOnerileri, ara: musteriAra, hemenAra: musteriOnerileriYukle } = useCariOnerileri()

const musteriSec = (event) => {
  seciliMusteri.value = event.value
  musteriGiris.value = ''
  sepeteCariFiyatUygula()
  cariOzelFiyatlariYukle()
}

const musteriTemizle = () => {
  seciliMusteri.value = null
  musteriGiris.value = ''
  cariOzelFiyatlar.value = {}
}

// Cariye özel tanımlı fiyatları yükler (stokId -> fiyat)
const cariOzelFiyatlariYukle = async () => {
  const cariId = seciliMusteri.value?.id
  if (!cariId) return
  try {
    const r = await cariHesapAPI.getFiyatlar(cariId)
    const liste = r.data || []
    const map = {}
    liste.forEach((f) => {
      if (f.stokId != null) map[f.stokId] = f.fiyat
    })
    cariOzelFiyatlar.value = map
  } catch {
    cariOzelFiyatlar.value = {}
  }
}

const musteriKaydet = async () => {
  if (!yeniMusteri.value.ad) {
    toastBildirim.uyari(t('hizliSatis.adFirmaZorunlu'))
    return
  }
  musteriKaydediliyor.value = true
  try {
    const r = await cariHesapAPI.create(yeniMusteri.value)
    seciliMusteri.value = r.data
    musteriGiris.value = ''
    yeniMusteriDialog.value = false
    yeniMusteri.value = { ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' }
    toastBildirim.basarili(t('hizliSatis.cariHesapOlusturuldu'))
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('hizliSatis.kayitBasarisiz'))
  }
  musteriKaydediliyor.value = false
}

// REDTEAM/Faz7: `hizliNakit` `usePosOdeme` icinde (HIZLI_NAKIT).

const fisiYazdir = (gercekFaturaNo, fiyatliOverride = null) => {
  if (!sepet.value.length) return
  // Gercek fatura numarasi varsa fise o yazilir (fisten faturaya ulasilabilir).
  // Yalnizca cevrimdisi/henuz olusmamis satislarda gecici numara uretilir.
  fisNo.value = gercekFaturaNo || ('F-' + Date.now().toString(36).toUpperCase())
  yazdirmaKaydet('TERMAL80')

  // Fis gorunumu utils/posFis.js icinde. Bu fonksiyon artik yalnizca veriyi
  // toplayip pencerede yazdirir; 60 satirlik HTML sablonu + gomulu CSS dahil
  // fis gorunumu tek yerde toplanir.
  const pencere = fisPenceresiAcVeYazdir(
    posFisHtml({
      t,
      sepet: sepet.value,
      fiyatli: fiyatliOverride !== null ? fiyatliOverride : fisFiyatliGecici.value,
      toplam: toplam.value,
      indirimDegeri: indirimDegeri.value,
      indirimTipi: indirimTipi.value,
      indirimTutari: indirimTutari.value,
      genelToplam: genelToplam.value,
      odenenTutar: odenenTutar.value,
      kalanTutar: kalanTutar.value,
      toplamAgirlik: toplamAgirlik.value,
      sirketLogosu: sirketLogosu.value,
      sirketAdi: sirketAdi.value,
      simdikiTarih: simdikiTarih.value,
      fisNo: fisNo.value,
      musteriAdi: musteriAdi.value,
      teslimEden: teslimEden.value,
      teslimDurumu: teslimDurumu.value,
      teslimNotu: teslimNotu.value,
      odemeDurumText: odemeDurumText.value,
      saticiAdi: authStore?.kullanici?.displayName || '',
      fisGenislik: fisGenislik.value
    })
  )
  if (!pencere) {
    toastBildirim.hata(t('hizliSatis.pencereEngellendi'))
  }
}

const termalYazdir = async () => {
  if (!sepet.value.length) return
  const bytes = escPosFisiUret({
    baslik: sirketAdi.value || 'RASPEL ERP',
    tarih: simdikiTarih.value,
    fisNo: fisNo.value || undefined,
    genislik: fisGenislik.value,
    kalemler: sepet.value.map((i) => ({ ad: i.ad, adet: i.miktar, tutar: i.miktar * i.fiyat })),
    toplamAgirlik: agirlikVarMi.value ? toplamAgirlik.value : undefined,
    toplam: genelToplam.value,
    altNot: fisAltNotu.value || undefined
  })
  try {
    const gonderildi = await escPosYazdir(bytes)
    if (!gonderildi) {
      // WebUSB desteklenmiyorsa browser print fallback
      fisiYazdir()
    } else {
      yazdirmaKaydet('TERMAL', gonderildi)
    }
  } catch {
    toastBildirim.hata(t('hizliSatis.termalGonderilemedi'))
  }
}

/** Son oluşturulan faturaya yazdırma izi kaydeder (fatura id yoksa sessizce atlanır). */
const yazdirmaKaydet = (format, yaziciAdi) => {
  const id = sonSatis.value?.id
  if (!id) return
  faturaAPI.yazdirmaKaydet(id, { format, yaziciAdi }).catch(() => {})
}

const hizliUrunKaydet = async () => {
  const u = hizliUrun.value
  if (!u.ad || !u.ad.trim()) {
    toast.add({ severity: 'warn', summary: t('hizliSatis.eksikBilgi'), detail: t('hizliSatis.urunAdiZorunlu'), life: 2500 })
    return
  }
  hizliUrunKaydediliyor.value = true
  try {
    const r = await stokAPI.create({
      ad: u.ad.trim(),
      barkod: u.barkod || null,
      fiyat: Number(u.fiyat) || 0,
      satisFiyati: Number(u.fiyat) || 0,
      miktar: Number(u.miktar) || 0,
      birim: 'Adet'
    })
    const olusan = r.data || { ...u }
    hizliUrunDialog.value = false
    toast.add({ severity: 'success', summary: t('hizliSatis.urunEklendi'), detail: u.ad, life: 2500 })
    await sepeteEkle(olusan)
    if (Number(u.miktar) > 1 && olusan?.id) {
      const item = sepet.value.find((i) => i.id === olusan.id)
      if (item) item.miktar = Number(u.miktar)
    }
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: t('hizliSatis.kaydedilemedi'),
      detail: e?.response?.data?.message || t('hizliSatis.urunOlusturulamadi'),
      life: 3000
    })
  } finally {
    hizliUrunKaydediliyor.value = false
  }
}

// ---------------------------------------------------------------------------
// SATIS DOGRULAMA HATALARI — satir ici gosterim (REDTEAM/Faz5)
// ---------------------------------------------------------------------------
// SORUN: Tamamlama dogrulamalari yalnizca TOAST ile bildiriyordu. Kasiyer
// hangi alanin eksik oldugunu ekranda goremiyordu; dahasi teslimat paneli
// KAPALI oldugu icin zorunlu adres alani HIC gorunmuyordu ("gizli zorunlu
// alan"). Kasa duruyor, kullanici toast'i okuyup paneli elle acmak zorunda
// kaliyordu.
//
// COZUM: Alan bazli hata durumu. Dogrulama basarisiz olunca ilgili panel
// OTOMATIK acilir, alan kirmiziya doner ve altinda aciklama cikar; alan
// doldurulunca hata kendiliginden temizlenir.
const hatalar = ref({ musteri: false, teslimatAdresi: false, taksit: false })

const hataTemizle = (alan) => {
  if (hatalar.value[alan]) hatalar.value = { ...hatalar.value, [alan]: false }
}

/** Dogrulama hatasini isaretler, ilgili paneli acar ve alana odaklanir. */
const hataGoster = (alan) => {
  hatalar.value = { ...hatalar.value, [alan]: true }
  if (alan === 'teslimatAdresi') {
    teslimatAcik.value = true
    nextTick(() => document.getElementById('hizli-teslim-adres')?.focus?.())
  } else if (alan === 'musteri') {
    musteriAcik.value = true
    musteriModu.value = 'musteri'
  }
}

// Surucu secilince teslimat paneli OTOMATIK acilir: adres zorunludur ve
// panel kapaliyken alan render edilmedigi icin kullanici zorunlu alani
// goremiyordu.
watch(seciliSofor, (sofor) => {
  if (sofor) teslimatAcik.value = true
})

// Alan doldurulunca ilgili hatayi temizle (kirmizi cerceve kalmasin).
watch(teslimatAdresi, (v) => {
  if (String(v || '').trim()) hataTemizle('teslimatAdresi')
})
watch([taksitKurum, taksitTutar], () => hataTemizle('taksit'))
watch(seciliMusteri, (m) => {
  if (m) hataTemizle('musteri')
})
watch(musteriModu, (m) => {
  if (m === 'perakende') hataTemizle('musteri')
})

const satisiTamamla = async () => {
  // Anlik (perakende) satista musteri aranmaz; yalniz "Musteri" modunda zorunlu.
  if (!anlikMusteri.value && !seciliMusteri.value) {
    hataGoster('musteri')
    toastBildirim.uyari(t('hizliSatis.musteriGerekli'))
    return
  }
  if (sepet.value.length === 0) {
    toast.add({ severity: 'warn', summary: t('hizliSatis.sepetBos'), detail: t('hizliSatis.onceUrunEkleyin'), life: 2500 })
    return
  }
  // Kazara satışı önleme: tercih açıksa kısa onay adımı göster.
  if (onayIste.value) {
    onayBekleyenSatis.value = true
    onayDialog.value = true
    return
  }
  await satisiTamamlaOnaysiz()
}

const satisOnayla = async () => {
  onayDialog.value = false
  onayBekleyenSatis.value = false
  await satisiTamamlaOnaysiz()
}

const satisOnayIptal = () => {
  onayDialog.value = false
  onayBekleyenSatis.value = false
}

const satisiTamamlaOnaysiz = async () => {
  if (odemeYontemi.value === 'TAKSIT' && (!taksitKurum.value.trim() || !taksitTutar.value || taksitTutar.value <= 0)) {
    hataGoster('taksit')
    toastBildirim.uyari(t('hizliSatis.taksitZorunlu'))
    return
  }
  // Sofor secildiyse teslimat adresi zorunlu; soforsuz satista adres sorulmaz.
  if (seciliSofor.value && !teslimatAdresi.value.trim()) {
    hataGoster('teslimatAdresi')
    toastBildirim.uyari(t('hizliSatis.teslimatAdresiGerekli'))
    return
  }
  kaydediliyor.value = true
  const satisVerisi = satisPayloadUret({
    cariHesapId: anlikMusteri.value ? null : seciliMusteri.value.id,
    cariHesapAdi: anlikMusteri.value ? t('hizliSatis.perakendeMusteri') : seciliMusteri.value.ad,
    tur: 'SATIS',
    durum: 'KESILDI',
    tarih: getLocalDateString(),
    aciklama: t('hizliSatis.hizliSatisAciklama'),
    indirim: indirimTutari.value,
    odenenTutar: odenenTutar.value,
    odemeDurumu: odemeDurumEnum.value,
    odemeYontemi: odemeYontemi.value,
    kalemler: sepet.value.map((i) => ({ stokId: i.id, aciklama: i.ad, adet: i.miktar, birimFiyat: i.fiyat, kdvOrani: i.kdvOrani ?? 0 })),
    ekstra: {
      teslimEden: teslimEden.value || null,
      teslimDurumu: teslimDurumu.value || 'BEKLIYOR',
      teslimNotu: teslimNotu.value || null,
      taksitKurum: odemeYontemi.value === 'TAKSIT' ? taksitKurum.value : null,
      taksitTutar: odemeYontemi.value === 'TAKSIT' ? taksitTutar.value : null,
      taksitSayisi: odemeYontemi.value === 'TAKSIT' ? taksitSayisi.value : null,
      kasaId: odemeYontemi.value === 'NAKIT' ? (seciliKasa.value || null) : null,
      bankaId: odemeYontemi.value === 'HAVALE' || (odemeYontemi.value === 'KART' && !seciliPos.value)
        ? (seciliBanka.value || null)
        : null,
      kartaBankaAktar: odemeYontemi.value === 'KART' && !seciliPos.value && !!seciliBanka.value,
      // POS terminali seçilirse tutar anında bankaya yazılmaz; POS gün sonunda komisyon
      // düşülerek terminalin bankasına aktarılır (çift giriş önlenir).
      posTerminaliId: odemeYontemi.value === 'KART' ? (seciliPos.value || null) : null,
      komisyonTutar: odemeYontemi.value === 'KART' && seciliPos.value ? hesaplananKomisyon.value : null
    }
  })
  try {
    const yanit = await faturaAPI.create(satisVerisi)
    satisBasarili(yanit)
  } catch (e) {
    // Ağ yoksa satışı kuyruğa al (offline satış). Şoför ataması da kuyrukla
    // taşınır; bağlantı gelince fatura oluşup teslimat kaydı otomatik açılır.
    if (!e?.response) {
      offlineKuyruk.ekle(satisVerisi, teslimatMetaUret())
      toast.add({
        severity: 'warn',
        summary: t('hizliSatis.cevrimdisiKuyruk'),
        detail: t('hizliSatis.baglantiGelince'),
        life: 4000
      })
      try {
        fisiYazdir()
      } catch {
        /* empty */
      }
      sepetiTemizle()
    } else if ((e?.response?.data?.message || '').includes('Kredi limiti')) {
      // Kredi limiti sunucu tarafında engellendi; açık onayla bayrak gönderilerek tekrar denenir.
      confirm.require({
        message: e.response.data.message + ' ' + t('faturalar.krediLimitiDevam'),
        header: t('faturalar.krediLimitiBaslik'),
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: t('faturalar.krediLimitiOnayla'),
        rejectLabel: t('common.vazgec'),
        accept: async () => {
          try {
            satisBasarili(await faturaAPI.create({ ...satisVerisi, krediLimitiGormezdenGel: true }))
          } catch (e2) {
            toastBildirim.hata(e2?.response?.data?.message || t('hizliSatis.satisBasarisiz'))
          }
        }
      })
    } else {
      toastBildirim.hata(e?.response?.data?.message || t('hizliSatis.satisBasarisiz'))
    }
  }
  kaydediliyor.value = false
}

// Başarılı satış sonrası ortak işlemler (normal ve kredi limiti onaylı akış).
const satisBasarili = (yanit) => {
  // Sonuc durumu + ozet (`usePosSatisGecmisi`) — tutarlar buradan verilir.
  satisSonucunuKaydet(yanit, {
    genelToplam: genelToplam.value,
    odenenTutar: odenenTutar.value,
    kalanTutar: kalanTutar.value,
    odemeYontemi: odemeYontemi.value,
    paraUstu: paraUstu.value,
    fisModu: fisFiyatliGecici.value
  })
  toastBildirim.basarili(t('hizliSatis.satisTamamlandi') + ' - ' + formatCurrency(genelToplam.value))
  if (otomatikYazdir.value) {
    try {
      fisiYazdir(yanit.data?.faturaNumarasi)
    } catch {
      /* empty */
    }
  }
  // Sofor secildiyse teslimat kaydi acilir (fatura olustuktan sonra).
  teslimatKaydiniOlustur(yanit.data?.id, teslimatMetaUret())
  sepetiTemizle()
  alinanNakit.value = 0
  // Fiş modu satış başına geçiciydi: sunucu ayarına dön.
  fisFiyatliGecici.value = fisFiyatli.value
  gunlukSatislariYukle()
  kasalariYukle()
}

// Son satisi iptal et (stok geri alinir). Iptal cagrisi ve sonuc/hata
// bildirimleri `usePosSatisGecmisi` icinde; burada iptal SONRASI bu ekrana ozgu
// tazelemeler yapilir (stok onbellegi + kasa listesi).
const sonSatisiIptalEt = () =>
  sonSatisiIptalEtCekirdek(async () => {
    // Satis iptal sonrasi stok onbellegi tazelenir.
    stokStore.getAll({ size: 200 })
    kasalariYukle()
  })

// Satış özeti kapatılıp yeni satışa hazırlanır: barkod alanı odaklanır.
const yeniSatisaBasla = () => {
  satisOzetiKapat()
  nextTick(() => odakla(barkodInputRef))
}

// Satış tamamlandıktan sonra: temizle (geri alınamaz) ve perakende moduna dön.
const sepetiTemizle = () => {
  // Sepet durumu (satirlar + geri alma pencereleri + aktif satir) ve odeme
  // alanlari composable'larin isi; burada yalniz KARSI ALAN (musteri/teslimat)
  // sifirlanir.
  sepetiSifirla()
  odemeSifirla()
  seciliMusteri.value = null
  musteriGiris.value = ''
  seciliSofor.value = null
  teslimatAdresi.value = ''
  teslimDurumu.value = 'BEKLIYOR'
  teslimNotu.value = ''
  musteriModu.value = 'perakende'
}

// POS klavye kisayollari: 25+ kisayol, once TEK bir `handlePosKeys` icinde
// ic ice `if` zinciriydi. Artik `usePosKisayollar` icinde bagimsiz gruplar
// (fonksiyon tuslari / izgara modu / sepet gezinme / harfler) olarak duruyor ve
// `window` dinleyicileri de orada baglaniyor. Siralama kurali (fonksiyon tuslari
// once, izgara modu "oda" gibi, harfler yazarken devre disi) composable
// icinde yorumla belgeli.
usePosKisayollar(
  {
    // durum
    urunIzgaraOdak,
    urunIzgaraIndeks,
    urunIzgaraRakam,
    gorunenUrunler,
    sepet,
    aktifSatir,
    musteriModu,
    odemeDurumu,
    odemeYontemi,
    // DOM ref'leri
    barkodInputRef,
    musteriPaneliRef,
    // eylemler
    sepetiGeriAlinabilirTemizle,
    urunIzgarayaGir,
    urunIzgaradanCik,
    urunIzgarayiOdakla,
    urunIzgaraHareket,
    urunIzgaraSec,
    adetPopoverAc,
    odaklaUrunArama,
    odaklaAktifAdet,
    bugunkuSatislariAc,
    fisOnizlemeToggle,
    termalYazdir,
    // `satisiTamamla` BILINCLI LISTEDE YOK: F9/F10 artik satisi tamamlamiyor
    // (kazara kayit engellendi), baska bir tus de tamamlamayi tetiklemiyor.
    miktarAzalt,
    sepetSil,
    fisDegiskeniniDegistir,
    geriAlYap,
    aktifSatiriCogalt
    // `ipucuToggle` BILINCLI LISTEDE YOK: `?` tusu `useKisayollar`'a ait
    // (`ipucu: () => ipucuToggle()` kaydi yukarida). Buraya da eklenseydi ayni
    // tus iki kez islenir ve ipucu acilip kapanirdi.
  },
  {
    t,
    bildir: (severity, detail) =>
      toast.add({ severity, summary: t('common.toastInfo'), detail, life: 2000 })
  }
)
</script>

<style scoped>
@import '../assets/pos-hizli-satis.css';
</style>

<style>
@media print {
  body * {
    visibility: hidden;
  }
  #fisOnizleme,
  #fisOnizleme * {
    visibility: visible;
  }
  #fisOnizleme {
    position: fixed;
    top: 0;
    left: 0;
    width: 80mm;
    padding: 10mm;
    background: white;
    color: black;
    font-size: 12px;
    font-family: 'Courier New', monospace;
  }
}
</style>