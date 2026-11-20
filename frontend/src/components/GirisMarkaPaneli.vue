<template>
  <aside
    class="giris-marka-paneli"
    :aria-label="t('giris.markaBaslik')"
  >
    <div class="gmp-marka">
      <img
        :src="markaLogo"
        alt=""
        class="gmp-logo"
        width="40"
        height="40"
      >
      <span class="gmp-ad">RasPel ERP</span>
    </div>

    <h1 class="gmp-baslik">
      {{ t('giris.markaBaslik') }}
    </h1>
    <p class="gmp-vaat">
      {{ t('giris.markaVaat') }}
    </p>

    <ul class="gmp-degerler">
      <li>
        <i
          class="pi pi-wallet"
          aria-hidden="true"
        />
        <span>{{ t('giris.degerFinans') }}</span>
      </li>
      <li>
        <i
          class="pi pi-box"
          aria-hidden="true"
        />
        <span>{{ t('giris.degerStok') }}</span>
      </li>
      <li>
        <i
          class="pi pi-file-check"
          aria-hidden="true"
        />
        <span>{{ t('giris.degerDonusum') }}</span>
      </li>
    </ul>

    <div class="gmp-vitrin">
      <LoginPreview />
    </div>

    <ul class="gmp-guven">
      <li class="gmp-guven-rozet">
        <i
          class="pi pi-shield"
          aria-hidden="true"
        />
        <span>{{ t('giris.guvenKvkk') }}</span>
      </li>
      <li class="gmp-guven-rozet">
        <i
          class="pi pi-lock"
          aria-hidden="true"
        />
        <span>{{ t('giris.guvenSsl') }}</span>
      </li>
      <li class="gmp-guven-rozet">
        <i
          class="pi pi-key"
          aria-hidden="true"
        />
        <span>{{ t('giris.guven2fa') }}</span>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import LoginPreview from './LoginPreview.vue'

const { t } = useI18n()

const markaLogo = '/logo-icon.png'
</script>

<style scoped>
.giris-marka-paneli {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  color: var(--giris-metin);
}

.gmp-marka {
  display: flex;
  align-items: center;
  gap: 10px;
}

.gmp-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.gmp-ad {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.gmp-baslik {
  margin: 6px 0 0;
  max-width: 15ch;
  font-size: 30px;
  line-height: 36px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--giris-metin);
}

.gmp-vaat {
  margin: 0;
  max-width: 46ch;
  font-size: 13.5px;
  line-height: 1.55;
  color: var(--giris-metin-2);
}

.gmp-degerler {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 4px 0 0;
  padding: 0;
  list-style: none;
}

.gmp-degerler li {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 10px 5px 6px;
  border-radius: var(--radius-pill, 999px);
  border: 1px solid var(--giris-aksan-yumusak-2);
  background: var(--giris-aksan-yumusak);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--giris-metin-2);
}

.gmp-degerler i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--giris-aksan-yumusak);
  color: var(--giris-aksan-parlak);
  font-size: 11px;
}

.gmp-vitrin {
  margin-top: 4px;
}

.gmp-guven {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.gmp-guven-rozet {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border-radius: var(--radius-pill, 999px);
  border: 1px solid var(--giris-aksan-yumusak-2);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--giris-metin-3);
}

.gmp-guven-rozet i {
  font-size: 11px;
  color: var(--giris-aksan);
}

@media (max-height: 760px) {
  .gmp-vitrin {
    display: none;
  }
  .gmp-baslik {
    font-size: 26px;
    line-height: 32px;
  }
}
</style>
