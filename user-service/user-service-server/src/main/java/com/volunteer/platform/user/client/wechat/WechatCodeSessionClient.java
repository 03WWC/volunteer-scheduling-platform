package com.volunteer.platform.user.client.wechat;

public interface WechatCodeSessionClient {

    WechatCodeSession exchange(String code);
}
