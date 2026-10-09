<template>
  <div class="hesap-ayarlari">
    <PageHeader
      :title="t('hesapAyarlari.title')"
      icon="pi pi-cog"
    />

    <IlkZiyaretIpuclari
      anahtar="hesap-ayarlari"
      :baslik="t('hesapAyarlari.title')"
      :metin="t('hesapAyarlari.introText')"
    />

    <div class="bolum-grup-secici">
      <SelectButton
        v-model="bolumGrubu"
        :options="bolumGruplari"
        option-label="ad"
        option-value="deger"
        :allow-empty="false"
      />
    </div>

    <div class="ayarlar-duzen">
      <TabView
        v-model:active-index="aktifBolum"
        class="ayar-icerik"
      >
        <TabPanel v-if="kisiselMi(0)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[0].ikon" /> {{ bolumler[0].label }}</span>
          </template>
          <div class="sekme-icerik">
            <div class="ayarlar-grid">
              <Card class="ayar-kart">
                <template #title>
                  <i class="pi pi-user" />{{ t('hesapAyarlari.profilBilgileri') }}
                </template>
                <template #content>
                  <div class="form-grid">
                    <div class="field">
                      <label>{{ t('hesapAyarlari.kullaniciAdi') }}</label>
                      <InputText
                        :model-value="kullanici?.username"
                        disabled
                        class="w-full"
                      />
                    </div>
                    <div class="field">
                      <label>{{ t('hesapAyarlari.gorunenAd') }}</label>
                      <InputText
                        v-model="profilForm.displayName"
                        class="w-full"
                      />
                    </div>
                    <div class="field">
                      <label>{{ t('hesapAyarlari.firmaAdi') }}</label>
                      <InputText
                        v-model="profilForm.companyName"
                        class="w-full"
                      />
                    </div>
                    <div class="field">
                      <label>{{ t('hesapAyarlari.avatar') }}</label>
                      <div class="avatar-satir">
                        <img
                          v-if="profilForm.avatarUrl"
                          :src="profilForm.avatarUrl"
                          class="avatar-onizleme"
                          alt=""
                        >
                        <span
                          v-else
                          class="avatar-bos"
                        ><i class="pi pi-user" /></span>
                        <Button
                          :label="t('hesapAyarlari.avatarYukle')"
                          icon="pi pi-upload"
                          size="small"
                          class="p-button-outlined"
                          :loading="avatarYukleniyor"
                          @click="avatarInput?.click()"
                        />
                        <input
                          ref="avatarInput"
                          type="file"
                          accept="image/*"
                          class="gizli-input"
                          @change="avatarSec"
                        >
                      </div>
                      <InputText
                        v-model="profilForm.avatarUrl"
                        class="w-full"
                        :placeholder="t('hesapAyarlari.avatarUrl')"
                      />
                    </div>
                    <Button
                      :label="t('hesapAyarlari.profiliKaydet')"
                      icon="pi pi-check"
                      :loading="kaydediliyor"
                      @click="profilKaydet"
                    />
                  </div>
                </template>
              </Card>

              <Card class="ayar-kart">
                <template #title>
                  <i class="pi pi-lock" />{{ t('hesapAyarlari.sifreDegistir') }}
                </template>
                <template #content>
                  <p class="ai-aciklama">
                    {{ t('hesapAyarlari.sifreDegistirAciklama') }}
                  </p>
                  <!-- Tek kaynak: ayni modal kenar cubugundan da acilir. -->
                  <Button
                    :label="t('hesapAyarlari.sifreDegistirBtn')"
                    icon="pi pi-key"
                    @click="sifreModalAcik = true"
                  />
                </template>
              </Card>
            </div>
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(1)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[1].ikon" /> {{ bolumler[1].label }}</span>
          </template>
          <div class="sekme-icerik">
            <div class="ayarlar-grid">
              <Card class="ayar-kart">
                <template #title>
                  <i class="pi pi-shield" />{{ t('hesapAyarlari.ikiFaktorluDogrulama') }}
                </template>
                <template #content>
                  <div
                    v-if="twoFactorDurum === 'ACIK'"
                    class="iki-fa-acik"
                  >
                    <div class="iki-fa-baslik">
                      <i class="pi pi-check-circle iki-fa-ok" />
                      <span>2FA <strong>{{ t('hesapAyarlari.aktif') }}</strong>. {{ t('hesapAyarlari.hesabinizGuvende') }}</span>
                    </div>
                    <div class="field iki-fa-kapat-alan">
                      <label>{{ t('hesapAyarlari.kapatmaKoduLabel') }}</label>
                      <div class="kod-satir">
                        <InputText
                          v-model="kapatmaKodu"
                          class="w-full"
                          :placeholder="t('hesapAyarlari.altiHaneliKod')"
                        />
                        <Button
                          :label="t('hesapAyarlari.ikiFaKapat')"
                          icon="pi pi-shield"
                          severity="danger"
                          outlined
                          :loading="kaydediliyor"
                          @click="ikiFakapat"
                        />
                      </div>
                    </div>
                  </div>
                  <div v-else>
                    <template v-if="!kurulumData">
                      <p class="iki-fa-aciklama">
                        {{ t('hesapAyarlari.ikiFaAciklama') }}
                      </p>
                      <Button
                        :label="t('hesapAyarlari.ikiFaKur')"
                        icon="pi pi-qrcode"
                        :loading="kaydediliyor"
                        @click="kurulumBaslat"
                      />
                    </template>
                    <template v-else>
                      <div class="iki-fa-kurulum">
                        <p>
                          <strong>1.</strong> {{ t('hesapAyarlari.adim1Metin') }}
                        </p>
                        <div class="secret-kutu">
                          <code>{{ kurulumData.secret }}</code>
                          <Button
                            icon="pi pi-copy"
                            :aria-label="$t('common.copy')"
                            rounded
                            text
                            @click="kopyala(kurulumData.secret)"
                          />
                        </div>
                        <div
                          v-if="qrGorselUrl"
                          class="qr-gorsel"
                        >
                          <img
                            :src="qrGorselUrl"
                            :alt="t('hesapAyarlari.qrAlt')"
                            width="180"
                            height="180"
                          >
                        </div>
                        <p class="otpauth-satir">
                          <small>{{ kurulumData.qrCodeUri }}</small>
                        </p>
                        <p><strong>2.</strong> {{ t('hesapAyarlari.adim2Metin') }}</p>
                        <div class="kod-satir">
                          <InputText
                            v-model="dogrulamaKodu"
                            class="w-full"
                            :placeholder="t('hesapAyarlari.altiHaneliKod')"
                          />
                          <Button
                            :label="t('hesapAyarlari.dogrulaAktifEt')"
                            icon="pi pi-check"
                            :loading="kaydediliyor"
                            @click="ikiFakAktifEt"
                          />
                        </div>
                      </div>
                    </template>
                  </div>
                </template>
              </Card>

              <Card class="ayar-kart">
                <template #title>
                  <div class="baslik-satir">
                    <span><i class="pi pi-desktop" />{{ t('hesapAyarlari.aktifOturumlar') }}</span>
                    <Button
                      icon="pi pi-refresh"
                      :aria-label="$t('common.refresh')"
                      size="small"
                      text
                      @click="oturumlariYukle"
                    />
                  </div>
                </template>
                <template #content>
                  <p class="ai-aciklama">
                    {{ t('hesapAyarlari.oturumAciklama') }}
                  </p>
                  <DataTable
                    :value="aktifOturumlar"
                    :loading="oturumYukleniyor"
                    striped-rows
                    size="small"
                  >
                    <template #empty>
                      <EmptyState />
                    </template>
                    <Column
                      :header="t('hesapAyarlari.kullanici')"
                      field="kullaniciAdi"
                    />
                    <Column header="IP">
                      <template #body="s">
                        {{ s.data.ip || '-' }}
                      </template>
                    </Column>
                    <Column :header="t('hesapAyarlari.girisZamani')">
                      <template #body="s">
                        {{ formatTarihSaat(s.data.girisZamani, '-') }}
                      </template>
                    </Column>
                    <Column
                      header=""
                      style="width: 90px"
                    >
                      <template #body="s">
                        <Button
                          v-if="s.data.kullaniciId !== authStore?.kullanici?.id"
                          :label="t('hesapAyarlari.sonlandir')"
                          icon="pi pi-sign-out"
                          size="small"
                          severity="danger"
                          text
                          @click="oturumSonlandir(s.data)"
                        />
                      </template>
                    </Column>
                  </DataTable>
                </template>
              </Card>
            </div>
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(2)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[2].ikon" /> {{ bolumler[2].label }}</span>
          </template>
          <div class="sekme-icerik">
            <Card class="ayar-kart">
              <template #title>
                <i class="pi pi-palette" />{{ t('hesapAyarlari.gorunum') }}
              </template>
              <template #content>
                <div class="form-grid">
                  <div class="field">
                    <label>{{ t('hesapAyarlari.temaModu') }}</label>
                    <div class="tema-butonlari">
                      <Button
                        :label="t('hesapAyarlari.acik')"
                        icon="pi pi-sun"
                        :severity="!isDark && mode !== 'system' ? 'contrast' : 'secondary'"
                        :outlined="isDark || mode === 'system'"
                        @click="applyMode('light')"
                      />
                      <Button
                        :label="t('hesapAyarlari.koyu')"
                        icon="pi pi-moon"
                        :severity="isDark && mode !== 'system' ? 'contrast' : 'secondary'"
                        :outlined="!isDark || mode === 'system'"
                        @click="applyMode('dark')"
                      />
                      <Button
                        :label="t('hesapAyarlari.sistem')"
                        icon="pi pi-desktop"
                        :severity="mode === 'system' ? 'contrast' : 'secondary'"
                        :outlined="mode !== 'system'"
                        :title="t('theme.systemHint')"
                        @click="applyMode('system')"
                      />
                    </div>
                  </div>
                  <div class="field">
                    <label>{{ t('hesapAyarlari.vurguRengi') }}</label>
                    <div class="renk-secenekleri">
                      <button
                        v-for="c in renkler"
                        :key="c.value"
                        class="renk-dot"
                        :class="{ aktif: accentColor === c.value }"
                        :style="{ background: c.value }"
                        :title="c.name"
                        :aria-label="c.name"
                        @click="applyColor(c.value)"
                      />
                      <label
                        class="renk-ozel"
                        :title="t('hesapAyarlari.ozelRenk')"
                      >
                        <input
                          type="color"
                          :value="accentColor"
                          :aria-label="t('hesapAyarlari.ozelRenk')"
                          @input="applyColor($event.target.value)"
                        >
                      </label>
                    </div>
                  </div>
                  <div class="field">
                    <label>{{ t('hesapAyarlari.dil') }}</label>
                    <div class="tema-butonlari">
                      <Button
                        :label="t('hesapAyarlari.turkce')"
                        icon="pi pi-flag"
                        :severity="aktifDil === 'tr' ? 'contrast' : 'secondary'"
                        :outlined="aktifDil !== 'tr'"
                        @click="dilDegistir('tr')"
                      />
                      <Button
                        label="English"
                        icon="pi pi-globe"
                        :severity="aktifDil === 'en' ? 'contrast' : 'secondary'"
                        :outlined="aktifDil !== 'en'"
                        @click="dilDegistir('en')"
                      />
                    </div>
                  </div>
                </div>
              </template>
            </Card>
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(3)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[3].ikon" /> {{ bolumler[3].label }}</span>
          </template>
          <div class="sekme-icerik">
            <Card class="ayar-kart">
              <template #title>
                <div class="baslik-satir">
                  <span><i class="pi pi-bell" />{{ t('hesapAyarlari.bildirimTercihleri') }}</span>
                  <Button
                    icon="pi pi-save"
                    :label="t('common.save')"
                    size="small"
                    :loading="tercihKaydediliyor"
                    @click="tercihleriKaydet"
                  />
                </div>
              </template>
              <template #content>
                <p class="ai-aciklama">
                  {{ t('hesapAyarlari.bildirimAciklama') }}
                </p>
                <div
                  v-for="tip in bildirimTipleri"
                  :key="tip.value"
                  class="tercih-satir"
                >
                  <span>{{ tip.label }}</span>
                  <ToggleSwitch v-model="tip.secili" />
                </div>
              </template>
            </Card>
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(4)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[4].ikon" /> {{ bolumler[4].label }}</span>
          </template>
          <div class="sekme-icerik">
            <div
              v-if="authStore.isAdmin"
              class="ayarlar-grid"
            >
              <Card class="ayar-kart ai-ayar-kart">
                <template #title>
                  <div class="ai-baslik-satir">
                    <div class="baslik-ic ai-baslik-ic">
                      <i class="pi pi-sparkles" />{{ t('hesapAyarlari.yapayZekaEntegrasyonu') }}
                    </div>
                    <Tag
                      :value="aiDurum === 'AKTIF' ? t('hesapAyarlari.aiAktif') : t('hesapAyarlari.yapilandirilmadi')"
                      :severity="aiDurum === 'AKTIF' ? 'success' : 'warn'"
                    />
                  </div>
                </template>
                <template #content>
                  <div class="form-grid">
                    <p class="ai-aciklama">
                      {{ t('hesapAyarlari.aiAciklama') }}
                    </p>
                    <div class="field">
                      <label>{{ t('hesapAyarlari.aiSaglayici') }}</label>
                      <Dropdown
                        v-model="aiForm.provider"
                        :options="aiSaglayicilar"
                        option-label="name"
                        option-value="value"
                        :placeholder="t('hesapAyarlari.saglayiciSecin')"
                        class="w-full"
                        @change="onProviderChange"
                      />
                    </div>
                    <div class="field">
                      <label>API Key</label>
                      <div class="p-inputgroup w-full">
                        <InputText
                          v-model="aiForm.apiKey"
                          :type="aiKeyGoster ? 'text' : 'password'"
                          :placeholder="t('hesapAyarlari.apiKeyPlaceholder')"
                          class="w-full"
                        />
                        <Button
                          :icon="aiKeyGoster ? 'pi pi-eye-slash' : 'pi pi-eye'"
                          :aria-label="aiKeyGoster ? t('common.hide') : t('common.show')"
                          severity="secondary"
                          outlined
                          @click="aiKeyGoster = !aiKeyGoster"
                        />
                      </div>
                      <small
                        v-if="aiMevcutMaskeliKey && !aiForm.apiKey"
                        class="text-muted"
                      >
                        {{ t('hesapAyarlari.mevcutAnahtar') }}: <code>{{ aiMevcutMaskeliKey }}</code>
                      </small>
                    </div>
                    <div class="field">
                      <label>{{ t('hesapAyarlari.model') }}</label>
                      <Dropdown
                        v-model="aiForm.model"
                        :options="aktifModelListesi"
                        option-label="name"
                        option-value="value"
                        :placeholder="t('hesapAyarlari.modelSecin')"
                        class="w-full"
                      />
                    </div>
                    <div class="ai-aksiyonlar">
                      <Button
                        :label="t('hesapAyarlari.baglantiyiTestEt')"
                        icon="pi pi-bolt"
                        severity="info"
                        outlined
                        :loading="aiTestEdiliyor"
                        :disabled="aiDurum !== 'AKTIF' && !aiForm.apiKey"
                        @click="aiBaglantiTestEt"
                      />
                      <Button
                        :label="t('hesapAyarlari.aiAyarlariniKaydet')"
                        icon="pi pi-check"
                        :loading="aiKaydediliyor"
                        @click="aiConfigKaydet"
                      />
                      <Button
                        v-if="aiDurum === 'AKTIF'"
                        :label="t('hesapAyarlari.kaldir')"
                        icon="pi pi-trash"
                        severity="danger"
                        outlined
                        :loading="aiKaydediliyor"
                        @click="aiConfigSil"
                      />
                    </div>
                  </div>
                </template>
              </Card>

              <Card class="ayar-kart">
                <template #title>
                  <div class="baslik-satir">
                    <span><i class="pi pi-key" />{{ t('hesapAyarlari.apiErisimTokenlari') }}</span>
                    <Button
                      icon="pi pi-plus"
                      :label="t('hesapAyarlari.yeniToken')"
                      size="small"
                      @click="tokenOlustur"
                    />
                  </div>
                </template>
                <template #content>
                  <p class="ai-aciklama">
                    {{ t('hesapAyarlari.tokenAciklama') }}
                    {{ t('hesapAyarlari.tokenUyariNotu') }} <code>Authorization: Bearer raspel_pat_...</code> {{ t('hesapAyarlari.tokenBaslikIleKullanilir') }}
                  </p>
                  <div
                    v-if="yeniToken"
                    class="token-uyari"
                  >
                    <i class="pi pi-info-circle" />
                    <span>{{ t('hesapAyarlari.yeniTokenUyari') }}</span>
                    <code class="token-deger">{{ yeniToken }}</code>
                  </div>
                  <div
                    v-if="tokenlar.length === 0 && !yeniToken"
                    class="token-bos"
                  >
                    {{ t('hesapAyarlari.tokenYok') }}
                  </div>
                  <div
                    v-for="token in tokenlar"
                    :key="token.id"
                    class="token-satir"
                  >
                    <div>
                      <span class="token-ad">{{ token.ad }}</span>
                      <span class="token-tarih">{{ token.olusturmaTarihi ? formatTarih(token.olusturmaTarihi) : '' }}</span>
                    </div>
                    <Button
                      icon="pi pi-trash"
                      :aria-label="$t('common.delete')"
                      rounded
                      text
                      severity="danger"
                      @click="tokenSil(token)"
                    />
                  </div>
                </template>
              </Card>
            </div>
            <Message
              v-else
              severity="info"
              :closable="false"
              :text="t('hesapAyarlari.adminGerekli')"
            />
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(5)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[5].ikon" /> {{ bolumler[5].label }}</span>
          </template>
          <div class="sekme-icerik">
            <div class="ayarlar-grid">
              <Card class="ayar-kart">
                <template #title>
                  <div class="baslik-satir">
                    <span><i class="pi pi-print" />{{ t('hesapAyarlari.faturaYazdirmaSablonu') }}</span>
                    <Tag
                      :value="t('hesapAyarlari.ozellestirilebilir')"
                      severity="info"
                    />
                  </div>
                </template>
                <template #content>
                  <div class="form-grid">
                    <p class="ai-aciklama">
                      {{ t('hesapAyarlari.faturaSablonAciklama') }}
                    </p>
                    <Button
                      :label="t('hesapAyarlari.faturaSablonTasarimci')"
                      icon="pi pi-palette"
                      @click="faturaTasarimModalAcik = true"
                    />
                  </div>
                </template>
              </Card>

              <FisYazdirmaAyarlari />
            </div>
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(6)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[6].ikon" /> {{ bolumler[6].label }}</span>
          </template>
          <div class="sekme-icerik">
            <div
              v-if="authStore.isAdmin"
              class="ayarlar-grid"
            >
              <Card class="ayar-kart">
                <template #title>
                  <i class="pi pi-cloud-download" />{{ t('hesapAyarlari.guncelleme') }}
                </template>
                <template #content>
                  <p class="ai-aciklama">
                    {{ t('hesapAyarlari.guncellemeAciklama') }}
                  </p>
                  <Button
                    :label="t('hesapAyarlari.githubdanGuncellemeAl')"
                    icon="pi pi-cloud-download"
                    :loading="guncellemeYukleniyor"
                    @click="guncellemeKontrol"
                  />
                  <div
                    v-if="guncellemeBilgi"
                    class="guncelleme-bilgi"
                  >
                    <div class="guncelleme-satir">
                      <strong>{{ t('hesapAyarlari.surum') }}:</strong> {{ guncellemeBilgi.mevcutSurum || '-' }}
                    </div>
                    <div
                      v-if="guncellemeBilgi.sonSurum"
                      class="guncelleme-satir"
                    >
                      <strong>{{ t('hesapAyarlari.sonSurum') }}:</strong> {{ guncellemeBilgi.sonSurum }}
                    </div>
                    <div
                      v-if="guncellemeBilgi.sonCommit"
                      class="guncelleme-satir"
                    >
                      <strong>{{ t('hesapAyarlari.sonCommit') }}:</strong> {{ guncellemeBilgi.sonCommit }} — {{ guncellemeBilgi.sonCommitMesaj }}
                    </div>
                    <div
                      v-if="guncellemeBilgi.guncellemeVar"
                      class="guncelleme-satir"
                    >
                      <Tag
                        :value="t('hesapAyarlari.yeniSurumMevcut')"
                        severity="warn"
                      />
                    </div>
                    <p
                      v-if="guncellemeBilgi.hata"
                      class="guncelleme-hata"
                    >
                      {{ guncellemeBilgi.hata }}
                    </p>
                    <p class="guncelleme-ipucu">
                      {{ t('hesapAyarlari.guncellemeIpucu') }}
                      <code>git pull &amp;&amp; docker compose up -d --build</code> {{ t('hesapAyarlari.calistirin') }}
                    </p>
                  </div>
                </template>
              </Card>
            </div>
            <Message
              v-else
              severity="info"
              :closable="false"
              :text="t('hesapAyarlari.adminGerekli')"
            />
          </div>
        </TabPanel>

        <TabPanel v-if="kisiselMi(7)">
          <template #header>
            <span class="sekme-baslik"><i :class="bolumler[7].ikon" /> {{ bolumler[7].label }}</span>
          </template>
          <div class="sekme-icerik">
            <div class="ayarlar-grid">
              <Card class="ayar-kart">
                <template #title>
                  <i class="pi pi-image" />{{ t('hesapAyarlari.markaBaslik') }}
                </template>
                <template #content>
                  <p class="marka-aciklama">
                    {{ t('hesapAyarlari.markaAciklama') }}
                  </p>
                  <div class="marka-onizleme">
                    <img
                      :src="markaOnizleme"
                      alt="logo"
                      class="marka-logo"
                    >
                  </div>
                  <input
                    ref="markaInput"
                    type="file"
                    accept="image/png,image/jpeg,image/webp,image/gif"
                    class="gizli-input"
                    @change="markaSec"
                  >
                  <div class="marka-aksiyonlar">
                    <Button
                      :label="t('hesapAyarlari.logoYukle')"
                      icon="pi pi-upload"
                      :loading="markaYukleniyor"
                      @click="$refs.markaInput.click()"
                    />
                    <Button
                      v-if="markaVar"
                      :label="t('hesapAyarlari.logoKaldir')"
                      icon="pi pi-trash"
                      severity="danger"
                      outlined
                      @click="markaKaldir"
                    />
                  </div>
                  <small
                    v-if="markaHata"
                    class="p-error"
                  >{{ markaHata }}</small>
                </template>
              </Card>
            </div>
          </div>
        </TabPanel>
      </TabView>
    </div>

    <FaturaTasarimModal v-model:visible="faturaTasarimModalAcik" />
    <PasswordChangeModal v-model:visible="sifreModalAcik" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { kullaniciAPI, aiConfigAPI, apiTokenAPI, sistemDurumAPI, sirketAPI, uploadAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useConfirm } from 'primevue/useconfirm'
import { useMarka } from '../composables/useMarka.js'
import { useTheme } from '../composables/useTheme.js'
import { useLocale } from '../composables/useLocale.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import FisYazdirmaAyarlari from '../components/FisYazdirmaAyarlari.vue'
import FaturaTasarimModal from '../components/FaturaTasarimModal.vue'
import PasswordChangeModal from '../components/PasswordChangeModal.vue'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import SelectButton from 'primevue/selectbutton'
import { formatTarihKisa as formatTarih, formatTarihSaat } from '../utils/format.js'

const { t } = useI18n()
const { aktifDil, dilDegistir } = useLocale()
const aktifBolum = ref(0)
const bolumler = computed(() => [
  { key: 'profil', label: t('hesapAyarlari.profil'), ikon: 'pi pi-user' },
  { key: 'guvenlik', label: t('hesapAyarlari.guvenlik'), ikon: 'pi pi-shield' },
  { key: 'gorunum', label: t('hesapAyarlari.gorunum'), ikon: 'pi pi-palette' },
  { key: 'bildirimler', label: t('hesapAyarlari.bildirimler'), ikon: 'pi pi-bell' },
  { key: 'entegrasyonlar', label: t('hesapAyarlari.entegrasyonlar'), ikon: 'pi pi-link' },
  { key: 'yazdirma', label: t('hesapAyarlari.yazdirma'), ikon: 'pi pi-print' },
  { key: 'sistem', label: t('hesapAyarlari.sistem'), ikon: 'pi pi-sync' },
  { key: 'marka', label: t('hesapAyarlari.marka'), ikon: 'pi pi-image' }
])
const faturaTasarimModalAcik = ref(false)

// Sekme gruplama: kisisel (0-3) / sistem (4-7). Duz 8 sekmeli serit yerine
// "Kisisel" ve "Sistem" secimi ile sayfa daha az dagilmis gorunur.
const bolumGrubu = ref('kisisel')
const bolumGruplari = computed(() => [
  { deger: 'kisisel', ad: t('hesapAyarlari.grupKisisel') },
  { deger: 'sistem', ad: t('hesapAyarlari.grupSistem') }
])
const kisiselMi = (i) => (bolumGrubu.value === 'kisisel') === (i <= 3)
watch(bolumGrubu, () => { aktifBolum.value = 0 })

// Sifre degistirme tek kaynaktan (PasswordChangeModal) yapilir.
const sifreModalAcik = ref(false)

// ---- Kurumsal marka (logo) ----
const { sirketLogosu, logoIcon, yukle: markaYukle, ayarla: markaAyarla } = useMarka()
const confirm = useConfirm()
const markaInput = ref(null)
const markaYukleniyor = ref(false)
const markaHata = ref('')
const markaVar = computed(() => !!sirketLogosu.value)
const markaOnizleme = computed(() => sirketLogosu.value || logoIcon.value)

const markaSec = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  markaHata.value = ''
  if (!file.type.startsWith('image/')) {
    markaHata.value = t('hesapAyarlari.logoGecersiz')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    markaHata.value = t('hesapAyarlari.logoBoyut')
    return
  }
  markaYukleniyor.value = true
  try {
    const r = await uploadAPI.uploadSirketLogo(file)
    const url = r.data?.url || ''
    await sirketAPI.updateLogo(authStore.sirketId, url)
    markaAyarla(url)
    toastBildirim.basarili(t('hesapAyarlari.logoYuklendi'))
  } catch (err) {
    markaHata.value = err?.response?.data?.message || t('hesapAyarlari.logoYuklenemedi')
  } finally {
    markaYukleniyor.value = false
    if (markaInput.value) markaInput.value.value = ''
  }
}

const markaKaldir = () => {
  confirm.require({
    message: t('hesapAyarlari.logoKaldirOnay'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await sirketAPI.deleteLogo(authStore.sirketId)
        markaAyarla('')
        toastBildirim.basarili(t('hesapAyarlari.logoKaldirildi'))
      } catch {
        toastBildirim.hata(t('hesapAyarlari.logoKaldirilamadi'))
      }
    }
  })
}

// Güncelleme kontrolü (GitHub)
const guncellemeYukleniyor = ref(false)
const guncellemeBilgi = ref(null)

const guncellemeKontrol = async () => {
  guncellemeYukleniyor.value = true
  try {
    const r = await sistemDurumAPI.guncelleme()
    guncellemeBilgi.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.guncellemeKontrolHatasi'))
  }
  guncellemeYukleniyor.value = false
}


const toast = useToast()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const { isDark, mode, accentColor, applyMode, applyColor, initTheme } = useTheme()

const renkler = [
  { name: t('hesapAyarlari.renkPetrol'), value: '#0f766e' },
  { name: t('hesapAyarlari.renkOkyanus'), value: '#3b82f6' },
  { name: t('hesapAyarlari.renkZumrut'), value: '#10b981' },
  { name: t('hesapAyarlari.renkAsilMor'), value: '#8b5cf6' },
  { name: t('hesapAyarlari.renkAmber'), value: '#f59e0b' }
]

const kullanici = computed(() => authStore?.kullanici)
const kaydediliyor = ref(false)

const profilForm = ref({ displayName: '', companyName: '', avatarUrl: '' })

// Avatar yukleme: URL elle girisi yerine dosya yukleme (onizlemeli).
const avatarInput = ref(null)
const avatarYukleniyor = ref(false)
const avatarSec = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  avatarYukleniyor.value = true
  try {
    const r = await uploadAPI.foto(file)
    const url = r.data?.url || ''
    if (url) profilForm.value.avatarUrl = url
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.avatarYuklenemedi'))
  } finally {
    avatarYukleniyor.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

const twoFactorDurum = ref('KAPALI') // ACIK / KAPALI / KURULUM
const kurulumData = ref(null)
const qrGorselUrl = ref('')
const dogrulamaKodu = ref('')
const kapatmaKodu = ref('')

// AI Yapılandırma State
const aiSaglayicilar = [
  { name: 'OpenAI (ChatGPT)', value: 'OPENAI' },
  { name: 'Google Gemini', value: 'GOOGLE' },
  { name: 'Anthropic Claude', value: 'ANTHROPIC' }
]

const aiModeller = {
  OPENAI: [
    { name: t('hesapAyarlari.modelGpt4o'), value: 'gpt-4o' },
    { name: t('hesapAyarlari.modelGpt4oMini'), value: 'gpt-4o-mini' },
    { name: t('hesapAyarlari.modelGpt4Turbo'), value: 'gpt-4-turbo' }
  ],
  GOOGLE: [
    { name: t('hesapAyarlari.modelGeminiFlash'), value: 'gemini-2.5-flash' },
    { name: t('hesapAyarlari.modelGeminiPro'), value: 'gemini-1.5-pro' },
    { name: t('hesapAyarlari.modelGemini15Flash'), value: 'gemini-1.5-flash' }
  ],
  ANTHROPIC: [
    { name: t('hesapAyarlari.modelClaudeSonnet'), value: 'claude-3-5-sonnet-20241022' },
    { name: t('hesapAyarlari.modelClaudeHaiku'), value: 'claude-3-5-haiku-20241022' },
    { name: t('hesapAyarlari.modelClaudeOpus'), value: 'claude-3-opus-20240229' }
  ]
}

const aiForm = ref({ provider: 'OPENAI', apiKey: '', model: 'gpt-4o' })
const aiDurum = ref('YAPILANDIRILMADI') // AKTIF / YAPILANDIRILMADI
const aiMevcutMaskeliKey = ref('')
const aiKeyGoster = ref(false)

// API Token state
const tokenlar = ref([])
const yeniToken = ref('')

const tokenlariYukle = async () => {
  try {
    const r = await apiTokenAPI.listele()
    tokenlar.value = r.data || []
  } catch {
    tokenlar.value = []
  }
}

const tokenOlustur = async () => {
  try {
    const r = await apiTokenAPI.olustur('API Token')
    yeniToken.value = r.data?.token || ''
    tokenlariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.tokenOlusturulamadi'))
  }
}

const tokeniSil = async (token) => {
  try {
    await apiTokenAPI.sil(token.id)
    tokenlar.value = tokenlar.value.filter((x) => x.id !== token.id)
    toast.add({ severity: 'success', summary: t('hesapAyarlari.tokenSilindi'), detail: t('hesapAyarlari.tokenSilindi'), life: 3000 })
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.tokenSilinemedi'))
  }
}

// API tokeni geri alinamaz; yeniden olusturulmasi gerekir. Onay istenir.
const tokenSil = (token) => {
  confirm.require({
    message: t('hesapAyarlari.tokenSilOnayMesaj', { ad: token.ad || token.aciklama || `#${token.id}` }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: () => tokeniSil(token),
    reject: () => {}
  })
}
const aiKaydediliyor = ref(false)
const aiTestEdiliyor = ref(false)

const aktifModelListesi = computed(() => {
  return aiModeller[aiForm.value.provider] || aiModeller.OPENAI
})

const onProviderChange = () => {
  const modeller = aktifModelListesi.value
  if (modeller && modeller.length > 0) {
    aiForm.value.model = modeller[0].value
  }
}

const aiConfigGetir = async () => {
  try {
    const res = await aiConfigAPI.getConfig()
    if (res.data) {
      aiDurum.value = res.data.durum || 'YAPILANDIRILMADI'
      if (res.data.provider) aiForm.value.provider = res.data.provider
      if (res.data.model) aiForm.value.model = res.data.model
      aiMevcutMaskeliKey.value = res.data.apiKey || ''
    }
  } catch {
    /* empty */
  }
}

const aiConfigKaydet = async () => {
  if (!aiForm.value.apiKey && !aiMevcutMaskeliKey.value) {
    toastBildirim.uyari(t('hesapAyarlari.gecerliApiAnahtari'))
    return
  }
  aiKaydediliyor.value = true
  try {
    const payload = {
      provider: aiForm.value.provider,
      apiKey: aiForm.value.apiKey || undefined,
      model: aiForm.value.model,
      aktif: true
    }
    const res = await aiConfigAPI.saveConfig(payload)
    if (res.data) {
      aiDurum.value = res.data.durum || 'AKTIF'
      aiMevcutMaskeliKey.value = res.data.apiKey || ''
      aiForm.value.apiKey = ''
    }
    toastBildirim.basarili(t('hesapAyarlari.aiAyarlariKaydedildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.ayarlarKaydedilemedi'))
  } finally {
    aiKaydediliyor.value = false
  }
}

const aiBaglantiTestEt = async () => {
  aiTestEdiliyor.value = true
  try {
    const res = await aiConfigAPI.testConnection()
    // Backend { success: boolean, message } dondurur (status degil).
    const basarili = res.data && (res.data.success === true || res.data.status === 'SUCCESS')
    if (basarili) {
      toast.add({
        severity: 'success',
      summary: t('hesapAyarlari.baglantiBasarili'),
      detail: res.data.message || t('hesapAyarlari.aiBaglantiDogrulandi'),
        life: 3000
      })
    } else {
      toast.add({
        severity: 'warn',
      summary: t('hesapAyarlari.baglantiUyarisi'),
      detail: res.data?.message || t('hesapAyarlari.baglantiKurulamadi'),
        life: 4000
      })
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.apiAnahtariDogrulanamadi'))
  } finally {
    aiTestEdiliyor.value = false
  }
}

const aiYapilandirmasiniSil = async () => {
  aiKaydediliyor.value = true
  try {
    await aiConfigAPI.deleteConfig()
    aiDurum.value = 'YAPILANDIRILMADI'
    aiMevcutMaskeliKey.value = ''
    aiForm.value = { provider: 'OPENAI', apiKey: '', model: 'gpt-4o' }
    toastBildirim.basarili(t('hesapAyarlari.aiYapilandirmasiKaldirildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.yapilandirmaSilinemedi'))
  } finally {
    aiKaydediliyor.value = false
  }
}

// AI API anahtari ve saglayici ayari silinir; yeniden girilmesi gerekir.
const aiConfigSil = () => {
  confirm.require({
    message: t('hesapAyarlari.aiYapilandirmaSilOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: () => aiYapilandirmasiniSil(),
    reject: () => {}
  })
}

onMounted(async () => {
  initTheme()
  markaYukle()
  const k = authStore?.kullanici
  if (k) {
    profilForm.value = {
      displayName: k.displayName || '',
      companyName: k.companyName || '',
      avatarUrl: k.avatarUrl || ''
    }
  }
  try {
    const r = await kullaniciAPI.ben()
    const ben = r.data
    twoFactorDurum.value = ben.twoFactorEnabled ? 'ACIK' : 'KAPALI'
  } catch {
    /* empty */
  }
  await aiConfigGetir()
  oturumlariYukle()
  tercihleriYukle()
  tokenlariYukle()
})

const aktifOturumlar = ref([])
const oturumYukleniyor = ref(false)

const oturumlariYukle = async () => {
  oturumYukleniyor.value = true
  try {
    const r = await kullaniciAPI.aktifOturumlar()
    aktifOturumlar.value = r.data || []
  } catch {
    aktifOturumlar.value = []
  } finally {
    oturumYukleniyor.value = false
  }
}

const oturumSonlandir = async (oturum) => {
  try {
    await kullaniciAPI.oturumIptal(oturum.jti)
    toastBildirim.basarili(t('hesapAyarlari.oturumSonlandirildi'))
    oturumlariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.oturumSonlandirilamadi'))
  }
}

const bildirimTipleri = ref([
  { label: t('hesapAyarlari.bildirimStok'), value: 'STOK', secili: true },
  { label: t('hesapAyarlari.bildirimSiparis'), value: 'SIPARIS', secili: true },
  { label: t('hesapAyarlari.bildirimTeklif'), value: 'TEKLIF', secili: true },
  { label: t('hesapAyarlari.bildirimTeslimat'), value: 'TESLIMAT', secili: true },
  { label: t('hesapAyarlari.bildirimFatura'), value: 'FATURA', secili: true },
  { label: t('hesapAyarlari.bildirimVade'), value: 'VADE', secili: true },
  { label: t('hesapAyarlari.bildirimTahsilat'), value: 'TAKSILAT', secili: true },
  { label: t('hesapAyarlari.bildirimOdeme'), value: 'ODEME', secili: true },
  { label: t('hesapAyarlari.bildirimMasraf'), value: 'MASRAF_TALEBI', secili: true },
  // Bildirim zili ile ayni liste (AJANDA dahil) — iki yerde ayrismasin.
  { label: t('hesapAyarlari.bildirimAjanda'), value: 'AJANDA', secili: true }
])
const tercihKaydediliyor = ref(false)

const tercihleriYukle = async () => {
  try {
    const r = await kullaniciAPI.bildirimTercihleriGetir()
    const secili = r.data || []
    if (secili.length) {
      bildirimTipleri.value.forEach((t) => { t.secili = secili.includes(t.value) })
    }
  } catch {
    /* empty */
  }
}

const tercihleriKaydet = async () => {
  tercihKaydediliyor.value = true
  try {
    const secili = bildirimTipleri.value.filter((t) => t.secili).map((t) => t.value)
    await kullaniciAPI.bildirimTercihleriGuncelle(secili)
    toastBildirim.basarili(t('hesapAyarlari.tercihlerGuncellendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.tercihlerKaydedilemedi'))
  } finally {
    tercihKaydediliyor.value = false
  }
}

const profilKaydet = async () => {
  kaydediliyor.value = true
  try {
    await kullaniciAPI.beniGuncelle(profilForm.value)
    await authStore.kullaniciGuncelle()
    toastBildirim.basarili(t('hesapAyarlari.profilGuncellendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.profilGuncellenemedi'))
  }
  kaydediliyor.value = false
}

const kurulumBaslat = async () => {
  kaydediliyor.value = true
  try {
    const r = await kullaniciAPI.setup2fa()
    kurulumData.value = r.data
    twoFactorDurum.value = 'KURULUM'
    // otpauth URI'sini taranabilir QR PNG'ye cevir (authenticator icin).
    qrGorselUrl.value = ''
    if (r.data?.qrCodeUri) {
      try {
        const qr = await kullaniciAPI.twoFactorQr(r.data.qrCodeUri)
        qrGorselUrl.value = window.URL.createObjectURL(new Blob([qr.data], { type: 'image/png' }))
      } catch {
        qrGorselUrl.value = ''
      }
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.ikiFaKurulumBaslatilamadi'))
  }
  kaydediliyor.value = false
}

const ikiFakAktifEt = async () => {
  if (!dogrulamaKodu.value) {
    toastBildirim.uyari(t('hesapAyarlari.dogrulamaKoduGirin'))
    return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.enable2fa({ code: dogrulamaKodu.value })
    twoFactorDurum.value = 'ACIK'
    kurulumData.value = null
    qrGorselUrl.value = ''
    dogrulamaKodu.value = ''
    toastBildirim.basarili(t('hesapAyarlari.ikifaAktifEdildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.kodGecersiz'))
  }
  kaydediliyor.value = false
}

const ikiFakapat = async () => {
  if (!kapatmaKodu.value) {
    toastBildirim.uyari(t('hesapAyarlari.dogrulamaKoduGirin'))
    return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.disable2fa({ code: kapatmaKodu.value })
    twoFactorDurum.value = 'KAPALI'
    kapatmaKodu.value = ''
    toastBildirim.basarili(t('hesapAyarlari.ikiFaKapatildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hesapAyarlari.kodGecersiz'))
  }
  kaydediliyor.value = false
}

const kopyala = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.add({ severity: 'success', summary: t('hesapAyarlari.kopyalandi'), detail: t('hesapAyarlari.gizliAnahtarKopyalandi'), life: 2000 })
  } catch {
    /* empty */
  }
}
</script>

<style scoped>
.hesap-ayarlari {
  padding: 0;
}
.sayfa-baslik {
  margin-bottom: 24px;
}
.ayarlar-duzen {
  display: block;
}
.ayar-icerik {
  min-width: 0;
}
/* PrimeVue v4 TabView: panelin varsayilan yan dolgusunu sifirla, sekmeler
   icerigi ile hizala. */
.ayar-icerik :deep(.p-tabview-panels) {
  padding: 1rem 0 0;
}
.ayar-icerik :deep(.p-tabview-tablist) {
  border-color: var(--border);
}
/* Tum sekmelerde ortak kart duzeni: sekme-icerik grid, ic grid'ler seffaf
   (display:contents) olur; boylece tek kartli ve cok kartli sekmeler ayni
   sutun sistemini kullanir ve genislik degismez. */
.sekme-icerik {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(400px, 100%), 1fr));
  gap: 16px;
  align-items: start;
}
.ayarlar-grid {
  display: contents;
}
.sekme-baslik {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.ayar-kart {
  display: flex;
  flex-direction: column;
}
.ayar-kart :deep(.p-card-content) {
  padding-top: 8px;
  flex: 1;
}
.ayar-kart :deep(.p-card-title) {
  display: flex;
  align-items: center;
}
.ayar-kart :deep(.p-card-title i) {
  margin-right: 8px;
}
.baslik-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.baslik-satir > span {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}
.baslik-satir > span i {
  margin-right: 8px;
}
.baslik-ic {
  display: inline-flex;
  align-items: center;
}
.baslik-ic i {
  margin-right: 8px;
}
.ai-baslik-ic i {
  color: var(--accent);
}
.iki-fa-ok {
  color: var(--success);
}
.iki-fa-kapat-alan {
  margin-top: 14px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: stretch;
}
/* Kaydet butonlari kart genisligine yayilmasin; sola hizali kalsin. */
.form-grid > button,
.form-grid > .p-button {
  align-self: flex-start;
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
.kod-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.iki-fa-acik .iki-fa-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.iki-fa-aciklama, .ai-aciklama {
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;
}
.guncelleme-bilgi {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.guncelleme-satir {
  font-size: 13px;
  color: var(--text-secondary);
}
.guncelleme-satir strong {
  color: var(--text-primary);
}
.guncelleme-hata {
  color: var(--danger);
  font-size: 12px;
}
.guncelleme-ipucu {
  color: var(--text-muted);
  font-size: 12px;
  margin: 6px 0 0;
}
.guncelleme-ipucu code {
  background: var(--bg-secondary);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 11px;
}
.ai-baslik-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.ai-aksiyonlar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}
.secret-kutu {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  margin: 8px 0;
}
.secret-kutu code {
  font-family: monospace;
  font-size: 15px;
  letter-spacing: 2px;
  flex: 1;
}
.otpauth-satir {
  color: var(--text-muted);
  word-break: break-all;
}
.tema-butonlari {
  display: flex;
  gap: 8px;
}
.renk-secenekleri {
  display: flex;
  gap: 12px;
  padding-top: 4px;
}
.renk-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.renk-dot:hover {
  transform: scale(1.12);
}
.renk-dot.aktif {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 3px var(--accent-border);
}
.renk-ozel {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--border);
  cursor: pointer;
  display: inline-flex;
  transition: all 0.15s;
}
.renk-ozel:hover {
  transform: scale(1.12);
}
.renk-ozel input {
  width: 200%;
  height: 200%;
  margin: -25%;
  border: none;
  padding: 0;
  cursor: pointer;
  background: none;
}
.empty-state {
  text-align: center;
  padding: 1.5rem;
  color: var(--text-muted);
}
.tercih-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  font-size: 0.9rem;
}
.tercih-satir:last-child {
  border-bottom: none;
}
.token-uyari {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  margin-bottom: 12px;
  background: var(--accent-soft);
  border: 1px solid var(--accent);
  border-radius: 8px;
  font-size: 0.85rem;
}
.token-deger {
  font-family: monospace;
  word-break: break-all;
  background: var(--bg-secondary);
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 0.8rem;
}
.token-bos {
  padding: 16px 0;
  color: var(--text-muted);
  font-size: 0.85rem;
}
.token-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.token-satir:last-child {
  border-bottom: none;
}
.token-ad {
  font-weight: 600;
  font-size: 0.9rem;
  display: block;
}
.token-tarih {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.marka-aciklama {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0 0 14px;
  max-width: 560px;
}
.marka-onizleme {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180px;
  height: 110px;
  padding: 10px;
  margin-bottom: 14px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  background: var(--bg-secondary);
}
.marka-logo {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.marka-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.gizli-input {
  display: none;
}
.bolum-grup-secici {
  margin-bottom: 1rem;
}
.avatar-satir {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar-onizleme,
.avatar-bos {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.avatar-bos {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft-strong);
  color: var(--accent);
  font-size: 20px;
}
</style>

