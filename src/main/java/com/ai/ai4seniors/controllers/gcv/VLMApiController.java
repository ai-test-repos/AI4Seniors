package com.ai.ai4seniors.controllers.gcv;

import com.google.api.client.util.Value;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class VLMApiController {

    @Value("${GOOGLE_APPLICATION_CREDENTIALS_PATH}")
    private String gcpPath;

    @GetMapping("/test")
    public String vision() throws Exception {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(gcpPath));
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            System.out.println("✅ Credentials are valid and client is ready.");
        }
        return "client is ready!";
    }

    @PostMapping("/gcv")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/shri/Desktop/WS/env/gcv-key.json"));
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        ByteString imgBytes = ByteString.readFrom(file.getInputStream());

        List<AnnotateImageRequest> requests = new ArrayList<>();

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            TextAnnotation annotation = response.getResponses(0).getFullTextAnnotation();
            return ResponseEntity.ok(annotation.getText());
        }
    }
}
