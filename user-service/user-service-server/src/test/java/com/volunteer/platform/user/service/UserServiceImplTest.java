package com.volunteer.platform.user.service;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.user.client.wechat.WechatCodeSession;
import com.volunteer.platform.user.client.wechat.WechatCodeSessionClient;
import com.volunteer.platform.user.dao.AdminPermissionDAO;
import com.volunteer.platform.user.dao.AdminRoleDAO;
import com.volunteer.platform.user.dao.AdminUserDAO;
import com.volunteer.platform.user.dao.UserAvailabilityDAO;
import com.volunteer.platform.user.dao.UserDAO;
import com.volunteer.platform.user.dao.UserSkillDAO;
import com.volunteer.platform.user.dto.AdminLoginDTO;
import com.volunteer.platform.user.dto.RealNameAuthDTO;
import com.volunteer.platform.user.dto.RegisterUserDTO;
import com.volunteer.platform.user.dto.SaveAvailabilityDTO;
import com.volunteer.platform.user.dto.SaveUserSkillDTO;
import com.volunteer.platform.user.dto.WechatLoginDTO;
import com.volunteer.platform.user.entity.AdminPermissionDO;
import com.volunteer.platform.user.entity.AdminRoleDO;
import com.volunteer.platform.user.entity.AdminUserDO;
import com.volunteer.platform.user.entity.UserAvailabilityDO;
import com.volunteer.platform.user.entity.UserDO;
import com.volunteer.platform.user.entity.UserSkillDO;
import com.volunteer.platform.user.query.UserQuery;
import com.volunteer.platform.user.service.impl.UserServiceImpl;
import com.volunteer.platform.user.vo.AuthSessionVO;
import com.volunteer.platform.user.vo.UserAvailabilityVO;
import com.volunteer.platform.user.vo.UserSkillVO;
import com.volunteer.platform.user.vo.UserVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceImplTest {

    @Test
    void registersVolunteerAndListsVolunteers() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUsername("volunteer01");
        dto.setMobile("13800000001");
        dto.setUserType("VOLUNTEER");

        UserVO created = service.register(dto);
        List<UserVO> volunteers = service.listVolunteers(new UserQuery());

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(volunteers).extracting(UserVO::getMobile).containsExactly("13800000001");
    }

    @Test
    void authenticatesRealNameInformation() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        UserVO userVO = service.register(registerVolunteerDTO("volunteer02", "13800000002"));
        RealNameAuthDTO authDTO = new RealNameAuthDTO();
        authDTO.setUserId(userVO.getId());
        authDTO.setRealName("Alice");
        authDTO.setIdCardNo("110101199001010011");

        UserVO authenticated = service.authenticate(authDTO);

        assertThat(authenticated.getRealName()).isEqualTo("Alice");
        assertThat(authenticated.getAuthStatus()).isEqualTo("AUTHENTICATED");
    }

    @Test
    void authenticateThrowsBusinessExceptionWhenUserMissing() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        RealNameAuthDTO authDTO = new RealNameAuthDTO();
        authDTO.setUserId(404L);
        authDTO.setRealName("Missing");
        authDTO.setIdCardNo("110101199001010099");

        assertThatThrownBy(() -> service.authenticate(authDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessage("user not found");
    }

    @Test
    void savesSkillAndAvailabilityForUser() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        UserVO userVO = service.register(registerVolunteerDTO("volunteer03", "13800000003"));
        SaveUserSkillDTO skillDTO = new SaveUserSkillDTO();
        skillDTO.setUserId(userVO.getId());
        skillDTO.setSkillCode("GUIDE");
        skillDTO.setSkillName("引导");
        skillDTO.setSkillLevel("SENIOR");
        SaveAvailabilityDTO availabilityDTO = new SaveAvailabilityDTO();
        availabilityDTO.setUserId(userVO.getId());
        availabilityDTO.setAvailableDate(LocalDate.of(2026, 8, 1));
        availabilityDTO.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        availabilityDTO.setEndTime(LocalDateTime.of(2026, 8, 1, 18, 0));

        UserSkillVO skillVO = service.saveSkill(skillDTO);
        UserAvailabilityVO availabilityVO = service.saveAvailability(availabilityDTO);

        assertThat(skillVO.getSkillCode()).isEqualTo("GUIDE");
        assertThat(service.listSkills(userVO.getId())).extracting(UserSkillVO::getSkillName).containsExactly("引导");
        assertThat(availabilityVO.getAvailableDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(service.listAvailability(userVO.getId())).extracting(UserAvailabilityVO::getUserId)
            .containsExactly(userVO.getId());
    }

    @Test
    void deletesSkillAndAvailabilityForUser() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        UserVO userVO = service.register(registerVolunteerDTO("volunteer-delete", "13800000031"));
        SaveUserSkillDTO skillDTO = new SaveUserSkillDTO();
        skillDTO.setUserId(userVO.getId());
        skillDTO.setSkillCode("GUIDE");
        skillDTO.setSkillName("秩序引导");
        skillDTO.setSkillLevel("BEGINNER");
        SaveAvailabilityDTO availabilityDTO = new SaveAvailabilityDTO();
        availabilityDTO.setUserId(userVO.getId());
        availabilityDTO.setAvailableDate(LocalDate.of(2026, 8, 1));
        availabilityDTO.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        availabilityDTO.setEndTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        UserSkillVO skillVO = service.saveSkill(skillDTO);
        UserAvailabilityVO availabilityVO = service.saveAvailability(availabilityDTO);

        service.deleteSkill(skillVO.getId(), userVO.getId());
        service.deleteAvailability(availabilityVO.getId(), userVO.getId());

        assertThat(service.listSkills(userVO.getId())).isEmpty();
        assertThat(service.listAvailability(userVO.getId())).isEmpty();
    }

    @Test
    void updatesAvailabilityWhenIdIsProvided() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        UserVO userVO = service.register(registerVolunteerDTO("volunteer-edit", "13800000032"));
        SaveAvailabilityDTO createDTO = new SaveAvailabilityDTO();
        createDTO.setUserId(userVO.getId());
        createDTO.setAvailableDate(LocalDate.of(2026, 8, 1));
        createDTO.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        createDTO.setEndTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        UserAvailabilityVO created = service.saveAvailability(createDTO);
        SaveAvailabilityDTO updateDTO = new SaveAvailabilityDTO();
        updateDTO.setId(created.getId());
        updateDTO.setUserId(userVO.getId());
        updateDTO.setAvailableDate(LocalDate.of(2026, 8, 2));
        updateDTO.setStartTime(LocalDateTime.of(2026, 8, 2, 13, 0));
        updateDTO.setEndTime(LocalDateTime.of(2026, 8, 2, 17, 0));

        UserAvailabilityVO updated = service.saveAvailability(updateDTO);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getAvailableDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(service.listAvailability(userVO.getId())).hasSize(1);
        assertThat(service.listAvailability(userVO.getId())).extracting(UserAvailabilityVO::getStartTime)
            .containsExactly(LocalDateTime.of(2026, 8, 2, 13, 0));
    }

    @Test
    void rejectsAvailabilityUpdateOwnedByAnotherUser() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        UserVO owner = service.register(registerVolunteerDTO("volunteer-owner", "13800000033"));
        UserVO other = service.register(registerVolunteerDTO("volunteer-other", "13800000034"));
        SaveAvailabilityDTO createDTO = new SaveAvailabilityDTO();
        createDTO.setUserId(owner.getId());
        createDTO.setAvailableDate(LocalDate.of(2026, 8, 1));
        createDTO.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        createDTO.setEndTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        UserAvailabilityVO created = service.saveAvailability(createDTO);
        SaveAvailabilityDTO updateDTO = new SaveAvailabilityDTO();
        updateDTO.setId(created.getId());
        updateDTO.setUserId(other.getId());
        updateDTO.setAvailableDate(LocalDate.of(2026, 8, 2));
        updateDTO.setStartTime(LocalDateTime.of(2026, 8, 2, 13, 0));
        updateDTO.setEndTime(LocalDateTime.of(2026, 8, 2, 17, 0));

        assertThatThrownBy(() -> service.saveAvailability(updateDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessage("availability not found");
    }

    @Test
    void logsInWechatUserByOpenidWithoutDuplicatingUser() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        WechatLoginDTO dto = new WechatLoginDTO();
        dto.setOpenid("wx-openid-001");
        dto.setNickname("wechat-user");
        dto.setAvatarUrl("https://example.com/avatar.png");

        AuthSessionVO firstLogin = service.wechatLogin(dto);
        AuthSessionVO secondLogin = service.wechatLogin(dto);

        assertThat(secondLogin.getUser().getId()).isEqualTo(firstLogin.getUser().getId());
        assertThat(secondLogin.getUser().getUsername()).isEqualTo("wechat-user");
        assertThat(secondLogin.getUser().getMobile()).startsWith("WX").hasSize(20);
        assertThat(secondLogin.getToken()).isNotBlank();
        assertThat(service.listVolunteers(new UserQuery())).hasSize(1);
    }

    @Test
    void logsInWechatUserByCodeThroughWechatSessionClient() {
        WechatCodeSessionClient wechatClient = code -> {
            WechatCodeSession session = new WechatCodeSession();
            session.setOpenid("wx-openid-from-code");
            return session;
        };
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO(), wechatClient);
        WechatLoginDTO dto = new WechatLoginDTO();
        dto.setCode("miniapp-login-code");
        dto.setNickname("wechat-code-user");

        AuthSessionVO sessionVO = service.wechatLogin(dto);

        assertThat(sessionVO.getUser().getOpenid()).isEqualTo("wx-openid-from-code");
        assertThat(sessionVO.getUser().getUsername()).isEqualTo("wechat-code-user");
        assertThat(sessionVO.getToken()).isNotBlank();
    }

    @Test
    void logsInAdminWithConfiguredCredential() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setAccount("admin");
        dto.setPassword("Admin123456");

        AuthSessionVO sessionVO = service.adminLogin(dto);

        assertThat(sessionVO.getToken()).isNotBlank();
        assertThat(sessionVO.getUser().getUserType()).isEqualTo("MANAGER");
        assertThat(sessionVO.getRoles()).containsExactly("SUPER_ADMIN");
        assertThat(sessionVO.getPermissions()).contains("activity:manage", "schedule:manage", "system:manage");
    }

    @Test
    void logsInAdminFromDatabaseWithRolesAndPermissions() {
        InMemoryAdminUserDAO adminUserDAO = new InMemoryAdminUserDAO();
        AdminUserDO adminUserDO = new AdminUserDO();
        adminUserDO.setId(100L);
        adminUserDO.setAccount("planner");
        adminUserDO.setPassword("Planner123");
        adminUserDO.setUsername("planner");
        adminUserDO.setRealName("排班管理员");
        adminUserDAO.adminUserDO = adminUserDO;
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO(), code -> {
                throw new BusinessException(500, "wechat unavailable");
            }, adminUserDAO, new InMemoryAdminRoleDAO(), new InMemoryAdminPermissionDAO());
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setAccount("planner");
        dto.setPassword("Planner123");

        AuthSessionVO sessionVO = service.adminLogin(dto);

        assertThat(sessionVO.getUser().getId()).isEqualTo(100L);
        assertThat(sessionVO.getUser().getRealName()).isEqualTo("排班管理员");
        assertThat(sessionVO.getRoles()).containsExactly("SCHEDULE_MANAGER");
        assertThat(sessionVO.getPermissions()).containsExactly("dashboard:view", "schedule:manage", "dispatch:manage");
    }

    @Test
    void rejectsInvalidAdminCredential() {
        UserService service = new UserServiceImpl(new InMemoryUserDAO(), new InMemoryUserSkillDAO(),
            new InMemoryAvailabilityDAO());
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setAccount("admin");
        dto.setPassword("wrong");

        assertThatThrownBy(() -> service.adminLogin(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("invalid account or password");
    }

    private RegisterUserDTO registerVolunteerDTO(String username, String mobile) {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUsername(username);
        dto.setMobile(mobile);
        dto.setUserType("VOLUNTEER");
        return dto;
    }

    private static class InMemoryUserDAO implements UserDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<UserDO> users = new ArrayList<>();

        @Override
        public int insert(UserDO userDO) {
            userDO.setId(idGenerator.getAndIncrement());
            users.add(userDO);
            return 1;
        }

        @Override
        public int updateAuth(UserDO userDO) {
            UserDO oldUser = selectById(userDO.getId());
            if (oldUser == null) {
                return 0;
            }
            oldUser.setRealName(userDO.getRealName());
            oldUser.setIdCardNo(userDO.getIdCardNo());
            oldUser.setAuthStatus(userDO.getAuthStatus());
            return 1;
        }

        @Override
        public UserDO selectById(Long id) {
            return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public UserDO selectByOpenid(String openid) {
            return users.stream()
                .filter(user -> openid.equals(user.getOpenid()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<UserDO> selectVolunteers(UserQuery query) {
            return users.stream()
                .filter(user -> "VOLUNTEER".equals(user.getUserType()))
                .toList();
        }
    }

    private static class InMemoryUserSkillDAO implements UserSkillDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<UserSkillDO> skills = new ArrayList<>();

        @Override
        public int insert(UserSkillDO userSkillDO) {
            userSkillDO.setId(idGenerator.getAndIncrement());
            skills.add(userSkillDO);
            return 1;
        }

        @Override
        public UserSkillDO selectById(Long id) {
            return skills.stream()
                .filter(skill -> skill.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int deleteById(Long id) {
            skills.removeIf(skill -> skill.getId().equals(id));
            return 1;
        }

        @Override
        public List<UserSkillDO> selectByUserId(Long userId) {
            return skills.stream()
                .filter(skill -> skill.getUserId().equals(userId))
                .toList();
        }
    }

    private static class InMemoryAvailabilityDAO implements UserAvailabilityDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<UserAvailabilityDO> availabilityList = new ArrayList<>();

        @Override
        public int insert(UserAvailabilityDO availabilityDO) {
            availabilityDO.setId(idGenerator.getAndIncrement());
            availabilityList.add(availabilityDO);
            return 1;
        }

        @Override
        public int update(UserAvailabilityDO availabilityDO) {
            UserAvailabilityDO oldAvailability = selectById(availabilityDO.getId());
            if (oldAvailability == null) {
                return 0;
            }
            oldAvailability.setAvailableDate(availabilityDO.getAvailableDate());
            oldAvailability.setStartTime(availabilityDO.getStartTime());
            oldAvailability.setEndTime(availabilityDO.getEndTime());
            oldAvailability.setStatus(availabilityDO.getStatus());
            return 1;
        }

        @Override
        public UserAvailabilityDO selectById(Long id) {
            return availabilityList.stream()
                .filter(availability -> availability.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int deleteById(Long id) {
            availabilityList.removeIf(availability -> availability.getId().equals(id));
            return 1;
        }

        @Override
        public List<UserAvailabilityDO> selectByUserId(Long userId) {
            return availabilityList.stream()
                .filter(availability -> availability.getUserId().equals(userId))
                .toList();
        }
    }

    private static class InMemoryAdminUserDAO implements AdminUserDAO {

        private AdminUserDO adminUserDO;

        @Override
        public AdminUserDO selectByAccount(String account) {
            if (adminUserDO == null || !adminUserDO.getAccount().equals(account)) {
                return null;
            }
            return adminUserDO;
        }

        @Override
        public AdminUserDO selectById(Long id) {
            return adminUserDO != null && adminUserDO.getId().equals(id) ? adminUserDO : null;
        }

        @Override
        public List<AdminUserDO> selectAll() {
            return adminUserDO == null ? List.of() : List.of(adminUserDO);
        }

        @Override
        public int insert(AdminUserDO adminUserDO) {
            this.adminUserDO = adminUserDO;
            return 1;
        }

        @Override
        public int update(AdminUserDO adminUserDO) {
            this.adminUserDO = adminUserDO;
            return 1;
        }
    }

    private static class InMemoryAdminRoleDAO implements AdminRoleDAO {

        @Override
        public List<AdminRoleDO> selectByAdminId(Long adminId) {
            AdminRoleDO roleDO = new AdminRoleDO();
            roleDO.setId(1L);
            roleDO.setRoleCode("SCHEDULE_MANAGER");
            roleDO.setRoleName("排班管理员");
            roleDO.setStatus("ENABLED");
            return List.of(roleDO);
        }

        @Override
        public List<AdminRoleDO> selectAll() {
            return selectByAdminId(1L);
        }

        @Override
        public int deleteUserRoles(Long adminId) {
            return 1;
        }

        @Override
        public int insertUserRole(Long adminId, Long roleId) {
            return 1;
        }
    }

    private static class InMemoryAdminPermissionDAO implements AdminPermissionDAO {

        @Override
        public List<AdminPermissionDO> selectByAdminId(Long adminId) {
            AdminPermissionDO dashboard = permission("dashboard:view", "调度总览", 1);
            AdminPermissionDO schedule = permission("schedule:manage", "排班计划", 2);
            AdminPermissionDO dispatch = permission("dispatch:manage", "智能调度", 3);
            return List.of(dashboard, schedule, dispatch);
        }

        @Override
        public List<AdminPermissionDO> selectAll() {
            return selectByAdminId(1L);
        }

        @Override
        public List<AdminPermissionDO> selectByRoleId(Long roleId) {
            return selectByAdminId(1L);
        }

        @Override
        public int deleteRolePermissions(Long roleId) {
            return 1;
        }

        @Override
        public int insertRolePermission(Long roleId, Long permissionId) {
            return 1;
        }

        private AdminPermissionDO permission(String code, String name, int sortNo) {
            AdminPermissionDO permissionDO = new AdminPermissionDO();
            permissionDO.setPermissionCode(code);
            permissionDO.setPermissionName(name);
            permissionDO.setSortNo(sortNo);
            permissionDO.setStatus("ENABLED");
            return permissionDO;
        }
    }
}
