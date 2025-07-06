package com.ai.ai4seniors.controllers.vlm.gcv;

import com.google.api.client.util.Value;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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


    @PostMapping("/gcvMulti")
    public ResponseEntity<String> extractText(@RequestParam("files") MultipartFile[] files) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream("/Users/shri/Desktop/WS/env/gcv-key.json")
        );
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();

        for (MultipartFile file : files) {
            ByteString imgBytes = ByteString.readFrom(file.getInputStream());

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();
            requests.add(request);
        }

        StringBuilder allText = new StringBuilder();
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (int i = 0; i < responses.size(); i++) {
                AnnotateImageResponse res = responses.get(i);
                if (res.hasError()) {
                    allText.append("Image ").append(i + 1).append(" error: ")
                            .append(res.getError().getMessage()).append("\n");
                } else {
                    TextAnnotation annotation = res.getFullTextAnnotation();
                    allText.append("Image ").append(i + 1).append(" text:\n")
                            .append(annotation.getText()).append("\n\n");
                }
            }
        }

        return ResponseEntity.ok(allText.toString());
    }

    @PostMapping("/gcvFlip")
     public ResponseEntity<String> detectTextFromImage(MultipartFile file) {
        try {
            // Load Google Cloud credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream("/Users/shri/Desktop/WS/env/gcv-key.json"));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            // Step 1: Read MultipartFile into BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // Step 2: Flip image horizontally (mirror correction)
            BufferedImage flippedImage = flipHorizontally(originalImage);

            // Step 3: Convert flipped image back to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(flippedImage, "jpg", baos);
            baos.flush();
            ByteString imgBytes = ByteString.copyFrom(baos.toByteArray());
            baos.close();

            // Step 4: Prepare Vision API request
            Image visionImage = Image.newBuilder().setContent(imgBytes).build();
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(visionImage)
                    .build();

            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);

            // Step 5: Call Vision API
            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);

                AnnotateImageResponse imageResponse = response.getResponses(0);
                if (imageResponse.hasError()) {
                    return ResponseEntity.status(500)
                            .body("Error: " + imageResponse.getError().getMessage());
                }

                TextAnnotation annotation = imageResponse.getFullTextAnnotation();
                return ResponseEntity.ok(annotation.getText());
            }

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Exception: " + e.getMessage());
        }
    }

    // Helper method to flip image horizontally
    private BufferedImage flipHorizontally(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage flipped = new BufferedImage(width, height, img.getType());
        Graphics2D g = flipped.createGraphics();
        AffineTransform transform = AffineTransform.getScaleInstance(-1, 1);
        transform.translate(-width, 0);
        g.drawImage(img, transform, null);
        g.dispose();
        return flipped;
    }


}
