package com.video.word.Controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transcription")
public class TranscriptionController {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String REPLICATE_API_URL = "https://api.replicate.com/v1/predictions";
    private static final String REPLICATE_API_KEY = "API_KEY";  // Replace with your Replicate API key

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            // Save the file locally
            File audioFile = convertMultipartFileToFile(file);

            // Prepare the API request
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token " + REPLICATE_API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = """
                {
                  "version": "9ca6e9d9d270e8e4bcb792b71cb3171ce172f6f589019083a5d9d54048c67e34",
                  "input": {
                    "audio": "%s"
                  }
                }
                """.formatted(audioFile.getAbsolutePath());

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(REPLICATE_API_URL, request, String.class);

            audioFile.delete();  // Cleanup temporary file

            return ResponseEntity.ok(response.getBody());

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processing failed");
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File("uploads/" + file.getOriginalFilename());
        convFile.getParentFile().mkdirs();
        convFile.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
