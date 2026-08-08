package com.volunteer.platform.user.service.impl;

import com.volunteer.platform.common.auth.JwtProperties;
import com.volunteer.platform.common.auth.JwtTokenService;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.user.client.wechat.WechatCodeSessionClient;
import com.volunteer.platform.user.config.AdminAuthProperties;
import com.volunteer.platform.user.dao.UserAvailabilityDAO;
import com.volunteer.platform.user.dao.UserDAO;
import com.volunteer.platform.user.dao.UserSkillDAO;
import com.volunteer.platform.user.dto.AdminLoginDTO;
import com.volunteer.platform.user.dto.RealNameAuthDTO;
import com.volunteer.platform.user.dto.RegisterUserDTO;
import com.volunteer.platform.user.dto.SaveAvailabilityDTO;
import com.volunteer.platform.user.dto.SaveUserSkillDTO;
import com.volunteer.platform.user.dto.WechatLoginDTO;
import com.volunteer.platform.user.entity.UserAvailabilityDO;
import com.volunteer.platform.user.entity.UserDO;
import com.volunteer.platform.user.entity.UserSkillDO;
import com.volunteer.platform.user.query.UserQuery;
import com.volunteer.platform.user.service.UserService;
import com.volunteer.platform.user.vo.AuthSessionVO;
import com.volunteer.platform.user.vo.UserAvailabilityVO;
import com.volunteer.platform.user.vo.UserSkillVO;
import com.volunteer.platform.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Integer NOT_FOUND_CODE = 404;
    private static final String DEFAULT_USER_TYPE = "VOLUNTEER";
    private static final String DEFAULT_AUTH_STATUS = "UNAUTHENTICATED";
    private static final String AUTHENTICATED_STATUS = "AUTHENTICATED";
    private static final String ENABLED_STATUS = "ENABLED";
    private static final String AVAILABLE_STATUS = "AVAILABLE";

    private final UserDAO userDAO;
    private final UserSkillDAO userSkillDAO;
    private final UserAvailabilityDAO userAvailabilityDAO;
    private final JwtProperties jwtProperties;
    private final JwtTokenService jwtTokenService;
    private final AdminAuthProperties adminAuthProperties;
    private final WechatCodeSessionClient wechatCodeSessionClient;

    public UserServiceImpl(UserDAO userDAO, UserSkillDAO userSkillDAO, UserAvailabilityDAO userAvailabilityDAO) {
        this(userDAO, userSkillDAO, userAvailabilityDAO, new JwtProperties(), new AdminAuthProperties(),
            code -> {
                throw new BusinessException(500, "wechat code session client unavailable");
            });
    }

    public UserServiceImpl(UserDAO userDAO, UserSkillDAO userSkillDAO, UserAvailabilityDAO userAvailabilityDAO,
                           WechatCodeSessionClient wechatCodeSessionClient) {
        this(userDAO, userSkillDAO, userAvailabilityDAO, new JwtProperties(), new AdminAuthProperties(),
            wechatCodeSessionClient);
    }

    @Autowired
    public UserServiceImpl(UserDAO userDAO, UserSkillDAO userSkillDAO, UserAvailabilityDAO userAvailabilityDAO,
                           JwtProperties jwtProperties, AdminAuthProperties adminAuthProperties,
                           WechatCodeSessionClient wechatCodeSessionClient) {
        this.userDAO = userDAO;
        this.userSkillDAO = userSkillDAO;
        this.userAvailabilityDAO = userAvailabilityDAO;
        this.jwtProperties = jwtProperties;
        this.jwtTokenService = new JwtTokenService(jwtProperties);
        this.adminAuthProperties = adminAuthProperties;
        this.wechatCodeSessionClient = wechatCodeSessionClient;
    }

    @Override
    public UserVO register(RegisterUserDTO dto) {
        UserDO userDO = new UserDO();
        userDO.setUsername(dto.getUsername());
        userDO.setMobile(dto.getMobile());
        userDO.setUserType(dto.getUserType() == null ? DEFAULT_USER_TYPE : dto.getUserType());
        userDO.setAuthStatus(DEFAULT_AUTH_STATUS);
        userDO.setStatus(ENABLED_STATUS);
        userDAO.insert(userDO);
        return toUserVO(userDO);
    }

    @Override
    public AuthSessionVO adminLogin(AdminLoginDTO dto) {
        if (dto == null || !adminAuthProperties.getAccount().equals(dto.getAccount())
            || !adminAuthProperties.getPassword().equals(dto.getPassword())) {
            throw new BusinessException(401, "invalid account or password");
        }
        UserVO userVO = new UserVO();
        userVO.setId(adminAuthProperties.getUserId());
        userVO.setUsername(adminAuthProperties.getUsername());
        userVO.setUserType(adminAuthProperties.getUserType());
        userVO.setAuthStatus(AUTHENTICATED_STATUS);
        return issueSession(userVO);
    }

    @Override
    public AuthSessionVO wechatLogin(WechatLoginDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "wechat login payload is required");
        }
        String openid = resolveWechatOpenid(dto);
        dto.setOpenid(openid);
        UserDO oldUser = userDAO.selectByOpenid(openid);
        if (oldUser != null) {
            return issueSession(toUserVO(oldUser));
        }
        UserDO userDO = new UserDO();
        userDO.setUsername(resolveWechatUsername(dto));
        userDO.setMobile(resolveWechatMobile(dto));
        userDO.setOpenid(openid);
        userDO.setAvatarUrl(dto.getAvatarUrl());
        userDO.setUserType(DEFAULT_USER_TYPE);
        userDO.setAuthStatus(DEFAULT_AUTH_STATUS);
        userDO.setStatus(ENABLED_STATUS);
        userDAO.insert(userDO);
        return issueSession(toUserVO(userDO));
    }

    @Override
    public UserVO getById(Long id) {
        if (id == null) {
            throw new BusinessException(400, "user id is required");
        }
        UserDO userDO = userDAO.selectById(id);
        if (userDO == null) {
            throw new BusinessException(NOT_FOUND_CODE, "user not found");
        }
        return toUserVO(userDO);
    }

    private String resolveWechatOpenid(WechatLoginDTO dto) {
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            return wechatCodeSessionClient.exchange(dto.getCode()).getOpenid();
        }
        if (dto.getOpenid() != null && !dto.getOpenid().isBlank()) {
            return dto.getOpenid();
        }
        throw new BusinessException(400, "wechat code or openid is required");
    }

    @Override
    public UserVO authenticate(RealNameAuthDTO dto) {
        UserDO oldUser = userDAO.selectById(dto.getUserId());
        if (oldUser == null) {
            throw new BusinessException(NOT_FOUND_CODE, "user not found");
        }
        UserDO userDO = new UserDO();
        userDO.setId(dto.getUserId());
        userDO.setRealName(dto.getRealName());
        userDO.setIdCardNo(dto.getIdCardNo());
        userDO.setAuthStatus(AUTHENTICATED_STATUS);
        userDAO.updateAuth(userDO);
        return toUserVO(userDAO.selectById(dto.getUserId()));
    }

    @Override
    public List<UserVO> listVolunteers(UserQuery query) {
        return userDAO.selectVolunteers(query).stream()
            .map(this::toUserVO)
            .toList();
    }

    @Override
    public UserSkillVO saveSkill(SaveUserSkillDTO dto) {
        UserSkillDO userSkillDO = new UserSkillDO();
        userSkillDO.setUserId(dto.getUserId());
        userSkillDO.setSkillCode(dto.getSkillCode());
        userSkillDO.setSkillName(dto.getSkillName());
        userSkillDO.setSkillLevel(dto.getSkillLevel());
        userSkillDAO.insert(userSkillDO);
        return toUserSkillVO(userSkillDO);
    }

    @Override
    public void deleteSkill(Long id, Long userId) {
        UserSkillDO oldSkill = userSkillDAO.selectById(id);
        if (oldSkill == null || !oldSkill.getUserId().equals(userId)) {
            throw new BusinessException(NOT_FOUND_CODE, "skill not found");
        }
        userSkillDAO.deleteById(id);
    }

    @Override
    public List<UserSkillVO> listSkills(Long userId) {
        return userSkillDAO.selectByUserId(userId).stream()
            .map(this::toUserSkillVO)
            .toList();
    }

    @Override
    public UserAvailabilityVO saveAvailability(SaveAvailabilityDTO dto) {
        if (dto.getId() != null) {
            UserAvailabilityDO oldAvailability = userAvailabilityDAO.selectById(dto.getId());
            if (oldAvailability == null || !oldAvailability.getUserId().equals(dto.getUserId())) {
                throw new BusinessException(NOT_FOUND_CODE, "availability not found");
            }
            UserAvailabilityDO updateDO = toAvailabilityDO(dto);
            updateDO.setStatus(oldAvailability.getStatus());
            userAvailabilityDAO.update(updateDO);
            return toAvailabilityVO(userAvailabilityDAO.selectById(dto.getId()));
        }
        UserAvailabilityDO availabilityDO = toAvailabilityDO(dto);
        availabilityDO.setStatus(AVAILABLE_STATUS);
        userAvailabilityDAO.insert(availabilityDO);
        return toAvailabilityVO(availabilityDO);
    }

    @Override
    public void deleteAvailability(Long id, Long userId) {
        UserAvailabilityDO oldAvailability = userAvailabilityDAO.selectById(id);
        if (oldAvailability == null || !oldAvailability.getUserId().equals(userId)) {
            throw new BusinessException(NOT_FOUND_CODE, "availability not found");
        }
        userAvailabilityDAO.deleteById(id);
    }

    private UserAvailabilityDO toAvailabilityDO(SaveAvailabilityDTO dto) {
        UserAvailabilityDO availabilityDO = new UserAvailabilityDO();
        availabilityDO.setId(dto.getId());
        availabilityDO.setUserId(dto.getUserId());
        availabilityDO.setAvailableDate(dto.getAvailableDate());
        availabilityDO.setStartTime(dto.getStartTime());
        availabilityDO.setEndTime(dto.getEndTime());
        return availabilityDO;
    }

    @Override
    public List<UserAvailabilityVO> listAvailability(Long userId) {
        return userAvailabilityDAO.selectByUserId(userId).stream()
            .map(this::toAvailabilityVO)
            .toList();
    }

    private UserVO toUserVO(UserDO userDO) {
        UserVO userVO = new UserVO();
        userVO.setId(userDO.getId());
        userVO.setUsername(userDO.getUsername());
        userVO.setRealName(userDO.getRealName());
        userVO.setMobile(userDO.getMobile());
        userVO.setOpenid(userDO.getOpenid());
        userVO.setUserType(userDO.getUserType());
        userVO.setAuthStatus(userDO.getAuthStatus());
        return userVO;
    }

    private AuthSessionVO issueSession(UserVO userVO) {
        JwtTokenService.AuthToken authToken = jwtTokenService.issue(new JwtTokenService.AuthSubject(userVO.getId(),
            userVO.getUsername(), userVO.getUserType()), jwtProperties.getTtl());
        AuthSessionVO sessionVO = new AuthSessionVO();
        sessionVO.setToken(authToken.getToken());
        sessionVO.setExpiresAt(authToken.getExpiresAt());
        sessionVO.setUser(userVO);
        return sessionVO;
    }

    private String resolveWechatUsername(WechatLoginDTO dto) {
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            return dto.getNickname();
        }
        return "wx_" + dto.getOpenid();
    }

    private String resolveWechatMobile(WechatLoginDTO dto) {
        if (dto.getMobile() != null && !dto.getMobile().isBlank()) {
            return dto.getMobile();
        }
        return "WX" + UUID.nameUUIDFromBytes(dto.getOpenid().getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "")
            .substring(0, 18);
    }

    private UserSkillVO toUserSkillVO(UserSkillDO userSkillDO) {
        UserSkillVO userSkillVO = new UserSkillVO();
        userSkillVO.setId(userSkillDO.getId());
        userSkillVO.setUserId(userSkillDO.getUserId());
        userSkillVO.setSkillCode(userSkillDO.getSkillCode());
        userSkillVO.setSkillName(userSkillDO.getSkillName());
        userSkillVO.setSkillLevel(userSkillDO.getSkillLevel());
        return userSkillVO;
    }

    private UserAvailabilityVO toAvailabilityVO(UserAvailabilityDO availabilityDO) {
        UserAvailabilityVO availabilityVO = new UserAvailabilityVO();
        availabilityVO.setId(availabilityDO.getId());
        availabilityVO.setUserId(availabilityDO.getUserId());
        availabilityVO.setAvailableDate(availabilityDO.getAvailableDate());
        availabilityVO.setStartTime(availabilityDO.getStartTime());
        availabilityVO.setEndTime(availabilityDO.getEndTime());
        availabilityVO.setStatus(availabilityDO.getStatus());
        return availabilityVO;
    }
}
