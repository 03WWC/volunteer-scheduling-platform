package com.volunteer.platform.activity.controller;

import com.volunteer.platform.activity.client.dto.PositionDTO;
import com.volunteer.platform.activity.dto.CreatePositionDTO;
import com.volunteer.platform.activity.dto.UpdatePositionDTO;
import com.volunteer.platform.activity.service.PositionService;
import com.volunteer.platform.activity.vo.PositionVO;
import com.volunteer.platform.common.api.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/position")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PostMapping("/create")
    public Result<PositionVO> create(@RequestBody CreatePositionDTO dto) {
        return Result.success(positionService.create(dto));
    }

    @PutMapping("/update")
    public Result<PositionVO> update(@RequestBody UpdatePositionDTO dto) {
        return Result.success(positionService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        positionService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<PositionDTO>> list(@RequestParam("activityId") Long activityId) {
        return Result.success(positionService.listClientPositions(activityId));
    }

    @GetMapping("/{id}")
    public Result<PositionDTO> detail(@PathVariable("id") Long id) {
        return Result.success(positionService.getClientPosition(id));
    }
}
