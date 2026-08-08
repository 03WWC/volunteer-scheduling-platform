package com.volunteer.platform.activity.controller;

import com.volunteer.platform.activity.dto.CreateActivityDTO;
import com.volunteer.platform.activity.dto.SignupActivityDTO;
import com.volunteer.platform.activity.dto.UpdateActivityDTO;
import com.volunteer.platform.activity.query.ActivityQuery;
import com.volunteer.platform.activity.query.ActivitySignupQuery;
import com.volunteer.platform.activity.service.ActivityService;
import com.volunteer.platform.activity.vo.ActivitySignupVO;
import com.volunteer.platform.activity.vo.ActivityVO;
import com.volunteer.platform.common.api.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/create")
    public Result<ActivityVO> create(@RequestBody CreateActivityDTO dto) {
        return Result.success(activityService.create(dto));
    }

    @GetMapping("/page")
    public Result<List<ActivityVO>> page(ActivityQuery query) {
        return Result.success(activityService.page(query));
    }

    @PutMapping("/update")
    public Result<ActivityVO> update(@RequestBody UpdateActivityDTO dto) {
        return Result.success(activityService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        activityService.delete(id);
        return Result.success();
    }

    @PostMapping("/{activityId}/signup")
    public Result<ActivitySignupVO> signup(@PathVariable("activityId") Long activityId,
                                           @RequestBody SignupActivityDTO dto) {
        return Result.success(activityService.signup(activityId, dto));
    }

    @GetMapping("/signups")
    public Result<List<ActivitySignupVO>> listSignups(ActivitySignupQuery query) {
        return Result.success(activityService.listSignups(query));
    }

    @GetMapping("/users/{userId}/signups")
    public Result<List<ActivitySignupVO>> listUserSignups(@PathVariable("userId") Long userId) {
        return Result.success(activityService.listUserSignups(userId));
    }

    @PostMapping("/signups/{signupId}/approve")
    public Result<ActivitySignupVO> approveSignup(@PathVariable("signupId") Long signupId) {
        return Result.success(activityService.approveSignup(signupId));
    }

    @PostMapping("/signups/{signupId}/reject")
    public Result<ActivitySignupVO> rejectSignup(@PathVariable("signupId") Long signupId) {
        return Result.success(activityService.rejectSignup(signupId));
    }

    @PostMapping("/signups/{signupId}/cancel")
    public Result<ActivitySignupVO> cancelSignup(@PathVariable("signupId") Long signupId, @RequestBody SignupActivityDTO dto) {
        Long userId = dto == null ? null : dto.getUserId();
        return Result.success(activityService.cancelSignup(signupId, userId));
    }
}
