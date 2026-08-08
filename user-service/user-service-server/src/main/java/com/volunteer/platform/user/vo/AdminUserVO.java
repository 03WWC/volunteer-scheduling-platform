package com.volunteer.platform.user.vo;

import java.util.ArrayList;
import java.util.List;

public class AdminUserVO {

    private Long id;
    private String account;
    private String username;
    private String realName;
    private String mobile;
    private String status;
    private List<AdminRoleVO> roles = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AdminRoleVO> getRoles() {
        return roles;
    }

    public void setRoles(List<AdminRoleVO> roles) {
        this.roles = roles == null ? new ArrayList<>() : roles;
    }
}
