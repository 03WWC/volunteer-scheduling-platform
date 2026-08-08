package com.volunteer.platform.activity.service.impl;

import com.volunteer.platform.activity.client.dto.PositionDTO;
import com.volunteer.platform.activity.dao.PositionDAO;
import com.volunteer.platform.activity.dto.CreatePositionDTO;
import com.volunteer.platform.activity.dto.UpdatePositionDTO;
import com.volunteer.platform.activity.entity.PositionDO;
import com.volunteer.platform.activity.service.PositionService;
import com.volunteer.platform.activity.vo.PositionVO;
import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PositionServiceImpl implements PositionService {

    private static final Integer NOT_FOUND_CODE = 404;

    private final PositionDAO positionDAO;

    public PositionServiceImpl(PositionDAO positionDAO) {
        this.positionDAO = positionDAO;
    }

    @Override
    public PositionVO create(CreatePositionDTO dto) {
        PositionDO positionDO = toPositionDO(dto);
        positionDAO.insert(positionDO);
        return toPositionVO(positionDO);
    }

    @Override
    public PositionVO update(UpdatePositionDTO dto) {
        if (positionDAO.selectById(dto.getId()) == null) {
            throw new BusinessException(NOT_FOUND_CODE, "position not found");
        }
        PositionDO positionDO = toPositionDO(dto);
        positionDO.setId(dto.getId());
        positionDAO.updateById(positionDO);
        return toPositionVO(positionDAO.selectById(dto.getId()));
    }

    @Override
    public void delete(Long id) {
        if (positionDAO.deleteById(id) == 0) {
            throw new BusinessException(NOT_FOUND_CODE, "position not found");
        }
    }

    @Override
    public PositionVO getById(Long id) {
        PositionDO positionDO = positionDAO.selectById(id);
        if (positionDO == null) {
            throw new BusinessException(NOT_FOUND_CODE, "position not found");
        }
        return toPositionVO(positionDO);
    }

    @Override
    public List<PositionVO> listByActivityId(Long activityId) {
        return positionDAO.selectByActivityId(activityId).stream()
            .map(this::toPositionVO)
            .toList();
    }

    @Override
    public PositionDTO getClientPosition(Long id) {
        return toPositionDTO(getById(id));
    }

    @Override
    public List<PositionDTO> listClientPositions(Long activityId) {
        return listByActivityId(activityId).stream()
            .map(this::toPositionDTO)
            .toList();
    }

    private PositionDO toPositionDO(CreatePositionDTO dto) {
        PositionDO positionDO = new PositionDO();
        positionDO.setActivityId(dto.getActivityId());
        positionDO.setAreaId(dto.getAreaId());
        positionDO.setName(dto.getName());
        positionDO.setNeedCount(dto.getNeedCount());
        positionDO.setSkillRequirement(dto.getSkillRequirement());
        positionDO.setSalary(dto.getSalary());
        positionDO.setStartTime(dto.getStartTime());
        positionDO.setEndTime(dto.getEndTime());
        return positionDO;
    }

    private PositionVO toPositionVO(PositionDO positionDO) {
        PositionVO positionVO = new PositionVO();
        positionVO.setId(positionDO.getId());
        positionVO.setActivityId(positionDO.getActivityId());
        positionVO.setAreaId(positionDO.getAreaId());
        positionVO.setName(positionDO.getName());
        positionVO.setNeedCount(positionDO.getNeedCount());
        positionVO.setSkillRequirement(positionDO.getSkillRequirement());
        positionVO.setSalary(positionDO.getSalary());
        positionVO.setStartTime(positionDO.getStartTime());
        positionVO.setEndTime(positionDO.getEndTime());
        return positionVO;
    }

    private PositionDTO toPositionDTO(PositionVO positionVO) {
        PositionDTO positionDTO = new PositionDTO();
        positionDTO.setId(positionVO.getId());
        positionDTO.setAreaId(positionVO.getAreaId());
        positionDTO.setName(positionVO.getName());
        positionDTO.setNeedCount(positionVO.getNeedCount());
        positionDTO.setSkillRequirement(positionVO.getSkillRequirement());
        positionDTO.setSalary(positionVO.getSalary());
        positionDTO.setStartTime(positionVO.getStartTime());
        positionDTO.setEndTime(positionVO.getEndTime());
        return positionDTO;
    }
}
