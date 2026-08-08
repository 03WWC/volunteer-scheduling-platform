package com.volunteer.platform.activity.service;

import com.volunteer.platform.activity.dto.CreateAreaDTO;
import com.volunteer.platform.activity.dto.UpdateAreaDTO;
import com.volunteer.platform.activity.vo.AreaVO;

import java.util.List;

public interface AreaService {

    AreaVO create(CreateAreaDTO dto);

    AreaVO update(UpdateAreaDTO dto);

    void delete(Long id);

    List<AreaVO> listByActivityId(Long activityId);
}
