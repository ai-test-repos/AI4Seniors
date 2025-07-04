package com.ai.ai4seniors.controllers.tesseract;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class TessController {

    @PostMapping("/tess")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) throws IOException {

        Path tempFile = Files.createTempFile("ocr-upload-", file.getOriginalFilename());
        file.transferTo(tempFile);
        System.setProperty("jna.library.path", "/opt/homebrew/lib");

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata"); // Adjust this path to match your Tesseract install location

        try {
            String result = tesseract.doOCR(tempFile.toFile());
            return ResponseEntity.ok(result);
        } catch (TesseractException e) {
            return ResponseEntity.internalServerError().body("OCR failed: " + e.getMessage());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
