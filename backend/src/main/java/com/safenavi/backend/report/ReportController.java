package com.safenavi.backend.report;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.safenavi.backend.auth.security.AuthUser;
import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.response.ApiResponse;
import com.safenavi.backend.common.response.PageResponse;
import com.safenavi.backend.report.dto.MyReportResponse;
import com.safenavi.backend.report.dto.ReportCreateRequest;
import com.safenavi.backend.report.dto.ReportDetailResponse;
import com.safenavi.backend.report.dto.ReportFlagRequest;
import com.safenavi.backend.report.dto.ReportSummaryResponse;
import com.safenavi.backend.report.dto.ReportUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportDetailResponse> create(@AuthenticationPrincipal AuthUser authUser,
            @Valid @ModelAttribute ReportCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ApiResponse.ok(reportService.create(authUser.id(), request, image));
    }

    /** status filters by review status (접수됨|검토중|반영됨); removed reports are never listed. */
    @GetMapping
    public ApiResponse<List<ReportSummaryResponse>> list(@RequestParam double swLat, @RequestParam double swLng,
            @RequestParam double neLat, @RequestParam double neLng,
            @RequestParam(required = false) String type, @RequestParam(required = false) String status) {
        return ApiResponse.ok(reportService.findInBox(new BoundingBox(swLat, swLng, neLat, neLng), type, status));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<MyReportResponse>> mine(@AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.findMine(authUser.id(), page, size));
    }

    /** Public; authUser is null for anonymous callers, which makes isOwner false. */
    @GetMapping("/{id}")
    public ApiResponse<ReportDetailResponse> detail(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        return ApiResponse.ok(reportService.getDetail(id, authUser == null ? null : authUser.id()));
    }

    /** Multipart variant: needed to replace the photo. */
    @PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReportDetailResponse> updateMultipart(@AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id, @Valid @ModelAttribute ReportUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ApiResponse.ok(reportService.update(authUser.id(), id, request, image));
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ReportDetailResponse> updateJson(@AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id, @Valid @RequestBody ReportUpdateRequest request) {
        return ApiResponse.ok(reportService.update(authUser.id(), id, request, null));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        reportService.delete(authUser.id(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/flags")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> flag(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
            @Valid @RequestBody ReportFlagRequest request) {
        reportService.flag(authUser.id(), id, request.reason());
        return ApiResponse.ok();
    }
}
