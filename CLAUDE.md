cat > CLAUDE.md << 'EOF'

\# AI Assistant Instructions



Đọc PROJECT\_CONTEXT.md trước khi làm bất kỳ task nào trong repo này.



Nguyên tắc bắt buộc:

1\. Mọi entity nghiệp vụ PHẢI có tenant\_id

2\. KHÔNG cross-service database join

3\. KHÔNG tạo service mới chưa được approve

4\. Mọi API thay đổi PHẢI update OpenAPI spec

5\. Code Java: package com.insightflow.{service}, dùng Lombok + MapStruct



Khi không chắc, hỏi lại thay vì đoán.

EOF

