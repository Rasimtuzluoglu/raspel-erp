package com.raspel.erp.service.sistem;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * CODE128 barkod görüntüsü (PNG) üretir. Etiket PDF'leri için kullanılır.
 */
@Service
public class BarkodService {

    public byte[] barkodPng(String icerik, int genislik, int yukseklik) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix matrix = writer.encode(icerik, BarcodeFormat.CODE_128, genislik, yukseklik);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Barkod oluşturulamadı", e);
        }
    }
}
