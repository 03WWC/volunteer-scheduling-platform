package com.volunteer.platform.location.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.location.client.dto.UserRealtimeStatusDTO;
import com.volunteer.platform.location.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/report")
    public Result<UserRealtimeStatusDTO> report(@RequestBody LocationDTO dto) {
        return Result.success(locationService.report(dto));
    }

    @GetMapping("/nearby")
    public Result<List<NearbyUserDTO>> nearby(@RequestParam("activityId") Long activityId,
                                             @RequestParam("longitude") BigDecimal longitude,
                                             @RequestParam("latitude") BigDecimal latitude,
                                             @RequestParam("radiusMeter") Integer radiusMeter) {
        return Result.success(locationService.nearby(activityId, longitude, latitude, radiusMeter));
    }

    @GetMapping("/{userId}/status")
    public Result<UserRealtimeStatusDTO> getStatus(@PathVariable("userId") Long userId,
                                                   @RequestParam(value = "activityId", required = false)
                                                   Long activityId) {
        return Result.success(locationService.getStatus(activityId, userId));
    }

    @PostMapping("/checkin")
    public Result<CheckinDTO> checkin(@RequestBody CheckinDTO dto) {
        return Result.success(locationService.checkin(dto));
    }

    @PostMapping("/checkin/async")
    public Result<CheckinDTO> submitAsyncCheckin(@RequestBody CheckinDTO dto) {
        return Result.success(locationService.submitAsyncCheckin(dto));
    }

    @PostMapping("/checkin/qrcode")
    public Result<CheckinQrCodeDTO> generateCheckinQrCode(@RequestBody CheckinDTO dto) {
        return Result.success(locationService.generateCheckinQrCode(dto));
    }

    @GetMapping("/checkins")
    public Result<List<CheckinDTO>> listCheckins(@RequestParam("activityId") Long activityId,
                                                 @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(locationService.listCheckins(activityId, userId));
    }
}
