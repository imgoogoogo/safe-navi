package com.safenavi.backend.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.auth.security.AuthUser;
import com.safenavi.backend.common.response.ApiResponse;
import com.safenavi.backend.common.response.PageResponse;
import com.safenavi.backend.graphbuild.GraphBuildRequest;
import com.safenavi.backend.graphbuild.GraphBuildResult;
import com.safenavi.backend.graphbuild.GraphBuildService;
import com.safenavi.backend.sync.PublicDataSyncService;
import com.safenavi.backend.sync.SyncResult;
import com.safenavi.backend.user.UserResponse;

import jakarta.validation.Valid;

/** Every route here requires ROLE_ADMIN (SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PublicDataSyncService publicDataSyncService;
    private final GraphBuildService graphBuildService;

    public AdminController(AdminService adminService, PublicDataSyncService publicDataSyncService,
            GraphBuildService graphBuildService) {
        this.adminService = adminService;
        this.publicDataSyncService = publicDataSyncService;
        this.graphBuildService = graphBuildService;
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<AdminReportResponse>> reports(
            @RequestParam(defaultValue = AdminService.FILTER_FLAGGED) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.findReports(status, page, size));
    }

    @PatchMapping("/reports/{id}/status")
    public ApiResponse<Void> changeReportStatus(@PathVariable Long id, @Valid @RequestBody ReportStatusChangeRequest request) {
        adminService.changeReportStatus(id, request.status());
        return ApiResponse.ok();
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.findUsers(page, size));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<UserResponse> changeUserStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
            @Valid @RequestBody UserStatusChangeRequest request) {
        return ApiResponse.ok(adminService.changeUserStatus(admin.id(), id, request.status()));
    }

    /** Runs the same sync as the nightly PublicDataSyncScheduler, synchronously. */
    @PostMapping("/public-data/sync")
    public ApiResponse<SyncResult> syncPublicData() {
        return ApiResponse.ok(publicDataSyncService.syncAll());
    }

    @PostMapping("/graph-build/run")
    public ApiResponse<GraphBuildResult> runGraphBuild(@Valid @RequestBody GraphBuildRequest request) {
        return ApiResponse.ok(graphBuildService.run(request));
    }
}
