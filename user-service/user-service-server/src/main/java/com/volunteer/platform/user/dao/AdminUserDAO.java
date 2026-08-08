package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.AdminUserDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminUserDAO {

    AdminUserDO selectByAccount(@Param("account") String account);

    AdminUserDO selectById(@Param("id") Long id);

    List<AdminUserDO> selectAll();

    int insert(AdminUserDO adminUserDO);

    int update(AdminUserDO adminUserDO);
}
