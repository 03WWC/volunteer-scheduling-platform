package com.volunteer.platform.activity.service;

import com.volunteer.platform.activity.dao.AreaDAO;
import com.volunteer.platform.activity.dao.PositionDAO;
import com.volunteer.platform.activity.dto.CreateAreaDTO;
import com.volunteer.platform.activity.dto.CreatePositionDTO;
import com.volunteer.platform.activity.dto.UpdateAreaDTO;
import com.volunteer.platform.activity.dto.UpdatePositionDTO;
import com.volunteer.platform.activity.entity.AreaDO;
import com.volunteer.platform.activity.entity.PositionDO;
import com.volunteer.platform.activity.service.impl.AreaServiceImpl;
import com.volunteer.platform.activity.service.impl.PositionServiceImpl;
import com.volunteer.platform.activity.vo.AreaVO;
import com.volunteer.platform.activity.vo.PositionVO;
import com.volunteer.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AreaPositionServiceImplTest {

    @Test
    void createsAreaAndListsByActivityId() {
        AreaService service = new AreaServiceImpl(new InMemoryAreaDAO());
        CreateAreaDTO dto = new CreateAreaDTO();
        dto.setActivityId(10L);
        dto.setName("Main Stage");
        dto.setGps("121.1,31.1;121.2,31.2");
        dto.setOwnerName("Bob");

        AreaVO created = service.create(dto);
        List<AreaVO> areas = service.listByActivityId(10L);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(areas).extracting(AreaVO::getName).containsExactly("Main Stage");
    }

    @Test
    void updatesAreaAndDeletesById() {
        AreaService service = new AreaServiceImpl(new InMemoryAreaDAO());
        CreateAreaDTO createDTO = new CreateAreaDTO();
        createDTO.setActivityId(10L);
        createDTO.setName("Main Stage");
        createDTO.setGps("121.1,31.1;121.2,31.2");
        createDTO.setOwnerName("Bob");
        AreaVO created = service.create(createDTO);

        UpdateAreaDTO updateDTO = new UpdateAreaDTO();
        updateDTO.setId(created.getId());
        updateDTO.setActivityId(10L);
        updateDTO.setName("North Gate");
        updateDTO.setGps("121.3,31.3;121.4,31.4");
        updateDTO.setOwnerName("Alice");
        AreaVO updated = service.update(updateDTO);
        service.delete(created.getId());

        assertThat(updated.getName()).isEqualTo("North Gate");
        assertThat(service.listByActivityId(10L)).isEmpty();
    }

    @Test
    void updateAreaThrowsBusinessExceptionWhenMissing() {
        AreaService service = new AreaServiceImpl(new InMemoryAreaDAO());
        UpdateAreaDTO updateDTO = new UpdateAreaDTO();
        updateDTO.setId(404L);
        updateDTO.setActivityId(10L);
        updateDTO.setName("Missing Area");

        assertThatThrownBy(() -> service.update(updateDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessage("area not found");
    }

    @Test
    void createsPositionAndListsByActivityId() {
        PositionService service = new PositionServiceImpl(new InMemoryPositionDAO());
        CreatePositionDTO dto = createPositionDTO(20L, 30L, "Entrance Security");

        PositionVO created = service.create(dto);
        List<PositionVO> positions = service.listByActivityId(20L);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(positions).extracting(PositionVO::getName).containsExactly("Entrance Security");
    }

    @Test
    void updatesPositionAndDeletesById() {
        PositionService service = new PositionServiceImpl(new InMemoryPositionDAO());
        PositionVO created = service.create(createPositionDTO(20L, 30L, "Entrance Security"));

        UpdatePositionDTO updateDTO = createUpdatePositionDTO(created.getId(), 20L, 30L, "Entrance Guide");
        PositionVO updated = service.update(updateDTO);
        service.delete(created.getId());

        assertThat(updated.getName()).isEqualTo("Entrance Guide");
        assertThat(service.listByActivityId(20L)).isEmpty();
    }

    @Test
    void getPositionThrowsBusinessExceptionWhenMissing() {
        PositionService service = new PositionServiceImpl(new InMemoryPositionDAO());

        assertThatThrownBy(() -> service.getById(404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("position not found");
    }

    private CreatePositionDTO createPositionDTO(Long activityId, Long areaId, String name) {
        CreatePositionDTO dto = new CreatePositionDTO();
        dto.setActivityId(activityId);
        dto.setAreaId(areaId);
        dto.setName(name);
        dto.setNeedCount(20);
        dto.setSkillRequirement("SECURITY");
        dto.setSalary(new BigDecimal("300.00"));
        dto.setStartTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 1, 23, 0));
        return dto;
    }

    private UpdatePositionDTO createUpdatePositionDTO(Long id, Long activityId, Long areaId, String name) {
        UpdatePositionDTO dto = new UpdatePositionDTO();
        dto.setId(id);
        dto.setActivityId(activityId);
        dto.setAreaId(areaId);
        dto.setName(name);
        dto.setNeedCount(10);
        dto.setSkillRequirement("GUIDE");
        dto.setSalary(new BigDecimal("200.00"));
        dto.setStartTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 1, 22, 0));
        return dto;
    }

    private static class InMemoryAreaDAO implements AreaDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<AreaDO> areas = new ArrayList<>();

        @Override
        public int insert(AreaDO areaDO) {
            areaDO.setId(idGenerator.getAndIncrement());
            areas.add(areaDO);
            return 1;
        }

        @Override
        public int updateById(AreaDO areaDO) {
            AreaDO oldArea = selectById(areaDO.getId());
            if (oldArea == null) {
                return 0;
            }
            oldArea.setActivityId(areaDO.getActivityId());
            oldArea.setName(areaDO.getName());
            oldArea.setGps(areaDO.getGps());
            oldArea.setOwnerName(areaDO.getOwnerName());
            return 1;
        }

        @Override
        public AreaDO selectById(Long id) {
            return areas.stream()
                .filter(area -> area.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<AreaDO> selectByActivityId(Long activityId) {
            return areas.stream()
                .filter(area -> area.getActivityId().equals(activityId))
                .toList();
        }

        @Override
        public int deleteById(Long id) {
            return areas.removeIf(area -> area.getId().equals(id)) ? 1 : 0;
        }
    }

    private static class InMemoryPositionDAO implements PositionDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<PositionDO> positions = new ArrayList<>();

        @Override
        public int insert(PositionDO positionDO) {
            positionDO.setId(idGenerator.getAndIncrement());
            positions.add(positionDO);
            return 1;
        }

        @Override
        public int updateById(PositionDO positionDO) {
            PositionDO oldPosition = selectById(positionDO.getId());
            if (oldPosition == null) {
                return 0;
            }
            oldPosition.setActivityId(positionDO.getActivityId());
            oldPosition.setAreaId(positionDO.getAreaId());
            oldPosition.setName(positionDO.getName());
            oldPosition.setNeedCount(positionDO.getNeedCount());
            oldPosition.setSkillRequirement(positionDO.getSkillRequirement());
            oldPosition.setSalary(positionDO.getSalary());
            oldPosition.setStartTime(positionDO.getStartTime());
            oldPosition.setEndTime(positionDO.getEndTime());
            return 1;
        }

        @Override
        public PositionDO selectById(Long id) {
            return positions.stream()
                .filter(position -> position.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<PositionDO> selectByActivityId(Long activityId) {
            return positions.stream()
                .filter(position -> position.getActivityId().equals(activityId))
                .toList();
        }

        @Override
        public int deleteById(Long id) {
            return positions.removeIf(position -> position.getId().equals(id)) ? 1 : 0;
        }
    }
}
