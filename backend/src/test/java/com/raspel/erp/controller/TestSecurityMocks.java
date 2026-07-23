package com.raspel.erp.controller;

import com.raspel.erp.config.security.CustomUserDetailsService;
import com.raspel.erp.config.security.JwtAuthFilter;
import com.raspel.erp.config.security.JwtUtil;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSecurityMocks {

    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }

    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    @Bean
    @Primary
    public JwtAuthFilter jwtAuthFilter() {
        return Mockito.mock(JwtAuthFilter.class);
    }
}
