package com.volunteer.platform.activity.service;

import com.volunteer.platform.activity.dto.CreateActivityDTO;
import com.volunteer.platform.activity.dto.SignupActivityDTO;
import com.volunteer.platform.activity.dto.UpdateActivityDTO;
import com.volunteer.platform.activity.query.ActivityQuery;
import com.volunteer.platform.activity.query.ActivitySignupQuery;
import com.volunteer.platform.activity.vo.ActivitySignupVO;
import com.volunteer.platform.activity.vo.ActivityVO;

import java.util.List;

public interface ActivityService {

    ActivityVO create(CreateActivityDTO dto);

    List<ActivityVO> page(ActivityQuery query);

    ActivityVO update(UpdateActivityDTO dto);

    void delete(Long id);

    ActivitySignupVO signup(Long activityId, SignupActivityDTO dto);

    List<ActivitySignupVO> listSignups(ActivitySignupQuery query);

    List<ActivitySignupVO> listUserSignups(Long userId);

    ActivitySignupVO approveSignup(Long signupId);

    ActivitySignupVO rejectSignup(Long signupId);

    ActivitySignupVO cancelSignup(Long signupId, Long userId);
}
