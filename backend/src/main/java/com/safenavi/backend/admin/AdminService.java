package com.safenavi.backend.admin;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.response.PageResponse;
import com.safenavi.backend.report.Report;
import com.safenavi.backend.report.ReportFlag;
import com.safenavi.backend.report.ReportFlagCount;
import com.safenavi.backend.report.ReportFlagRepository;
import com.safenavi.backend.report.ReportRepository;
import com.safenavi.backend.user.User;
import com.safenavi.backend.user.UserRepository;
import com.safenavi.backend.user.UserResponse;

@Service
public class AdminService {

    public static final String FILTER_FLAGGED = "flagged";
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;
    private final ReportFlagRepository reportFlagRepository;
    private final UserRepository userRepository;

    public AdminService(ReportRepository reportRepository, ReportFlagRepository reportFlagRepository,
            UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.reportFlagRepository = reportFlagRepository;
        this.userRepository = userRepository;
    }

    /** Active reports that have been flagged at least once, most-flagged first. */
    @Transactional(readOnly = true)
    public PageResponse<AdminReportResponse> findReports(String statusFilter, int page, int size) {
        if (!FILTER_FLAGGED.equals(statusFilter)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하는 status 필터: " + FILTER_FLAGGED);
        }
        Page<ReportFlagCount> counts = reportFlagRepository.findFlaggedActiveReports(pageRequest(page, size, Sort.unsorted()));
        List<Long> ids = counts.getContent().stream().map(ReportFlagCount::reportId).toList();
        Map<Long, Report> reports = reportRepository.findWithUserByIdIn(ids).stream()
                .collect(Collectors.toMap(Report::getId, Function.identity()));
        Map<Long, List<String>> reasons = reportFlagRepository.findByReportIdIn(ids).stream()
                .collect(Collectors.groupingBy(f -> f.getReport().getId(),
                        Collectors.mapping(ReportFlag::getReason, Collectors.toList())));
        return PageResponse.of(counts, c -> AdminReportResponse.from(reports.get(c.reportId()), c.flagCount(),
                reasons.getOrDefault(c.reportId(), List.of())));
    }

    @Transactional
    public void changeReportStatus(Long reportId, String status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        if (Report.STATUS_REMOVED.equals(status)) {
            report.remove();
        } else {
            reportFlagRepository.deleteByReportId(reportId);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findUsers(int page, int size) {
        return PageResponse.of(userRepository.findAll(pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"))),
                UserResponse::from);
    }

    @Transactional
    public UserResponse changeUserStatus(Long adminId, Long userId, String status) {
        if (adminId.equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "본인 계정의 상태는 변경할 수 없습니다.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.changeStatus(status);
        return UserResponse.from(user);
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return PageRequest.of(page, size, sort);
    }
}
