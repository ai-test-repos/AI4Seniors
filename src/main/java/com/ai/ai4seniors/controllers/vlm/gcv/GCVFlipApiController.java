package com.ai.ai4seniors.controllers.vlm.gcv;

import com.ai.ai4seniors.data.MedicationInfo;
import com.ai.ai4seniors.services.ParseService;
import com.google.api.client.util.Value;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@RestController
public class GCVFlipApiController {

    @Value("${GOOGLE_APPLICATION_CREDENTIALS_PATH}")
    private String gcpPath;

    @Autowired
    ParseService parseService;

    @PostMapping("/gcvFlip1")
    public ResponseEntity<String> detectTextFromMultipleImages(List<MultipartFile> files) {
        Map<String, String> results = new LinkedHashMap<>(); // filename -> best detected text

        try {
            // Load Google Cloud credentials once
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream("/Users/shri/Desktop/WS/env/gcv-key.json"));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            for (MultipartFile file : files) {
                String filename = file.getOriginalFilename();

                try {
                    BufferedImage originalImage = ImageIO.read(file.getInputStream());
                    ByteString originalBytes = bufferedImageToByteString(originalImage);
                    ByteString flippedBytes = bufferedImageToByteString(flipHorizontally(originalImage));

                    String originalText = detectTextWithVisionAPI(originalBytes, settings);
                    String flippedText = detectTextWithVisionAPI(flippedBytes, settings);

                    int originalWords = countWords(originalText);
                    int flippedWords = countWords(flippedText);

                    String bestText = (flippedWords > originalWords) ? flippedText : originalText;
                    results.put(filename, bestText);

                } catch (Exception fileEx) {
                    results.put(filename, "Error processing file: " + fileEx.getMessage());
                }
            }

            String combinedText = String.join("\n", results.values());
            MedicationInfo mi = parseService.parseMedicationText(combinedText);

            return ResponseEntity.ok(mi.toString());
           // return ResponseEntity.ok(results);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(  e.getMessage());
        }
    }

    // Helper: Convert BufferedImage to ByteString
    private ByteString bufferedImageToByteString(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        baos.flush();
        ByteString bytes = ByteString.copyFrom(baos.toByteArray());
        baos.close();
        return bytes;
    }

    // Helper: Call Vision API to get text from image
    private String detectTextWithVisionAPI(ByteString imageBytes, ImageAnnotatorSettings settings) throws IOException {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            Image image = Image.newBuilder().setContent(imageBytes).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature).setImage(image).build();

            BatchAnnotateImagesResponse response = client.batchAnnotateImages(Collections.singletonList(request));
            AnnotateImageResponse imgResponse = response.getResponses(0);
            if (imgResponse.hasError()) {
                return ""; // fallback on error
            }
            return imgResponse.getFullTextAnnotation().getText();
        }
    }

    // Helper: Horizontally flip an image
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

    // Helper: Count words in detected text
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

}
