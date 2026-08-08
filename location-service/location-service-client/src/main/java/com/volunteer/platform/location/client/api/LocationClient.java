package com.volunteer.platform.location.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.location.client.dto.UserRealtimeStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "location-service")
public interface LocationClient {

    @PostMapping("/location/report")
    Result<UserRealtimeStatusDTO> report(@RequestBody LocationDTO locationDTO);

    @GetMapping("/location/nearby")
    Result<List<NearbyUserDTO>> nearby(@RequestParam("activityId") Long activityId,
                                       @RequestParam("longitude") BigDecimal longitude,
                                       @RequestParam("latitude") BigDecimal latitude,
                                       @RequestParam("radiusMeter") Integer radiusMeter);

    @GetMapping("/location/{userId}/status")
    Result<UserRealtimeStatusDTO> getStatus(@PathVariable("userId") Long userId,
                                            @RequestParam(value = "activityId", required = false) Long activityId);

    @PostMapping("/location/checkin")
    Result<CheckinDTO> checkin(@RequestBody CheckinDTO checkinDTO);

    @PostMapping("/location/checkin/async")
    Result<CheckinDTO> submitAsyncCheckin(@RequestBody CheckinDTO checkinDTO);

    @PostMapping("/location/checkin/qrcode")
    Result<CheckinQrCodeDTO> generateCheckinQrCode(@RequestBody CheckinDTO checkinDTO);

    @GetMapping("/location/checkins")
    Result<List<CheckinDTO>> listCheckins(@RequestParam("activityId") Long activityId,
                                          @RequestParam("userId") Long userId);
}
