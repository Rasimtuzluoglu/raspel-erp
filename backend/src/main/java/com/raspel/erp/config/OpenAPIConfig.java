package com.raspel.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RasPel ERP API")
                        .version("1.0.0")
                        .description("""
                            RasPel ERP REST API Dokümantasyonu

                            ## Kimlik Doğrulama
                            Tüm endpoint'ler (kullanıcı girişi hariç) JWT Bearer token gerektirir.
                            1. `POST /api/kullanicilar/giris` ile giriş yapın
                            2. Dönen `token` değerini **Authorize** butonuna yapıştırın

                            ## Çoklu Şirket (Multi-tenancy)
                            Her istek, token içindeki `sirketId` üzerinden ilgili şirketin verilerine erişir.

                            ## Ortak Hata Formatı
                            Tüm hatalar şu formatta döner:
                            ```
                            { "timestamp": "...", "message": "...", "status": 400 }
                            ```

                            ## Modüller
                            - Finans: Cari Hesap, Fatura, Banka, Kasa, Çek/Senet, Bütçe, Masraf
                            - Ticaret: Satış, Satın Alma, Sipariş, İrsaliye, İade, Fiyat Listesi
                            - Envanter: Stok, Depo, Seri/Lot, Stok Sayımı
                            - İK: Personel, İzin, Puantaj, Maaş Bordro, Vardiya
                            - Sistem: Şirket, Dönem, Kullanıcı, Yetki, Notlar, Yedekleme
                            - Rapor: Raporlar, Denetim Log, Anomali Tespiti
                            """)
                        .contact(new Contact().name("RasPel ERP").email("info@raspel.com"))
                        .license(new License().name("Ticari").url("https://raspel.com")))
                .tags(List.of(
                        new Tag().name("Kullanıcılar").description("Giriş, şifre yönetimi ve kullanıcı CRUD işlemleri"),
                        new Tag().name("Dashboard").description("Özet istatistikler ve grafik verileri"),
                        new Tag().name("Cari Hesap").description("Müşteri/tedarikçi hesap yönetimi"),
                        new Tag().name("Faturalar").description("Satış ve alış faturaları yönetimi"),
                        new Tag().name("Stoklar").description("Ürün ve stok yönetimi"),
                        new Tag().name("Notlar").description("Kullanıcı notları"),
                        new Tag().name("Denetim Log").description("İşlem geçmişi ve denetim kayıtları"),
                        new Tag().name("Veri Import").description("CSV ile toplu veri aktarımı"),
                        new Tag().name("PDF Raporlar").description("PDF rapor indirme")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Yerel Geliştirme"),
                        new Server().url("/").description("Docker / Proxy")));
    }
}
