package com.insightflow.userworkspace.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.userworkspace.dto.event.EventEnvelope;
import com.insightflow.userworkspace.dto.event.InventoryFileUploadedPayload;
import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;
import com.insightflow.userworkspace.entity.FileMetadata;
import com.insightflow.userworkspace.entity.OutboxEvent;
import com.insightflow.userworkspace.entity.Workspace;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.userworkspace.repository.FileMetadataRepository;
import com.insightflow.userworkspace.repository.OutboxRepository;
import com.insightflow.userworkspace.repository.WorkspaceRepository;
import com.insightflow.userworkspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final S3Presigner s3Presigner;
    private final software.amazon.awssdk.services.s3.S3Client s3Client;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.presign-expiration-minutes}")
    private Long presignMinutes;
    @Value("${aws.s3.endpoint-url:#{null}}")
    private String endpointUrl;

    @Override
    @Transactional
    public CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request, String tenantId, String userId) {
        String nameLower = request.getFileName().toLowerCase();
        if (!nameLower.endsWith(".csv") && !nameLower.endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Hệ thống chỉ chấp nhận định dạng file .csv hoặc .xlsx!");
        }

        String workspaceId = UUID.randomUUID().toString();

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .tenantId(tenantId)
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
                .tenantId(tenantId)
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
    @Transactional
    public void confirmUpload(String workspaceId, String tenantId) {
        Workspace workspace = workspaceRepository.findByIdAndTenantId(workspaceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên làm việc này"));

        FileMetadata metadata = fileMetadataRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy siêu dữ liệu của file"));

        try {
            String s3Key = "uploads/" + workspaceId + "/" + metadata.getFileName();
            software.amazon.awssdk.services.s3.model.HeadObjectRequest headRequest =
                    software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build();

            software.amazon.awssdk.services.s3.model.HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            metadata.setFileSize(headResponse.contentLength());
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "File chưa được tải lên S3! Vui lòng kiểm tra lại bước Upload.");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_ERROR, "Lỗi kết nối đến S3 để kiểm tra file: " + e.getMessage());
        }

        workspace.setStatus("PROCESSING");
        workspace.setProgress(10);
        workspaceRepository.save(workspace);

        metadata.setUploadedAt(OffsetDateTime.now());
        fileMetadataRepository.save(metadata);

        InventoryFileUploadedPayload payload = InventoryFileUploadedPayload.builder()
                .tenantId(workspace.getTenantId())
                .workspaceId(workspaceId)
                .userId(workspace.getUserId())
                .fileName(metadata.getFileName())
                .s3FileUrl(metadata.getS3FileUrl())
                .build();

        EventEnvelope<InventoryFileUploadedPayload> envelope = EventEnvelope.<InventoryFileUploadedPayload>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("inventory.file.uploaded")
                .timestamp(OffsetDateTime.now().toString())
                .source("user-workspace-service")
                .payload(payload)
                .build();

        Map<String, Object> envelopeMap = objectMapper.convertValue(
                envelope, new TypeReference<Map<String, Object>>() {});

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(UUID.fromString(workspace.getTenantId()))
                .eventType("inventory.file.uploaded")
                .payload(envelopeMap)
                .build();

        outboxRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(String workspaceId, String tenantId) {
        Workspace workspace = workspaceRepository.findByIdAndTenantId(workspaceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên làm việc này"));

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .userId(workspace.getUserId())
                .name(workspace.getName())
                .status(workspace.getStatus())
                .errorMessage(workspace.getErrorMessage())
                .progress(workspace.getProgress())
                .createdAt(workspace.getCreatedAt() != null ? workspace.getCreatedAt().toString() : null)
                .updatedAt(workspace.getUpdatedAt() != null ? workspace.getUpdatedAt().toString() : null)
                .build();
    }

    @Override
    @Transactional
    public void updateStatus(String workspaceId, String status, String errorMessage) {
        workspaceRepository.findById(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(status);
            if ("COMPLETED".equals(status)) {
                workspace.setProgress(100);
                workspace.setErrorMessage(null);
            } else if ("FAILED".equals(status)) {
                workspace.setErrorMessage(errorMessage);
            }
            workspaceRepository.save(workspace);
        });
    }

        private String buildS3Url(String key) {
        // Bạn đang hardcode amazonaws.com, đổi lại cho linh hoạt với MinIO
        if (endpointUrl != null) {
            return "%s/%s/%s".formatted(endpointUrl, bucket, key);
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    private String generatePresignedUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(java.time.Duration.ofMinutes(presignMinutes))
                .putObjectRequest(putObjectRequest));

        return presignedRequest.url().toString();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getCompletedHistories(String tenantId) {
        List<Workspace> workspaces = workspaceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        return workspaces.stream().map(workspace -> WorkspaceResponse.builder()
                .id(workspace.getId())
                .userId(workspace.getUserId())
                .name(workspace.getName())
                .status(workspace.getStatus())
                .progress(workspace.getProgress())
                .createdAt(workspace.getCreatedAt() != null ? workspace.getCreatedAt().toString() : null)
                .updatedAt(workspace.getUpdatedAt() != null ? workspace.getUpdatedAt().toString() : null)
                .build()).toList();
    }
}