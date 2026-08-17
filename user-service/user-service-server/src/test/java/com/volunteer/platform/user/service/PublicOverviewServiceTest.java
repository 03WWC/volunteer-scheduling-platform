package com.volunteer.platform.user.service;

import com.volunteer.platform.user.service.impl.PublicOverviewServiceImpl;
import com.volunteer.platform.user.vo.PublicOverviewVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicOverviewServiceTest {

    @Test
    void overviewUsesRealCountsAndCalculatesSatisfactionRate() {
        PublicOverviewService service = new PublicOverviewServiceImpl(new StubPublicOverviewRepository(2, 7, 8, 10));

        PublicOverviewVO overview = service.getOverview();

        assertThat(overview.getTodayActivityCount()).isEqualTo(2);
        assertThat(overview.getActiveVolunteerCount()).isEqualTo(7);
        assertThat(overview.getPositionSatisfactionRate()).isEqualTo(80.0);
    }

    @Test
    void overviewReturnsZeroRateWhenNoPositionDemandExists() {
        PublicOverviewService service = new PublicOverviewServiceImpl(new StubPublicOverviewRepository(1, 3, 5, 0));

        PublicOverviewVO overview = service.getOverview();

        assertThat(overview.getPositionSatisfactionRate()).isEqualTo(0.0);
    }

    private static class StubPublicOverviewRepository implements PublicOverviewRepository {

        private final int todayActivityCount;
        private final int activeVolunteerCount;
        private final int assignedCount;
        private final int requiredCount;

        StubPublicOverviewRepository(int todayActivityCount, int activeVolunteerCount,
                                     int assignedCount, int requiredCount) {
            this.todayActivityCount = todayActivityCount;
            this.activeVolunteerCount = activeVolunteerCount;
            this.assignedCount = assignedCount;
            this.requiredCount = requiredCount;
        }

        @Override
        public int countTodayActivities() {
            return todayActivityCount;
        }

        @Override
        public int countActiveVolunteers() {
            return activeVolunteerCount;
        }

        @Override
        public int countAssignedVolunteers() {
            return assignedCount;
        }

        @Override
        public int countRequiredPositions() {
            return requiredCount;
        }
    }
}
