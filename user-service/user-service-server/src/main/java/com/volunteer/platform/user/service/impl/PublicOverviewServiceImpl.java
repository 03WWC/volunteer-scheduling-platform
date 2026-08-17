package com.volunteer.platform.user.service.impl;

import com.volunteer.platform.user.service.PublicOverviewRepository;
import com.volunteer.platform.user.service.PublicOverviewService;
import com.volunteer.platform.user.vo.PublicOverviewVO;
import org.springframework.stereotype.Service;

@Service
public class PublicOverviewServiceImpl implements PublicOverviewService {

    private final PublicOverviewRepository publicOverviewRepository;

    public PublicOverviewServiceImpl(PublicOverviewRepository publicOverviewRepository) {
        this.publicOverviewRepository = publicOverviewRepository;
    }

    @Override
    public PublicOverviewVO getOverview() {
        int requiredCount = publicOverviewRepository.countRequiredPositions();
        int assignedCount = publicOverviewRepository.countAssignedVolunteers();
        PublicOverviewVO overviewVO = new PublicOverviewVO();
        overviewVO.setTodayActivityCount(publicOverviewRepository.countTodayActivities());
        overviewVO.setActiveVolunteerCount(publicOverviewRepository.countActiveVolunteers());
        overviewVO.setPositionSatisfactionRate(calculateSatisfactionRate(assignedCount, requiredCount));
        return overviewVO;
    }

    private double calculateSatisfactionRate(int assignedCount, int requiredCount) {
        if (requiredCount <= 0) {
            return 0.0;
        }
        double rate = Math.min(assignedCount * 100.0 / requiredCount, 100.0);
        return Math.round(rate * 10.0) / 10.0;
    }
}
