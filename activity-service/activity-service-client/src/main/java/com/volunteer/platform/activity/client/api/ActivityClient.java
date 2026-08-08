package com.volunteer.platform.activity.client.api;

import com.volunteer.platform.activity.client.dto.AreaDTO;
import com.volunteer.platform.activity.client.dto.ActivitySignupDTO;
import com.volunteer.platform.activity.client.dto.PositionDTO;
import com.volunteer.platform.common.api.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "activity-service")
public interface ActivityClient {

    @GetMapping("/position/list")
    Result<List<PositionDTO>> getPositionList(@RequestParam("activityId") Long activityId);

    @GetMapping("/position/{id}")
    Result<PositionDTO> getPosition(@PathVariable("id") Long id);

    @GetMapping("/area/list")
    Result<List<AreaDTO>> getAreaList(@RequestParam("activityId") Long activityId);

    @GetMapping("/activity/signups")
    Result<List<ActivitySignupDTO>> listSignups(@RequestParam("activityId") Long activityId,
                                                @RequestParam("signupStatus") String signupStatus);
}
