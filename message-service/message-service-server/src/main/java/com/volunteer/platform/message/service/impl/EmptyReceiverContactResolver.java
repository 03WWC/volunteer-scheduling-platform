package com.volunteer.platform.message.service.impl;

public class EmptyReceiverContactResolver implements ReceiverContactResolver {

    @Override
    public ReceiverContact resolve(Long receiverId) {
        return new ReceiverContact(null, null);
    }
}
