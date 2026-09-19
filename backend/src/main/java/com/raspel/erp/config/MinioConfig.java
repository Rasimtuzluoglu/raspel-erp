package com.raspel.erp.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${app.storage.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.minio.access-key:${MINIO_ROOT_USER:}}")
    private String accessKey;

    @Value("${app.storage.minio.secret-key:${MINIO_ROOT_PASSWORD:}}")
    private String secretKey;

    /** Sarkma yapan obje deposu baglanti/kaynak tuketmesin (ms). */
    @Value("${app.storage.minio.connect-timeout-ms:5000}")
    private long baglantiZamanAsimi;

    @Value("${app.storage.minio.write-timeout-ms:15000}")
    private long yazmaZamanAsimi;

    @Value("${app.storage.minio.read-timeout-ms:15000}")
    private long okumaZamanAsimi;

    @Bean
    public MinioClient minioClient() {
        okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(baglantiZamanAsimi, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(yazmaZamanAsimi, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(okumaZamanAsimi, java.util.concurrent.TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .httpClient(okHttpClient)
                .build();
    }
}
