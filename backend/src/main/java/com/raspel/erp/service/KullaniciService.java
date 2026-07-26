package com.raspel.erp.service;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.dto.KullaniciDTO;
import com.raspel.erp.dto.LoginRequest;
import com.raspel.erp.dto.LoginResponse;
import com.raspel.erp.entity.Kullanici;
import com.raspel.erp.entity.Sirket;
import com.raspel.erp.repository.KullaniciRepository;
import com.raspel.erp.repository.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KullaniciService {

    private final KullaniciRepository kullaniciRepository;
    private final SirketRepository sirketRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public List<KullaniciDTO> tumunuGetir() {
        return kullaniciRepository.findAll().stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    public KullaniciDTO getir(Long id) {
        return entityToDTO(kullaniciRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + id)));
    }

    public KullaniciDTO olustur(KullaniciDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new RuntimeException("Kullanıcı şifresi zorunludur");
        }
        if (kullaniciRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor: " + dto.getUsername());
        }
        Kullanici k = Kullanici.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .displayName(dto.getDisplayName())
                .avatarUrl(dto.getAvatarUrl())
                .companyName(dto.getCompanyName())
                .sirketId(dto.getSirketId())
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .active(true)
                .build();
        return entityToDTO(kullaniciRepository.save(k));
    }

    public KullaniciDTO guncelle(Long id, KullaniciDTO dto) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + id));
        if (dto.getDisplayName() != null) k.setDisplayName(dto.getDisplayName());
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        if (dto.getSirketId() != null) k.setSirketId(dto.getSirketId());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) k.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getActive() != null) k.setActive(dto.getActive());
        if (dto.getRole() != null) k.setRole(dto.getRole());
        return entityToDTO(kullaniciRepository.save(k));
    }

    public void sil(Long id) {
        if (!kullaniciRepository.existsById(id)) throw new RuntimeException("Kullanıcı bulunamadı: " + id);
        kullaniciRepository.deleteById(id);
    }

    public LoginResponse giris(LoginRequest req) {
        log.info("Giriş denemesi: {}", req.getUsername());
        Kullanici k = kullaniciRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Kullanıcı adı veya şifre hatalı"));

        if (!k.getActive()) throw new RuntimeException("Bu kullanıcı aktif değil");

        if (!passwordEncoder.matches(req.getPassword(), k.getPassword())) {
            throw new RuntimeException("Kullanıcı adı veya şifre hatalı");
        }

        String company = req.getCompanyName() != null && !req.getCompanyName().isBlank()
                ? req.getCompanyName() : k.getCompanyName();

        Long sirketId = req.getSirketId() != null ? req.getSirketId() : k.getSirketId();
        String sirketAdi = null;
        if (sirketId != null) {
            sirketAdi = sirketRepository.findById(sirketId).map(Sirket::getAd).orElse(null);
        }

        String token = jwtUtil.generateToken(k, sirketId, sirketAdi);

        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .sirketId(sirketId)
                .sirketAdi(sirketAdi)
                .companyName(company)
                .role(k.getRole())
                .token(token)
                .build();
    }

    private KullaniciDTO entityToDTO(Kullanici k) {
        return KullaniciDTO.builder()
                .id(k.getId()).username(k.getUsername())
                .displayName(k.getDisplayName()).avatarUrl(k.getAvatarUrl())
                .companyName(k.getCompanyName()).sirketId(k.getSirketId())
                .role(k.getRole()).active(k.getActive())
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .build();
    }
}
