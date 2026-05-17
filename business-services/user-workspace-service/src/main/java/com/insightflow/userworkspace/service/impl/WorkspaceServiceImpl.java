package com.insightflow.userworkspace.service.impl;


import com.insightflow.userworkspace.dto.event.InventoryFileUploadedPayload;
import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;
import com.insightflow.userworkspace.entity.FileMetadata;
import com.insightflow.userworkspace.entity.Workspace;
import com.insightflow.userworkspace.exception.ApiException;
import com.insightflow.userworkspace.messaging.InventoryEventProducer;
import com.insightflow.userworkspace.repository.FileMetadataRepository;
import com.insightflow.userworkspace.repository.WorkspaceRepository;
import com.insightflow.userworkspace.security.UserContext;
import com.insightflow.userworkspace.service.WorkspaceService;
import com.insightflow.userworkspace.dto.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final InventoryEventProducer eventProducer;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.presign-expiration-minutes}")
    private Long presignMinutes;

    @Override
    public CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        String workspaceId = UUID.randomUUID().toString();
        String userId = UserContext.getCurrentUserId();

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .userId(userId)
                .name(request.getWorkspaceName())
                .status("INIT")
                .progress(0)
                .build();
        workspaceRepository.save(workspace);

        String s3Key = "uploads/" + workspaceId + "/" + request.getFileName();
        String presignedUrl = generatePresignedUrl(s3Key, request.getContentType());

        FileMetadata metadata = FileMetadata.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .fileName(request.getFileName())
                .fileSize(0L)
                .contentType(request.getContentType())
                .s3FileUrl(buildS3Url(s3Key))
                .uploadedAt(null)
                .build();
        fileMetadataRepository.save(metadata);

        return CreateWorkspaceResponse.builder()
                .workspaceId(workspaceId)
                .s3PresignedUrl(presignedUrl)
                .build();
    }

    @Override
    public void confirmUpload(String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException("Workspace not found", "WORKSPACE_NOT_FOUND"));

        workspace.setStatus("PROCESSING");
        workspace.setProgress(1);
        workspaceRepository.save(workspace);

        FileMetadata metadata = fileMetadataRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ApiException("File metadata not found", "FILE_METADATA_NOT_FOUND"));
        metadata.setUploadedAt(OffsetDateTime.now());
        fileMetadataRepository.save(metadata);

        InventoryFileUploadedPayload payload = InventoryFileUploadedPayload.builder()
                .workspaceId(workspaceId)
                .userId(workspace.getUserId())
                .s3FileUrl(metadata.getS3FileUrl())
                .build();

        EventEnvelope<InventoryFileUploadedPayload> envelope = EventEnvelope.<InventoryFileUploadedPayload>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("inventory.file.uploaded")
                .timestamp(OffsetDateTime.now().toString())
                .source("user-workspace-service")
                .payload(payload)
                .build();

        eventProducer.sendFileUploaded(envelope);
    }

    @Override
    public WorkspaceResponse getWorkspace(String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException("Workspace not found", "WORKSPACE_NOT_FOUND"));

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .userId(workspace.getUserId())
                .name(workspace.getName())
                .status(workspace.getStatus())
                .errorMessage(workspace.getErrorMessage())
                .aiRecommendation(workspace.getAiRecommendation())
                .progress(workspace.getProgress())
                .createdAt(workspace.getCreatedAt() != null ? workspace.getCreatedAt().toString() : null)
                .updatedAt(workspace.getUpdatedAt() != null ? workspace.getUpdatedAt().toString() : null)
                .build();
    }

    @Override
    public void updateRecommendation(String workspaceId, String recommendationText, Integer progress) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException("Workspace not found", "WORKSPACE_NOT_FOUND"));
        workspace.setStatus("COMPLETED");
        workspace.setAiRecommendation(recommendationText);
        workspace.setProgress(progress != null ? progress : 100);
        workspaceRepository.save(workspace);
    }

    @Override
    public void updateFailure(String workspaceId, String errorMessage) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException("Workspace not found", "WORKSPACE_NOT_FOUND"));
        workspace.setStatus("FAILED");
        workspace.setErrorMessage(errorMessage);
        workspaceRepository.save(workspace);
    }

    private String buildS3Url(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    private String generatePresignedUrl(String key, String contentType) {
        try (S3Presigner presigner = S3Presigner.builder().region(software.amazon.awssdk.regions.Region.of(region)).build()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                    .signatureDuration(java.time.Duration.ofMinutes(presignMinutes))
                    .putObjectRequest(putObjectRequest));
            return presignedRequest.url().toString();
        }
    }
}