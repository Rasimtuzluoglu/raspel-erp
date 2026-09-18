package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.DonemKapanis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonemKapanisRepository extends JpaRepository<DonemKapanis, Long> {

    List<DonemKapanis> findBySirketIdOrderByYilDesc(Long sirketId);

    Optional<DonemKapanis> findBySirketIdAndYil(Long sirketId, Integer yil);
}
