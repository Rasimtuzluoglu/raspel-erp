package com.raspel.erp.controller;

import com.raspel.erp.dto.BankaDTO;
import com.raspel.erp.service.BankaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bankalar")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class BankaController {

    private final BankaService bankaService;

    @GetMapping
    public ResponseEntity<List<BankaDTO>> tumBankalariGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(bankaService.tumBankalariGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankaDTO> bankaGetir(@PathVariable Long id) {
        return ResponseEntity.ok(bankaService.bankaGetir(id));
    }

    @PostMapping
    public ResponseEntity<BankaDTO> bankaOlustur(@RequestBody @jakarta.validation.Valid BankaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(bankaService.bankaOlustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BankaDTO> bankaGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid BankaDTO dto) {
        return ResponseEntity.ok(bankaService.bankaGuncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bankaSil(@PathVariable Long id) {
        bankaService.bankaSil(id);
        return ResponseEntity.noContent().build();
    }
}
