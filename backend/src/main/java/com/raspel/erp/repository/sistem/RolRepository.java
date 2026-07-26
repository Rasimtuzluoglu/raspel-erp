package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByAd(String ad);
}
