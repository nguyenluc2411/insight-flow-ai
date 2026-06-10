package com.insightflow.userworkspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.s3.endpoint-url:#{null}}")
    private String endpointUrl;

    // Public HTTPS endpoint the BROWSER uses to PUT/GET via presigned URLs.
    // Server-side ops (s3Client) keep the internal endpoint-url (e.g. http://minio:9000),
    // but presigned URLs must be signed with a publicly reachable HTTPS host, else the
    // browser hits Mixed Content / an unresolvable internal hostname. Falls back to
    // endpoint-url when unset (local dev).
    @Value("${aws.s3.public-endpoint-url:#{null}}")
    private String publicEndpointUrl;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                // AWS SDK v2 yêu cầu forcePathStyle true đối với S3/Minio local
                .forcePathStyle(true);

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }

        return builder.build();
    }


   // ...

       @Bean
       public S3Presigner s3Presigner() {
           S3Presigner.Builder builder = S3Presigner.builder()
                   .region(Region.of(region))
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create(accessKey, secretKey)
                   ))
                   // Tách biệt việc ép kiểu PathStyle cho Presigner
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)
                           .build());

           // Sign with the public HTTPS endpoint when configured so browser-facing
           // presigned URLs are reachable over HTTPS; otherwise fall back to internal.
           String presignEndpoint = (publicEndpointUrl != null && !publicEndpointUrl.isBlank())
                   ? publicEndpointUrl : endpointUrl;
           if (presignEndpoint != null && !presignEndpoint.isBlank()) {
               builder.endpointOverride(URI.create(presignEndpoint));
           }

           return builder.build();
       }
}