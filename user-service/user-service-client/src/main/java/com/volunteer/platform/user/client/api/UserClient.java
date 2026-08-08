package com.volunteer.platform.user.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.user.client.dto.UserAvailabilityDTO;
import com.volunteer.platform.user.client.dto.UserDTO;
import com.volunteer.platform.user.client.dto.UserSkillDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/user/list/volunteers")
    Result<List<UserDTO>> listVolunteers();

    @GetMapping("/user/{id}")
    Result<UserDTO> getById(@PathVariable("id") Long id);

    @GetMapping("/user/{id}/skills")
    Result<List<UserSkillDTO>> listSkills(@PathVariable("id") Long id);

    @GetMapping("/user/{id}/availability")
    Result<List<UserAvailabilityDTO>> listAvailability(@PathVariable("id") Long id);
}
