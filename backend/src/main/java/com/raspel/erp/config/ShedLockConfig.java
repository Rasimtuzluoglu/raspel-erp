package com.raspel.erp.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Zamanlanmis islerin cok instance'li (yatay olceklendirilmis) ortamda tek bir
 * dugumde calismasini saglar. Kilit bilgisi PostgreSQL'deki {@code sistem.shedlock}
 * tablosunda tutulur; varsayilan maksimum kilit suresi isler bittikten sonra
 * otomatik serbest birakilir.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTableName("sistem.shedlock")
                        .usingDbTime()
                        .build()
        );
    }
}
