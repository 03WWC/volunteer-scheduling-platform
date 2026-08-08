package com.volunteer.platform.user.service;

import com.volunteer.platform.user.dao.AdminPermissionDAO;
import com.volunteer.platform.user.dao.AdminRoleDAO;
import com.volunteer.platform.user.dao.AdminUserDAO;
import com.volunteer.platform.user.dto.SaveAdminUserDTO;
import com.volunteer.platform.user.dto.UpdateRolePermissionsDTO;
import com.volunteer.platform.user.entity.AdminPermissionDO;
import com.volunteer.platform.user.entity.AdminRoleDO;
import com.volunteer.platform.user.entity.AdminUserDO;
import com.volunteer.platform.user.service.impl.AdminManagementServiceImpl;
import com.volunteer.platform.user.vo.AdminPermissionTreeVO;
import com.volunteer.platform.user.vo.AdminRoleVO;
import com.volunteer.platform.user.vo.AdminUserVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AdminManagementServiceImplTest {

    @Test
    void listsAdminsWithAssignedRoles() {
        InMemoryAdminUserDAO adminUserDAO = new InMemoryAdminUserDAO();
        InMemoryAdminRoleDAO adminRoleDAO = new InMemoryAdminRoleDAO();
        AdminManagementService service = new AdminManagementServiceImpl(adminUserDAO, adminRoleDAO,
            new InMemoryAdminPermissionDAO());
        AdminUserDO adminUserDO = adminUser("planner", "排班员");
        adminUserDAO.insert(adminUserDO);
        adminRoleDAO.userRoles.put(adminUserDO.getId(), List.of(2L));

        List<AdminUserVO> admins = service.listAdmins();

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getAccount()).isEqualTo("planner");
        assertThat(admins.get(0).getRoles()).extracting(AdminRoleVO::getRoleCode).containsExactly("SCHEDULE_MANAGER");
    }

    @Test
    void savesAdminAndReplacesRoles() {
        InMemoryAdminUserDAO adminUserDAO = new InMemoryAdminUserDAO();
        InMemoryAdminRoleDAO adminRoleDAO = new InMemoryAdminRoleDAO();
        AdminManagementService service = new AdminManagementServiceImpl(adminUserDAO, adminRoleDAO,
            new InMemoryAdminPermissionDAO());
        SaveAdminUserDTO dto = new SaveAdminUserDTO();
        dto.setAccount("reviewer");
        dto.setPassword("Reviewer123");
        dto.setUsername("reviewer");
        dto.setRealName("审核员");
        dto.setRoleIds(List.of(3L));

        AdminUserVO saved = service.saveAdmin(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(adminRoleDAO.userRoles.get(saved.getId())).containsExactly(3L);
        assertThat(service.listAdmins()).extracting(AdminUserVO::getAccount).containsExactly("reviewer");
    }

    @Test
    void buildsPermissionTreeAndUpdatesRolePermissions() {
        InMemoryAdminPermissionDAO permissionDAO = new InMemoryAdminPermissionDAO();
        AdminManagementService service = new AdminManagementServiceImpl(new InMemoryAdminUserDAO(),
            new InMemoryAdminRoleDAO(), permissionDAO);
        UpdateRolePermissionsDTO dto = new UpdateRolePermissionsDTO();
        dto.setRoleId(2L);
        dto.setPermissionIds(List.of(1L, 4L));

        service.updateRolePermissions(dto);
        List<AdminPermissionTreeVO> tree = service.permissionTree();

        assertThat(permissionDAO.rolePermissions.get(2L)).containsExactly(1L, 4L);
        assertThat(tree).extracting(AdminPermissionTreeVO::getPermissionCode).contains("activity:manage");
        assertThat(tree.stream()
            .filter(item -> "activity:manage".equals(item.getPermissionCode()))
            .findFirst()
            .orElseThrow()
            .getChildren()).extracting(AdminPermissionTreeVO::getPermissionCode).containsExactly("position:manage");
    }

    private AdminUserDO adminUser(String account, String realName) {
        AdminUserDO adminUserDO = new AdminUserDO();
        adminUserDO.setAccount(account);
        adminUserDO.setPassword("password");
        adminUserDO.setUsername(account);
        adminUserDO.setRealName(realName);
        adminUserDO.setStatus("ENABLED");
        return adminUserDO;
    }

    private static class InMemoryAdminUserDAO implements AdminUserDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<AdminUserDO> admins = new ArrayList<>();

        @Override
        public AdminUserDO selectByAccount(String account) {
            return admins.stream()
                .filter(admin -> admin.getAccount().equals(account))
                .findFirst()
                .orElse(null);
        }

        @Override
        public AdminUserDO selectById(Long id) {
            return admins.stream()
                .filter(admin -> admin.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<AdminUserDO> selectAll() {
            return admins;
        }

        @Override
        public int insert(AdminUserDO adminUserDO) {
            adminUserDO.setId(idGenerator.getAndIncrement());
            admins.add(adminUserDO);
            return 1;
        }

        @Override
        public int update(AdminUserDO adminUserDO) {
            AdminUserDO old = selectById(adminUserDO.getId());
            old.setUsername(adminUserDO.getUsername());
            old.setRealName(adminUserDO.getRealName());
            old.setMobile(adminUserDO.getMobile());
            old.setStatus(adminUserDO.getStatus());
            if (adminUserDO.getPassword() != null) {
                old.setPassword(adminUserDO.getPassword());
            }
            return 1;
        }
    }

    private static class InMemoryAdminRoleDAO implements AdminRoleDAO {

        private final Map<Long, List<Long>> userRoles = new LinkedHashMap<>();
        private final List<AdminRoleDO> roles = List.of(role(1L, "SUPER_ADMIN", "超级管理员"),
            role(2L, "SCHEDULE_MANAGER", "排班管理员"), role(3L, "SIGNUP_REVIEWER", "报名审核员"));

        @Override
        public List<AdminRoleDO> selectByAdminId(Long adminId) {
            List<Long> roleIds = userRoles.getOrDefault(adminId, List.of());
            return roles.stream().filter(role -> roleIds.contains(role.getId())).toList();
        }

        @Override
        public List<AdminRoleDO> selectAll() {
            return roles;
        }

        @Override
        public int deleteUserRoles(Long adminId) {
            userRoles.remove(adminId);
            return 1;
        }

        @Override
        public int insertUserRole(Long adminId, Long roleId) {
            List<Long> roleIds = new ArrayList<>(userRoles.getOrDefault(adminId, List.of()));
            roleIds.add(roleId);
            userRoles.put(adminId, roleIds);
            return 1;
        }

        private AdminRoleDO role(Long id, String code, String name) {
            AdminRoleDO roleDO = new AdminRoleDO();
            roleDO.setId(id);
            roleDO.setRoleCode(code);
            roleDO.setRoleName(name);
            roleDO.setStatus("ENABLED");
            return roleDO;
        }
    }

    private static class InMemoryAdminPermissionDAO implements AdminPermissionDAO {

        private final Map<Long, List<Long>> rolePermissions = new LinkedHashMap<>();
        private final List<AdminPermissionDO> permissions = List.of(permission(1L, "activity:manage", "活动管理", null, 1),
            permission(4L, "position:manage", "岗位管理", "activity:manage", 2));

        @Override
        public List<AdminPermissionDO> selectByAdminId(Long adminId) {
            return List.of();
        }

        @Override
        public List<AdminPermissionDO> selectAll() {
            return permissions;
        }

        @Override
        public List<AdminPermissionDO> selectByRoleId(Long roleId) {
            List<Long> permissionIds = rolePermissions.getOrDefault(roleId, List.of());
            return permissions.stream().filter(permission -> permissionIds.contains(permission.getId())).toList();
        }

        @Override
        public int deleteRolePermissions(Long roleId) {
            rolePermissions.remove(roleId);
            return 1;
        }

        @Override
        public int insertRolePermission(Long roleId, Long permissionId) {
            List<Long> permissionIds = new ArrayList<>(rolePermissions.getOrDefault(roleId, List.of()));
            permissionIds.add(permissionId);
            rolePermissions.put(roleId, permissionIds);
            return 1;
        }

        private AdminPermissionDO permission(Long id, String code, String name, String parentCode, int sortNo) {
            AdminPermissionDO permissionDO = new AdminPermissionDO();
            permissionDO.setId(id);
            permissionDO.setPermissionCode(code);
            permissionDO.setPermissionName(name);
            permissionDO.setParentCode(parentCode);
            permissionDO.setPermissionType("MENU");
            permissionDO.setSortNo(sortNo);
            permissionDO.setStatus("ENABLED");
            return permissionDO;
        }
    }
}
