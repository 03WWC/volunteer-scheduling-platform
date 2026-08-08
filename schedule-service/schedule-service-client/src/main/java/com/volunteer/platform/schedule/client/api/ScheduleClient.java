package com.volunteer.platform.schedule.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "schedule-service")
public interface ScheduleClient {

    @GetMapping("/schedule/{activityId}/detail")
    Result<ScheduleDTO> getActivityDetail(@PathVariable("activityId") Long activityId);

    @GetMapping("/schedule/users/{userId}/assignments")
    Result<List<ScheduleAssignmentDTO>> listUserAssignments(@PathVariable("userId") Long userId);
}
