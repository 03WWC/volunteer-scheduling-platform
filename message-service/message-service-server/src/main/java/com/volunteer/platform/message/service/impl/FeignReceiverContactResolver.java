package com.volunteer.platform.message.service.impl;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.user.client.api.UserClient;
import com.volunteer.platform.user.client.dto.UserDTO;
import org.springframework.stereotype.Component;

@Component
public class FeignReceiverContactResolver implements ReceiverContactResolver {

    private final UserClient userClient;

    public FeignReceiverContactResolver(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public ReceiverContact resolve(Long receiverId) {
        Result<UserDTO> result = userClient.getById(receiverId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return new ReceiverContact(null, null);
        }
        UserDTO userDTO = result.getData();
        return new ReceiverContact(userDTO.getOpenid(), userDTO.getMobile());
    }
}
