package com.example.mvc_default.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@Service
public class QRCodeService {
    public String productUrl(String baseUrl, String slug) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("product", slug)
                .build()
                .encode()
                .toUriString();
    }

    public String toBase64Png(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    250,
                    250,
                    Map.of(EncodeHintType.CHARACTER_SET, "UTF-8")
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("QR generation failed", e);
        }
    }
}
