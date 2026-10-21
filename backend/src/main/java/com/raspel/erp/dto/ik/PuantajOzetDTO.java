package com.raspel.erp.dto.ik;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuantajOzetDTO {
    private long toplam;
    private long geldi;
    private long gelmedi;
    private long izinli;
    private long mazeretli;
}
