package com.safenavi.backend.sync;

import java.time.LocalDateTime;
import java.util.List;

public record SyncResult(LocalDateTime startedAt, LocalDateTime finishedAt, List<SourceResult> sources) {

    /**
     * @param fetched  items received from the API
     * @param upserted valid records written (inserted or updated)
     * @param skipped  items without usable coordinates
     * @param deleted  previously synced rows no longer present in the source
     * @param error    null on success
     */
    public record SourceResult(String source, String facilityType, int fetched, int upserted, int skipped,
            int deleted, String error) {
    }
}
