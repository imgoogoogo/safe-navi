package com.safenavi.backend.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.safenavi.backend.report.dto.ReportSummaryResponse;

class ReportSummaryResponseTest {

    @Test
    void shortMemoIsKeptWhole() {
        assertThat(ReportSummaryResponse.summarize("  가로등이 꺼져 있어요  ")).isEqualTo("가로등이 꺼져 있어요");
    }

    @Test
    void longMemoIsCutAtFortyCharacters() {
        String memo = "가".repeat(60);

        assertThat(ReportSummaryResponse.summarize(memo)).isEqualTo("가".repeat(40) + "…");
    }

    @Test
    void blankMemoHasNoSummary() {
        assertThat(ReportSummaryResponse.summarize("   ")).isNull();
    }
}
