# Email biên nhận thanh toán — Hướng dẫn cấu hình (Deploy / EC2)

Tính năng: **Sau khi khách thanh toán thành công, hệ thống tự gửi email biên nhận (hóa đơn)
chuyên nghiệp về hộp thư của khách.**

## Luồng hoạt động

```
Khách thanh toán (SePay webhook)
   → billing-service: kích hoạt gói + ghi outbox event "payment.success"
   → OutboxPublisher đẩy lên Kafka topic  billing.payment.success
   → notification-service: PaymentInvoiceConsumer nhận event
   → InvoiceEmailService dựng email HTML biên nhận
   → gửi qua SMTP tới email khách
```

- Email người nhận được lấy từ bảng `billing_db.tenant_contact`, được billing cache lại từ
  sự kiện `auth.tenant.registered` (mang sẵn `owner_email` + `owner_name`).
- Consumer **idempotent** (chống gửi trùng) qua bảng `processed_events` của notification-service.

---

## ⚠️ Bắt buộc để email GỬI THẬT (không thì chỉ vào MailHog)

Mặc định, `notification-service` gửi email vào **MailHog** (container bắt mail để test — khách
KHÔNG nhận được gì). Muốn gửi thật, phải set SMTP thật qua biến môi trường.

### Cách 1 (khuyến nghị MVP): Gmail App Password

1. Dùng 1 tài khoản Gmail. Bật **2-Step Verification**.
2. Tạo **App Password**: Google Account → Security → 2-Step Verification → App passwords →
   tạo password 16 ký tự (bỏ khoảng trắng khi dán).
3. Trên EC2, tại thư mục chạy docker compose (`infrastructure/docker/`), tạo/ sửa file `.env`:

```dotenv
# --- SMTP thật (Gmail) ---
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=your-account@gmail.com
MAIL_PASS=xxxxxxxxxxxxxxxx          # App Password 16 ký tự, KHÔNG phải mật khẩu Gmail
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_SMTP_SSL_TRUST=smtp.gmail.com
MAIL_FROM=your-account@gmail.com    # nên trùng MAIL_USER để Gmail không rewrite/chặn

# --- (tùy chọn) Thông tin bên bán in trên biên nhận ---
INVOICE_COMPANY_NAME=Insight Flow AI
INVOICE_COMPANY_ADDRESS=123 Đường ABC, Quận 1, TP.HCM
INVOICE_COMPANY_TAX_CODE=0312345678
INVOICE_SUPPORT_EMAIL=support@insightflow.ai
INVOICE_SUPPORT_HOTLINE=1900 1234
INVOICE_WEBSITE=https://insightflow.ai
```

4. Khởi động lại notification-service để nó nạp lại config:

```bash
cd infrastructure/docker
docker compose -f docker-compose.yml -f docker-compose.services.yml up -d notification-service
# hoặc: docker compose restart notification-service
```

> Lưu ý: `notification-service` lấy cấu hình mail từ **config-server** (file
> `config-repo/notification-service.yml`), và tự resolve `${...}` từ biến môi trường **của chính
> container notification-service** (đã map sẵn trong `docker-compose.services.yml`). Vì vậy chỉ cần
> đặt biến trong `.env` là đủ — không cần sửa code.

### Cách 2: Amazon SES / SMTP khác

Đặt tương tự với `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASS` của nhà cung cấp
(SES cần verify domain/email và ra khỏi sandbox trước).

---

## Kiểm thử

- **Dev (mặc định MailHog):** mở MailHog UI (cổng 8025) để xem email biên nhận — không cần SMTP thật.
- **Prod (Gmail):** thanh toán thử 1 giao dịch → kiểm tra hộp thư khách.
- Log để soi:
  - billing: `🧾 [SEPAY] Đã tạo biên nhận HD... cho tenant=... email=...`
  - notification: `[INVOICE-CONSUMER] Xử lý biên nhận ...` → `[INVOICE] Đã gửi biên nhận ...`
    → `[SMTP] SENT ...`

## Lưu ý về tenant cũ

`tenant_contact` chỉ được điền khi tenant **đăng ký sau khi bản này lên**. Với tenant đã tồn tại
từ trước (chưa có dòng contact), biên nhận sẽ log cảnh báo "không có email người nhận" và bỏ qua gửi.
Nếu cần gửi cho tenant cũ, chèn tay:

```sql
INSERT INTO billing_db.tenant_contact (tenant_id, email, name)
VALUES ('<tenant-uuid>', 'khach@example.com', 'Tên khách')
ON CONFLICT (tenant_id) DO UPDATE SET email = EXCLUDED.email, name = EXCLUDED.name;
```

## Không phải hóa đơn GTGT hợp pháp

Đây là **biên nhận / xác nhận thanh toán** (payment receipt) phục vụ khách và đối soát nội bộ,
KHÔNG phải hóa đơn GTGT điện tử có mã cơ quan thuế. Hóa đơn GTGT hợp pháp cần tích hợp nhà cung cấp
HĐĐT (VNPT/Viettel/MISA) — làm sau khi cần.
