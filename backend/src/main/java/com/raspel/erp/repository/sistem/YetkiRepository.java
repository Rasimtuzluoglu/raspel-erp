package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.Yetki;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YetkiRepository extends JpaRepository<Yetki, Long> {
}
