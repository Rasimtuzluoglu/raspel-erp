<template>
  <Dialog
    v-model:visible="goster"
    header="Yenilikler"
    :modal="true"
    style="width: 520px"
    :closable="true"
  >
    <div class="changelog">
      <div class="surum">
        <span class="surum-etiketi">v{{ SURUM }}</span>
        <ul>
          <li><i class="pi pi-check-circle" /> Şoförler artık Saha Portalı'na da giriş yapabilir</li>
          <li><i class="pi pi-check-circle" /> Fatura "Ürün Seç" listesi açık temada okunabilir hâle getirildi</li>
          <li><i class="pi pi-check-circle" /> Şoför/saha girişinde görülen yetki/404 ekranı giderildi</li>
          <li><i class="pi pi-check-circle" /> Kağıtsız dijital teslimat: teslim alan + zorunlu imza + PDF fiş</li>
          <li><i class="pi pi-check-circle" /> Saha Portalı: tahsilat, sipariş, ziyaret, masraf, görev/not, performans</li>
          <li><i class="pi pi-check-circle" /> Test ve gösterim için örnek (demo) veriler eklendi</li>
        </ul>
      </div>
      <div class="surum">
        <span class="surum-etiketi">v1.1.0</span>
        <ul>
          <li><i class="pi pi-check-circle" /> Notlar modülü eklendi (renkli etiketler, önem derecesi)</li>
          <li><i class="pi pi-check-circle" /> Fiyatlı / fiyatsız fiş ve fatura yazdırma seçeneği</li>
          <li><i class="pi pi-check-circle" /> Klavye kısayolları (Ctrl+S, F2, Ctrl+P, Esc)</li>
          <li><i class="pi pi-check-circle" /> Genel arama artık 9 modülü tarıyor</li>
          <li><i class="pi pi-check-circle" /> Tablo sütun ayarları ve yoğunluk seçimi</li>
          <li><i class="pi pi-check-circle" /> Toplu işlemler (stok ve cari)</li>
          <li><i class="pi pi-check-circle" /> Silme geri alma, taslak otomatik kayıt</li>
          <li><i class="pi pi-check-circle" /> Denetim log filtreleme + Excel export</li>
          <li><i class="pi pi-check-circle" /> WebSocket anlık bildirimler ve bildirim tercihleri</li>
          <li><i class="pi pi-check-circle" /> CSV toplu veri aktarımı (stok, cari)</li>
          <li><i class="pi pi-check-circle" /> Profesyonel fatura PDF şablonu</li>
          <li><i class="pi pi-check-circle" /> Oturum zaman aşımı uyarısı</li>
        </ul>
      </div>
      <div class="surum">
        <span class="surum-etiketi">v1.0.0</span>
        <ul>
          <li><i class="pi pi-check-circle" /> İlk sürüm: finans, ticaret, envanter, İK ve sistem modülleri</li>
        </ul>
      </div>
    </div>
    <template #footer>
      <Button
        label="Tamam"
        icon="pi pi-check"
        @click="kapat"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const SURUM = typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : '1.17.0'
const ANAHTAR = 'raspel_gorulen_surum'
const goster = ref(false)

onMounted(() => {
  if (localStorage.getItem(ANAHTAR) !== SURUM) {
    goster.value = true
  }
})

// Pencere X ile veya maske ile kapatılsa bile bir daha gösterilmemesi için
// görünürlük false olduğunda görülen sürümü kalıcı olarak kaydet.
watch(goster, (acik) => {
  if (!acik) localStorage.setItem(ANAHTAR, SURUM)
})

const kapat = () => {
  goster.value = false
  localStorage.setItem(ANAHTAR, SURUM)
}
</script>

<style scoped>
.changelog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.surum ul {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
}
.surum li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.surum li i {
  color: #4ade80;
  font-size: 14px;
  margin-top: 2px;
}
.surum-etiketi {
  display: inline-block;
  background: var(--accent-soft-strong);
  color: var(--accent);
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
</style>
