package com.volunteer.platform.activity.controller;

import com.volunteer.platform.activity.dto.CreateAreaDTO;
import com.volunteer.platform.activity.dto.UpdateAreaDTO;
import com.volunteer.platform.activity.service.AreaService;
import com.volunteer.platform.activity.vo.AreaVO;
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
@RequestMapping("/area")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @PostMapping("/create")
    public Result<AreaVO> create(@RequestBody CreateAreaDTO dto) {
        return Result.success(areaService.create(dto));
    }

    @PutMapping("/update")
    public Result<AreaVO> update(@RequestBody UpdateAreaDTO dto) {
        return Result.success(areaService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        areaService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<AreaVO>> list(@RequestParam("activityId") Long activityId) {
        return Result.success(areaService.listByActivityId(activityId));
    }
}
