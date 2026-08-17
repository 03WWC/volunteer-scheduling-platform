package com.volunteer.platform.schedule.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "schedule-service")
public interface ScheduleClient {

    @GetMapping("/schedule/{activityId}/detail")
    Result<ScheduleDTO> getActivityDetail(@PathVariable("activityId") Long activityId);

    @GetMapping("/schedule/users/{userId}/assignments")
    Result<List<ScheduleAssignmentDTO>> listUserAssignments(@PathVariable("userId") Long userId);

    @PostMapping("/schedule/assignments/supplement")
    Result<ScheduleDTO> supplementAssignment(@RequestBody SupplementScheduleAssignmentDTO dto);
}
