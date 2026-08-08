package com.volunteer.platform.user.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.user.client.dto.UserAvailabilityDTO;
import com.volunteer.platform.user.client.dto.UserDTO;
import com.volunteer.platform.user.client.dto.UserSkillDTO;
import com.volunteer.platform.user.dto.RealNameAuthDTO;
import com.volunteer.platform.user.dto.RegisterUserDTO;
import com.volunteer.platform.user.dto.SaveAvailabilityDTO;
import com.volunteer.platform.user.dto.SaveUserSkillDTO;
import com.volunteer.platform.user.dto.WechatLoginDTO;
import com.volunteer.platform.user.query.UserQuery;
import com.volunteer.platform.user.service.UserService;
import com.volunteer.platform.user.vo.AuthSessionVO;
import com.volunteer.platform.user.vo.UserAvailabilityVO;
import com.volunteer.platform.user.vo.UserSkillVO;
import com.volunteer.platform.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterUserDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/wechat/login")
    public Result<AuthSessionVO> wechatLogin(@RequestBody WechatLoginDTO dto) {
        return Result.success(userService.wechatLogin(dto));
    }

    @PutMapping("/auth/real-name")
    public Result<UserVO> authenticate(@RequestBody RealNameAuthDTO dto) {
        return Result.success(userService.authenticate(dto));
    }

    @GetMapping("/list/volunteers")
    public Result<List<UserDTO>> listVolunteers(UserQuery query) {
        return Result.success(userService.listVolunteers(query).stream()
            .map(this::toUserDTO)
            .toList());
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable("id") Long id) {
        return Result.success(toUserDTO(userService.getById(id)));
    }

    @PostMapping("/skill/save")
    public Result<UserSkillVO> saveSkill(@RequestBody SaveUserSkillDTO dto) {
        return Result.success(userService.saveSkill(dto));
    }

    @PostMapping("/skill/{id}/delete")
    public Result<Void> deleteSkill(@PathVariable("id") Long id, @RequestBody Map<String, Long> payload) {
        userService.deleteSkill(id, payload.get("userId"));
        return Result.success();
    }

    @GetMapping("/{id}/skills")
    public Result<List<UserSkillDTO>> listSkills(@PathVariable("id") Long id) {
        return Result.success(userService.listSkills(id).stream()
            .map(this::toUserSkillDTO)
            .toList());
    }

    @PostMapping("/availability/save")
    public Result<UserAvailabilityVO> saveAvailability(@RequestBody SaveAvailabilityDTO dto) {
        return Result.success(userService.saveAvailability(dto));
    }

    @PostMapping("/availability/{id}/delete")
    public Result<Void> deleteAvailability(@PathVariable("id") Long id, @RequestBody Map<String, Long> payload) {
        userService.deleteAvailability(id, payload.get("userId"));
        return Result.success();
    }

    @GetMapping("/{id}/availability")
    public Result<List<UserAvailabilityDTO>> listAvailability(@PathVariable("id") Long id) {
        return Result.success(userService.listAvailability(id).stream()
            .map(this::toUserAvailabilityDTO)
            .toList());
    }

    private UserDTO toUserDTO(UserVO userVO) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userVO.getId());
        userDTO.setUsername(userVO.getUsername());
        userDTO.setRealName(userVO.getRealName());
        userDTO.setMobile(userVO.getMobile());
        userDTO.setOpenid(userVO.getOpenid());
        userDTO.setUserType(userVO.getUserType());
        userDTO.setAuthStatus(userVO.getAuthStatus());
        return userDTO;
    }

    private UserSkillDTO toUserSkillDTO(UserSkillVO userSkillVO) {
        UserSkillDTO userSkillDTO = new UserSkillDTO();
        userSkillDTO.setId(userSkillVO.getId());
        userSkillDTO.setUserId(userSkillVO.getUserId());
        userSkillDTO.setSkillCode(userSkillVO.getSkillCode());
        userSkillDTO.setSkillName(userSkillVO.getSkillName());
        userSkillDTO.setSkillLevel(userSkillVO.getSkillLevel());
        return userSkillDTO;
    }

    private UserAvailabilityDTO toUserAvailabilityDTO(UserAvailabilityVO availabilityVO) {
        UserAvailabilityDTO availabilityDTO = new UserAvailabilityDTO();
        availabilityDTO.setId(availabilityVO.getId());
        availabilityDTO.setUserId(availabilityVO.getUserId());
        availabilityDTO.setAvailableDate(availabilityVO.getAvailableDate());
        availabilityDTO.setStartTime(availabilityVO.getStartTime());
        availabilityDTO.setEndTime(availabilityVO.getEndTime());
        availabilityDTO.setStatus(availabilityVO.getStatus());
        return availabilityDTO;
    }
}
