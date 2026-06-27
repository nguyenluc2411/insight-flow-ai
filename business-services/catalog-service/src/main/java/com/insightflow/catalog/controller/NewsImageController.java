package com.insightflow.catalog.controller;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
public class NewsImageController {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket:insightflow-bucket}")
    private String bucketName;

    // Public URL base to return to Editor.js (can be changed in production)
    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    @PostMapping("/admin/news/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) originalFilename = "image.png";
            String filename = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String objectName = "news/" + filename;

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", 1);
            Map<String, String> fileData = new HashMap<>();
            fileData.put("url", publicUrl + "/api/v1/catalog/public/news/uploads/" + filename);
            response.put("file", fileData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", 0));
        }
    }

    @GetMapping("/public/news/uploads/{filename}")
    public ResponseEntity<?> getImage(@PathVariable String filename) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object("news/" + filename)
                            .build()
            );

            // Determine content type based on extension
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (filename.toLowerCase().endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
            else if (filename.toLowerCase().endsWith(".gif")) mediaType = MediaType.IMAGE_GIF;
            
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
