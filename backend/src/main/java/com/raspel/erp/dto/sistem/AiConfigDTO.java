package com.raspel.erp.dto.sistem;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfigDTO {
    private String provider; // OPENAI, GOOGLE, ANTHROPIC
    private String apiKey;   // Full on save, masked on read (sk-****...4f2a)
    private String model;    // gpt-4o, gemini-2.5-flash, claude-sonnet-4
    private Boolean aktif;
    private String durum;    // AKTIF, PASIF, YAPILANDIRILMADI (read-only)
}
