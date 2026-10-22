package com.safenavi.backend.realtime;

public record ReportAlert(Long reportId, String type, double lat, double lng) {
}
