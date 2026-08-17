package com.volunteer.platform.user.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.user.service.PublicOverviewService;
import com.volunteer.platform.user.vo.PublicOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicOverviewController {

    private final PublicOverviewService publicOverviewService;

    public PublicOverviewController(PublicOverviewService publicOverviewService) {
        this.publicOverviewService = publicOverviewService;
    }

    @GetMapping("/overview")
    public Result<PublicOverviewVO> overview() {
        return Result.success(publicOverviewService.getOverview());
    }
}
