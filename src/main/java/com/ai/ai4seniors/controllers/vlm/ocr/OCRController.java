package com.ai.ai4seniors.controllers.vlm.ocr;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
public class OCRController {

    @PostMapping("/ocr")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) throws IOException {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
       // body.add("apikey", "K86063825388957"); // Replace with your actual API key
        body.add("apikey", "helloworld");
        body.add("language", "eng");

        Path tempFile = Files.createTempFile("ocr-remote-", file.getOriginalFilename());
        file.transferTo(tempFile);
        //body.add("file", new FileSystemResource(tempFile.toFile()));

        ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        body.add("file", fileAsResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String ocrUrl = "https://api.ocr.space/parse/image";
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(ocrUrl, requestEntity, String.class);
            return ResponseEntity.ok(response.getBody());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
