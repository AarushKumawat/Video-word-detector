package com.video.word.Controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.video.word.config.ReplicateConfig;

@CrossOrigin(origins = "chrome-extension://${replicateConfig.extensionId}")  // Dynamic extension ID
@RestController
@RequestMapping("/api/transcription")
public class TranscriptionController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ReplicateConfig replicateConfig;

    public TranscriptionController(ReplicateConfig replicateConfig) {
        this.replicateConfig = replicateConfig;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            // Save the file locally
            File audioFile = convertMultipartFileToFile(file);

            // Convert audio file to a Base64-encoded string
            String base64Audio = encodeFileToBase64(audioFile);

            // Prepare the API request
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token " + replicateConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = """
                {
                  "version": "9ca6e9d9d270e8e4bcb792b71cb3171ce172f6f589019083a5d9d54048c67e34",
                  "input": {
                    "audio": "%s"
                  }
                }
                """.formatted(base64Audio);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity("https://api.replicate.com/v1/predictions", request, String.class);

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

    private String encodeFileToBase64(File file) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        return java.util.Base64.getEncoder().encodeToString(fileBytes);
    }
}
