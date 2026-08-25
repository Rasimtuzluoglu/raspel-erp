<template>
  <div>
    <div class="kurulum-ikon">
      <i class="pi pi-rocket" />
    </div>
    <h2 class="iki-fa-baslik">
      {{ $t('kurulum.welcome') }}
    </h2>
    <p class="iki-fa-alt">
      {{ $t('kurulum.hint') }}
    </p>

    <div class="form-grup">
      <label>{{ $t('kurulum.companyName') }}</label>
      <div class="input-wrapper">
        <i class="pi pi-building" />
        <InputText
          v-model="ad"
          :placeholder="$t('kurulum.companyName')"
          @keyup.enter="$emit('baslat')"
        />
      </div>
    </div>

    <div class="kurulum-iki-kolon">
      <div class="form-grup">
        <label>{{ $t('kurulum.taxNumber') }}</label>
        <div class="input-wrapper">
          <i class="pi pi-hashtag" />
          <InputText
            v-model="vergiNo"
            :placeholder="$t('kurulum.taxNumber')"
          />
        </div>
      </div>
      <div class="form-grup">
        <label>{{ $t('kurulum.taxOffice') }}</label>
        <div class="input-wrapper">
          <i class="pi pi-map-marker" />
          <InputText
            v-model="vergiDairesi"
            :placeholder="$t('kurulum.taxOffice')"
          />
        </div>
      </div>
    </div>

    <div class="kurulum-iki-kolon">
      <div class="form-grup">
        <label>{{ $t('kurulum.phone') }}</label>
        <div class="input-wrapper">
          <i class="pi pi-phone" />
          <InputText
            v-model="telefon"
            :placeholder="$t('kurulum.phone')"
          />
        </div>
      </div>
      <div class="form-grup">
        <label>{{ $t('kurulum.email') }}</label>
        <div class="input-wrapper">
          <i class="pi pi-envelope" />
          <InputText
            v-model="email"
            :placeholder="$t('kurulum.email')"
          />
        </div>
      </div>
    </div>

    <div class="kurulum-ayrac" />

    <div class="form-grup">
      <label>{{ $t('kurulum.adminUsername') }}</label>
      <div class="input-wrapper">
        <i class="pi pi-user" />
        <InputText
          v-model="adminUsername"
          :placeholder="$t('kurulum.adminUsername')"
        />
      </div>
    </div>

    <div class="form-grup">
      <label>{{ $t('kurulum.fullName') }}</label>
      <div class="input-wrapper">
        <i class="pi pi-id-card" />
        <InputText
          v-model="adminDisplayName"
          :placeholder="$t('kurulum.fullName')"
        />
      </div>
    </div>

    <div class="form-grup">
      <label>{{ $t('kurulum.password') }}</label>
      <div class="input-wrapper">
        <i class="pi pi-lock" />
        <InputText
          v-model="adminPassword"
          :type="sifreGorunur ? 'text' : 'password'"
          placeholder="••••••"
          @keyup="e => e.key === 'Enter' && $emit('baslat')"
        />
        <button
          type="button"
          class="sifre-toggle"
          tabindex="-1"
          @click="sifreGorunur = !sifreGorunur"
        >
          <i :class="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'" />
        </button>
      </div>
      <small class="sifre-ipucu">{{ $t('kurulum.passwordHint') }}</small>
    </div>

    <Button
      :label="$t('kurulum.submit')"
      icon="pi pi-check"
      :loading="yukleniyor"
      class="giris-buton"
      @click="$emit('baslat')"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  yukleniyor: { type: Boolean, default: false }
})

defineEmits(['baslat'])

const sifreGorunur = ref(false)

const ad = defineModel('ad', { type: String, default: '' })
const vergiNo = defineModel('vergiNo', { type: String, default: '' })
const vergiDairesi = defineModel('vergiDairesi', { type: String, default: '' })
const telefon = defineModel('telefon', { type: String, default: '' })
const email = defineModel('email', { type: String, default: '' })
const adminUsername = defineModel('adminUsername', { type: String, default: '' })
const adminDisplayName = defineModel('adminDisplayName', { type: String, default: '' })
const adminPassword = defineModel('adminPassword', { type: String, default: '' })
</script>

<style scoped>
.kurulum-ikon {
  width: 54px;
  height: 54px;
  margin: 0 auto 12px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
  box-shadow: 0 6px 20px rgba(139, 92, 246, 0.3);
}
.kurulum-ikon i {
  font-size: 24px;
  color: white;
}
.kurulum-iki-kolon {
  display: flex;
  gap: 10px;
}
.kurulum-iki-kolon .form-grup {
  flex: 1;
}
.kurulum-ayrac {
  border-top: 1px dashed var(--border);
  margin: 14px 0;
}
.sifre-ipucu {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 11px;
}
.iki-fa-baslik {
  text-align: center;
  color: var(--text-primary);
  font-size: 17px;
  margin: 0 0 4px;
}
.iki-fa-alt {
  text-align: center;
  color: var(--text-secondary);
  font-size: 12.5px;
  margin: 0 0 16px;
}
.form-grup {
  margin-bottom: 15px;
}
.form-grup label {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
.input-wrapper {
  position: relative;
}
.input-wrapper > i {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  font-size: 16px;
  z-index: 2;
  pointer-events: none;
  transition: color 0.2s ease;
}
.input-wrapper :deep(.p-inputtext) {
  width: 100%;
  padding: 12px 14px 12px 40px !important;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 14.5px;
  min-height: 46px;
  transition: all 0.2s ease;
}
.input-wrapper :deep(.p-inputtext:focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.22) !important;
  outline: none;
}
.input-wrapper:focus-within > i {
  color: #3b82f6;
}
.sifre-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 16px;
  z-index: 2;
  padding: 4px;
  border-radius: 6px;
  transition: color 0.2s;
}
.sifre-toggle:hover {
  color: var(--text-primary);
}
.giris-buton {
  width: 100%;
  padding: 12px;
  margin-top: 4px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: white;
  transition: all 0.2s ease;
}
.giris-buton:hover {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-1px);
  box-shadow: 0 6px 22px rgba(59, 130, 246, 0.4);
}
</style>
