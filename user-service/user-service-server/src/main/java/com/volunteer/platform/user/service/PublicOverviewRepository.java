package com.volunteer.platform.user.service;

public interface PublicOverviewRepository {

    int countTodayActivities();

    int countActiveVolunteers();

    int countAssignedVolunteers();

    int countRequiredPositions();
}
