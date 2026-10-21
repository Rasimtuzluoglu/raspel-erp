package com.raspel.erp.dto.ik;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelDetayDTO {
    private PersonelDTO personel;
    /** Son izin kayıtları (en yeni önce, en fazla 10). */
    private List<PersonelIzinDTO> sonIzinler;
    private long puantajKayitSayisi;
    private long geldi;
    private long gelmedi;
    private long izinli;
    private long mazeretli;
    /** Bugün onaylı izni var mı? */
    private boolean bugunIzinli;
    private long bekleyenIzin;
}
