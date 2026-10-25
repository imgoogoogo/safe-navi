package com.safenavi.backend.report;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.geo.BoundingBox;
import com.safenavi.backend.common.geo.Coordinate;
import com.safenavi.backend.common.response.PageResponse;
import com.safenavi.backend.geo.GeoService;
import com.safenavi.backend.realtime.AlertPublisher;
import com.safenavi.backend.realtime.ReportAlert;
import com.safenavi.backend.region.RegionCodeResolver;
import com.safenavi.backend.report.dto.MyReportResponse;
import com.safenavi.backend.report.dto.ReportCreateRequest;
import com.safenavi.backend.report.dto.ReportDetailResponse;
import com.safenavi.backend.report.dto.ReportSummaryResponse;
import com.safenavi.backend.report.dto.ReportUpdateRequest;
import com.safenavi.backend.storage.ImageStorageService;
import com.safenavi.backend.storage.StoredImage;
import com.safenavi.backend.user.User;
import com.safenavi.backend.user.UserRepository;

/**
 * Methods that call S3 or Nominatim keep those calls outside the DB transaction and only open a
 * short transaction for the write, cleaning up uploaded images if that write fails.
 */
@Service
public class ReportService {

    private static final int LIST_LIMIT = 500;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;
    private final ReportFlagRepository reportFlagRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final GeoService geoService;
    private final RegionCodeResolver regionCodeResolver;
    private final AlertPublisher alertPublisher;
    private final TransactionTemplate transactionTemplate;

    public ReportService(ReportRepository reportRepository, ReportFlagRepository reportFlagRepository,
            UserRepository userRepository, ImageStorageService imageStorageService, GeoService geoService,
            RegionCodeResolver regionCodeResolver, AlertPublisher alertPublisher,
            PlatformTransactionManager transactionManager) {
        this.reportRepository = reportRepository;
        this.reportFlagRepository = reportFlagRepository;
        this.userRepository = userRepository;
        this.imageStorageService = imageStorageService;
        this.geoService = geoService;
        this.regionCodeResolver = regionCodeResolver;
        this.alertPublisher = alertPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ReportDetailResponse create(Long userId, ReportCreateRequest request, MultipartFile image) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!User.STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }
        String type = ReportCodes.requireType(request.type());
        Coordinate location = new Coordinate(request.lat(), request.lng());

        StoredImage stored = hasFile(image) ? imageStorageService.upload(image) : null;
        String address = geoService.findAddress(location).orElse(null);

        Report saved;
        try {
            saved = reportRepository.save(new Report(user, type, location.lat(), location.lng(), address,
                    stored == null ? null : stored.imageUrl(), stored == null ? null : stored.thumbnailUrl(),
                    blankToNull(request.memo())));
        } catch (RuntimeException e) {
            imageStorageService.delete(stored);
            throw e;
        }

        alertPublisher.publish(regionCodeResolver.resolve(saved.getLat(), saved.getLng()),
                new ReportAlert(saved.getId(), saved.getType(), saved.getLat(), saved.getLng()));
        return ReportDetailResponse.from(saved, userId);
    }

    /** reviewStatus filters by 접수됨/검토중/반영됨; admin-removed reports are never listed. */
    @Transactional(readOnly = true)
    public List<ReportSummaryResponse> findInBox(BoundingBox box, String type, String reviewStatus) {
        box.requireMaxSpan(BoundingBox.MAX_SPAN_DEGREES);
        String typeFilter = isBlank(type) ? null : ReportCodes.requireType(type);
        String statusFilter = isBlank(reviewStatus) ? null : ReportCodes.requireReviewStatus(reviewStatus.trim());
        return reportRepository.findActiveInBox(box.swLat(), box.swLng(), box.neLat(), box.neLng(),
                        typeFilter, statusFilter, PageRequest.of(0, LIST_LIMIT))
                .stream().map(ReportSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getDetail(Long reportId, Long requesterId) {
        Report report = reportRepository.findWithUserById(reportId)
                .filter(Report::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        return ReportDetailResponse.from(report, requesterId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MyReportResponse> findMine(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.of(reportRepository.findByUserId(userId, pageable), MyReportResponse::from);
    }

    public ReportDetailResponse update(Long userId, Long reportId, ReportUpdateRequest request, MultipartFile image) {
        Report current = loadOwnedActive(userId, reportId);

        String type = request.type() == null ? null : ReportCodes.requireType(request.type());
        if ((request.lat() == null) != (request.lng() == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "lat와 lng는 함께 보내야 합니다.");
        }
        Coordinate newLocation = request.lat() == null ? null : new Coordinate(request.lat(), request.lng());
        boolean moved = newLocation != null
                && (!Objects.equals(newLocation.lat(), current.getLat()) || !Objects.equals(newLocation.lng(), current.getLng()));
        String newAddress = moved ? geoService.findAddress(newLocation).orElse(null) : null;

        StoredImage oldImage = current.getImageUrl() == null ? null
                : new StoredImage(current.getImageUrl(), current.getThumbnailUrl());
        StoredImage newImage = hasFile(image) ? imageStorageService.upload(image) : null;

        Report updated;
        try {
            updated = transactionTemplate.execute(status -> {
                Report report = loadOwnedActive(userId, reportId);
                if (type != null) {
                    report.changeType(type);
                }
                if (request.memo() != null) {
                    report.changeMemo(blankToNull(request.memo()));
                }
                if (moved) {
                    report.relocate(newLocation.lat(), newLocation.lng(), newAddress);
                }
                if (newImage != null) {
                    report.replaceImage(newImage.imageUrl(), newImage.thumbnailUrl());
                }
                reportRepository.flush();
                report.getUser().getName(); // initialize for the response outside the transaction
                return report;
            });
        } catch (RuntimeException e) {
            imageStorageService.delete(newImage);
            throw e;
        }
        if (newImage != null) {
            imageStorageService.delete(oldImage);
        }
        return ReportDetailResponse.from(updated, userId);
    }

    /** Hard delete by the author, including flags on it and the stored photos. */
    public void delete(Long userId, Long reportId) {
        StoredImage image = transactionTemplate.execute(status -> {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
            if (!report.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            reportFlagRepository.deleteByReportId(reportId);
            reportRepository.delete(report);
            return report.getImageUrl() == null ? null : new StoredImage(report.getImageUrl(), report.getThumbnailUrl());
        });
        imageStorageService.delete(image);
    }

    @Transactional
    public void flag(Long userId, Long reportId, String reason) {
        Report report = reportRepository.findById(reportId)
                .filter(Report::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        if (report.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_FLAG_OWN_REPORT);
        }
        if (reportFlagRepository.existsByReportIdAndReporterId(reportId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_FLAGGED);
        }
        reportFlagRepository.save(new ReportFlag(report, userRepository.getReferenceById(userId), reason.trim()));
    }

    private Report loadOwnedActive(Long userId, Long reportId) {
        Report report = reportRepository.findWithUserById(reportId)
                .filter(Report::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        if (!report.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return report;
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.strip();
    }
}
