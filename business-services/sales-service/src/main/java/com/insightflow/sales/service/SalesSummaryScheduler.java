package com.insightflow.sales.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesSummaryScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Refreshes daily_sales_summary materialized view daily at 02:00 AM (Asia/Ho_Chi_Minh).
     *
     * CONCURRENTLY: does not lock the view during refresh — readers continue unblocked.
     * Requires the unique index on (tenant_id, location_id, variant_id, sale_date) from V6 migration.
     *
     * NOTE: REFRESH MATERIALIZED VIEW CONCURRENTLY cannot run inside a transaction,
     * so this method is intentionally NOT @Transactional.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void refreshDailySalesSummary() {
        refresh();
    }

    /**
     * Public so controllers can trigger a manual refresh during testing/backfill.
     */
    public void refresh() {
        log.info("Starting daily_sales_summary refresh ...");
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.execute(
                    "REFRESH MATERIALIZED VIEW CONCURRENTLY sales_db.daily_sales_summary");
            log.info("daily_sales_summary refreshed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("daily_sales_summary refresh failed", e);
            throw e;
        }
    }
}
