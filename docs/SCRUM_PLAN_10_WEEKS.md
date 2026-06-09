# Kế Hoạch Scrum 10 Tuần — Insight Flow AI (Backend MVP)

> **Mục tiêu tối thượng:** Cuối **Tuần 5** deploy MVP cho khách hàng dùng thử. **Tuần 6–10** chỉ chỉnh sửa theo feedback người dùng (không thêm feature lớn).
>
> **Phương pháp:** Scrum sprint 1 tuần (Tuần 1–5) → chuyển sang Kanban feedback-driven (Tuần 6–10).
>
> **Ngày tạo:** 2026-06-03 · **Repo:** `insight-flow-ai` (backend only)

---

## 1. Tóm Tắt Trạng Thái Hiện Tại (Baseline)

| Hạng mục | Trạng thái | Ghi chú |
|----------|-----------|---------|
| `discovery-server` (Eureka) | ✅ Done | Port 8761 |
| `config-server` | ✅ Done | Port 8888 |
| `shared-core` (common-web/security/events) | ✅ Done | Đã `mvn install` được |
| docker-compose (Kafka/Redis/Postgres) | ✅ Done | Infra dev |
| `auth-service` | ✅ Done | Register-tenant, login, refresh, user/role mgmt, Flyway V1–V6, port 8081 |
| `api-gateway` | 🔄 In progress | Mới có `CorrelationIdFilter`. **Thiếu JWT validation filter + routing + CORS + rate limit** |
| `catalog-service` | 📋 Chưa làm | Products, variants, inventory |
| `sales-service` | 📋 Chưa làm | Orders, customers, suppliers |
| `ml-service` | 📋 Chưa làm | Prophet forecast + rule-based recommendation |
| `integration-service` | 📋 Chưa làm | **MVP: CSV import**. KiotViet đẩy sang Phase 2 |
| `dashboard-bff` | 📋 Chưa làm | Aggregate API cho frontend |
| `notification-service` | 📋 Chưa làm | Email/in-app (tối giản cho MVP) |
| CI/CD, observability, deploy | 📋 Chưa làm | GitHub Actions, Prometheus/Grafana, deploy VM |

---

## 2. Quyết Định Chốt (Scope MVP)

1. **Nguồn data pilot = CSV/Excel import** (shop export từ KiotViet/Sapo → upload). KiotViet connector real-time đẩy sang Phase 2 để giảm rủi ro deadline.
2. **Sprint 1 tuần**, 10 sprints. Deploy rơi đúng cuối Sprint 5.
3. **Tuần 6–10 chạy Kanban**: feedback khách → triage theo ưu tiên → fix. Không feature mới trừ khi khách yêu cầu là blocker.
4. **ML Phase 1**: Prophet (category-level fallback cho cold start) + recommendation rule-based. KHÔNG deep learning.
5. **notification-service tối giản**: chỉ in-app + email alert cơ bản. Zalo đẩy Phase 2.

---

## 3. Team & Ownership

| Người | Vai trò | Service sở hữu (agent owner trong CLAUDE.md) |
|-------|---------|-----------------------------------------------|
| **BE1** | Backend Java | `api-gateway` (gateway-agent), `auth-service` (maintain), `catalog-service` (database-agent) |
| **BE2** | Backend Java | `sales-service`, `integration-service` (CSV), `dashboard-bff` (frontend-agent) |
| **ML** | Python | `ml-service` (ai-agent) |
| **FS** | Fullstack | Repo `insight-flow-frontend` + hỗ trợ deploy/devops + nghiệm thu integration |

> Nguyên tắc CLAUDE.md: **1 service = 1 owner agent**. Cross-service feature do root điều phối, gọi agent tuần tự.

---

## 4. Backlog Theo Epic

| Epic | Mô tả | Service liên quan | Ưu tiên |
|------|-------|-------------------|---------|
| **E1 — Gateway hoàn chỉnh** | JWT validation filter, routing tới tất cả service, CORS frontend, rate limit | api-gateway | P0 |
| **E2 — Catalog** | Products, variants, locations, inventory (append-only movements) + events | catalog-service | P0 |
| **E3 — Sales** | Orders, order items, customers, suppliers + event `sales.order.completed` | sales-service | P0 |
| **E4 — Data ingestion (CSV)** | Upload CSV products + sales history → publish vào catalog/sales | integration-service | P0 |
| **E5 — ML Forecast + Recommend** | Prophet forecast, rule-based recommendation, consume Kafka, publish ml events | ml-service | P0 |
| **E6 — Dashboard BFF** | Aggregate forecast + inventory + recommendation cho frontend | dashboard-bff | P0 |
| **E7 — Notification** | In-app + email alert (clearance/restock) | notification-service | P1 |
| **E8 — Deploy & Observability** | Dockerfile mỗi service, CI/CD, deploy VM, Prometheus/Grafana/Loki, healthcheck | infra/observability | P0 |
| **E9 — API contracts** | Export OpenAPI specs cho frontend mỗi sprint | api-contracts | P0 |
| **E10 — Pilot onboarding** | Seed tenant, hướng dẫn, thu thập feedback | docs/runbooks | P0 (tuần 5+) |

---

## 5. Roadmap Tổng

```
PHASE 1 — BUILD (Scrum, sprint 1 tuần)
 Tuần 1  Sprint 1  Gateway hoàn chỉnh + Catalog skeleton + CI nền
 Tuần 2  Sprint 2  Catalog đầy đủ + Sales skeleton + CSV import v1
 Tuần 3  Sprint 3  Sales đầy đủ + ML forecast v1 + event wiring
 Tuần 4  Sprint 4  ML recommendation + Dashboard BFF + Notification tối giản
 Tuần 5  Sprint 5  Hardening + Deploy production + Pilot onboarding   ⭐ GO-LIVE
─────────────────────────────────────────────────────────────────────
PHASE 2 — FEEDBACK (Kanban)
 Tuần 6-10        Triage feedback → fix → release nhỏ liên tục (no big feature)
```

---

## 6. Chi Tiết Sprint (Phase 1)

### 🟦 Sprint 1 — Tuần 1: "Gateway thông + Catalog khởi động"

**Sprint Goal:** Request có JWT đi qua gateway tới được auth & catalog skeleton; CI build xanh.

| ID | Task | Owner | DoD |
|----|------|-------|-----|
| S1-1 | Gateway: JWT validation filter (verify signature + expiry, dùng `app.jwt.secret` chung auth) | BE1 | Token hợp lệ pass, hết hạn/sai chữ ký → 401 RFC7807 |
| S1-2 | Gateway: routing rule cho auth + propagate `X-Tenant-Id`/`X-User-Id` xuống downstream | BE1 | Route `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/roles/**` chạy qua 8080 |
| S1-3 | Gateway: CORS whitelist `localhost:3000` + rate limit cơ bản (Redis) | BE1 | CORS preflight pass; vượt limit → 429 |
| S1-4 | Catalog: scaffold service (pom, application.yml, Eureka/config client, package layout) | BE1 | Service register Eureka, `/actuator/health` UP, port 8082 |
| S1-5 | Catalog: Flyway V1 — bảng `products`, `product_variants`, `locations`, `inventory_levels` (UUID, tenant_id, created/updated_at) | BE1 | Migration chạy trên schema `catalog_db` |
| S1-6 | CI/CD nền: GitHub Actions `ci-java.yml` build + test toàn bộ Java service | FS | PR mở → CI chạy, build xanh |
| S1-7 | Config-repo: thêm `api-gateway.yml`, `catalog-service.yml` | BE1 | Service load config từ config-server |
| S1-8 | Frontend (repo riêng): khung dự án + auth flow gọi qua gateway | FS | Login UI gọi `/api/v1/auth/login` thật |
| S1-9 | ML: setup repo Python (FastAPI scaffold, `app/{api,services,models,utils}`, ruff/black, Dockerfile) | ML | `/health` 200, container build được |

**Demo cuối tuần:** Login qua gateway (8080) → nhận JWT → gọi `/api/v1/auth/me` thành công. Catalog health UP.

---

### 🟦 Sprint 2 — Tuần 2: "Catalog đầy đủ + Sales khởi động + nạp data"

**Sprint Goal:** Quản lý sản phẩm/tồn kho hoàn chỉnh; import CSV products chạy được.

| ID | Task | Owner | DoD |
|----|------|-------|-----|
| S2-1 | Catalog: CRUD products + variants (controller→service→repo, DTO, MapStruct, validate, tenant filter) | BE1 | CRUD chạy, mọi query filter `tenant_id`, có `@Operation` |
| S2-2 | Catalog: inventory `inventory_movements` append-only + tính tồn hiện tại | BE1 | Nhập/xuất tồn → movement ghi nhận, tồn đúng |
| S2-3 | Catalog: publish `catalog.product.created`, `catalog.inventory.updated` (common-events) | BE1 | Event lên Kafka đúng schema |
| S2-4 | Catalog: export OpenAPI → `api-contracts/catalog-service.yaml` | BE1 | Spec commit, frontend pull được |
| S2-5 | Sales: scaffold + Flyway V1 (`orders`, `order_items`, `customers`, `suppliers`) | BE2 | Health UP port 8083, schema `sales_db` |
| S2-6 | Integration: scaffold + endpoint upload CSV products (validate, map → gọi catalog API/event) | BE2 | Upload CSV mẫu → products xuất hiện trong catalog |
| S2-7 | Integration: định nghĩa format CSV chuẩn (products, sales history) + docs | BE2 | Template CSV + hướng dẫn trong `docs/` |
| S2-8 | Gateway: thêm route catalog + sales | BE1 | `/api/v1/products/**`, `/api/v1/orders/**` qua 8080 |
| S2-9 | Frontend: màn quản lý sản phẩm + upload CSV | FS | UI list/create product, upload CSV chạy |
| S2-10 | ML: ingest layer — consume `catalog.*`, lưu vào store nội bộ (Postgres/parquet) | ML | Event catalog → ML lưu được |

**Demo:** Upload CSV → sản phẩm + tồn kho hiển thị; event chảy sang ML.

---

### 🟦 Sprint 3 — Tuần 3: "Sales đầy đủ + Forecast v1"

**Sprint Goal:** Có đơn hàng + lịch sử bán; ML sinh forecast đầu tiên từ data thật.

| ID | Task | Owner | DoD |
|----|------|-------|-----|
| S3-1 | Sales: CRUD orders + order_items + customers (tenant filter, DTO, validate) | BE2 | CRUD chạy, OpenAPI export |
| S3-2 | Sales: publish `sales.order.completed` khi đơn hoàn tất | BE2 | Event lên Kafka, catalog consume trừ tồn |
| S3-3 | Integration: upload CSV sales history → tạo orders | BE2 | CSV bán hàng → orders + events |
| S3-4 | Catalog: consume `sales.order.completed` → cập nhật inventory movement | BE1 | Bán hàng tự trừ tồn |
| S3-5 | ML: forecast Prophet theo SKU + fallback category-level (cold start) | ML | Forecast 30 ngày cho SKU có ≥N data point; SKU mới dùng category |
| S3-6 | ML: publish `ml.forecast.generated` | ML | Event lên Kafka đúng schema |
| S3-7 | ML: REST endpoint `GET /forecast/{sku}` (cho BFF gọi sync khi cần) | ML | Trả forecast JSON |
| S3-8 | Observability: Prometheus scrape + Grafana dashboard cơ bản (health, latency) | FS | Dashboard thấy được trạng thái service |
| S3-9 | Frontend: màn đơn hàng + biểu đồ forecast (đọc từ BFF stub) | FS | UI hiển thị forecast giả lập/thật |

**Demo:** Import lịch sử bán → ML sinh forecast → xem được số liệu forecast theo SKU.

---

### 🟦 Sprint 4 — Tuần 4: "Recommendation + BFF + Notification"

**Sprint Goal:** Khép vòng giá trị: forecast → đề xuất hành động tồn kho → hiển thị + cảnh báo.

| ID | Task | Owner | DoD |
|----|------|-------|-----|
| S4-1 | ML: recommendation rule-based ("tồn >90 ngày + sales giảm >50% → clearance"; sắp hết + forecast tăng → restock) | ML | Sinh recommendation cho data pilot |
| S4-2 | ML: publish `ml.recommendation.created` | ML | Event lên Kafka |
| S4-3 | Dashboard BFF: scaffold + aggregate endpoint (inventory + forecast + recommendation) cho dashboard | BE2 | `GET /api/v1/dashboard/overview` trả data gộp |
| S4-4 | BFF: consume ml events / gọi ml + catalog REST, transform cho FE | BE2 | Response khớp contract FE cần |
| S4-5 | BFF: export OpenAPI → `api-contracts/dashboard-bff.yaml` | BE2 | Spec commit |
| S4-6 | Gateway: route BFF (`/api/v1/dashboard/**`) | BE1 | Qua 8080, JWT enforced |
| S4-7 | Notification: scaffold + consume `ml.recommendation.created`/`ml.forecast.generated` → in-app + email | BE1 | Có recommendation → alert in-app + email gửi (SMTP dev) |
| S4-8 | Notification: idempotent consumer (dedupe `event_id`) | BE1 | Event trùng không gửi 2 lần |
| S4-9 | Frontend: dashboard tổng quan (KPI, forecast chart, danh sách đề xuất, alert) | FS | Dashboard end-to-end với data thật |
| S4-10 | Dockerfile cho tất cả service còn thiếu | BE1/BE2/ML | `docker build` mỗi service OK |

**Demo:** Dashboard hiển thị: tồn kho + forecast + danh sách đề xuất clearance/restock + alert. **Đây là vòng giá trị MVP hoàn chỉnh.**

---

### 🟥 Sprint 5 — Tuần 5: "Hardening + GO-LIVE" ⭐

**Sprint Goal:** Deploy toàn hệ thống lên môi trường production-like; onboard 2–3 shop pilot.

| ID | Task | Owner | DoD |
|----|------|-------|-----|
| S5-1 | `docker-compose.prod.yml`: tất cả service + infra, env qua biến môi trường, secret encrypt (Jasypt) | FS+BE1 | `docker compose up` chạy full stack |
| S5-2 | Deploy lên VM (cloud) + reverse proxy + HTTPS (domain pilot) | FS | Truy cập production URL qua HTTPS |
| S5-3 | CI/CD: pipeline deploy (build image → push registry → deploy VM) | FS | Merge `main` → auto deploy |
| S5-4 | Smoke test end-to-end production: register tenant → upload CSV → forecast → dashboard | BE2 | Checklist pass 100% trên prod |
| S5-5 | Hardening bảo mật: rà tenant_id mọi query, rate limit, CORS production domain, secrets không plaintext | BE1 | Checklist security review pass |
| S5-6 | Seed dữ liệu + tạo tenant cho 2–3 shop pilot (import CSV thật của họ) | BE2 | Mỗi shop login được, thấy data của mình |
| S5-7 | Runbook + hướng dẫn sử dụng cho shop (`docs/runbooks/pilot-guide.md`) | FS | Tài liệu onboarding hoàn chỉnh |
| S5-8 | Kênh thu thập feedback (form/Zalo/sheet) + template bug report | FS | Khách biết báo lỗi/đề xuất ở đâu |
| S5-9 | Backup DB + alert cơ bản (service down → notify team) | BE1 | Backup tự động daily, alert chạy |
| S5-10 | Performance check: load nhẹ, log review, fix lỗi nghiêm trọng | All | Không có P0/P1 bug treo |

**Mốc GO-LIVE:** Cuối tuần 5 — 2–3 shop pilot có tài khoản, data thật, dùng được dashboard + đề xuất.

**Definition of Done cho GO-LIVE:**
- [ ] Toàn bộ service chạy trên production URL (HTTPS)
- [ ] Luồng: đăng ký → upload CSV → forecast → đề xuất → alert chạy thông
- [ ] Multi-tenant: data shop A không lẫn shop B (đã test)
- [ ] OpenAPI specs trong `api-contracts/` cập nhật, frontend tích hợp xong
- [ ] Backup + healthcheck + kênh feedback sẵn sàng
- [ ] Không còn bug P0/P1

---

## 7. Phase 2 — Tuần 6–10: Kanban Feedback-Driven

> **Nguyên tắc:** KHÔNG thêm feature lớn mới. Chỉ chỉnh sửa theo feedback shop pilot. Mọi thay đổi xuất phát từ nhu cầu thật của người dùng.

### Quy trình Kanban

```
[Inbox feedback] → [Triage] → [To Do] → [In Progress] → [Review/QA] → [Deploy] → [Verify với khách]
```

**Cột Triage — phân loại ưu tiên:**
- **P0 Blocker** — khách không dùng được → fix trong ngày, hotfix deploy.
- **P1 Quan trọng** — ảnh hưởng giá trị chính (forecast sai, đề xuất vô lý) → fix trong tuần.
- **P2 UX/nhỏ** — chỉnh giao diện, copy, tiện ích → gom batch release.
- **P3 Nice-to-have / Phase 3** — ghi vào backlog tương lai, không làm bây giờ.

### Nhịp làm việc tuần 6–10

| Cadence | Hoạt động |
|---------|-----------|
| Hằng ngày | Standup 15' + review feedback inbox + cập nhật Kanban |
| 2 lần/tuần | Release nhỏ (gom P1/P2 đã xong) → deploy → báo khách |
| Cuối tuần | Tổng hợp feedback + metrics pilot (xem mục 9) gửi stakeholder |
| Tuần 8 | Mid-pilot review: đánh giá North Star (shop có giảm dead stock?) |
| Tuần 10 | Pilot wrap-up: báo cáo kết quả + quyết định scope Phase 2 (KiotViet, Zalo, deep ML...) |

### Loại công việc cho phép trong Phase 2
- ✅ Sửa bug, tinh chỉnh rule recommendation theo phản hồi shop
- ✅ Tinh chỉnh tham số forecast (cold-start threshold, seasonality)
- ✅ Cải thiện UX/copy, thêm filter/sort nhỏ trên dashboard
- ✅ Cải thiện format/validate CSV import theo data thật của shop
- ✅ Tối ưu performance, sửa lỗi multi-tenant nếu phát hiện
- ❌ KHÔNG: KiotViet connector, Zalo, deep learning, service mới (→ backlog Phase 2 thật sự)

---

## 8. Scrum Ceremonies (Phase 1)

| Ceremony | Khi nào | Thời lượng | Mục đích |
|----------|---------|-----------|----------|
| Sprint Planning | Thứ 2 đầu tuần | 1h | Chốt sprint goal + task tuần |
| Daily Standup | Mỗi sáng | 15' | Hôm qua / hôm nay / blocker |
| Sprint Review (Demo) | Thứ 6 chiều | 45' | Demo deliverable, nghiệm thu DoD |
| Retrospective | Thứ 6 sau review | 30' | Cải tiến quy trình |
| Backlog Refinement | Giữa tuần (thứ 4) | 30' | Chuẩn bị task sprint sau |

**Definition of Done (mọi task code):**
- [ ] Code theo convention CLAUDE.md (layered, DTO riêng, tenant filter)
- [ ] Mọi query nghiệp vụ filter `tenant_id`
- [ ] Endpoint có `@Operation` + `@ApiResponse`; OpenAPI spec cập nhật cùng PR
- [ ] Migration Flyway nếu đổi schema (không sửa migration cũ)
- [ ] Event schema cập nhật `shared-core/common-events` nếu đổi payload
- [ ] PR review xong, CI xanh, 1 service/PR
- [ ] Không commit `target/`, `.idea/`, secrets

---

## 9. Metrics Theo Dõi

**Delivery (Phase 1):**
- Sprint burndown (task done / committed)
- CI pass rate, số PR merge/tuần
- Số bug P0/P1 mở

**Pilot (Phase 2 — North Star):**
- Tỷ lệ shop giảm dead stock sau khi áp dụng đề xuất (mục tiêu cốt lõi PROJECT_CONTEXT)
- Số đề xuất shop **chấp nhận / bỏ qua** (đo độ hữu ích recommendation)
- Độ chính xác forecast (MAPE) trên data thật
- Số feedback / tuần, thời gian fix trung bình theo mức ưu tiên
- Tần suất shop đăng nhập + upload CSV (engagement)

---

## 10. Rủi Ro & Giảm Thiểu

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|--------|-----------|-----------|
| 6 service trong 5 tuần với 2 BE quá tải | Trễ deploy | Cắt notification xuống tối giản; CSV thay KiotViet; scaffold dùng mẫu auth-service đã có |
| Cold start ML — pilot thiếu lịch sử bán | Forecast yếu | Fallback category-level; yêu cầu shop export ≥3–6 tháng lịch sử |
| Chất lượng/định dạng CSV của shop lung tung | Import lỗi | Template CSV chuẩn + validate rõ + thông báo lỗi dòng cụ thể |
| Deploy lần đầu tốn thời gian | Trễ go-live | Dockerfile + compose.prod làm dần từ Sprint 1–4, không dồn tuần 5 |
| Multi-tenant leak data | Sự cố bảo mật pilot | Test cách ly tenant trong S5-5; common-security enforce sẵn |
| Feedback tuần 6+ biến thành feature mới | Vỡ scope | Triage nghiêm: P3/feature mới → backlog Phase 2, không làm trong pilot |

---

## 11. Phụ Lục — Thứ Tự Phụ Thuộc (Dependency)

```
auth ✅ ──► gateway(JWT) ──► tất cả service
shared-core ✅ ──► catalog ──► sales (consume order→trừ tồn)
                      │            │
                CSV import ───────►┘
                      │
            catalog+sales events ──► ml-service ──► BFF ──► frontend
                                          │
                                    notification
deploy/observability: làm song song, gom ở Sprint 5
```

---

*Cập nhật kế hoạch này mỗi cuối sprint. Khi đổi scope, ghi lý do vào PR description + thông báo team.*
