package com.video.word.Controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;  // ✅ FIX: Import this to resolve "Files cannot be resolved" error

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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j; // Import SLF4J Logger

@RestController
@RequestMapping("/api/transcription")
@Slf4j // SLF4J Logger annotation
public class TranscriptionController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ReplicateConfig replicateConfig;
    private final String allowedOrigin;

    public TranscriptionController(ReplicateConfig replicateConfig) {
        this.replicateConfig = replicateConfig;
        this.allowedOrigin = "chrome-extension://" + replicateConfig.getExtensionId();
    }

    @PostConstruct
    public void configureCors() {
        log.info("CORS Allowed Origin: {}", allowedOrigin); // Log CORS allowed origin
    }

    @CrossOrigin(origins = "*")  // Set dynamically using response headers
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        log.info("Received upload request for file: {}", file.getOriginalFilename()); // Log file name

        try {
            File audioFile = convertMultipartFileToFile(file);
            log.info("File converted to temp file: {}", audioFile.getAbsolutePath()); // Log temp file path

            String base64Audio = encodeFileToBase64(audioFile);
            log.debug("Base64 encoded audio: {}", base64Audio.substring(0, Math.min(100, base64Audio.length()))); // Log the first 100 chars of base64 string

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token " + replicateConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = """
                {
                  "version": "8099696689d249cf8b122d833c36ac3f75505c666a395ca40ef26f68e7d3d16e",
                  "input": {
                    "audio": "%s"
                  }
                }
                """.formatted(base64Audio);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.replicate.com/v1/predictions", request, String.class);
            
            audioFile.delete();  // Cleanup temporary file
            log.info("Temporary file deleted: {}", audioFile.getAbsolutePath());

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("Access-Control-Allow-Origin", allowedOrigin);

            log.info("Transcription API response: {}", response.getBody()); // Log the API response

            return ResponseEntity.ok().headers(responseHeaders).body(response.getBody());

        } catch (IOException e) {
            log.error("File processing failed for file: {}", file.getOriginalFilename(), e); // Log error with exception
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
        byte[] fileBytes = Files.readAllBytes(file.toPath());  // ✅ Uses correct import now
        return java.util.Base64.getEncoder().encodeToString(fileBytes);
    }
}
