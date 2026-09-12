package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.TaksitDTO;
import com.raspel.erp.dto.finans.TaksitOdeDTO;
import com.raspel.erp.dto.finans.TaksitPlanDTO;
import com.raspel.erp.service.finans.TaksitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Taksit", description = "Taksit plani ve tahsilat takvimi API")
@RestController
@RequestMapping("/api/taksitler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class TaksitController {

    private final TaksitService taksitService;

    @GetMapping
    @Operation(summary = "Taksit listesi", description = "Cari/durum/tarih filtreleriyle sayfali taksit kalemlerini getirir")
    public ResponseEntity<Page<TaksitDTO>> listele(
            HttpServletRequest request,
            @RequestParam(required = false) Long cariId,
            @RequestParam(required = false) String durum,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @PageableDefault(size = 50, sort = "vadeTarihi") Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(taksitService.listele(sirketId, cariId, durum, baslangic, bitis, pageable));
    }

    @GetMapping("/yaklasan")
    @Operation(summary = "Yaklasan taksitler", description = "Onumuzdeki N gun icinde vadesi gelecek/gecmis odenmemis taksitler")
    public ResponseEntity<List<TaksitDTO>> yaklasan(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") int gun) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(taksitService.yaklasan(sirketId, gun));
    }

    @GetMapping("/takvim")
    @Operation(summary = "Taksit takvimi", description = "Belirtilen yil/ay icin taksit kalemlerini getirir")
    public ResponseEntity<List<TaksitDTO>> takvim(
            HttpServletRequest request,
            @RequestParam int yil,
            @RequestParam int ay) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(taksitService.takvim(sirketId, yil, ay));
    }

    @GetMapping("/ozet")
    @Operation(summary = "Taksit ozeti", description = "Bekleyen, gecikmis ve bu ay vadesi gelen taksit toplamlari")
    public ResponseEntity<Map<String, Object>> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(taksitService.ozet(sirketId));
    }

    @PostMapping("/plan")
    @Operation(summary = "Taksit plani olustur", description = "Toplam tutari verilen taksit sayisina gore vade bazli bolerek plan olusturur")
    public ResponseEntity<List<TaksitDTO>> planOlustur(
            @RequestBody @Valid TaksitPlanDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(taksitService.planOlustur(dto, sirketId));
    }

    @PostMapping("/{id}/ode")
    @Operation(summary = "Taksit ode", description = "Taksit kalemini odendi olarak isaretler")
    public ResponseEntity<TaksitDTO> ode(
            @PathVariable Long id,
            @RequestBody(required = false) TaksitOdeDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(taksitService.ode(id, dto, sirketId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Taksit sil", description = "Tek bir taksit kalemini siler")
    public ResponseEntity<Void> sil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        taksitService.sil(id, sirketId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/plan/{planNo}")
    @Operation(summary = "Taksit plani sil", description = "Bir plana ait tum taksit kalemlerini siler")
    public ResponseEntity<Void> planSil(@PathVariable String planNo, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        taksitService.planSil(planNo, sirketId);
        return ResponseEntity.noContent().build();
    }
}
