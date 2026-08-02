<template>
  <div class="yasal-page">
    <PageHeader title="Gizlilik Politikası" subtitle="Verilerinizin nasıl toplandığı, işlendiği ve korunduğu." />
    <div class="yasal-icerik">
      <section>
        <h2>1. Toplanan Veriler</h2>
        <p>Platform; şirket bilgileriniz, kullanıcı hesaplarınız, müşteri ve tedarikçi bilgileriniz, stok verileriniz ve finansal işlem kayıtlarınızı işler. Bu veriler yalnızca platformun temel işlevlerini yerine getirmek için kullanılır.</p>
      </section>
      <section>
        <h2>2. Veri Kullanım Amaçları</h2>
        <p>Toplanan veriler; fatura kesme, stok yönetimi, raporlama, denetim ve yasal yükümlülüklerin yerine getirilmesi amacıyla kullanılır. Verileriniz üçüncü kişilere satılmaz veya pazarlama amacıyla paylaşılmaz.</p>
      </section>
      <section>
        <h2>3. Veri Güvenliği</h2>
        <p>Verileriniz; şifreli bağlantı (SSL), rol bazlı erişim kontrolü, güvenli kimlik doğrulama (JWT) ve düzenli yedekleme ile korunur. Yetkisiz erişime karşı teknik ve idari tedbirler alınmaktadır.</p>
      </section>
      <section>
        <h2>4. KVKK Kapsamı</h2>
        <p>6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) kapsamında, kişisel verileriniz yalnızca işleme amacıyla sınırlı olarak ve hukuka uygun şekilde işlenir. Haklarınız kapsamında (düzeltme, silme, itiraz) talepleriniz değerlendirilir.</p>
      </section>
      <section>
        <h2>5. Veri Saklama Süresi</h2>
        <p>Verileriniz, yasal düzenlemelerin gerektirdiği süre boyunca saklanır. Hesabınızın kapatılması durumunda verileriniz ilgili mevzuat çerçevesinde arşivlenir veya imha edilir.</p>
      </section>
      <section>
        <h2>6. Erişim ve İmha Hakkı</h2>
        <p>Verilerinize ilişkin bilgi talep etme, düzeltme, silme ve işlemeye itiraz etme hakkına sahipsiniz. Bu talepleriniz için yöneticinizle veya platform destek ekibiyle iletişime geçebilirsiniz.</p>
      </section>
      <section>
        <h2>7. Politika Güncellemeleri</h2>
        <p>Bu politika, mevzuat değişiklikleri veya hizmet güncellemeleri doğrultusunda güncellenebilir. Güncellemeler platform üzerinden duyurulur.</p>
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
