package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.AdminPermissionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminPermissionDAO {

    List<AdminPermissionDO> selectByAdminId(@Param("adminId") Long adminId);

    List<AdminPermissionDO> selectAll();

    List<AdminPermissionDO> selectByRoleId(@Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
