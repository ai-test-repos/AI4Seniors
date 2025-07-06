package com.ai.ai4seniors.controllers.medication1;

import com.ai.ai4seniors.data.MedicationInfo;
import com.ai.ai4seniors.services.CSVService;
import com.ai.ai4seniors.services.DriveService;
import com.ai.ai4seniors.services.ParseService;
import com.google.api.client.util.Value;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class MedicationApiController {

    @Autowired
    ParseService parseService;

    @Autowired
    CSVService csvService;

    @Autowired
    private DriveService driveService;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS_PATH}")
    private String gcpPath;

    @Value("${medication.csv.path}")
    private String csvFilePath;

    @PostMapping("/med1")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) throws IOException {
        String extractedText = "";

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
            extractedText = annotation.getText();
        }

        String[] lines = extractedText.split("\\r?\\n");
        String medication = "";
        String strength = "";
        String frequency = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (medication.isEmpty() && line.matches("^[A-Z]{2,}(\\s[A-Z]{2,})?$")) {
                medication = line;
            }

            if (strength.isEmpty() && line.matches("(?i).*\\d+\\s*mg.*")) {
                strength = line;
            }

            if (frequency.isEmpty() && line.toLowerCase().contains("every")) {
                frequency = line;
            }
        }

        StringBuilder reminders = new StringBuilder();
        if (!medication.isEmpty() && !frequency.isEmpty()) {
            reminders.append("Reminder set for ")
                    .append(medication);
            if (!strength.isEmpty()) {
                reminders.append(" (" + strength + ")");
            }
            reminders.append(" - ").append(frequency).append("\n");
        } else {
            reminders.append("Could not detect valid medication name and frequency.");
        }

        return ResponseEntity.ok(extractedText + " *** "+ reminders.toString());
    }

    @PostMapping("/med2")
    public ResponseEntity<String> extractText(@RequestParam("files") MultipartFile[] files) throws IOException {
      //  GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(gcpPath));
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/shri/Desktop/WS/env/gcv-key.json"));

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

        MedicationInfo mi = parseService.parseMedicationText(allText.toString());

        return ResponseEntity.ok(mi.toString());
    }

    @PostMapping("/med3")
    public ResponseEntity<String> extractText3(@RequestParam("files") MultipartFile[] files) throws IOException {
        //  GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(gcpPath));
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/shri/Desktop/WS/env/gcv-key.json"));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        List<MedicationInfo> newData = new ArrayList<>();

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

        MedicationInfo mi = parseService.parseMedicationText(allText.toString());
        newData.add(mi);

        String csvPath = csvService.writeOrAppendToCSV(newData);
        String link = driveService.uploadOrUpdateCsv(csvPath, "medication_log.csv");
        System.out.println(link);
        return ResponseEntity.ok(mi.toString());
    }

}
