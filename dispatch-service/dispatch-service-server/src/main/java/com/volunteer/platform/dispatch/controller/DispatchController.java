package com.volunteer.platform.dispatch.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.dispatch.client.dto.DispatchDTO;
import com.volunteer.platform.dispatch.client.dto.DispatchRecommendationDTO;
import com.volunteer.platform.dispatch.dto.DetectShortageDTO;
import com.volunteer.platform.dispatch.dto.ExecuteDispatchDTO;
import com.volunteer.platform.dispatch.service.DispatchService;
import com.volunteer.platform.dispatch.vo.DispatchRecommendationVO;
import com.volunteer.platform.dispatch.vo.DispatchResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/execute")
    public Result<DispatchDTO> execute(@RequestBody ExecuteDispatchDTO dto) {
        return Result.success(toDispatchDTO(dispatchService.execute(dto)));
    }

    @GetMapping("/{id}/result")
    public Result<DispatchDTO> getResult(@PathVariable("id") Long id) {
        return Result.success(toDispatchDTO(dispatchService.getResult(id)));
    }

    @PostMapping("/detect-shortage")
    public Result<List<DispatchDTO>> detectShortage(@RequestBody DetectShortageDTO dto) {
        return Result.success(dispatchService.detectShortage(dto).stream()
            .map(this::toDispatchDTO)
            .toList());
    }

    @PostMapping("/recommendations/{recommendationId}/accept")
    public Result<DispatchDTO> acceptRecommendation(@PathVariable("recommendationId") Long recommendationId) {
        return Result.success(toDispatchDTO(dispatchService.acceptRecommendation(recommendationId)));
    }

    private DispatchDTO toDispatchDTO(DispatchResultVO resultVO) {
        DispatchDTO dispatchDTO = new DispatchDTO();
        dispatchDTO.setId(resultVO.getId());
        dispatchDTO.setActivityId(resultVO.getActivityId());
        dispatchDTO.setAreaId(resultVO.getAreaId());
        dispatchDTO.setPositionId(resultVO.getPositionId());
        dispatchDTO.setRequiredCount(resultVO.getRequiredCount());
        dispatchDTO.setReason(resultVO.getReason());
        dispatchDTO.setDispatchStatus(resultVO.getDispatchStatus());
        dispatchDTO.setCreatedBy(resultVO.getCreatedBy());
        dispatchDTO.setFinishedTime(resultVO.getFinishedTime());
        dispatchDTO.setRecommendations(resultVO.getRecommendations().stream()
            .map(this::toRecommendationDTO)
            .toList());
        return dispatchDTO;
    }

    private DispatchRecommendationDTO toRecommendationDTO(DispatchRecommendationVO recommendationVO) {
        DispatchRecommendationDTO recommendationDTO = new DispatchRecommendationDTO();
        recommendationDTO.setId(recommendationVO.getId());
        recommendationDTO.setUserId(recommendationVO.getUserId());
        recommendationDTO.setDistanceMeter(recommendationVO.getDistanceMeter());
        recommendationDTO.setMatchScore(recommendationVO.getMatchScore());
        recommendationDTO.setRecommendStatus(recommendationVO.getRecommendStatus());
        return recommendationDTO;
    }
}
