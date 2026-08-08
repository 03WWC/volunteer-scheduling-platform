package com.volunteer.platform.user.service;

import com.volunteer.platform.user.dto.SaveAdminUserDTO;
import com.volunteer.platform.user.dto.UpdateRolePermissionsDTO;
import com.volunteer.platform.user.vo.AdminPermissionTreeVO;
import com.volunteer.platform.user.vo.AdminRoleVO;
import com.volunteer.platform.user.vo.AdminUserVO;

import java.util.List;

public interface AdminManagementService {

    List<AdminUserVO> listAdmins();

    AdminUserVO saveAdmin(SaveAdminUserDTO dto);

    List<AdminRoleVO> listRoles();

    List<AdminPermissionTreeVO> permissionTree();

    void updateRolePermissions(UpdateRolePermissionsDTO dto);
}
