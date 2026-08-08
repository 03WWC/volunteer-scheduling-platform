package com.volunteer.platform.activity.service;

import com.volunteer.platform.activity.client.dto.PositionDTO;
import com.volunteer.platform.activity.dto.CreatePositionDTO;
import com.volunteer.platform.activity.dto.UpdatePositionDTO;
import com.volunteer.platform.activity.vo.PositionVO;

import java.util.List;

public interface PositionService {

    PositionVO create(CreatePositionDTO dto);

    PositionVO update(UpdatePositionDTO dto);

    void delete(Long id);

    PositionVO getById(Long id);

    List<PositionVO> listByActivityId(Long activityId);

    PositionDTO getClientPosition(Long id);

    List<PositionDTO> listClientPositions(Long activityId);
}
