package com.raspel.erp.config.security;

import com.raspel.erp.entity.Kullanici;
import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.repository.KullaniciRepository;
import com.raspel.erp.repository.sistem.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final KullaniciRepository kullaniciRepository;
    private final RolRepository rolRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Kullanici kullanici = kullaniciRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        String roleName = kullanici.getRole() != null ? kullanici.getRole() : "USER";
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

        Optional<Rol> rolOpt = rolRepository.findByAd(roleName);
        if (rolOpt.isPresent() && rolOpt.get().getYetkiler() != null) {
            for (Yetki y : rolOpt.get().getYetkiler()) {
                if (y.getKod() != null && !y.getKod().isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(y.getKod()));
                }
            }
        }

        return new User(
                kullanici.getUsername(),
                kullanici.getPassword(),
                kullanici.getActive(),
                true, true, true,
                authorities
        );
    }
}
