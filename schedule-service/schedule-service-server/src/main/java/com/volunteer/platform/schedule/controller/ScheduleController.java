package com.volunteer.platform.schedule.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import com.volunteer.platform.schedule.dto.AutoGenerateScheduleDTO;
import com.volunteer.platform.schedule.dto.GenerateScheduleDTO;
import com.volunteer.platform.schedule.service.ScheduleService;
import com.volunteer.platform.schedule.vo.ScheduleAssignmentVO;
import com.volunteer.platform.schedule.vo.ScheduleDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/generate")
    public Result<ScheduleDetailVO> generate(@RequestBody GenerateScheduleDTO dto) {
        return Result.success(scheduleService.generate(dto));
    }

    @PostMapping("/auto-generate")
    public Result<ScheduleDetailVO> autoGenerate(@RequestBody AutoGenerateScheduleDTO dto) {
        return Result.success(scheduleService.autoGenerate(dto));
    }

    @PostMapping("/{planId}/publish")
    public Result<ScheduleDetailVO> publish(@PathVariable("planId") Long planId) {
        return Result.success(scheduleService.publish(planId));
    }

    @PostMapping("/assignment/{assignmentId}/confirm")
    public Result<Void> confirmAssignment(@PathVariable("assignmentId") Long assignmentId) {
        scheduleService.confirmAssignment(assignmentId);
        return Result.success();
    }

    @PostMapping("/assignments/supplement")
    public Result<ScheduleDTO> supplementAssignment(@RequestBody SupplementScheduleAssignmentDTO dto) {
        return Result.success(toScheduleDTO(scheduleService.supplementAssignment(dto)));
    }

    @GetMapping("/{activityId}/detail")
    public Result<ScheduleDTO> getActivityDetail(@PathVariable("activityId") Long activityId) {
        return Result.success(toScheduleDTO(scheduleService.getActivityDetail(activityId)));
    }

    @GetMapping("/users/{userId}/assignments")
    public Result<List<com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO>> listUserAssignments(
        @PathVariable("userId") Long userId) {
        return Result.success(scheduleService.listUserAssignments(userId).stream()
            .map(this::toAssignmentDTO)
            .toList());
    }

    private ScheduleDTO toScheduleDTO(ScheduleDetailVO detailVO) {
        ScheduleDTO scheduleDTO = new ScheduleDTO();
        scheduleDTO.setPlanId(detailVO.getPlanId());
        scheduleDTO.setActivityId(detailVO.getActivityId());
        scheduleDTO.setPlanNo(detailVO.getPlanNo());
        scheduleDTO.setPlanName(detailVO.getPlanName());
        scheduleDTO.setPlanStatus(detailVO.getPlanStatus());
        scheduleDTO.setAssignments(detailVO.getAssignments().stream()
            .map(this::toAssignmentDTO)
            .toList());
        return scheduleDTO;
    }

    private com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO toAssignmentDTO(
        ScheduleAssignmentVO assignmentVO) {
        com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO assignmentDTO =
            new com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO();
        assignmentDTO.setId(assignmentVO.getId());
        assignmentDTO.setActivityId(assignmentVO.getActivityId());
        assignmentDTO.setAreaId(assignmentVO.getAreaId());
        assignmentDTO.setPositionId(assignmentVO.getPositionId());
        assignmentDTO.setUserId(assignmentVO.getUserId());
        assignmentDTO.setWorkDate(assignmentVO.getWorkDate());
        assignmentDTO.setStartTime(assignmentVO.getStartTime());
        assignmentDTO.setEndTime(assignmentVO.getEndTime());
        assignmentDTO.setAssignmentStatus(assignmentVO.getAssignmentStatus());
        return assignmentDTO;
    }
}
