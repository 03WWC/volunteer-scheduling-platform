package com.volunteer.platform.user.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.user.dto.AdminLoginDTO;
import com.volunteer.platform.user.service.UserService;
import com.volunteer.platform.user.vo.AuthSessionVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/admin/login")
    public Result<AuthSessionVO> adminLogin(@RequestBody AdminLoginDTO dto) {
        return Result.success(userService.adminLogin(dto));
    }
}
