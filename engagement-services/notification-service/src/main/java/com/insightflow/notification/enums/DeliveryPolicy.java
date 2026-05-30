package com.insightflow.notification.enums;

/**
 * How a notification type reaches the user. Drives channel routing instead of
 * raw severity (Insight Flow AI strategy: few, high-value notifications).
 *
 * <ul>
 *   <li>{@link #REALTIME} — business-critical, acted on immediately. Inbox +
 *       Email + WebSocket, never suppressed by user preferences.</li>
 *   <li>{@link #DAILY_REPORT} — non-urgent operational items folded into the
 *       single 04:00 Daily Inventory Report (Inbox + one batched Email).</li>
 *   <li>{@link #DASHBOARD_NONE} — pure AI insight (forecast/trend). NOT a
 *       notification: shown on the Dashboard AI Center, no inbox record.</li>
 * </ul>
 */
public enum DeliveryPolicy {
    REALTIME,
    DAILY_REPORT,
    DASHBOARD_NONE
}
