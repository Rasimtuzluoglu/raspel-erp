package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopluFiyatDTO {
    private String kategori;
    private String stokGrubu;
    private String marka;

    @NotBlank(message = "Yön seçilmelidir (ARTIR/AZALT)")
    private String yon;

    @NotNull(message = "Oran girilmelidir")
    @DecimalMin(value = "0.1", message = "Oran 0.1'den küçük olamaz")
    @DecimalMax(value = "1000", message = "Oran 1000'den büyük olamaz")
    private Double oran;

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public String getStokGrubu() { return stokGrubu; }
    public void setStokGrubu(String stokGrubu) { this.stokGrubu = stokGrubu; }
    public String getMarka() { return marka; }
    public void setMarka(String marka) { this.marka = marka; }
    public String getYon() { return yon; }
    public void setYon(String yon) { this.yon = yon; }
    public Double getOran() { return oran; }
    public void setOran(Double oran) { this.oran = oran; }
}
