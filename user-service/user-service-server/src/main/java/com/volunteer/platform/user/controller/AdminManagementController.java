package com.volunteer.platform.user.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.user.dto.SaveAdminUserDTO;
import com.volunteer.platform.user.dto.UpdateRolePermissionsDTO;
import com.volunteer.platform.user.service.AdminManagementService;
import com.volunteer.platform.user.vo.AdminPermissionTreeVO;
import com.volunteer.platform.user.vo.AdminRoleVO;
import com.volunteer.platform.user.vo.AdminUserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @GetMapping("/admins")
    public Result<List<AdminUserVO>> listAdmins() {
        return Result.success(adminManagementService.listAdmins());
    }

    @PostMapping("/admins")
    public Result<AdminUserVO> saveAdmin(@RequestBody SaveAdminUserDTO dto) {
        return Result.success(adminManagementService.saveAdmin(dto));
    }

    @GetMapping("/roles")
    public Result<List<AdminRoleVO>> listRoles() {
        return Result.success(adminManagementService.listRoles());
    }

    @GetMapping("/permissions/tree")
    public Result<List<AdminPermissionTreeVO>> permissionTree() {
        return Result.success(adminManagementService.permissionTree());
    }

    @PutMapping("/roles/permissions")
    public Result<Void> updateRolePermissions(@RequestBody UpdateRolePermissionsDTO dto) {
        adminManagementService.updateRolePermissions(dto);
        return Result.success(null);
    }
}
