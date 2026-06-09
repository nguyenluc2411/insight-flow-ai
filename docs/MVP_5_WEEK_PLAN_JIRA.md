# Kế Hoạch MVP 5 Tuần — Insight Flow AI (Jira)

> **Phạm vi:** Xây MVP backend + frontend từ con số 0 trong **5 tuần**, kết thúc tuần 5 deploy cho shop dùng thử.
> **Team:** 5 người — 4 dev + 1 AI. **Công cụ quản lý:** Jira (Scrum, sprint 1 tuần).
> **Ngày tạo:** 2026-06-03 · **Project key giả định:** `IF`

---

## 1. Giả Định & Quyết Định Scope

1. Lập kế hoạch **greenfield** — coi như chưa code gì, tính cả phần dựng nền (infra, platform).
2. **5 sprint, mỗi sprint 1 tuần.** Cuối Sprint 5 = GO-LIVE dùng thử.
3. **Nguồn data MVP = CSV/Excel import** (shop export từ KiotViet/Sapo → upload). KiotViet connector real-time, Zalo, deep learning → **defer Phase 2**.
4. **ML Phase 1:** Prophet (fallback category-level cho cold start) + recommendation **rule-based**.
5. **notification-service tối giản:** in-app + email.
6. Frontend ở **repo riêng** `insight-flow-frontend`, contract-first qua OpenAPI trong `api-contracts/`.

---

## 2. Team & Capacity

| Code | Vai trò | Trọng tâm |
|------|---------|-----------|
| **BE1** | Backend Java (Senior) | Platform (discovery/config/gateway), Auth, Multi-tenancy |
| **BE2** | Backend Java | Catalog, Sales |
| **BE3** | Backend Java + DevOps | Integration (CSV), Dashboard BFF, CI/CD & Deploy |
| **FE** | Frontend (repo riêng) | Web app, dashboard, tích hợp OpenAPI |
| **AI** | ML Engineer (Python) | ml-service: forecast + recommendation |

**Capacity giả định:** sprint 1 tuần ≈ **8 SP/người** (trừ ceremony & ramp) → **velocity mục tiêu ~36–40 SP/sprint**.
**Thang Story Point (Fibonacci):** 1 = vài giờ · 2 = nửa ngày · 3 = ~1 ngày · 5 = ~2 ngày · 8 = ~3–4 ngày · 13 = quá lớn, phải tách.

---

## 3. Cấu Trúc Jira

```
Epic  (capability lớn, ví dụ "Auth & Multi-tenancy")
 └─ Story  (giá trị giao được, có Acceptance Criteria + Story Point)
     └─ Sub-task  (việc kỹ thuật nhỏ, không tính SP riêng — gói trong Story)
```

**Components** (gắn cho Issue để lọc theo service): `gateway`, `auth`, `catalog`, `sales`, `integration`, `ml`, `bff`, `notification`, `frontend`, `infra`.
**Labels** đề xuất: `mvp`, `p0`, `p1`, `tech-debt`, `contract` (đụng OpenAPI), `security`.
**Sprint field:** Sprint 1 → Sprint 5.

---

## 4. Danh Sách Epic

| Epic | Tên | Component | Tổng SP (ước) |
|------|-----|-----------|---------------|
| **IF-E0** | Infrastructure & DevOps Foundation | infra | 21 |
| **IF-E1** | Platform Services (Discovery/Config/Gateway) | gateway | 26 |
| **IF-E2** | Auth & Multi-tenancy | auth | 29 |
| **IF-E3** | Catalog (Products + Inventory) | catalog | 26 |
| **IF-E4** | Sales (Orders + Customers) | sales | 21 |
| **IF-E5** | Data Ingestion (CSV Import) | integration | 18 |
| **IF-E6** | ML Forecast & Recommendation | ml | 29 |
| **IF-E7** | Dashboard BFF | bff | 16 |
| **IF-E8** | Notification (in-app + email) | notification | 11 |
| **IF-E9** | Frontend Web App | frontend | 34 |
| **IF-E10** | Deploy & Pilot Onboarding | infra | 21 |

> Tổng ~252 SP / 5 sprint ≈ 50 SP/sprint khi tính cả 5 luồng song song (mỗi người ~10 SP gross). Phần "đệm" nằm trong rủi ro mục 9.

---

## 4b. Lịch Sprint (bắt đầu 12/05/2026, trừ T7/CN)

12/05/2026 là **Thứ Ba**. Mỗi sprint = **5 ngày làm việc** (Thứ 3 → Thứ 2 tuần sau).

| Sprint | Bắt đầu | Kết thúc | Ngày làm việc | Demo |
|--------|---------|----------|---------------|------|
| Sprint 1 | T3 12/05 | T2 18/05 | 12,13,14,15,18/05 | T2 18/05 |
| Sprint 2 | T3 19/05 | T2 25/05 | 19,20,21,22,25/05 | T2 25/05 |
| Sprint 3 | T3 26/05 | T2 01/06 | 26,27,28,29/05; 01/06 | T2 01/06 |
| Sprint 4 | T3 02/06 | T2 08/06 | 02,03,04,05,08/06 | T2 08/06 |
| Sprint 5 | T3 09/06 | **T2 15/06** | 09,10,11,12,15/06 | ⭐ **GO-LIVE 15/06** |

> **25 ngày làm việc.** File `docs/jira-import-mvp.csv` đã gán `Start Date`/`Due Date` cho từng story khớp lịch này.

---

## 5. Sprint Plan Chi Tiết

Ký hiệu: **[SP]** = story point · **(Owner)**.

---

### 🟦 SPRINT 1 (Tuần 1) — Nền tảng & Khung dự án
**Sprint Goal:** Dựng xong hạ tầng + platform + khung auth; request có JWT đi qua gateway; CI chạy.

#### Epic IF-E0 — Infrastructure
| Story | AC tóm tắt | SP | Owner |
|-------|-----------|----|-------|
| IF-101 Dựng `docker-compose` dev (Postgres 16, Kafka, Redis, MinIO) | `docker compose up` lên đủ service, healthcheck OK | 5 | BE3 |
| IF-102 Khởi tạo schema DB (`auth_db`, `catalog_db`, `sales_db`...) + `init.sql` | Mỗi service có schema riêng | 2 | BE3 |
| IF-103 CI nền GitHub Actions (`ci-java.yml`, `ci-python.yml`) | PR mở → build + test chạy | 3 | BE3 |

#### Epic IF-E1 — Platform
| Story | AC tóm tắt | SP | Owner |
|-------|-----------|----|-------|
| IF-111 `discovery-server` Eureka | Service register được, port 8761 | 3 | BE1 |
| IF-112 `config-server` + `config-repo` | Service load config tập trung, port 8888 | 5 | BE1 |
| IF-113 `shared-core`: common-web (RFC7807), common-security (TenantContext), common-events | `mvn install`, import được vào service | 8 | BE1 |

#### Epic IF-E2 — Auth (khởi động)
| Story | AC tóm tắt | SP | Owner |
|-------|-----------|----|-------|
| IF-121 Scaffold `auth-service` + Flyway schema (tenants, users, roles, permissions, refresh_tokens) | Migration chạy trên `auth_db` | 5 | BE2 |
| IF-122 Register-tenant + Login (JWT HS256 issuance, BCrypt) | Tạo tenant+OWNER, login trả access+refresh token | 8 | BE2 |

#### Epic IF-E6 / IF-E9 — Scaffold song song
| Story | AC tóm tắt | SP | Owner |
|-------|-----------|----|-------|
| IF-161 Scaffold `ml-service` (FastAPI, app/{api,services,models,utils}, ruff/black, Dockerfile) | `/health` 200, container build | 3 | AI |
| IF-191 Scaffold frontend repo + routing + state + gọi `/auth/login` | Login UI gọi API thật | 5 | FE |

**Sprint 1 total ≈ 47 SP** · **Demo:** login → nhận JWT.

---

### 🟦 SPRINT 2 (Tuần 2) — Gateway hoàn chỉnh + Auth xong + Catalog
**Sprint Goal:** Mọi request đi qua gateway có JWT + tenant header; quản lý sản phẩm chạy.

#### Epic IF-E1 — Gateway
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-114 Gateway skeleton + routing tới auth | Route `/api/v1/auth/**` qua 8080 | 3 | BE1 |
| IF-115 JWT validation filter + propagate `X-Tenant-Id`/`X-User-Id` | Token sai/hết hạn → 401 RFC7807; header xuống downstream | 5 | BE1 |
| IF-116 CORS whitelist frontend + rate limit (Redis) | Preflight pass; vượt limit → 429 | 5 | BE1 |

#### Epic IF-E2 — Auth (hoàn thiện)
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-123 Refresh token rotation + logout (revoke) | Refresh đổi token; logout vô hiệu hóa | 3 | BE2 |
| IF-124 User management + roles (CRUD user, assign role, RBAC) | OWNER/MANAGER tạo/sửa user, gán role | 5 | BE2 |
| IF-125 Export OpenAPI `auth-service.yaml` | Spec commit `api-contracts/` | 2 | BE2 |

#### Epic IF-E3 — Catalog
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-131 Scaffold catalog + Flyway (products, variants, locations, inventory_levels, inventory_movements) | Health UP 8082, schema `catalog_db` | 3 | BE3 |
| IF-132 CRUD products + variants (DTO, MapStruct, tenant filter) | CRUD chạy, mọi query filter tenant_id | 8 | BE3 |

#### Epic IF-E9 — Frontend
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-192 Layout app + auth guard + màn quản lý user/role | Đăng nhập, điều hướng, quản lý user | 5 | FE |

#### Epic IF-E6 — ML
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-162 ML data store + consumer skeleton (Kafka consumer config) | Kết nối Kafka, sẵn sàng nhận event | 3 | AI |

**Sprint 2 total ≈ 42 SP** · **Demo:** login qua gateway 8080 → quản lý sản phẩm + user.

---

### 🟦 SPRINT 3 (Tuần 3) — Inventory + Sales + CSV import + Forecast v1
**Sprint Goal:** Có sản phẩm, tồn kho, đơn hàng từ CSV; ML sinh forecast đầu tiên.

#### Epic IF-E3 — Catalog inventory
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-133 Inventory movements (append-only) + tính tồn hiện tại | Nhập/xuất → movement, tồn đúng | 5 | BE3 |
| IF-134 Publish `catalog.product.created`, `catalog.inventory.updated` + export OpenAPI | Event lên Kafka đúng schema; spec commit | 3 | BE3 |

#### Epic IF-E4 — Sales
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-141 Scaffold sales + Flyway (orders, order_items, customers, suppliers) | Health UP 8083, schema `sales_db` | 3 | BE2 |
| IF-142 CRUD orders + customers (tenant filter, DTO) + OpenAPI | CRUD chạy, spec commit | 8 | BE2 |
| IF-143 Publish `sales.order.completed`; catalog consume → trừ tồn | Đơn hoàn tất → tồn giảm tự động | 5 | BE2 |

#### Epic IF-E5 — CSV Import
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-151 Scaffold integration + endpoint upload CSV products | Upload CSV → products vào catalog | 5 | BE1 |
| IF-152 Template + validate CSV (báo lỗi theo dòng) + import sales history | CSV chuẩn hóa, lỗi rõ ràng; tạo orders | 5 | BE1 |

#### Epic IF-E6 — Forecast
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-163 Prophet forecast theo SKU + fallback category (cold start) | Forecast 30 ngày; SKU mới dùng category | 8 | AI |
| IF-164 Publish `ml.forecast.generated` + REST `GET /forecast/{sku}` | Event + endpoint trả JSON | 3 | AI |

#### Epic IF-E9 — Frontend
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-193 Màn sản phẩm + upload CSV + màn đơn hàng | UI list/create + upload chạy | 5 | FE |

**Sprint 3 total ≈ 50 SP** · **Demo:** upload lịch sử bán → forecast hiển thị theo SKU.

---

### 🟦 SPRINT 4 (Tuần 4) — Recommendation + BFF + Notification + Dashboard
**Sprint Goal:** Khép vòng giá trị MVP: forecast → đề xuất → dashboard + cảnh báo.

#### Epic IF-E6 — Recommendation
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-165 Recommendation rule-based (clearance: tồn>90 ngày + sales↓>50%; restock: forecast↑ + tồn thấp) | Sinh đề xuất cho data pilot | 8 | AI |
| IF-166 Publish `ml.recommendation.created` | Event lên Kafka | 2 | AI |

#### Epic IF-E7 — Dashboard BFF
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-171 Scaffold bff + aggregate `GET /dashboard/overview` (inventory + forecast + recommendation) | Trả data gộp đúng contract | 8 | BE3 |
| IF-172 Route BFF qua gateway + export OpenAPI `dashboard-bff.yaml` | `/api/v1/dashboard/**` qua 8080; spec commit | 3 | BE3 |

#### Epic IF-E8 — Notification
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-181 Scaffold notification + consume ml events → in-app + email (SMTP dev) | Có đề xuất → alert in-app + email | 5 | BE1 |
| IF-182 Idempotent consumer (dedupe `event_id`) | Event trùng không gửi 2 lần | 3 | BE1 |

#### Epic IF-E4 — Sales
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-144 Customers analytics nhẹ cho dashboard | BFF lấy được số liệu khách | 3 | BE2 |

#### Epic IF-E9 — Frontend
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-194 Dashboard tổng quan (KPI, forecast chart, danh sách đề xuất, alert) | Dashboard end-to-end data thật | 8 | FE |

#### Epic IF-E0 — Dockerize
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-104 Dockerfile cho mọi service còn thiếu | `docker build` mỗi service OK | 3 | BE3 |

**Sprint 4 total ≈ 46 SP** · **Demo:** dashboard hiển thị tồn kho + forecast + đề xuất clearance/restock + alert (**vòng giá trị MVP đủ**).

---

### 🟥 SPRINT 5 (Tuần 5) — Hardening + Deploy + Pilot ⭐ GO-LIVE
**Sprint Goal:** Deploy toàn hệ thống lên production-like; onboard 2–3 shop dùng thử.

#### Epic IF-E10 — Deploy & Pilot
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-201 `docker-compose.prod.yml` (full stack, env vars, secret Jasypt) | `docker compose up` lên full stack | 5 | BE3 |
| IF-202 Deploy VM cloud + reverse proxy + HTTPS domain | Truy cập production URL qua HTTPS | 5 | BE3 |
| IF-203 CI/CD deploy pipeline (build image → registry → deploy) | Merge `main` → auto deploy | 5 | BE3 |
| IF-204 Seed tenant cho 2–3 shop pilot + import CSV thật | Mỗi shop login, thấy data của mình | 3 | BE2 |
| IF-205 Runbook + hướng dẫn sử dụng + kênh feedback | Tài liệu onboarding + form feedback | 2 | FE |

#### Epic IF-E0 — Hardening & Observability
| Story | AC | SP | Owner |
|-------|----|----|-------|
| IF-105 Observability: Prometheus + Grafana + alert service-down | Dashboard trạng thái + alert chạy | 5 | BE1 |
| IF-106 Backup DB tự động daily | Backup chạy, restore thử OK | 2 | BE1 |
| IF-211 Security review: rà tenant_id mọi query, CORS prod, secrets, rate limit | Checklist security pass | 5 | BE1 |
| IF-212 Smoke test E2E prod (đăng ký→upload→forecast→dashboard) | Checklist E2E pass 100% | 3 | BE2 |
| IF-213 Frontend: hoàn thiện responsive + xử lý lỗi/empty state | UI ổn định trên prod | 5 | FE |
| IF-214 ML: validate forecast/recommendation trên data pilot thật | Output hợp lý, không lỗi | 3 | AI |

**Sprint 5 total ≈ 48 SP** · **Mốc GO-LIVE.**

---

## 6. Bảng Velocity & Phân Bổ

| Sprint | Tổng SP | BE1 | BE2 | BE3 | FE | AI |
|--------|---------|-----|-----|-----|----|----|
| 1 | 47 | 16 | 13 | 10 | 5 | 3 |
| 2 | 42 | 13 | 10 | 11 | 5 | 3 |
| 3 | 50 | 10 | 16 | 8 | 5 | 11 |
| 4 | 46 | 8 | 3 | 14 | 8 | 10 |
| 5 | 48 | 12 | 6 | 15 | 10 | 3 |

> Tải BE3 (DevOps) nặng ở Sprint 5 — cân nhắc kéo IF-201/203 sớm hơn (làm dần từ Sprint 4) để giảm dồn.

---

## 7. Definition of Ready (Story vào Sprint)
- [ ] Có mô tả + Acceptance Criteria rõ
- [ ] Đã gắn Component + Story Point + Owner
- [ ] Phụ thuộc đã xác định (link "blocks/blocked by" trong Jira)
- [ ] Đụng API public → có ghi chú cập nhật OpenAPI

## 8. Definition of Done (Story)
- [ ] Code theo convention (layered, DTO riêng, mọi query filter `tenant_id`)
- [ ] Endpoint có `@Operation`/`@ApiResponse`; OpenAPI cập nhật cùng PR
- [ ] Flyway migration nếu đổi schema (không sửa migration đã merge)
- [ ] Event schema cập nhật `common-events` nếu đổi payload
- [ ] PR review xong, CI xanh, 1 service/PR
- [ ] AC pass; demo được

---

## 9. Rủi Ro & Giảm Thiểu

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|--------|-----------|-----------|
| Greenfield 11 service trong 5 tuần | Trễ go-live | Scaffold dùng template chung; cắt notification tối giản; CSV thay KiotViet |
| Cold start ML thiếu lịch sử bán | Forecast yếu | Fallback category; yêu cầu shop export ≥3–6 tháng |
| CSV shop không chuẩn | Import lỗi | Template + validate báo lỗi theo dòng |
| Deploy dồn Sprint 5 | Trễ go-live | Dockerfile + compose.prod làm dần từ Sprint 4 (IF-104, kéo IF-201/203 sớm) |
| Multi-tenant leak data | Sự cố pilot | IF-211 security review + common-security enforce |
| BE3 quá tải (DevOps + BFF + Integration) | Nghẽn | San IF-151/152 cho BE1; FE hỗ trợ deploy docs |

---

## 10. Ceremonies (Scrum 1 tuần)

| Ceremony | Khi nào | Thời lượng |
|----------|---------|-----------|
| Sprint Planning | Thứ 2 sáng | 1h |
| Daily Standup | Mỗi sáng | 15' |
| Backlog Refinement | Thứ 4 | 30' |
| Sprint Review (Demo) | Thứ 6 chiều | 45' |
| Retrospective | Thứ 6 sau review | 30' |

---

## 11. Import Vào Jira Bằng File CSV (1 lần, tạo hết Epic + Story)

File sẵn sàng: **`docs/jira-import-mvp.csv`** (11 Epic + 50 Story, có Sprint, Story Point, Component, Assignee, Start/Due Date).

### Các bước
1. Tạo **Project kiểu Scrum**, key `IF` (hoặc key bất kỳ — không bắt buộc trùng mã trong file).
2. Vào **Settings → System → External System Import → CSV** (hoặc Project → Import issues). Chọn `jira-import-mvp.csv`.
3. **Map field** khi import:
   - `Issue Type` → Issue Type · `Summary` → Summary · `Description` → Description
   - `Epic Name` → Epic Name (Jira tự dựng Epic) · `Epic Link` → Epic Link (Story tự gắn vào Epic theo tên)
   - `Story Points` → Story point estimate · `Component` → Components · `Sprint` → Sprint (Jira tự tạo Sprint 1–5)
   - `Assignee` → Assignee · `Priority` → Priority · `Labels` → Labels
   - `Start Date` → Start date · `Due Date` → Due date
4. **Định dạng ngày**: chọn `yyyy-MM-dd` (file dùng ISO, vd `2026-05-12`).
5. **Assignee**: file dùng mã `BE1/BE2/BE3/FE/AI` — khi import map sang **username/email thật** của từng thành viên (hoặc sửa cột này trong CSV trước khi import).
6. Sau import: chỉnh **ngày bắt đầu/kết thúc của từng Sprint** trên Backlog (Sprint 1 = 12→18/05, … Sprint 5 = 09→15/06) rồi **Start sprint**.

### Sau khi import
- Board: `Backlog → To Do → In Progress → In Review → Done`.
- Link phụ thuộc thủ công bằng "is blocked by" (vd IF-132 blocked by IF-113; IF-143 blocked by IF-142 & IF-133).
- Mỗi Story nên tách **Sub-task** kỹ thuật khi vào sprint (không bắt buộc trong file import).

> **Lưu ý:** mã `IF-1xx/2xx` nằm trong phần Summary chỉ để tra cứu — Jira sẽ tự cấp key riêng (IF-1, IF-2…). Nếu muốn giữ mã, để nguyên trong Summary như file đã làm.
