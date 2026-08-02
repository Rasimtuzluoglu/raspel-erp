<template>
  <div class="yasal-page">
    <PageHeader title="Kullanım Şartları" subtitle="RasPel ERP platformunu kullanırken kabul ettiğiniz koşullar." />
    <div class="yasal-icerik">
      <section>
        <h2>1. Hizmet Tanımı</h2>
        <p>RasPel ERP, kurumsal kaynak planlama (ERP) ve müşteri ilişkileri yönetimi (CRM) hizmetleri sunan bir yazılım platformudur. Bu platform; stok, cari hesap, fatura, sipariş, personel ve raporlama modüllerini içerir.</p>
      </section>
      <section>
        <h2>2. Hesap ve Sorumluluk</h2>
        <p>Hesabınızın güvenliğinden ve şifrenizin gizliliğinden siz sorumlusunuz. Hesabınız üzerinden yapılan tüm işlemler sizi bağlar. Başkalarıyla yetki paylaşımı yapmamalısınız.</p>
      </section>
      <section>
        <h2>3. Veri Sorumluluğu</h2>
        <p>Platforma yüklediğiniz tüm verilerin (müşteri, stok, finansal bilgiler) mülkiyeti size aittir. Verilerinizin yasalara uygun şekilde işlenmesinden siz sorumlusunuz. Kişisel verilerin korunması (KVKK) yükümlülüklerinizi yerine getirmeniz gerekmektedir.</p>
      </section>
      <section>
        <h2>4. Hizmet Kesintisi ve Sorumluluk Sınırı</h2>
        <p>Platform bakım, güncelleme veya mücbir sebep durumlarında geçici olarak erişilemez olabilir. Bu durumlardan kaynaklanan doğrudan veya dolaylı zararlardan platform sağlayıcısı sorumlu tutulamaz.</p>
      </section>
      <section>
        <h2>5. Yedekleme</h2>
        <p>Sistem, düzenli aralıklarla otomatik veri yedeklemesi yapar. Ancak verilerinizin son yedeklemeden sonraki değişikliklerini yönetmek size aittir. Kritik verilerinizi düzenli olarak indirerek arşivlemeniz önerilir.</p>
      </section>
      <section>
        <h2>6. Fikri Mülkiyet</h2>
        <p>Platformun yazılım kodu, arayüz tasarımı ve markası sağlayıcıya aittir. Yazılımın izinsiz kopyalanması, dağıtılması veya değiştirilmesi yasaktır.</p>
      </section>
      <section>
        <h2>7. İletişim</h2>
        <p>Sorularınız ve talepleriniz için platform içi destek kanallarını veya yöneticinizi kullanabilirsiniz.</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.yasal-page { padding: 1.5rem; }
.yasal-icerik {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 12px; padding: 2rem; margin-top: 1.5rem; max-width: 800px;
}
.yasal-icerik h2 { font-size: 1.05rem; margin: 0 0 0.5rem; }
.yasal-icerik section { margin-bottom: 1.5rem; }
.yasal-icerik section:last-child { margin-bottom: 0; }
.yasal-icerik p { color: var(--text-secondary); font-size: 0.9rem; line-height: 1.6; }
</style>
