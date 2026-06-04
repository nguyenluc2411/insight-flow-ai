package com.insightflow.dataingestion.service;

import java.io.InputStream;

public interface S3StorageService {
    InputStream downloadFileStream(String bucketName, String objectKey);
}