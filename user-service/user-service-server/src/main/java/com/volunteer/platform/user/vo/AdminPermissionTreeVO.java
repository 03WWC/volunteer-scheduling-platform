package com.volunteer.platform.user.vo;

import java.util.ArrayList;
import java.util.List;

public class AdminPermissionTreeVO {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String parentCode;
    private String permissionType;
    private Integer sortNo;
    private List<AdminPermissionTreeVO> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public List<AdminPermissionTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<AdminPermissionTreeVO> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }
}
