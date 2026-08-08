package com.volunteer.platform.schedule.manager;

import java.time.LocalDate;
import java.util.List;

public interface FreeVolunteerCacheManager {

    void cacheFreeVolunteerIds(LocalDate date, List<Long> userIds);

    List<Long> listFreeVolunteerIds(LocalDate date);
}
