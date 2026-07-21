package com.insightflow.notification.service.email;

import com.insightflow.common.events.billing.PaymentReceiptEvent;
import com.insightflow.notification.provider.email.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Dựng và gửi email <b>biên nhận thanh toán</b> chuyên nghiệp (HTML) cho khách sau khi
 * thanh toán thành công. Nhận dữ liệu từ {@link PaymentReceiptEvent}.
 *
 * <p>HTML được viết dạng table + inline-CSS để hiển thị ổn định trên mọi mail client
 * (Gmail, Outlook, Apple Mail...). Toàn bộ thông tin bên bán có thể cấu hình qua env
 * {@code app.invoice.*}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEmailService {

    private final EmailProvider emailProvider;

    // --- Thông tin bên bán (branding) — cấu hình qua env, có default an toàn cho MVP ---
    @Value("${app.invoice.company-name:Insight Flow AI}")
    private String companyName;
    @Value("${app.invoice.company-tagline:Dự báo nhu cầu & tối ưu tồn kho cho nhà bán lẻ}")
    private String companyTagline;
    @Value("${app.invoice.company-address:}")
    private String companyAddress;
    @Value("${app.invoice.company-tax-code:}")
    private String companyTaxCode;
    @Value("${app.invoice.support-email:support@insightflow.ai}")
    private String supportEmail;
    @Value("${app.invoice.support-hotline:}")
    private String supportHotline;
    @Value("${app.invoice.website:https://insightflow.ai}")
    private String website;
    /** Màu nhấn thương hiệu. */
    @Value("${app.invoice.brand-color:#4f46e5}")
    private String brandColor;

    private static final DateTimeFormatter D_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter D_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void sendReceipt(PaymentReceiptEvent e) {
        String subject = "Biên nhận thanh toán " + safe(e.invoiceNumber()) + " · " + companyName;
        String html = buildHtml(e);
        String textFallback = buildText(e);
        emailProvider.send(e.recipientEmail(), subject, textFallback, html);
        log.info("[INVOICE] Đã gửi biên nhận {} tới {}", e.invoiceNumber(), e.recipientEmail());
    }

    // =========================================================================
    // HTML
    // =========================================================================
    private String buildHtml(PaymentReceiptEvent e) {
        String greetingName = (e.recipientName() != null && !e.recipientName().isBlank())
                ? e.recipientName() : "Quý khách";
        String amount = formatVnd(e.amount());
        String cycle = cycleLabel(e.billingCycle());

        String sellerExtra = "";
        if (companyTaxCode != null && !companyTaxCode.isBlank()) {
            sellerExtra += row("Mã số thuế", esc(companyTaxCode));
        }
        if (companyAddress != null && !companyAddress.isBlank()) {
            sellerExtra += row("Địa chỉ", esc(companyAddress));
        }

        String periodRow = "";
        if (e.startDate() != null && e.endDate() != null) {
            periodRow = row("Hiệu lực", fmtDate(e.startDate()) + " → " + fmtDate(e.endDate()));
        }

        String html = TEMPLATE
                .replace("{{brand}}", brandColor)
                .replace("{{companyName}}", esc(companyName))
                .replace("{{companyTagline}}", esc(companyTagline))
                .replace("{{invoiceNumber}}", esc(safe(e.invoiceNumber())))
                .replace("{{issuedAt}}", fmtDateTime(e.issuedAt()))
                .replace("{{greetingName}}", esc(greetingName))
                .replace("{{packageName}}", esc(safe(e.packageName())))
                .replace("{{cycle}}", esc(cycle))
                .replace("{{amount}}", esc(amount))
                .replace("{{periodRow}}", periodRow)
                .replace("{{transactionCode}}", esc(safe(e.transactionCode())))
                .replace("{{paymentMethod}}", esc(safe(e.paymentMethod())))
                .replace("{{transactionDate}}", fmtDateTime(e.transactionDate()))
                .replace("{{bankLine}}", esc(bankLine(e)))
                .replace("{{sellerExtra}}", sellerExtra)
                .replace("{{supportEmail}}", esc(supportEmail))
                .replace("{{supportHotlineBlock}}", supportHotlineBlock())
                .replace("{{website}}", esc(website))
                .replace("{{websiteDisplay}}", esc(displayHost(website)))
                .replace("{{year}}", String.valueOf(LocalDate.now().getYear()));
        return html;
    }

    private String supportHotlineBlock() {
        if (supportHotline == null || supportHotline.isBlank()) {
            return "";
        }
        return " &nbsp;•&nbsp; Hotline: <strong>" + esc(supportHotline) + "</strong>";
    }

    private String bankLine(PaymentReceiptEvent e) {
        StringBuilder sb = new StringBuilder();
        if (e.bankAccountName() != null) sb.append(e.bankAccountName());
        if (e.bankAccountNo() != null) sb.append(" · ").append(e.bankAccountNo());
        if (e.bankName() != null) sb.append(" (").append(e.bankName()).append(")");
        return sb.toString().trim().isEmpty() ? "—" : sb.toString();
    }

    private String row(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:8px 0;color:#6b7280;font-size:13px;\">" + label + "</td>"
                + "<td style=\"padding:8px 0;color:#111827;font-size:13px;text-align:right;font-weight:600;\">" + value + "</td>"
                + "</tr>";
    }

    // =========================================================================
    // Plain-text fallback (mail client không render HTML)
    // =========================================================================
    private String buildText(PaymentReceiptEvent e) {
        return "BIÊN NHẬN THANH TOÁN - " + companyName + "\n"
                + "Số biên nhận: " + safe(e.invoiceNumber()) + "\n"
                + "Ngày: " + fmtDateTime(e.issuedAt()) + "\n\n"
                + "Gói dịch vụ: " + safe(e.packageName()) + " (" + cycleLabel(e.billingCycle()) + ")\n"
                + "Số tiền: " + formatVnd(e.amount()) + "\n"
                + "Mã giao dịch: " + safe(e.transactionCode()) + "\n"
                + "Hình thức: " + safe(e.paymentMethod()) + "\n\n"
                + "Cảm ơn Quý khách đã tin tưởng " + companyName + ".\n"
                + "Hỗ trợ: " + supportEmail
                + (supportHotline != null && !supportHotline.isBlank() ? " · " + supportHotline : "") + "\n";
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private String cycleLabel(String cycle) {
        if (cycle == null) return "";
        return switch (cycle.trim().toUpperCase()) {
            case "MONTHLY" -> "Theo tháng";
            case "YEARLY" -> "Theo năm";
            case "TRIAL" -> "Dùng thử";
            default -> cycle;
        };
    }

    private String formatVnd(Long amount) {
        if (amount == null) return "—";
        String grouped = String.format(Locale.US, "%,d", amount).replace(',', '.');
        return grouped + " ₫";
    }

    private String fmtDate(String iso) {
        if (iso == null || iso.isBlank()) return "—";
        try {
            return LocalDate.parse(iso, D_ISO).format(D_VN);
        } catch (Exception ex) {
            return iso;
        }
    }

    private String fmtDateTime(String iso) {
        if (iso == null || iso.isBlank()) return "—";
        try {
            return LocalDateTime.parse(iso).format(DT_VN);
        } catch (Exception ex1) {
            try {
                return LocalDate.parse(iso, D_ISO).format(D_VN);
            } catch (Exception ex2) {
                return iso;
            }
        }
    }

    private String displayHost(String url) {
        if (url == null) return "";
        return url.replaceFirst("^https?://", "").replaceFirst("/$", "");
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }

    /** Escape ký tự HTML để tránh vỡ layout / injection từ dữ liệu động. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // =========================================================================
    // Template HTML (email-safe: table + inline CSS)
    // =========================================================================
    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:24px 12px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <tr><td style="background:{{brand}};padding:28px 32px;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr>
                        <td style="color:#ffffff;font-size:20px;font-weight:700;letter-spacing:-0.02em;">{{companyName}}</td>
                        <td style="text-align:right;color:rgba(255,255,255,0.85);font-size:12px;">BIÊN NHẬN THANH TOÁN</td>
                      </tr></table>
                      <div style="color:rgba(255,255,255,0.8);font-size:12px;margin-top:4px;">{{companyTagline}}</div>
                    </td></tr>

                    <!-- Intro + số biên nhận -->
                    <tr><td style="padding:28px 32px 8px 32px;">
                      <div style="font-size:15px;color:#111827;">Xin chào <strong>{{greetingName}}</strong>,</div>
                      <div style="font-size:14px;color:#4b5563;margin-top:8px;line-height:1.6;">
                        Cảm ơn bạn đã thanh toán. Đây là biên nhận xác nhận giao dịch đã thành công.
                      </div>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:18px;">
                        <tr>
                          <td style="font-size:13px;color:#6b7280;">Số biên nhận<br><strong style="color:#111827;font-size:14px;">{{invoiceNumber}}</strong></td>
                          <td style="font-size:13px;color:#6b7280;text-align:right;">Ngày phát hành<br><strong style="color:#111827;font-size:14px;">{{issuedAt}}</strong></td>
                        </tr>
                      </table>
                    </td></tr>

                    <!-- Chi tiết gói -->
                    <tr><td style="padding:12px 32px;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e5e7eb;border-radius:10px;">
                        <tr><td style="padding:16px 18px;border-bottom:1px solid #f3f4f6;">
                          <table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr>
                            <td style="font-size:14px;color:#111827;font-weight:600;">{{packageName}}</td>
                            <td style="font-size:14px;color:#111827;font-weight:700;text-align:right;">{{amount}}</td>
                          </tr></table>
                          <div style="font-size:12px;color:#6b7280;margin-top:4px;">Chu kỳ: {{cycle}}</div>
                        </td></tr>
                        <tr><td style="padding:14px 18px;">
                          <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                            {{periodRow}}
                            <tr><td style="padding-top:6px;font-size:15px;color:#111827;font-weight:700;">Tổng cộng</td>
                                <td style="padding-top:6px;font-size:18px;color:{{brand}};font-weight:800;text-align:right;">{{amount}}</td></tr>
                          </table>
                        </td></tr>
                      </table>
                    </td></tr>

                    <!-- Thông tin thanh toán -->
                    <tr><td style="padding:8px 32px 4px 32px;">
                      <div style="font-size:12px;color:#9ca3af;text-transform:uppercase;letter-spacing:0.05em;margin-bottom:6px;">Thông tin thanh toán</div>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                        {{sellerExtra}}
                        <tr><td style="padding:8px 0;color:#6b7280;font-size:13px;">Mã giao dịch</td>
                            <td style="padding:8px 0;color:#111827;font-size:13px;text-align:right;font-weight:600;">{{transactionCode}}</td></tr>
                        <tr><td style="padding:8px 0;color:#6b7280;font-size:13px;">Hình thức</td>
                            <td style="padding:8px 0;color:#111827;font-size:13px;text-align:right;font-weight:600;">{{paymentMethod}}</td></tr>
                        <tr><td style="padding:8px 0;color:#6b7280;font-size:13px;">Thời gian GD</td>
                            <td style="padding:8px 0;color:#111827;font-size:13px;text-align:right;font-weight:600;">{{transactionDate}}</td></tr>
                        <tr><td style="padding:8px 0;color:#6b7280;font-size:13px;">Tài khoản nhận</td>
                            <td style="padding:8px 0;color:#111827;font-size:13px;text-align:right;font-weight:600;">{{bankLine}}</td></tr>
                      </table>
                    </td></tr>

                    <!-- Footer -->
                    <tr><td style="padding:20px 32px 28px 32px;border-top:1px solid #f3f4f6;">
                      <div style="font-size:13px;color:#4b5563;line-height:1.6;">
                        Cần hỗ trợ? Liên hệ <a href="mailto:{{supportEmail}}" style="color:{{brand}};text-decoration:none;">{{supportEmail}}</a>{{supportHotlineBlock}}.
                      </div>
                      <div style="font-size:12px;color:#9ca3af;margin-top:12px;">
                        <a href="{{website}}" style="color:#9ca3af;text-decoration:none;">{{websiteDisplay}}</a> · © {{year}} {{companyName}}. Biên nhận này được tạo tự động.
                      </div>
                    </td></tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
}
