package com.volunteer.platform.activity.service.impl;

import com.volunteer.platform.activity.dao.AreaDAO;
import com.volunteer.platform.activity.dto.CreateAreaDTO;
import com.volunteer.platform.activity.dto.UpdateAreaDTO;
import com.volunteer.platform.activity.entity.AreaDO;
import com.volunteer.platform.activity.service.AreaService;
import com.volunteer.platform.activity.vo.AreaVO;
import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaServiceImpl implements AreaService {

    private static final Integer NOT_FOUND_CODE = 404;

    private final AreaDAO areaDAO;

    public AreaServiceImpl(AreaDAO areaDAO) {
        this.areaDAO = areaDAO;
    }

    @Override
    public AreaVO create(CreateAreaDTO dto) {
        AreaDO areaDO = toAreaDO(dto);
        areaDAO.insert(areaDO);
        return toAreaVO(areaDO);
    }

    @Override
    public AreaVO update(UpdateAreaDTO dto) {
        if (areaDAO.selectById(dto.getId()) == null) {
            throw new BusinessException(NOT_FOUND_CODE, "area not found");
        }
        AreaDO areaDO = toAreaDO(dto);
        areaDO.setId(dto.getId());
        areaDAO.updateById(areaDO);
        return toAreaVO(areaDAO.selectById(dto.getId()));
    }

    @Override
    public void delete(Long id) {
        if (areaDAO.deleteById(id) == 0) {
            throw new BusinessException(NOT_FOUND_CODE, "area not found");
        }
    }

    @Override
    public List<AreaVO> listByActivityId(Long activityId) {
        return areaDAO.selectByActivityId(activityId).stream()
            .map(this::toAreaVO)
            .toList();
    }

    private AreaDO toAreaDO(CreateAreaDTO dto) {
        AreaDO areaDO = new AreaDO();
        areaDO.setActivityId(dto.getActivityId());
        areaDO.setName(dto.getName());
        areaDO.setGps(dto.getGps());
        areaDO.setOwnerName(dto.getOwnerName());
        return areaDO;
    }

    private AreaVO toAreaVO(AreaDO areaDO) {
        AreaVO areaVO = new AreaVO();
        areaVO.setId(areaDO.getId());
        areaVO.setActivityId(areaDO.getActivityId());
        areaVO.setName(areaDO.getName());
        areaVO.setGps(areaDO.getGps());
        areaVO.setOwnerName(areaDO.getOwnerName());
        return areaVO;
    }
}
