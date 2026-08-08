package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.AdminRoleDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminRoleDAO {

    List<AdminRoleDO> selectByAdminId(@Param("adminId") Long adminId);

    List<AdminRoleDO> selectAll();

    int deleteUserRoles(@Param("adminId") Long adminId);

    int insertUserRole(@Param("adminId") Long adminId, @Param("roleId") Long roleId);
}
