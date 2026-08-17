package com.volunteer.platform.schedule.service;

import com.volunteer.platform.schedule.dto.AutoGenerateScheduleDTO;
import com.volunteer.platform.schedule.dto.GenerateScheduleDTO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import com.volunteer.platform.schedule.vo.ScheduleAssignmentVO;
import com.volunteer.platform.schedule.vo.ScheduleDetailVO;

import java.util.List;

public interface ScheduleService {

    ScheduleDetailVO generate(GenerateScheduleDTO dto);

    ScheduleDetailVO autoGenerate(AutoGenerateScheduleDTO dto);

    ScheduleDetailVO publish(Long planId);

    void confirmAssignment(Long assignmentId);

    ScheduleDetailVO supplementAssignment(SupplementScheduleAssignmentDTO dto);

    ScheduleDetailVO getActivityDetail(Long activityId);

    List<ScheduleAssignmentVO> listUserAssignments(Long userId);
}
