package com.volunteer.platform.user.dto;

import java.util.ArrayList;
import java.util.List;

public class UpdateRolePermissionsDTO {

    private Long roleId;
    private List<Long> permissionIds = new ArrayList<>();

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds == null ? new ArrayList<>() : permissionIds;
    }
}
