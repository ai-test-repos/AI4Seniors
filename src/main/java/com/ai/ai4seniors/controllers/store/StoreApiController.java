package com.ai.ai4seniors.controllers.store;

import com.ai.ai4seniors.data.MedicationInfo;
import com.ai.ai4seniors.services.CSVService;
import com.ai.ai4seniors.services.DriveService;
import com.ai.ai4seniors.services.GCVService;
import com.ai.ai4seniors.services.ParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
public class StoreApiController {

    @Autowired
    GCVService gcvService;

    @Autowired
    ParseService parseService;

    @Autowired
    CSVService csvService;

    @Autowired
    DriveService driveService;

    @PostMapping("/store")
    public ResponseEntity<String> storeData(@RequestParam("files") MultipartFile[] files) throws IOException {

        String ocrResponse = gcvService.getGCVResponse(files);
        MedicationInfo entry = parseService.parseMedicationText(ocrResponse);

        File csvFile = driveService.downloadCsv();
        csvService.writeOrAppendToCSV(entry, csvFile);
        String link = driveService.uploadOrUpdateCsv(csvFile);

        return ResponseEntity.ok(entry.toString());
    }

    @PostMapping("/extractData")
    public ResponseEntity<String> extractData(@RequestParam("files") MultipartFile[] files) throws IOException {

        String ocrResponse = gcvService.getGCVResponse(files);
        MedicationInfo entry = parseService.parseMedicationText(ocrResponse);

        return ResponseEntity.ok(entry.toString());
    }

}
