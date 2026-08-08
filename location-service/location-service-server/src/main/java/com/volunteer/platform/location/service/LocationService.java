package com.volunteer.platform.location.service;

import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.location.client.dto.UserRealtimeStatusDTO;

import java.math.BigDecimal;
import java.util.List;

public interface LocationService {

    UserRealtimeStatusDTO report(LocationDTO dto);

    List<NearbyUserDTO> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude, Integer radiusMeter);

    UserRealtimeStatusDTO getStatus(Long activityId, Long userId);

    CheckinQrCodeDTO generateCheckinQrCode(CheckinDTO dto);

    CheckinDTO checkin(CheckinDTO dto);

    CheckinDTO submitAsyncCheckin(CheckinDTO dto);

    List<CheckinDTO> listCheckins(Long activityId, Long userId);
}
