package com.raspel.erp.controller.ik;

import com.raspel.erp.dto.ik.PersonelDTO;
import com.raspel.erp.service.ik.PersonelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;
import com.raspel.erp.entity.ik.Personel;

@Tag(name = "Personel", description = "Personel yönetimi API")
@RestController
@RequestMapping("/api/personel")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PersonelController {

    private final PersonelService personelService;

    @GetMapping
    @Operation(summary = "Tüm personeli getir (sayfalı)", description = "Tüm personel kayıtlarını sayfalı olarak listeler")
    public ResponseEntity<Page<PersonelDTO>> tumu(
            @RequestParam(required = false) Long sirketId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(personelService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre personel getir", description = "Personel ID'sine göre detayları getirir")
    public ResponseEntity<PersonelDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(personelService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni personel oluştur", description = "Yeni bir personel kaydı oluşturur")
    public ResponseEntity<PersonelDTO> olustur(@Valid @RequestBody PersonelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personelService.olustur(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Personel güncelle", description = "Personel bilgilerini günceller")
    public ResponseEntity<PersonelDTO> guncelle(@PathVariable Long id, @Valid @RequestBody PersonelDTO dto) {
        return ResponseEntity.ok(personelService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Personel sil", description = "Personel kaydını siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        personelService.sil(id);
        return ResponseEntity.noContent().build();
    }
}