package com.insightflow.userworkspace.service.impl;

import com.insightflow.userworkspace.dto.event.EventEnvelope;
import com.insightflow.userworkspace.dto.event.InventoryFileUploadedPayload;
import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;
import com.insightflow.userworkspace.entity.FileMetadata;
import com.insightflow.userworkspace.entity.Workspace;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.common.web.exception.UnauthorizedException;
import com.insightflow.userworkspace.messaging.InventoryEventProducer;
import com.insightflow.userworkspace.repository.FileMetadataRepository;
import com.insightflow.userworkspace.repository.WorkspaceRepository;
import com.insightflow.security.UserContext;
import com.insightflow.security.UserContextHolder;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final InventoryEventProducer eventProducer;
    private final S3Presigner s3Presigner;
    private final software.amazon.awssdk.services.s3.S3Client s3Client;


    @Value("${aws.s3.bucket-name}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.presign-expiration-minutes}")
    private Long presignMinutes;

    // Tenant context is populated by common-security's UserContextFilter from gateway headers.
    private UserContext requireContext() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.tenantId() == null) {
            throw new UnauthorizedException("Thiếu thông tin xác thực (tenant) — request phải đi qua gateway");
        }
        return ctx;
    }

    @Override
    @Transactional
    public CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        // 🛡️ LỚP GIÁP GÁC CỔNG: Chặn rác ngay tại cửa khi thấy sai đuôi mở rộng file
        String nameLower = request.getFileName().toLowerCase();
        if (!nameLower.endsWith(".csv") && !nameLower.endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Hệ thống chỉ chấp nhận định dạng file .csv hoặc .xlsx!");
        }

        UserContext ctx = requireContext();
        String tenantId = ctx.tenantId().toString();
        String userId = ctx.userId() != null ? ctx.userId().toString() : null;
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
                .fileSize(0L) // Sẽ được cập nhật thật khi FE đẩy thẳng lên S3
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
    public void confirmUpload(String workspaceId) {
        Workspace workspace = workspaceRepository.findByIdAndTenantId(workspaceId, requireContext().tenantId().toString())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên làm việc này"));

        FileMetadata metadata = fileMetadataRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy siêu dữ liệu của file"));

        // 🚨 CHỐT CHẶN KIỂM TRA S3: Phải thấy file vật lý mới cho phép chạy tiếp!
        try {
            String s3Key = "uploads/" + workspaceId + "/" + metadata.getFileName();
            software.amazon.awssdk.services.s3.model.HeadObjectRequest headRequest =
                    software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build();

            // Gọi lệnh HeadObject (chỉ lấy metadata, không tải file, tốc độ cực nhanh)
            software.amazon.awssdk.services.s3.model.HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            // Tiện tay cập nhật dung lượng file thật vào DB luôn cho xịn
            metadata.setFileSize(headResponse.contentLength());

        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            // NẾU FILE KHÔNG CÓ TRÊN S3 -> QUĂNG LỖI 400 NGAY LẬP TỨC!
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "File chưa được tải lên S3! Vui lòng kiểm tra lại bước Upload (API 2).");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_ERROR, "Lỗi kết nối đến S3 để kiểm tra file: " + e.getMessage());
        }

        // ✅ Qua được đoạn trên nghĩa là S3 ĐÃ CÓ FILE. Lúc này mới cấp phép chạy ngầm.
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

        eventProducer.sendFileUploaded(envelope);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(String workspaceId) {
        Workspace workspace = workspaceRepository.findByIdAndTenantId(workspaceId, requireContext().tenantId().toString())
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
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    private String generatePresignedUrl(String key, String contentType) {
        // Use the injected presigner bean so the MinIO endpoint override is honoured
        // (the old code built a fresh presigner that pointed at real AWS).
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
    public List<WorkspaceResponse> getCompletedHistories() {

        String tenantId = requireContext().tenantId().toString();
        List<Workspace> workspaces = workspaceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        // Map sang DTO Response
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