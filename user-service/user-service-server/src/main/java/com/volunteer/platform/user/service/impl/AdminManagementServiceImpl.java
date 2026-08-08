package com.volunteer.platform.user.service.impl;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.user.dao.AdminPermissionDAO;
import com.volunteer.platform.user.dao.AdminRoleDAO;
import com.volunteer.platform.user.dao.AdminUserDAO;
import com.volunteer.platform.user.dto.SaveAdminUserDTO;
import com.volunteer.platform.user.dto.UpdateRolePermissionsDTO;
import com.volunteer.platform.user.entity.AdminPermissionDO;
import com.volunteer.platform.user.entity.AdminRoleDO;
import com.volunteer.platform.user.entity.AdminUserDO;
import com.volunteer.platform.user.service.AdminManagementService;
import com.volunteer.platform.user.vo.AdminPermissionTreeVO;
import com.volunteer.platform.user.vo.AdminRoleVO;
import com.volunteer.platform.user.vo.AdminUserVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final String ENABLED_STATUS = "ENABLED";

    private final AdminUserDAO adminUserDAO;
    private final AdminRoleDAO adminRoleDAO;
    private final AdminPermissionDAO adminPermissionDAO;

    public AdminManagementServiceImpl(AdminUserDAO adminUserDAO, AdminRoleDAO adminRoleDAO,
                                      AdminPermissionDAO adminPermissionDAO) {
        this.adminUserDAO = adminUserDAO;
        this.adminRoleDAO = adminRoleDAO;
        this.adminPermissionDAO = adminPermissionDAO;
    }

    @Override
    public List<AdminUserVO> listAdmins() {
        return adminUserDAO.selectAll().stream()
            .map(this::toAdminUserVO)
            .toList();
    }

    @Override
    public AdminUserVO saveAdmin(SaveAdminUserDTO dto) {
        validateAdmin(dto);
        AdminUserDO adminUserDO = toAdminUserDO(dto);
        if (adminUserDO.getId() == null) {
            adminUserDAO.insert(adminUserDO);
        } else {
            adminUserDAO.update(adminUserDO);
        }
        replaceUserRoles(adminUserDO.getId(), dto.getRoleIds());
        return toAdminUserVO(adminUserDAO.selectById(adminUserDO.getId()));
    }

    @Override
    public List<AdminRoleVO> listRoles() {
        return adminRoleDAO.selectAll().stream()
            .map(this::toAdminRoleVOWithPermissions)
            .toList();
    }

    @Override
    public List<AdminPermissionTreeVO> permissionTree() {
        List<AdminPermissionTreeVO> nodes = adminPermissionDAO.selectAll().stream()
            .map(this::toPermissionTreeVO)
            .sorted(Comparator.comparing(AdminPermissionTreeVO::getSortNo, Comparator.nullsLast(Integer::compareTo)))
            .toList();
        Map<String, AdminPermissionTreeVO> nodeByCode = new LinkedHashMap<>();
        nodes.forEach(node -> nodeByCode.put(node.getPermissionCode(), node));
        List<AdminPermissionTreeVO> roots = new ArrayList<>();
        for (AdminPermissionTreeVO node : nodes) {
            if (node.getParentCode() == null || !nodeByCode.containsKey(node.getParentCode())) {
                roots.add(node);
            } else {
                nodeByCode.get(node.getParentCode()).getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public void updateRolePermissions(UpdateRolePermissionsDTO dto) {
        if (dto == null || dto.getRoleId() == null) {
            throw new BusinessException(400, "role id is required");
        }
        adminPermissionDAO.deleteRolePermissions(dto.getRoleId());
        for (Long permissionId : dto.getPermissionIds()) {
            adminPermissionDAO.insertRolePermission(dto.getRoleId(), permissionId);
        }
    }

    private void validateAdmin(SaveAdminUserDTO dto) {
        if (dto == null || dto.getAccount() == null || dto.getAccount().isBlank()) {
            throw new BusinessException(400, "admin account is required");
        }
        if (dto.getId() == null && (dto.getPassword() == null || dto.getPassword().isBlank())) {
            throw new BusinessException(400, "admin password is required");
        }
    }

    private void replaceUserRoles(Long adminId, List<Long> roleIds) {
        adminRoleDAO.deleteUserRoles(adminId);
        for (Long roleId : roleIds) {
            adminRoleDAO.insertUserRole(adminId, roleId);
        }
    }

    private AdminUserDO toAdminUserDO(SaveAdminUserDTO dto) {
        AdminUserDO adminUserDO = new AdminUserDO();
        adminUserDO.setId(dto.getId());
        adminUserDO.setAccount(dto.getAccount());
        adminUserDO.setPassword(dto.getPassword());
        adminUserDO.setUsername(dto.getUsername() == null || dto.getUsername().isBlank() ? dto.getAccount() : dto.getUsername());
        adminUserDO.setRealName(dto.getRealName());
        adminUserDO.setMobile(dto.getMobile());
        adminUserDO.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? ENABLED_STATUS : dto.getStatus());
        return adminUserDO;
    }

    private AdminUserVO toAdminUserVO(AdminUserDO adminUserDO) {
        AdminUserVO adminUserVO = new AdminUserVO();
        adminUserVO.setId(adminUserDO.getId());
        adminUserVO.setAccount(adminUserDO.getAccount());
        adminUserVO.setUsername(adminUserDO.getUsername());
        adminUserVO.setRealName(adminUserDO.getRealName());
        adminUserVO.setMobile(adminUserDO.getMobile());
        adminUserVO.setStatus(adminUserDO.getStatus());
        adminUserVO.setRoles(adminRoleDAO.selectByAdminId(adminUserDO.getId()).stream()
            .map(this::toAdminRoleVO)
            .toList());
        return adminUserVO;
    }

    private AdminRoleVO toAdminRoleVOWithPermissions(AdminRoleDO adminRoleDO) {
        AdminRoleVO adminRoleVO = toAdminRoleVO(adminRoleDO);
        adminRoleVO.setPermissionIds(adminPermissionDAO.selectByRoleId(adminRoleDO.getId()).stream()
            .map(AdminPermissionDO::getId)
            .toList());
        return adminRoleVO;
    }

    private AdminRoleVO toAdminRoleVO(AdminRoleDO adminRoleDO) {
        AdminRoleVO adminRoleVO = new AdminRoleVO();
        adminRoleVO.setId(adminRoleDO.getId());
        adminRoleVO.setRoleCode(adminRoleDO.getRoleCode());
        adminRoleVO.setRoleName(adminRoleDO.getRoleName());
        adminRoleVO.setStatus(adminRoleDO.getStatus());
        return adminRoleVO;
    }

    private AdminPermissionTreeVO toPermissionTreeVO(AdminPermissionDO permissionDO) {
        AdminPermissionTreeVO permissionVO = new AdminPermissionTreeVO();
        permissionVO.setId(permissionDO.getId());
        permissionVO.setPermissionCode(permissionDO.getPermissionCode());
        permissionVO.setPermissionName(permissionDO.getPermissionName());
        permissionVO.setParentCode(permissionDO.getParentCode());
        permissionVO.setPermissionType(permissionDO.getPermissionType());
        permissionVO.setSortNo(permissionDO.getSortNo());
        return permissionVO;
    }
}
