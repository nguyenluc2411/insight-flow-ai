package com.insightflow.dataingestion.service.impl;

import com.insightflow.dataingestion.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3Client;

    @Override
    public InputStream downloadFileStream(String bucketName, String objectKey) {
        try {
            log.info("Đang kết nối S3 để tải luồng dữ liệu: s3://{}/{}", bucketName, objectKey);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            // Trả về ResponseInputStream để đọc từng dòng (Stream), không gây tràn RAM
            ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);
            return s3ObjectStream;

        } catch (S3Exception e) {
            log.error("Lỗi S3 khi tải file: s3://{}/{} - Mã lỗi: {}", bucketName, objectKey, e.awsErrorDetails().errorCode());
            throw new RuntimeException("Không thể tải file từ S3: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Lỗi không xác định khi tải file từ S3: s3://{}/{}", bucketName, objectKey, e);
            throw new RuntimeException("Lỗi hệ thống khi đọc file Storage: " + e.getMessage(), e);
        }
    }
}