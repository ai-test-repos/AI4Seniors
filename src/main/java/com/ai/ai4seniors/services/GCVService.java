package com.ai.ai4seniors.services;

import com.ai.ai4seniors.data.MedicationInfo;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GCVService {

    @Value("${drive.credentials.path}")
    private String apiKeyPath;

    public String getGCVResponse(MultipartFile[] files) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(apiKeyPath));

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
        return allText.toString();
    }

}
