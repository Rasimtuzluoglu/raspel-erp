<template>
  <div class="pos-container">
    <div class="pos-header">
      <div class="breadcrumb">
        <i class="pi pi-home" /> Anasayfa / POS / Yeni Satış
      </div>
      <div
        v-if="authStore?.kullanici"
        class="user-info"
      >
        <i class="pi pi-user" /> {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username }}
      </div>
    </div>

    <div class="pos-body grid">
      <div class="pos-left">
        <div class="pos-arac-cubugu">
          <span class="p-input-icon-left arama-kutusu">
            <i class="pi pi-barcode" />
            <InputText
              ref="barkodInputRef"
              v-model="globalBarkod"
              placeholder="Barkod okutun (Enter)"
              class="w-full"
              autofocus
              @keyup.enter="globalBarkodEkle"
            />
          </span>
          <span class="p-input-icon-left arama-kutusu">
            <i class="pi pi-search" />
            <InputText
              v-model="seriNoArama"
              placeholder="Ürün / kod / seri ara..."
              class="w-full"
            />
          </span>
          <Dropdown
            v-model="filtreKategori"
            :options="kategoriler"
            option-label="ad"
            placeholder="Kategori"
            class="arac-dropdown"
            show-clear
          />
          <Dropdown
            v-model="filtreArac"
            :options="aracListesi"
            placeholder="Marka"
            class="arac-dropdown"
            show-clear
          />
          <Button
            icon="pi pi-camera"
            label="Barkod"
            severity="secondary"
            outlined
            @click="scannerAcik = true"
          />
        </div>

        <div
          v-if="cokSatanlar.length > 0"
          class="cok-satanlar-section"
        >
          <div class="product-header">
            <h3><i class="pi pi-star-fill" /> Çok Satanlar</h3>
          </div>
          <div class="cok-satanlar-grid">
            <button
              v-for="u in cokSatanlar"
              :key="u.id"
              type="button"
              class="cok-satan-chip"
              @click="sepeteEkle(u)"
            >
              <span class="cok-satan-ad">{{ u.ad }}</span>
              <span class="cok-satan-fiyat">{{ formatCurrency(u.fiyat || u.satisFiyati || 0) }}</span>
            </button>
          </div>
        </div>

        <div class="product-section">
          <div class="product-header">
            <h3>Mevcut Ürünler <span class="urun-sayaci">{{ filtrelenmisUrunler ? filtrelenmisUrunler.length : 0 }}</span></h3>
          </div>
          <div class="product-grid">
            <div
              v-for="u in filtrelenmisUrunler"
              :key="u.id"
              class="product-card"
              @click="sepeteEkle(u)"
            >
              <span class="product-kod">{{ u.barkod || u.stokKodu || '-' }}</span>
              <span class="product-name">{{ u.ad }}</span>
              <Tag
                :value="kritikStokMu(u) ? 'Son ' + Math.floor(u.miktar) : (u.miktar || 0) + ' ' + (u.birim || 'adet')"
                :severity="kritikStokMu(u) ? 'danger' : 'info'"
              />
              <span class="product-price">{{ formatCurrency(u.fiyat || u.satisFiyati || 0) }}</span>
              <span
                v-if="cariFiyati(u.id)"
                class="product-cari-fiyat"
                :title="'Cari özel fiyat'"
              >
                <i class="pi pi-user" /> {{ formatCurrency(cariFiyati(u.id)) }}
              </span>
            </div>
            <div
              v-if="filtrelenmisUrunler && filtrelenmisUrunler.length === 0"
              class="empty-products"
            >
              <i class="pi pi-inbox" />
              <p>Ürün bulunamadı</p>
            </div>
          </div>
        </div>
      </div>

      <div class="pos-right">
        <Card class="siparis-kart">
          <template #content>
            <div class="pos-bolum">
              <div class="pos-bolum-baslik">
                <i class="pi pi-user" /> Müşteri
              </div>
              <div class="customer-field">
                <SelectButton
                  v-model="musteriModu"
                  :options="musteriModlari"
                  option-label="label"
                  option-value="value"
                  class="w-full musteri-modu"
                />
                <template v-if="musteriModu === 'musteri'">
                  <AutoComplete
                    v-model="musteriGiris"
                    :suggestions="musteriOnerileri"
                    option-label="ad"
                    placeholder="Müşteri ara (isim, vergi no, telefon)..."
                    class="w-full"
                    @complete="musteriAra($event)"
                    @option-select="musteriSec"
                  >
                    <template #option="slotProps">
                      <div class="musteri-option">
                        {{ slotProps.option.ad }}
                        <span class="musteri-option-detay">{{
                          slotProps.option.vergiNo || slotProps.option.telefon
                        }}</span>
                      </div>
                    </template>
                  </AutoComplete>
                  <div
                    v-if="seciliMusteri"
                    class="secili-musteri-chip"
                  >
                    <i class="pi pi-user" />
                    <span class="secili-musteri-ad">{{ seciliMusteri.ad }}</span>
                    <button
                      type="button"
                      class="secili-musteri-sil"
                      title="Müşteriyi Kaldır"
                      @click="musteriTemizle"
                    >
                      <i class="pi pi-times" />
                    </button>
                  </div>
                  <div
                    v-if="musteriBakiyeUyarisi"
                    class="musteri-bakiye-uyari"
                    :class="musteriBakiyeUyarisi.seviye"
                  >
                    <i :class="musteriBakiyeUyarisi.seviye === 'danger' ? 'pi pi-exclamation-triangle' : 'pi pi-info-circle'" />
                    {{ musteriBakiyeUyarisi.mesaj }}
                  </div>
                  <Button
                    label="+ Yeni"
                    severity="secondary"
                    size="small"
                    @click="yeniMusteriDialog = true"
                  />
                </template>
              </div>
            </div>

            <div class="pos-bolum sepet-bolum">
              <div class="pos-bolum-baslik sepet-baslik">
                <span>Sipariş Özeti ({{ sepet ? sepet.length : 0 }})</span>
                <div class="sepet-baslik-btnler">
                  <Button
                    v-if="sepet && sepet.length"
                    icon="pi pi-save"
                    class="p-button-rounded p-button-text p-button-sm"
                    title="Sepeti Kaydet"
                    @click="sepetKaydet"
                  />
                  <Button
                    v-if="kayitliSepetVar && sepet.length === 0"
                    icon="pi pi-folder-open"
                    class="p-button-rounded p-button-text p-button-sm"
                    title="Kayıtlı Sepeti Yükle"
                    @click="sepetYukle"
                  />
                  <Button
                    v-if="sepet && sepet.length"
                    label="Temizle"
                    icon="pi pi-trash"
                    severity="danger"
                    size="small"
                    @click="sepet = []"
                  />
                </div>
              </div>
              <div
                v-if="sepet && sepet.length === 0"
                class="sepet-bos"
              >
                Sepete ürün ekleyin
              </div>
              <div
                v-for="(item, idx) in sepet"
                :key="idx"
                class="sepet-item"
              >
                <div class="sepet-ust">
                  <span
                    class="sepet-kod"
                    :title="item.barkod"
                  >{{ item.barkod || item.stokKodu }}</span>
                  <span class="sepet-ad">{{ item.ad }}</span>
                  <span class="sepet-tutar">{{ formatCurrency(item.miktar * item.fiyat) }}</span>
                  <button
                    type="button"
                    class="sepet-sil"
                    title="Kaldır"
                    @click="sepetSil(idx)"
                  >
                    <i class="pi pi-times" />
                  </button>
                </div>
                <div class="sepet-kontroller">
                  <div class="sepet-adet-grup">
                    <button
                      type="button"
                      class="adet-btn"
                      @click="miktarAzalt(idx)"
                    >
                      −
                    </button>
                    <input
                      v-model.number="item.miktar"
                      type="number"
                      min="1"
                      class="sepet-adet-input"
                      title="Adet"
                    >
                    <button
                      type="button"
                      class="adet-btn"
                      @click="item.miktar++"
                    >
                      +
                    </button>
                  </div>
                  <select
                    v-model="item.fiyatTipi"
                    class="fiyat-tip-select"
                    @change="fiyatTipiDegisti(item)"
                  >
                    <option
                      v-for="f in item.fiyatlar"
                      :key="f.ad"
                      :value="f.ad"
                    >
                      {{ f.ad }}
                    </option>
                  </select>
                  <input
                    v-model.number="item.fiyat"
                    type="number"
                    step="0.01"
                    class="fiyat-giris-input"
                    title="Birim Fiyatı"
                  >
                </div>
                <div
                  v-if="item.sonAldigiFiyat"
                  class="sepet-son-alis"
                >
                  <i class="pi pi-history" />
                  {{ seciliMusteri?.ad || 'Müşteri' }} bu ürünü en son
                  <strong>{{ formatCurrency(item.sonAldigiFiyat) }}</strong>
                  {{ item.sonAldigiTarih ? '(' + formatDate(item.sonAldigiTarih) + ')' : '' }} aldı
                </div>
              </div>
              <hr class="ozet-ayrac">
              <div class="ozet-satir">
                <span>Toplam Ft³</span>
                <span>{{ toplamFt3.toFixed(2) }} ft³</span>
              </div>
              <div class="ozet-satir">
                <span>İndirim</span>
                <div class="ozet-indirim">
                  <SelectButton
                    v-model="indirimTipi"
                    :options="indirimTipleri"
                    option-label="label"
                    option-value="value"
                  />
                  <InputNumber
                    v-model="indirimDegeri"
                    :min="0"
                    :max="indirimTipi === 'yuzde' ? 100 : toplam"
                    :suffix="indirimTipi === 'yuzde' ? '%' : ' ₺'"
                    class="indirim-input"
                  />
                </div>
              </div>
              <div class="ozet-satir ozet-genel">
                <span>Genel Toplam</span>
                <span class="genel-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
              </div>
            </div>

            <div class="pos-bolum">
              <div class="pos-bolum-baslik">
                <i class="pi pi-wallet" /> Ödeme
              </div>
              <SelectButton
                v-model="odemeDurumu"
                :options="odemeTipleri"
                option-label="label"
                option-value="value"
                class="w-full"
              />
              <div
                v-if="odemeDurumu !== 'yok'"
                class="odenen-satir"
              >
                <label>Ödenen Tutar</label>
                <InputNumber
                  v-model="odenenTutar"
                  :min="0"
                  :max="genelToplam"
                  mode="currency"
                  currency="TRY"
                  locale="tr-TR"
                  class="w-full"
                />
              </div>

              <div
                v-if="odemeDurumu !== 'yok' && odemeYontemi === 'NAKIT'"
                class="odenen-satir"
              >
                <label>Alınan Nakit</label>
                <InputNumber
                  v-model="alinanNakit"
                  :min="0"
                  mode="currency"
                  currency="TRY"
                  locale="tr-TR"
                  class="w-full"
                />
                <div
                  v-if="paraUstu > 0"
                  class="para-ustu"
                >
                  <span>Para Üstü:</span>
                  <strong>{{ formatCurrency(paraUstu) }}</strong>
                </div>
              </div>

              <div
                v-if="odemeDurumu !== 'yok'"
                class="odenen-satir"
              >
                <label>Ödeme Yöntemi</label>
                <div class="odeme-yontem-grid">
                  <button
                    v-for="y in odemeYontemleri"
                    :key="y.value"
                    type="button"
                    class="odeme-yontem-btn"
                    :class="{ active: odemeYontemi === y.value }"
                    @click="odemeYontemi = y.value"
                  >
                    <i :class="y.icon" />
                    {{ y.label }}
                  </button>
                </div>
              </div>

              <div
                v-if="odemeDurumu !== 'yok' && odemeYontemi === 'TAKSIT'"
                class="taksit-panel"
              >
                <div class="odenen-satir">
                  <label>Taksit Çekilen Kurum</label>
                  <InputText
                    v-model="taksitKurum"
                    placeholder="Banka / finans kurumu"
                    class="w-full"
                  />
                </div>
                <div class="odenen-satir">
                  <label>Çekilen Taksit Tutarı</label>
                  <InputNumber
                    v-model="taksitTutar"
                    :min="0"
                    :max="genelToplam"
                    mode="currency"
                    currency="TRY"
                    locale="tr-TR"
                    class="w-full"
                  />
                </div>
              </div>

              <div class="odenen-satir">
                <label>Kasa</label>
                <Dropdown
                  v-model="seciliKasa"
                  :options="kasalar"
                  option-label="ad"
                  option-value="id"
                  placeholder="Kasa seçin"
                  class="w-full"
                />
              </div>

              <div class="odeme-durum">
                <Tag
                  :value="odemeDurumText"
                  :severity="odemeDurumSeverity"
                  class="w-full"
                />
              </div>
              <div
                v-if="kalanTutar > 0"
                class="odeme-kalan"
              >
                <span>Kalan:</span>
                <span class="kalan-deger">{{ formatCurrency(kalanTutar) }}</span>
              </div>
            </div>

            <Button
              label="Satışı Tamamla"
              icon="pi pi-check"
              class="p-button-success w-full satis-buton"
              :loading="kaydediliyor"
              :disabled="sepet.length === 0 || (!anlikMusteri && !seciliMusteri)"
              @click="satisiTamamla"
            />
            <Button
              v-if="sonSatis"
              label="Son Satışı İptal Et"
              icon="pi pi-undo"
              class="p-button-outlined p-button-danger w-full"
              @click="sonSatisiIptalEt"
            />

            <div class="pos-bolum">
              <div class="pos-bolum-baslik">
                <i class="pi pi-truck" /> Teslimat
              </div>
              <div class="teslim-eden-alan">
                <label for="hizli-teslim-eden">Teslim Eden</label>
                <Dropdown
                  id="hizli-teslim-eden"
                  v-model="teslimEden"
                  :options="personelSecenekleri"
                  option-label="label"
                  option-value="value"
                  filter
                  editable
                  placeholder="Personel seçin veya yazın"
                  class="w-full"
                  :show-clear="true"
                >
                  <template #option="s">
                    <div class="personel-opsiyon">
                      <i class="pi pi-user" />
                      <span>{{ s.option.label }}</span>
                    </div>
                  </template>
                </Dropdown>
              </div>

              <div class="teslim-eden-alan">
                <label for="hizli-teslim-durum">Teslim Durumu</label>
                <Dropdown
                  id="hizli-teslim-durum"
                  v-model="teslimDurumu"
                  :options="teslimDurumSecenekleri"
                  option-label="label"
                  option-value="value"
                  class="w-full"
                />
              </div>

              <div class="teslim-eden-alan">
                <label for="hizli-teslim-not">Teslim Notu</label>
                <Textarea
                  id="hizli-teslim-not"
                  v-model="teslimNotu"
                  rows="2"
                  placeholder="Teslimat notu (isteğe bağlı)"
                  class="w-full"
                />
              </div>
            </div>

            <div
              v-if="sepet.length > 0"
              class="pos-bolum"
            >
              <div class="pos-bolum-baslik fis-baslik-satir">
                <span><i class="pi pi-print" /> Fiş Önizleme</span>
                <div class="fis-ayarlar">
                  <Button
                    label="Yazdır (F9)"
                    icon="pi pi-print"
                    size="small"
                    @click="fisiYazdir"
                  />
                  <Button
                    label="Termal"
                    icon="pi pi-send"
                    size="small"
                    severity="secondary"
                    outlined
                    @click="termalYazdir"
                  />
                </div>
              </div>
              <div class="fis-onizleme-kapsam">
                <div
                  id="fisOnizleme"
                  class="fis-onizleme"
                >
                  <div class="fis-header">
                    <div class="fis-baslik">
                      {{ sirketAdi || 'RASPEL ERP' }}
                    </div>
                    <div class="fis-tarih">
                      {{ simdikiTarih }}
                    </div>
                    <div class="fis-fisno">
                      Fiş No: {{ fisNo || '-------' }}
                    </div>
                  </div>
                  <div
                    v-if="musteriAdi"
                    class="fis-musteri"
                  >
                    <span>Müşteri: {{ musteriAdi }}</span>
                  </div>
                  <div class="fis-ayrac">
                    ---
                  </div>
                  <div class="fis-kalemler">
                    <div
                      v-for="i in sepet"
                      :key="i.id"
                      class="fis-kalem"
                    >
                      <div class="fis-kalem-ad">
                        {{ i.ad }} x{{ i.miktar }}
                      </div>
                      <div
                        v-if="fisFiyatli"
                        class="fis-kalem-tutar"
                      >
                        {{ formatCurrency(i.miktar * i.fiyat) }}
                      </div>
                    </div>
                  </div>
                  <div class="fis-ayrac">
                    ---
                  </div>
                  <template v-if="fisFiyatli">
                    <div class="fis-toplam">
                      <span>Ara Toplam</span>
                      <span>{{ formatCurrency(toplam) }}</span>
                    </div>
                    <div
                      v-if="indirimDegeri > 0"
                      class="fis-indirim"
                    >
                      <span>İndirim ({{ indirimTipi === 'yuzde' ? indirimDegeri + '%' : '' }})</span>
                      <span>-{{ formatCurrency(indirimTutari) }}</span>
                    </div>
                    <div class="fis-genel-toplam">
                      <span>GENEL TOPLAM</span>
                      <span class="fis-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
                    </div>
                    <div class="fis-ayrac">
                      ---
                    </div>
                  </template>
                  <div class="fis-odeme">
                    <div
                      v-if="fisFiyatli"
                      class="fis-odeme-satir"
                    >
                      <span>Ödenen</span>
                      <span>{{ formatCurrency(odenenTutar) }}</span>
                    </div>
                    <div
                      v-if="fisFiyatli && kalanTutar > 0"
                      class="fis-odeme-satir"
                    >
                      <span>Kalan</span>
                      <span>{{ formatCurrency(kalanTutar) }}</span>
                    </div>
                    <div class="fis-odeme-satir fis-odeme-durum">
                      <span>Toplam Ürün</span>
                      <span>{{ sepet ? sepet.length : 0 }}</span>
                    </div>
                    <div class="fis-odeme-satir fis-odeme-durum">
                      <span>Durum</span>
                      <span>{{ odemeDurumText }}</span>
                    </div>
                  </div>
                  <div class="fis-footer">
                    <div class="fis-ayrac">
                      ---
                    </div>
                    <div class="fis-tesekkur">
                      {{ fisAltNotu || 'Bizi tercih ettiginiz icin tesekkur ederiz!' }}
                    </div>
                    <div
                      v-if="authStore?.kullanici?.displayName"
                      class="fis-satici"
                    >
                      Islem Yapan: {{ authStore?.kullanici?.displayName }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="pos-bolum">
              <div class="gunluk-baslik">
                <span><i class="pi pi-clock" /> Bugünkü Satışlar ({{ gunlukSatislar.length }})</span>
                <Button
                  icon="pi pi-refresh"
                  class="p-button-sm p-button-text"
                  @click="gunlukSatislariYukle"
                />
              </div>
              <div
                v-if="gunlukSatislar.length === 0"
                class="sepet-bos"
              >
                Bugün henüz satış yapılmadı
              </div>
              <div
                v-for="s in gunlukSatislar"
                :key="s.id"
                class="gunluk-satis-satir"
              >
                <div class="gunluk-satis-bilgi">
                  <span class="gunluk-satis-no">{{ s.faturaNumarasi }}</span>
                  <span class="gunluk-satis-cari">{{ s.cariHesapAd || 'Anlık' }}</span>
                </div>
                <span class="gunluk-satis-tutar">{{ formatCurrency(s.genelToplam) }}</span>
              </div>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>

  <Dialog
    v-model:visible="yeniMusteriDialog"
    header="Yeni Cari Hesap Ekle"
    :modal="true"
    :style="{ width: '520px' }"
    class="yeni-musteri-dialog"
  >
    <div class="ym-form-grid">
      <div class="field full-width">
        <label for="ym-ad">Ad / Firma Adı <span class="required">*</span></label>
        <InputText
          id="ym-ad"
          v-model="yeniMusteri.ad"
          placeholder="Örn: Ahmet Yılmaz veya Yılmaz A.Ş."
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-telefon">Telefon</label>
        <InputText
          id="ym-telefon"
          v-model="yeniMusteri.telefon"
          placeholder="05XX XXX XX XX"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-email">E-posta</label>
        <InputText
          id="ym-email"
          v-model="yeniMusteri.email"
          placeholder="ornek@domain.com"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-vergi">Vergi / TC No</label>
        <InputText
          id="ym-vergi"
          v-model="yeniMusteri.vergiNo"
          placeholder="10 veya 11 haneli numara"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-tur">Cari Türü</label>
        <Dropdown
          id="ym-tur"
          v-model="yeniMusteri.tur"
          :options="['Musteri', 'Tedarikci', 'Her Ikisi']"
          placeholder="MÜŞTERİ"
          class="w-full"
        />
      </div>
      <div class="field full-width">
        <label for="ym-adres">Adres</label>
        <Textarea
          id="ym-adres"
          v-model="yeniMusteri.adres"
          rows="2"
          placeholder="Fatura adresi..."
          class="w-full"
        />
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer-btns">
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="yeniMusteriDialog = false"
        />
        <Button
          label="Kaydet & Seç"
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
    v-model:visible="satisOzetDialog"
    header="Satış Tamamlandı"
    :modal="true"
    style="width: 420px"
  >
    <div
      v-if="satisOzet"
      class="satis-ozet"
    >
      <div class="satis-ozet-baslik">
        <i class="pi pi-check-circle" />
        <span>İşlem Başarılı</span>
      </div>
      <div class="satis-ozet-satir">
        <span>Fatura No</span>
        <strong>{{ satisOzet.faturaNo }}</strong>
      </div>
      <div class="satis-ozet-satir">
        <span>Toplam</span>
        <strong>{{ formatCurrency(satisOzet.toplam) }}</strong>
      </div>
      <div class="satis-ozet-satir">
        <span>Ödenen</span>
        <strong>{{ formatCurrency(satisOzet.odenen) }}</strong>
      </div>
      <div
        v-if="satisOzet.kalan > 0"
        class="satis-ozet-satir"
      >
        <span>Kalan</span>
        <strong class="borc">{{ formatCurrency(satisOzet.kalan) }}</strong>
      </div>
      <div
        v-if="satisOzet.paraUstu > 0"
        class="satis-ozet-satir"
      >
        <span>Para Üstü</span>
        <strong class="para">{{ formatCurrency(satisOzet.paraUstu) }}</strong>
      </div>
    </div>
    <template #footer>
      <Button
        label="Kapat"
        icon="pi pi-check"
        class="p-button-primary"
        @click="satisOzetDialog = false"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { faturaAPI, cariHesapAPI, personelAPI, stokAPI, kasaAPI } from '../api/index.js'
import { useOfflineSatisKuyrugu } from '../composables/useOfflineSatisKuyrugu.js'
import AutoComplete from 'primevue/autocomplete'
import SelectButton from 'primevue/selectbutton'
import { useKisayollar } from '../composables/useKisayollar.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import { escPosFisiUret, escPosYazdir } from '../utils/escpos.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()
const kategoriStore = useKategoriStore()
const offlineKuyruk = useOfflineSatisKuyrugu()

const offlineKuyruguSenkronizeEt = async () => {
  try {
    const gonderilen = await offlineKuyruk.senkronizeEt((s) => faturaAPI.create(s))
    if (gonderilen > 0) {
      toast.add({ severity: 'success', summary: 'Senkronize edildi', detail: `${gonderilen} satış gönderildi`, life: 3000 })
    }
  } catch {
    /* empty */
  }
}

useKisayollar({
  kaydet: () => satisiTamamla(),
  iptal: () => {
    if (yeniMusteriDialog.value) yeniMusteriDialog.value = false
  },
  yeni: () => {
    seriNoArama.value = ''
    sepet.value = []
  },
  yazdir: () => fisiYazdir()
})

const handlePosKeys = (e) => {
  if (e.key === 'F2') {
    e.preventDefault()
    sepet.value = []
    toast.add({ severity: 'info', summary: 'Kısayol F2', detail: 'Sepet temizlendi', life: 2000 })
  } else if (e.key === 'F4') {
    e.preventDefault()
    musteriModu.value = 'musteri'
    toast.add({ severity: 'info', summary: 'Kısayol F4', detail: 'Müşteri seçimi aktif', life: 2000 })
  } else if (e.key === 'F9') {
    e.preventDefault()
    odemeDurumu.value = 'tam'
    if (sepet.value.length && (anlikMusteri.value || seciliMusteri.value)) satisiTamamla()
  } else if (e.key === 'F10') {
    e.preventDefault()
    odemeDurumu.value = 'kismi'
    if (sepet.value.length && (anlikMusteri.value || seciliMusteri.value)) satisiTamamla()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handlePosKeys)
  window.addEventListener('online', offlineKuyruguSenkronizeEt)
  if (navigator.onLine) offlineKuyruguSenkronizeEt()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePosKeys)
  window.removeEventListener('online', offlineKuyruguSenkronizeEt)
})

const sirketAdi = computed(() => authStore.sirketAdi || '')

const seriNoArama = ref('')
const globalBarkod = ref('')
const scannerAcik = ref(false)
const barkodInputRef = ref(null)

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
  let urun = stokStore.stoklar.find((s) => s.barkod === barkod || s.seriNo === barkod)
  if (urun) {
    sepeteEkle(urun)
    toast.add({ severity: 'success', summary: 'Ürün Eklendi', detail: urun.ad, life: 2000 })
  } else {
    // Sunucuda ara (büyük envanterde tümü yüklenmemiş olabilir)
    try {
      const r = await stokAPI.ara(barkod)
      const bulunan = (r.data || []).find((s) => s.barkod === barkod || s.seriNo === barkod)
      if (bulunan) {
        sepeteEkle(bulunan)
        toast.add({ severity: 'success', summary: 'Ürün Eklendi', detail: bulunan.ad, life: 2000 })
        return
      }
    } catch {
      /* sunucu araması başarısız olabilir */
    }
    toast.add({ severity: 'warn', summary: 'Bulunamadı', detail: `"${barkod}" barkodlu ürün bulunamadı`, life: 3000 })
  }
}

const filtreKategori = ref(null)
const filtreArac = ref(null)

const seciliMusteri = ref(null)
const musteriGiris = ref('')
const teslimEden = ref('')
const teslimDurumu = ref('BEKLIYOR')
const teslimNotu = ref('')
const personelListesi = ref([])
const musteriModu = ref('musteri')
const musteriModlari = ref([
  { label: 'Perakende', value: 'perakende', icon: 'pi pi-shopping-cart' },
  { label: 'Müşteri', value: 'musteri', icon: 'pi pi-users' }
])
const anlikMusteri = computed(() => musteriModu.value === 'perakende')

watch(musteriModu, (mod) => {
  if (mod === 'perakende') {
    seciliMusteri.value = null
    musteriGiris.value = ''
  }
})
const musteriOnerileri = ref([])
const yeniMusteriDialog = ref(false)
const yeniMusteri = ref({ ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' })
const musteriKaydediliyor = ref(false)

const sepet = ref([])
const kaydediliyor = ref(false)
const fisNo = ref('')
const fisFiyatli = ref(localStorage.getItem('raspel_fis_fiyatli') !== 'false')
const fisAltNotu = ref(localStorage.getItem('raspel_fis_notu') || 'Bizi tercih ettiğiniz için teşekkür ederiz!')

// Sekmeler arası canlı senkron: Ayarlar'da değişince POS'a anında yansır
const dinleyici = (e) => {
  if (e.key === 'raspel_fis_fiyatli' && e.newValue !== null) {
    fisFiyatli.value = e.newValue !== 'false'
  } else if (e.key === 'raspel_fis_notu' && e.newValue !== null) {
    fisAltNotu.value = e.newValue
  }
}
onMounted(() => window.addEventListener('storage', dinleyici))
onUnmounted(() => window.removeEventListener('storage', dinleyici))

const indirimTipi = ref('tutar')
const indirimTipleri = ref([
  { label: '₺', value: 'tutar' },
  { label: '%', value: 'yuzde' }
])
const indirimDegeri = ref(0)

const odemeDurumu = ref('tam')
const odemeTipleri = ref([
  { label: 'Tam Ödeme', value: 'tam' },
  { label: 'Yarım Ödeme', value: 'yarim' },
  { label: 'Ödeme Yok', value: 'yok' }
])
const odenenTutar = ref(0)

// Ödeme yöntemi (Nakit/Kart/Havale/Taksit)
const odemeYontemi = ref('NAKIT')
const odemeYontemleri = [
  { label: 'Nakit', value: 'NAKIT', icon: 'pi pi-money-bill' },
  { label: 'Kart', value: 'KART', icon: 'pi pi-credit-card' },
  { label: 'Havale', value: 'HAVALE', icon: 'pi pi-send' },
  { label: 'Taksit', value: 'TAKSIT', icon: 'pi pi-calendar' }
]

// Taksit bilgisi
const taksitKurum = ref('')
const taksitTutar = ref(0)

// Kasa seçimi
const seciliKasa = ref(null)
const kasalar = ref([])

// Para üstü
const alinanNakit = ref(0)
const paraUstu = computed(() => {
  if (odemeYontemi.value !== 'NAKIT' || odemeDurumu.value === 'yok') return 0
  return Math.max(0, (alinanNakit.value || 0) - (odenenTutar.value || 0))
})

// Satış sonrası özet
const satisOzet = ref(null)
const satisOzetDialog = ref(false)

// Günlük satış geçmişi
const gunlukSatislar = ref([])
const sonSatis = ref(null)

const kasalariYukle = async () => {
  try {
    const r = await kasaAPI.getAllKasalar()
    kasalar.value = r.data?.content || r.data || []
    if (kasalar.value.length > 0 && !seciliKasa.value) {
      seciliKasa.value = kasalar.value[0].id
    }
  } catch {
    kasalar.value = []
  }
}

const gunlukSatislariYukle = async () => {
  try {
    const bugun = new Date().toISOString().split('T')[0]
    const r = await faturaAPI.getAll({ size: 50, sort: 'tarih,desc' })
    const list = r.data?.content || r.data || []
    gunlukSatislar.value = list.filter((f) => f.tur === 'SATIS' && f.tarih === bugun)
  } catch {
    gunlukSatislar.value = []
  }
}

const kategoriler = computed(() => kategoriStore.kategoriler || [])
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
  await Promise.all((urunler || []).map(async (u) => {
    if (u.id == null || yeni[u.id]) return
    try {
      const r = await stokAPI.getFiyatlar(u.id)
      const liste = r.data || []
      if (liste.length) {
        yeni[u.id] = liste.map((f) => ({ ad: f.ad || f.fiyatTipi || f.tip || 'Fiyat', fiyat: Number(f.fiyat) }))
      }
    } catch {
      /* fiyat listesi alınamadı */
    }
  }))
  urunFiyatlari.value = yeni
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
  toast.add({ severity: 'success', summary: 'Sepet Kaydedildi', detail: 'Kayıtlı sepete istediğinizde dönebilirsiniz.', life: 3000 })
}

const sepetYukle = () => {
  try {
    const kayit = JSON.parse(localStorage.getItem(SEPET_KEY) || '[]')
    sepet.value = kayit
    kayitliSepetVar.value = false
    localStorage.removeItem(SEPET_KEY)
    toast.add({ severity: 'info', summary: 'Sepet Yüklendi', detail: 'Kayıtlı sepet geri yüklendi.', life: 3000 })
  } catch {
    /* empty */
  }
}

const aracListesi = computed(() => {
  const araclar = new Set()
  stokStore.stoklar.forEach((s) => {
    if (s.marka) araclar.add(s.marka)
  })
  return [...araclar].sort()
})

const toplam = computed(() => sepet.value.reduce((t, i) => t + i.miktar * i.fiyat, 0))

const toplamFt3 = computed(() =>
  sepet.value.reduce((t, i) => {
    const hacim = i.birimHacim || 1
    return t + i.miktar * hacim
  }, 0)
)

const indirimTutari = computed(() => {
  if (indirimDegeri.value <= 0) return 0
  if (indirimTipi.value === 'yuzde') return toplam.value * (Math.min(indirimDegeri.value, 100) / 100)
  return Math.min(indirimDegeri.value, toplam.value)
})

const genelToplam = computed(() => Math.max(0, toplam.value - indirimTutari.value))

watch(odemeDurumu, (v) => {
  if (v === 'tam') odenenTutar.value = genelToplam.value
  else if (v === 'yarim') odenenTutar.value = genelToplam.value / 2
  else odenenTutar.value = 0
})

watch(genelToplam, () => {
  if (odemeDurumu.value === 'tam') odenenTutar.value = genelToplam.value
  else if (odemeDurumu.value === 'yarim') odenenTutar.value = genelToplam.value / 2
})

const kalanTutar = computed(() => Math.max(0, genelToplam.value - odenenTutar.value))

const odemeDurumText = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'Ödenmedi'
  if (odenenTutar.value >= genelToplam.value) return 'Tamamen Ödendi'
  return 'Kısmi Ödendi'
})

const odemeDurumEnum = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'ODENMEDI'
  if (odenenTutar.value >= genelToplam.value) return 'ODENDI'
  return 'KISMI_ODENDI'
})

const odemeDurumSeverity = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'danger'
  if (odenenTutar.value >= genelToplam.value) return 'success'
  return 'warning'
})

const musteriAdi = computed(() => {
  if (anlikMusteri.value) return 'Anlık Müşteri'
  return seciliMusteri.value?.ad || ''
})

const musteriBakiyeUyarisi = computed(() => {
  if (!seciliMusteri.value) return null
  const bakiye = seciliMusteri.value.bakiye
  const krediLimiti = seciliMusteri.value.krediLimiti
  if (krediLimiti != null && bakiye != null && bakiye < 0 && Math.abs(bakiye) >= krediLimiti) {
    return { seviye: 'danger', mesaj: `Kredi limiti aşıldı! Borç: ${formatCurrency(Math.abs(bakiye))}` }
  }
  if (bakiye != null && bakiye < 0) {
    return { seviye: 'warn', mesaj: `Borç: ${formatCurrency(Math.abs(bakiye))}` }
  }
  if (bakiye != null && bakiye > 0) {
    return { seviye: 'info', mesaj: `Alacak: ${formatCurrency(bakiye)}` }
  }
  return null
})

const filtrelenmisUrunler = computed(() => {
  let list = stokStore.stoklar || []

  if (filtreKategori.value) {
    list = list.filter((u) => u.kategori === filtreKategori.value.ad)
  }

  if (filtreArac.value) {
    list = list.filter((u) => u.marka === filtreArac.value)
  }

  if (seriNoArama.value) {
    const q = seriNoArama.value.toLowerCase()
    list = list.filter(
      (u) =>
        u.ad?.toLowerCase().includes(q) ||
        u.stokKodu?.toLowerCase().includes(q) ||
        u.barkod?.toLowerCase().includes(q) ||
        u.seriNo?.toLowerCase().includes(q)
    )
  }

  return list.slice(0, 100)
})

// Görünen ürünler değiştiğinde çoklu fiyat listelerini besle
watch(filtrelenmisUrunler, (list) => {
  if (list && list.length) urunFiyatlariniYukle(list)
}, { immediate: true })

const kritikStokMu = (u) => {
  if (!u?.miktar) return false
  if (u.minMiktar != null && u.miktar <= u.minMiktar) return true
  return u.miktar <= 10
}

const simdikiTarih = computed(() => {
  const d = new Date()
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
})


onMounted(async () => {
  try {
    await Promise.all([
      cariHesapStore.getAllCariHesaplar(),
      stokStore.getAll(),
      kategoriStore.getAllKategoriler(),
      personelListesiniYukle(),
      cokSatanlariYukle(),
      kasalariYukle(),
      gunlukSatislariYukle()
    ])
    kayitliSepetVar.value = !!localStorage.getItem('raspel_kayitli_sepet')
  } catch (e) {
    console.error('Yukleme hatasi', e)
  }
})

const personelSecenekleri = computed(() =>
  personelListesi.value
    .filter((p) => p.aktif !== false)
    .map((p) => ({ label: `${p.ad || ''} ${p.soyad || ''}`.trim(), value: `${p.ad || ''} ${p.soyad || ''}`.trim() }))
)

const teslimDurumSecenekleri = [
  { label: 'Bekliyor', value: 'BEKLIYOR' },
  { label: 'Yolda', value: 'YOLDA' },
  { label: 'Teslim Edildi', value: 'TESLIM_EDILDI' }
]

const personelListesiniYukle = async () => {
  try {
    const r = await personelAPI.getAll({ size: 500 })
    personelListesi.value = r.data?.content || r.data || []
  } catch {
    personelListesi.value = []
  }
}

const musteriAra = (event) => {
  const query = event.query
  const kaynak = cariHesapStore?.cariHesaplar || []
  if (!query) {
    musteriOnerileri.value = kaynak.slice(0, 20)
    return
  }
  const q = query.toLowerCase()
  musteriOnerileri.value = kaynak.filter(
      (c) => c.ad?.toLowerCase().includes(q) || c.vergiNo?.toLowerCase().includes(q) || c.telefon?.includes(query)
    )
    .slice(0, 20)
}

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
    toastBildirim.uyari('Ad / Firma adı zorunludur')
    return
  }
  musteriKaydediliyor.value = true
  try {
    const r = await cariHesapAPI.create(yeniMusteri.value)
    seciliMusteri.value = r.data
    musteriGiris.value = ''
    yeniMusteriDialog.value = false
    yeniMusteri.value = { ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' }
    toastBildirim.basarili('Cari hesap oluşturuldu')
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || 'Kayıt başarısız')
  }
  musteriKaydediliyor.value = false
}

const sepeteEkle = async (u) => {
  const varOlan = sepet.value.find((i) => i.id === u.id)
  if (varOlan) {
    varOlan.miktar++
    return
  }
  const stdFiyat = u.fiyat || u.satisFiyati || 0
  // Çoklu fiyat tanımlıysa onları kullan, yoksa stoğun fiyat listesini çek, yoksa sabit kademelere düş
  let fiyatlar = (u.fiyatlar && u.fiyatlar.length > 0)
    ? u.fiyatlar.map((f) => ({ ad: f.ad, fiyat: f.fiyat }))
    : null
  if (!fiyatlar) {
    const tckilen = await urunFiyatlariniYukleTek(u)
    fiyatlar = (tckilen && tckilen.length > 0) ? tckilen : [
      { ad: 'Perakende', fiyat: stdFiyat },
      { ad: 'Toptan', fiyat: Math.round(stdFiyat * 0.9 * 100) / 100 },
      { ad: 'Özel', fiyat: Math.round(stdFiyat * 0.8 * 100) / 100 }
    ]
  }

  const yeniItem = {
    id: u.id,
    ad: u.ad,
    stokKodu: u.stokKodu,
    barkod: u.barkod,
    miktar: 1,
    fiyat: fiyatlar[0]?.fiyat ?? stdFiyat,
    fiyatlar,
    fiyatTipi: fiyatlar[0]?.ad ?? 'Perakende',
    birim: u.birim || 'adet',
    birimHacim: u.birimHacim || 1,
    sonAldigiFiyat: null,
    sonAldigiTarih: null,
    sonAldigiBilgisiYukleniyor: false
  }

  // Seçili müşteri varsa ürünü en son hangi fiyata aldığını sor
  if (seciliMusteri.value?.id) {
    yeniItem.sonAldigiBilgisiYukleniyor = true
    try {
      const r = await faturaAPI.cariUrunFiyatGecmisi(seciliMusteri.value.id, u.id)
      const data = r.data
      if (data && data.sonFiyat != null) {
        yeniItem.sonAldigiFiyat = data.sonFiyat
        const enSon = (data.gecmis || [])[0]
        yeniItem.sonAldigiTarih = enSon?.tarih || null
        // Müşteri daha önce almışsa son aldığı fiyat ile öner, fiyatlara da ekle
        yeniItem.fiyat = data.sonFiyat
        if (!fiyatlar.some((f) => f.ad === 'Son Aldığı')) {
          fiyatlar.unshift({ ad: 'Son Aldığı', fiyat: data.sonFiyat })
        }
        yeniItem.fiyatTipi = fiyatlar[0]?.ad ?? 'Perakende'
      }
    } catch {
      /* cari fiyat geçmişi alınamadı */
    }
    yeniItem.sonAldigiBilgisiYukleniyor = false
  }

  sepet.value.push(yeniItem)
}

const fiyatTipiDegisti = (item) => {
  const secili = item.fiyatlar?.find((f) => f.ad === item.fiyatTipi)
  if (secili) item.fiyat = secili.fiyat
}

// Müşteri seçilince sepetteki tüm ürünlere cari bazlı fiyatı uygular
const sepeteCariFiyatUygula = async () => {
  const cariId = seciliMusteri.value?.id
  if (!cariId || !sepet.value.length) return
  await Promise.all(sepet.value.map(async (item) => {
    try {
      const r = await faturaAPI.cariUrunFiyatGecmisi(cariId, item.id)
      const data = r.data
      if (data && data.sonFiyat != null) {
        item.sonAldigiFiyat = data.sonFiyat
        const enSon = (data.gecmis || [])[0]
        item.sonAldigiTarih = enSon?.tarih || null
        item.fiyat = data.sonFiyat
        if (!item.fiyatlar.some((f) => f.ad === 'Son Aldığı')) {
          item.fiyatlar.unshift({ ad: 'Son Aldığı', fiyat: data.sonFiyat })
        }
        item.fiyatTipi = 'Son Aldığı'
      }
    } catch {
      /* cari fiyat geçmişi alınamadı */
    }
  }))
}

const miktarAzalt = (idx) => {
  if (sepet.value[idx].miktar > 1) sepet.value[idx].miktar--
  else sepetSil(idx)
}

const sepetSil = (idx) => {
  sepet.value.splice(idx, 1)
}

const fisiYazdir = () => {
  if (!sepet.value.length) return
  fisNo.value = 'F-' + Date.now().toString(36).toUpperCase()

  const fiyatli = fisFiyatli.value
  const kalemHtml = sepet.value
    .map((i) => {
      const ad = escapeHtml(i.ad || '')
      const satir = `<div class="satir"><span class="ad">${ad} x${i.miktar}</span>${fiyatli ? `<span class="tutar">${formatCurrency(i.miktar * i.fiyat)}</span>` : ''}</div>`
      return satir
    })
    .join('')

  const ozetHtml = fiyatli
    ? `
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="satir"><span class="ad">Ara Toplam</span><span class="tutar">${formatCurrency(toplam.value)}</span></div>
    ${indirimDegeri.value > 0 ? `<div class="satir"><span class="ad">İndirim${indirimTipi.value === 'yuzde' ? ' (' + indirimDegeri.value + '%)' : ''}</span><span class="tutar">-${formatCurrency(indirimTutari.value)}</span></div>` : ''}
    <div class="satir genel"><span class="ad">GENEL TOPLAM</span><span class="tutar">${formatCurrency(genelToplam.value)}</span></div>
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="satir"><span class="ad">Ödenen</span><span class="tutar">${formatCurrency(odenenTutar.value)}</span></div>
    ${kalanTutar.value > 0 ? `<div class="satir"><span class="ad">Kalan</span><span class="tutar">${formatCurrency(kalanTutar.value)}</span></div>` : ''}
  `
    : ''

  const musteriHtml = musteriAdi.value ? `<div class="musteri">Müşteri: ${escapeHtml(musteriAdi.value)}</div>` : ''

  const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Fiş Önizleme</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: 'Courier New', monospace; width: 80mm; margin: 0 auto; color: #000; font-size: 12px; }
  .aracubuk {
    position: fixed; top: 0; left: 0; right: 0; z-index: 10;
    width: 100%; padding: 10px; text-align: center;
    background: #1e293b; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  }
  .aracubuk button {
    font-family: Arial, sans-serif; font-size: 14px; font-weight: 600;
    padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer;
    background: #3b82f6; color: #fff; margin: 0 4px;
  }
  .aracubuk button.iptal { background: #475569; }
  .fis { padding: 6px 4px; margin-top: 52px; }
  .baslik { text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 4px; }
  .tarih, .fisno { text-align: center; font-size: 10px; margin-top: 2px; }
  .musteri { margin-top: 6px; font-size: 11px; }
  .ayrac { text-align: center; color: #555; margin: 4px 0; letter-spacing: 1px; }
  .satir { display: flex; justify-content: space-between; padding: 2px 0; }
  .satir .ad { flex: 1; white-space: pre-wrap; word-break: break-word; padding-right: 6px; }
  .satir .tutar { white-space: nowrap; }
  .satir.genel { border-top: 2px solid #000; font-weight: bold; padding-top: 4px; margin-top: 4px; }
  .tesekkur { text-align: center; margin-top: 8px; font-size: 10px; }
  @media print {
    .aracubuk { display: none !important; }
    .fis { margin-top: 0; }
  }
</style>
</head>
<body>
  <div class="aracubuk">
    <button onclick="window.print()">Yazdır</button>
    <button class="iptal" onclick="window.close()">Kapat</button>
  </div>
  <div class="fis">
    <div class="baslik">${escapeHtml(sirketAdi.value || 'RASPEL ERP')}</div>
    <div class="tarih">${simdikiTarih.value}</div>
    <div class="fisno">Fiş No: ${fisNo.value}</div>
    ${musteriHtml}
    ${teslimEden.value ? `<div class="musteri">Teslim Eden: ${escapeHtml(teslimEden.value)}</div>` : ''}
    ${teslimDurumu.value && teslimDurumu.value !== 'BEKLIYOR' ? `<div class="musteri">Teslim: ${teslimDurumEtiketi(teslimDurumu.value)}</div>` : ''}
    ${teslimNotu.value ? `<div class="musteri">Not: ${escapeHtml(teslimNotu.value)}</div>` : ''}
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    ${kalemHtml}
    ${ozetHtml}
    <div class="satir"><span class="ad">Toplam Ürün</span><span class="tutar">${sepet.value.length}</span></div>
    <div class="satir"><span class="ad">Durum</span><span class="tutar">${odemeDurumText.value}</span></div>
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="tesekkur">Islem Yapan: ${escapeHtml(authStore?.kullanici?.displayName || '-')}</div>
    <div class="tesekkur">Iyi gunler dileriz</div>
  </div>
</body>
</html>`

  const win = window.open('', '_blank', 'width=400,height=600')
  if (!win) {
    toastBildirim.hata('Pencere engellendi. Pop-up engelleyiciyi kapatın.')
    return
  }
  win.document.open()
  win.document.write(html)
  win.document.close()
  setTimeout(() => {
    try {
      win.focus()
      win.print()
    } catch (e) {
      console.error('Termal yazıcı hatası:', e)
    }
  }, 300)
}

const termalYazdir = async () => {
  if (!sepet.value.length) return
  const bytes = escPosFisiUret({
    baslik: sirketAdi.value || 'RASPEL ERP',
    tarih: simdikiTarih.value,
    fisNo: fisNo.value || undefined,
    kalemler: sepet.value.map((i) => ({ ad: i.ad, adet: i.miktar, tutar: i.miktar * i.fiyat })),
    toplam: genelToplam.value,
    altNot: fisAltNotu.value || undefined
  })
  try {
    const gonderildi = await escPosYazdir(bytes)
    if (!gonderildi) {
      // WebUSB desteklenmiyorsa browser print fallback
      fisiYazdir()
    }
  } catch {
    toastBildirim.hata('Termal yazıcıya gönderilemedi')
  }
}

const escapeHtml = (metin) => {
  return String(metin ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const teslimDurumEtiketi = (d) => ({ BEKLIYOR: 'Bekliyor', YOLDA: 'Yolda', TESLIM_EDILDI: 'Teslim Edildi' })[d] || d

const satisiTamamla = async () => {
  if (!anlikMusteri.value && !seciliMusteri.value) return
  if (sepet.value.length === 0) return
  if (odemeYontemi.value === 'TAKSIT' && (!taksitKurum.value.trim() || !taksitTutar.value || taksitTutar.value <= 0)) {
    toastBildirim.uyari('Taksit seçildiğinde kurum ve çekilen tutar girilmelidir')
    return
  }
  kaydediliyor.value = true
  const satisVerisi = {
    cariHesapId: anlikMusteri.value ? null : seciliMusteri.value.id,
    cariHesapAdi: anlikMusteri.value ? 'Anlik Musteri' : seciliMusteri.value.ad,
    tur: 'SATIS',
    durum: 'KESILDI',
    tarih: new Date().toISOString().split('T')[0],
    teslimEden: teslimEden.value || null,
    teslimDurumu: teslimDurumu.value || 'BEKLIYOR',
    teslimNotu: teslimNotu.value || null,
    aciklama: 'Hizli Satis',
    araToplam: toplam.value,
    indirim: indirimTutari.value,
    genelToplam: genelToplam.value,
    odenenTutar: odenenTutar.value,
    odemeDurumu: odemeDurumEnum.value,
    odemeYontemi: odemeYontemi.value,
    taksitKurum: odemeYontemi.value === 'TAKSIT' ? taksitKurum.value : null,
    taksitTutar: odemeYontemi.value === 'TAKSIT' ? taksitTutar.value : null,
    kasaId: seciliKasa.value || null,
    kalemler: sepet.value.map((i) => ({
      stokId: i.id,
      aciklama: i.ad,
      adet: i.miktar,
      birimFiyat: i.fiyat,
      kdvOrani: 20,
      tutar: Math.round(i.miktar * i.fiyat * 100) / 100
    }))
  }
  try {
    const yanit = await faturaAPI.create(satisVerisi)
    sonSatis.value = yanit.data
    satisOzet.value = {
      faturaNo: yanit.data?.faturaNumarasi,
      toplam: genelToplam.value,
      odenen: odenenTutar.value,
      kalan: kalanTutar.value,
      yontem: odemeYontemi.value,
      paraUstu: paraUstu.value
    }
    toastBildirim.basarili(`Satış tamamlandı - ${formatCurrency(genelToplam.value)}`)
    try {
      fisiYazdir()
    } catch {
      /* empty */
    }
    sepetiTemizle()
    alinanNakit.value = 0
    satisOzetDialog.value = true
    gunlukSatislariYukle()
    kasalariYukle()
  } catch (e) {
    // Ağ yoksa satışı kuyruğa al (offline satış)
    if (!e?.response) {
      offlineKuyruk.ekle(satisVerisi)
      toast.add({
        severity: 'warn',
        summary: 'Çevrimdışı satış kuyruğa alındı',
        detail: 'Bağlantı gelince otomatik gönderilecek',
        life: 4000
      })
      try {
        fisiYazdir()
      } catch {
        /* empty */
      }
      sepetiTemizle()
    } else {
      toastBildirim.hata(e?.response?.data?.message || 'Satış başarısız')
    }
  }
  kaydediliyor.value = false
}

// Son satışı iptal et (stok geri alınır)
const sonSatisiIptalEt = async () => {
  if (!sonSatis.value?.id) {
    toastBildirim.uyari('Geri alınacak son satış yok')
    return
  }
  try {
    await faturaAPI.updateDurum(sonSatis.value.id, 'IPTAL')
    toastBildirim.basarili('Son satış iptal edildi, stok geri alındı')
    sonSatis.value = null
    gunlukSatislariYukle()
    stokStore.getAll()
    kasalariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İptal başarısız')
  }
}

const sepetiTemizle = () => {
  sepet.value = []
  seciliMusteri.value = null
  musteriGiris.value = ''
  teslimEden.value = ''
  teslimDurumu.value = 'BEKLIYOR'
  teslimNotu.value = ''
  musteriModu.value = 'musteri'
  indirimDegeri.value = 0
  odemeDurumu.value = 'tam'
  odenenTutar.value = 0
}
</script>

<style scoped>
.pos-container {
  padding: 0;
  min-height: 0;
  max-width: 100%;
}
.pos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.breadcrumb {
  font-size: 13px;
  color: var(--text-muted);
}
.breadcrumb i {
  margin-right: 4px;
}
.user-info {
  font-size: 13px;
  color: var(--text-secondary);
}
.user-info i {
  margin-right: 4px;
}

.pos-body {
  display: flex;
  gap: 16px;
  align-items: stretch;
}
.pos-left {
  flex: 1 1 0;
  min-width: 0;
}
.pos-right {
  flex: 0 0 400px;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.siparis-kart :deep(.p-card-content) {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.pos-bolum {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pos-bolum + .pos-bolum {
  border-top: 1px solid var(--border);
  padding-top: 18px;
}
.pos-bolum-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.pos-bolum-baslik i {
  color: var(--accent);
  font-size: 14px;
}
.pos-bolum-baslik.sepet-baslik {
  justify-content: space-between;
}

.filter-card :deep(.p-card-content) {
  padding-top: 0;
}
.filter-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.filter-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-select {
  flex: 1;
}

.pos-arac-cubugu {
  display: grid;
  grid-template-columns: 1.4fr 1.6fr 1fr 1fr auto;
  gap: 8px;
  align-items: stretch;
  margin-bottom: 12px;
}
.pos-arac-cubugu .arama-kutusu {
  display: block;
  position: relative;
}
.pos-arac-cubugu .arama-kutusu .p-inputtext {
  width: 100%;
  height: 42px;
  padding-left: 2.75rem !important;
}
.pos-arac-cubugu .arama-kutusu > i {
  position: absolute;
  left: 0.9rem;
  top: 50%;
  transform: translateY(-50%);
  margin: 0;
  font-size: 15px;
  color: var(--text-muted);
  z-index: 1;
}
.pos-arac-cubugu .arac-dropdown {
  height: 42px;
  width: 100%;
}
.pos-arac-cubugu .arac-dropdown :deep(.p-dropdown) {
  width: 100%;
  height: 42px;
}
.pos-arac-cubugu .arac-dropdown :deep(.p-dropdown-label) {
  line-height: 42px;
  padding-top: 0;
  padding-bottom: 0;
}
.pos-arac-cubugu .p-button {
  height: 42px;
}

@media (max-width: 900px) {
  .pos-arac-cubugu {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 520px) {
  .pos-arac-cubugu {
    grid-template-columns: 1fr;
  }
}

.product-section {
  margin-top: 4px;
}
.product-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.product-header h3 i {
  color: var(--accent);
}
.urun-sayaci {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent);
  background: rgba(59, 130, 246, 0.12);
  padding: 1px 8px;
  border-radius: 10px;
}
.product-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding-bottom: 8px;
}

.product-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-card:hover {
  border-color: var(--accent);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}
.product-kod {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  min-width: 90px;
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-name {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-price {
  display: inline-block;
  font-size: 14px;
  font-weight: 700;
  color: var(--accent);
  padding: 2px 10px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  white-space: nowrap;
}
.product-cari-fiyat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #10b981;
  background: rgba(16, 185, 129, 0.12);
  padding: 2px 8px;
  border-radius: 12px;
  white-space: nowrap;
}
.product-cari-fiyat i {
  font-size: 11px;
}
.empty-products {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.cok-satanlar-section {
  margin-top: 8px;
}
.cok-satanlar-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.cok-satan-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.cok-satan-chip:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.cok-satan-ad {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.cok-satan-fiyat {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}
.empty-products i {
  font-size: 36px;
  display: block;
  margin-bottom: 8px;
}

.customer-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.teslim-eden-alan {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.teslim-eden-alan label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.personel-opsiyon {
  display: flex;
  align-items: center;
  gap: 8px;
}
.personel-opsiyon i {
  font-size: 12px;
  color: var(--text-muted);
}
.anlik-musteri {
  margin-bottom: 4px;
}
.musteri-modu {
  display: flex;
}
.musteri-modu .p-selectbutton .p-button {
  flex: 1;
  justify-content: center;
}
.musteri-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.musteri-option-detay {
  font-size: 11px;
  color: var(--text-muted);
}
.secili-musteri-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
}
.secili-musteri-chip i {
  color: #60a5fa;
  font-size: 14px;
}
.secili-musteri-ad {
  flex: 1;
  color: var(--text-primary);
  font-weight: 500;
}
.secili-musteri-sil {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  padding: 2px;
}
.secili-musteri-sil:hover {
  color: #f87171;
}

.sepet-bolum {
  max-height: 350px;
  overflow-y: auto;
}
.sepet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.sepet-baslik-btnler {
  display: flex;
  align-items: center;
  gap: 4px;
}
.sepet-bos {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 13px;
}
.sepet-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.sepet-item:last-child {
  border-bottom: none;
}
.sepet-ust {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}
.sepet-kod {
  font-size: 10px;
  font-weight: 700;
  color: var(--text-muted);
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 5px;
  white-space: nowrap;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}
.sepet-ad {
  flex: 1;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sepet-sil {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.sepet-sil:hover {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
}
.sepet-kontroller {
  display: flex;
  align-items: center;
  gap: 6px;
}
.sepet-adet-grup {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.adet-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
.adet-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.sepet-adet-input {
  width: 46px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
  height: 26px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0 4px;
  outline: none;
}
.odeme-yontem-grid {
  display: flex;
  gap: 6px;
}
.taksit-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.08);
  margin-top: 8px;
}
.odeme-yontem-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  transition: all 0.15s;
}
.odeme-yontem-btn:hover {
  border-color: var(--accent);
}
.odeme-yontem-btn.active {
  border-color: var(--accent);
  background: rgba(59, 130, 246, 0.12);
  color: var(--accent);
}
.odeme-yontem-btn i {
  font-size: 16px;
}
.para-ustu {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 8px;
  font-size: 14px;
}
.para-ustu strong {
  color: #34d399;
  font-size: 16px;
}
.satis-ozet {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.satis-ozet-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #34d399;
  margin-bottom: 8px;
}
.satis-ozet-baslik i {
  font-size: 22px;
}
.satis-ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}
.satis-ozet-satir span {
  color: var(--text-muted);
}
.satis-ozet-satir .borc {
  color: #f87171;
}
.satis-ozet-satir .para {
  color: #34d399;
}
.musteri-bakiye-uyari {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}
.musteri-bakiye-uyari.danger {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.25);
}
.musteri-bakiye-uyari.warn {
  background: rgba(245, 158, 11, 0.12);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.25);
}
.musteri-bakiye-uyari.info {
  background: rgba(59, 130, 246, 0.1);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.25);
}
.gunluk-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}
.gunluk-satis-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.gunluk-satis-bilgi {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.gunluk-satis-no {
  font-size: 12px;
  font-weight: 600;
}
.gunluk-satis-cari {
  font-size: 11px;
  color: var(--text-muted);
}
.gunluk-satis-tutar {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}
.sepet-birimfiyat {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
}
.sepet-tutar {
  font-size: 13px;
  font-weight: 700;
  min-width: 60px;
  text-align: right;
}
.sepet-son-alis {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 5px 8px;
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 8px;
}
.sepet-son-alis i {
  font-size: 12px;
  color: #f59e0b;
}
.sepet-son-alis strong {
  color: var(--accent);
}

.ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.ozet-indirim {
  display: flex;
  align-items: center;
  gap: 6px;
}
.indirim-input {
  width: 100px;
}
.ozet-ayrac {
  border: none;
  border-top: 1px solid var(--border);
  margin: 6px 0;
}
.ozet-genel {
  border-top: 2px solid var(--border);
  margin-top: 4px;
  padding-top: 8px;
}
.genel-toplam-deger {
  font-size: 18px;
  font-weight: 800;
  color: var(--accent);
}

.siparis-kart :deep(.p-selectbutton) {
  display: flex;
}
.siparis-kart :deep(.p-selectbutton .p-button) {
  flex: 1;
  font-size: 12px;
}
.odenen-satir {
  margin-top: 8px;
}
.odenen-satir label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.odeme-durum {
  margin-top: 8px;
}
.odeme-durum :deep(.p-tag) {
  justify-content: center;
}
.odeme-kalan {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.kalan-deger {
  font-weight: 700;
  color: var(--accent);
}

.fis-card :deep(.p-card-content) {
  padding: 0;
}
.fis-baslik-satir {
  justify-content: space-between;
}
.fis-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 8px;
  flex-wrap: wrap;
}
.fis-ayarlar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fis-ayarlar .p-selectbutton .p-button {
  padding: 4px 10px;
  font-size: 11px;
}
.fis-onizleme-kapsam {
  overflow-x: auto;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 0 0 8px 8px;
}
.fis-onizleme {
  width: 80mm;
  margin: 0 auto;
  padding: 12px 8px;
  background: white;
  color: black;
  font-size: 11px;
  font-family: 'Courier New', monospace;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
.fis-header {
  text-align: center;
  margin-bottom: 6px;
}
.fis-baslik {
  font-size: 13px;
  font-weight: 700;
}
.fis-tarih {
  font-size: 10px;
  margin-top: 2px;
}
.fis-fisno {
  font-size: 10px;
  margin-top: 1px;
  color: #555;
}
.fis-musteri {
  margin-bottom: 4px;
  font-size: 10px;
}
.fis-ayrac {
  text-align: center;
  color: #999;
  margin: 3px 0;
  letter-spacing: 2px;
}
.fis-kalemler {
}
.fis-kalem {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
}
.fis-kalem-ad {
}
.fis-kalem-tutar {
  white-space: nowrap;
}
.fis-toplam {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 12px;
}
.fis-indirim {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  color: #c00;
  font-size: 11px;
}
.fis-genel-toplam {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  border-top: 2px solid #000;
  font-weight: 700;
  font-size: 13px;
}
.fis-toplam-deger {
}
.fis-odeme {
  margin-top: 4px;
}
.fis-odeme-satir {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  font-size: 10px;
}
.fis-odeme-durum {
  font-weight: 600;
}
.fis-footer {
  text-align: center;
  margin-top: 4px;
}
.fis-tesekkur {
  font-size: 10px;
  color: #555;
}
.fis-satici {
  font-size: 9px;
  color: #888;
  margin-top: 2px;
  text-align: center;
}

.satis-buton {
  margin-top: 4px;
}

.ym-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.ym-form-grid .full-width {
  grid-column: span 2;
}
.dialog-footer-btns {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  width: 100%;
}

.field {
  margin-bottom: 12px;
}
.field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.required {
  color: #f87171;
}

.fiyat-tip-select {
  flex: 1;
  min-width: 0;
  height: 26px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 11px;
  padding: 0 4px;
  outline: none;
}
.fiyat-giris-input {
  width: 68px;
  height: 26px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  padding: 0 4px;
  text-align: right;
  outline: none;
}

@media (max-width: 1100px) {
  .pos-body {
    flex-direction: column;
  }
  .pos-left,
  .pos-right {
    flex: 1 1 100%;
    max-width: 100%;
  }
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(min(160px, 100%), 1fr));
  }
}
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
