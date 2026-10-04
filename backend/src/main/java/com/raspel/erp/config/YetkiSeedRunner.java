package com.raspel.erp.config;

import com.raspel.erp.service.sistem.YetkiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Yetki/rol seed güvenlik ağı.
 *
 * <p>{@code sistem.yetki} ve {@code sistem.rol} tablolarının dolu olması,
 * {@code YetkiKontrol} fail-closed çalıştığı için zorunludur: rol bulunamazsa
 * {@code @yetkiKontrol} korumalı uçlar reddedilir. Bu tablolar önceden yalnızca
 * "Yetki Yönetimi" ekranı açıldığında dolduruluyordu; yeni kurulumda bir
 * yönetici o ekranı açana kadar {@code USER} rolü hiçbir yazma işlemi
 * yapamıyordu ve bunun tek izi log.warn idi.
 *
 * <p>Migration seed'i (V147) birincil çözümdür; bu runner migration
 * uygulanmamış veya yarım kalmış veritabanlarında, eski kurulumlarda ve
 * tabloların yanlışlıkla boşaltıldığı durumlarda devreye girer.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class YetkiSeedRunner implements ApplicationRunner {

    private final YetkiService yetkiService;

    @Override
    public void run(ApplicationArguments args) {
        yetkiService.seedKontrolu();
    }
}