package com.volunteer.platform.user.service;

import com.volunteer.platform.user.dto.AdminLoginDTO;
import com.volunteer.platform.user.dto.RealNameAuthDTO;
import com.volunteer.platform.user.dto.RegisterUserDTO;
import com.volunteer.platform.user.dto.SaveAvailabilityDTO;
import com.volunteer.platform.user.dto.SaveUserSkillDTO;
import com.volunteer.platform.user.dto.WechatLoginDTO;
import com.volunteer.platform.user.query.UserQuery;
import com.volunteer.platform.user.vo.AuthSessionVO;
import com.volunteer.platform.user.vo.UserAvailabilityVO;
import com.volunteer.platform.user.vo.UserSkillVO;
import com.volunteer.platform.user.vo.UserVO;

import java.util.List;

public interface UserService {

    UserVO register(RegisterUserDTO dto);

    AuthSessionVO adminLogin(AdminLoginDTO dto);

    AuthSessionVO wechatLogin(WechatLoginDTO dto);

    UserVO getById(Long id);

    UserVO authenticate(RealNameAuthDTO dto);

    List<UserVO> listVolunteers(UserQuery query);

    UserSkillVO saveSkill(SaveUserSkillDTO dto);

    void deleteSkill(Long id, Long userId);

    List<UserSkillVO> listSkills(Long userId);

    UserAvailabilityVO saveAvailability(SaveAvailabilityDTO dto);

    void deleteAvailability(Long id, Long userId);

    List<UserAvailabilityVO> listAvailability(Long userId);
}
